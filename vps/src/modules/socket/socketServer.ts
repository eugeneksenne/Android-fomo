import { Server as SocketIOServer, Socket } from 'socket.io';
import { Server as HttpServer } from 'http';
import { FastifyInstance } from 'fastify';
import { env } from '../../config/env.js';
import { logger } from '../../core/logger/logger.js';
import { socketAuthMiddleware } from '../../core/middleware/auth.js';
import { socketRateLimitMiddleware, cleanupRateLimiters } from '../../core/middleware/rateLimit.js';
import { setupRedisAdapter } from '../../core/redis/adapter.js';
import { connectionManager } from './connectionManager.js';
import { socketLifecycleManager, SocketLifecycleState } from './lifecycle.js';
import { sessionService } from '../auth/session.service.js';
import { deviceValidatorService } from '../auth/deviceValidator.service.js';
import { ReplayCache } from '../../shared/utils/correlation.js';
import { generateCorrelationId, generateShortId } from '../../shared/utils/id.js';
import { DeviceType } from '../../shared/types/enums.js';
import { SocketSession } from '../../shared/types/socket.js';
import { metricsCollector } from '../../core/monitoring/metrics.js';

import { presenceHandlers } from '../presence/handlers.js';
import { callHandlers } from '../calls/handlers.js';
import { friendsHandlers } from '../friends/handlers.js';
import { groupHandlers } from '../groups/handlers.js';
import { clubLobbyHandlers } from '../club-lobby/handlers.js';
import { nightguardHandlers } from '../nightguard/handlers.js';
import { liveLocationHandlers } from '../live-location/handlers.js';

import { EVENT_VERSION } from '../../shared/types/events.js';

const replayCache = new ReplayCache(env.REPLAY_CACHE_SIZE, env.REPLAY_WINDOW_MS);

export async function createSocketServer(httpServer: HttpServer, app: FastifyInstance): Promise<SocketIOServer> {
  const io = new SocketIOServer(httpServer, {
    cors: {
      origin: env.ALLOWED_ORIGINS === '*' ? true : env.ALLOWED_ORIGINS.split(',').map(o => o.trim()),
      credentials: env.CORS_CREDENTIALS,
      methods: ['GET', 'POST'],
    },
    pingInterval: env.SOCKET_PING_INTERVAL_MS,
    pingTimeout: env.SOCKET_PING_TIMEOUT_MS,
    maxHttpBufferSize: env.SOCKET_MAX_HTTP_BUFFER_SIZE,
    connectTimeout: env.SOCKET_CONNECT_TIMEOUT_MS,
    transports: ['websocket', 'polling'],
    connectionStateRecovery: env.SOCKET_CONNECTION_STATE_RECOVERY ? {
      maxDisconnectionDuration: env.SOCKET_RECOVERY_MAX_DISCONNECT_MS,
      skipMiddlewares: false,
    } : undefined,
    allowRequest: (req, callback) => {
      // Origin validation
      const origin = req.headers.origin as string | undefined;
      if (env.ALLOWED_ORIGINS === '*') return callback(null, true);
      const allowed = env.ALLOWED_ORIGINS.split(',').map(o => o.trim());
      if (!origin || allowed.includes(origin) || allowed.includes('*')) {
        return callback(null, true);
      }
      logger.warn({ origin, allowed }, 'Origin validation failed');
      return callback(null, false);
    },
  });

  // Redis adapter for horizontal scaling
  await setupRedisAdapter(io);

  // Global middlewares - auth first
  io.use(socketAuthMiddleware);
  io.use(socketRateLimitMiddleware);

  connectionManager.start();

  io.on('connection', async (socket: Socket) => {
    const userData = (socket.data as any).user as any;
    const deviceData = (socket.data as any).device as any;
    const sessionId = (socket.data as any).sessionId as string;
    const correlationId = (socket.data as any).correlationId || generateCorrelationId();

    const userId = userData?.id || userData?.sub;
    if (!userId) {
      logger.warn({ socketId: socket.id }, 'Connection without userId after auth');
      socket.disconnect(true);
      return;
    }

    const deviceId = deviceData?.deviceId || `temp_${socket.id.slice(0, 8)}`;
    const deviceType = deviceData?.deviceType || DeviceType.UNKNOWN;

    // Duplicate detection
    if (connectionManager.hasDuplicateConnection(userId, deviceId, socket.id)) {
      logger.info({ userId, deviceId, socketId: socket.id }, 'Duplicate connection detected - allowing, will route to primary');
    }

    const now = Date.now();
    const session: SocketSession = {
      userId,
      deviceId,
      device: {
        deviceId,
        deviceType,
        priority: deviceData?.priority || 10,
        appVersion: deviceData?.appVersion,
        ip: socket.handshake.address as string,
        userAgent: socket.handshake.headers['user-agent']?.slice(0, 200),
      } as any,
      socketId: socket.id,
      connectedAt: now,
      lastHeartbeatAt: now,
      lastActivityAt: now,
      connectionQuality: 'EXCELLENT' as any,
      reconnectAttempts: 0,
      sessionId,
      correlationId,
      authUser: userData,
    };

    // Create session record
    await sessionService.createSession({
      sessionId,
      userId,
      deviceId,
      deviceType,
      socketIds: [socket.id],
      createdAt: session.connectedAt,
      lastActiveAt: session.lastActivityAt,
      ip: session.device.ip,
      isRevoked: false,
      correlationId,
    }).catch(err => logger.warn({ err: err.message }, 'Failed to create session record'));

    // Connection manager
    connectionManager.addConnection(socket, session);
    socketLifecycleManager.createContext(socket, session);

    logger.info({
      event: 'connection',
      userId,
      socketId: socket.id,
      deviceId,
      ip: socket.handshake.address,
      ua: socket.handshake.headers['user-agent']?.slice(0, 100),
    }, `Socket connected ${userId} ${socket.id}`);

    // Emit connected ack with version & recovery info
    socket.emit('connected', {
      socketId: socket.id,
      userId,
      sessionId,
      correlationId,
      serverTime: Date.now(),
      timestamp: new Date().toISOString(),
      version: EVENT_VERSION,
      onlineCount: connectionManager.getOnlineCount(),
      recoveryEnabled: env.SOCKET_CONNECTION_STATE_RECOVERY,
    });

    // Broadcast presence online (distributed via Redis)
    socket.broadcast.emit('user:online', {
      userId,
      status: 'ONLINE',
      lastSeen: new Date().toISOString(),
      timestamp: new Date().toISOString(),
      correlationId: generateCorrelationId(),
    });

    // Register domain handlers
    try {
      presenceHandlers(io, socket);
      callHandlers(io, socket);
      friendsHandlers(io, socket);
      groupHandlers(io, socket);
      clubLobbyHandlers(io, socket);
      nightguardHandlers(io, socket);
      liveLocationHandlers(io, socket);

      // Generic heartbeat handler
      socket.on('heartbeat', async (payload: any, ack?: (res: any) => void) => {
        const start = Date.now();
        if (payload?.correlationId && env.SESSION_REPLAY_PROTECTION) {
          if (replayCache.checkAndAdd(payload.correlationId)) {
            return ack?.({ success: false, error: { code: 'REPLAY_DETECTED', correlationId: payload.correlationId } });
          }
        }

        socketLifecycleManager.handleHeartbeat(socket);
        connectionManager.updateHeartbeat(socket.id);

        const latencyMs = payload?.timestamp ? Date.now() - new Date(payload.timestamp).getTime() : undefined;
        if (latencyMs) metricsCollector.recordLatency(latencyMs);

        const res = {
          timestamp: new Date().toISOString(),
          serverTime: Date.now(),
          latencyMs,
          correlationId: payload?.correlationId || generateCorrelationId(),
        };

        socket.emit('heartbeat:ack', res);
        if (typeof ack === 'function') ack({ success: true, data: res });
      });

      // Reconnect attempt handler
      socket.on('reconnect:attempt', async (payload: any, ack?: (res: any) => void) => {
        const { sessionId: reqSessionId, lastEventId, deviceId: reqDeviceId } = payload || {};
        const success = await socketLifecycleManager.handleReconnect(socket, reqSessionId || sessionId, lastEventId);
        if (typeof ack === 'function') {
          ack({ success, sessionId: reqSessionId, restored: success });
        }
        if (success) {
          socket.emit('session:restored', { sessionId: reqSessionId, timestamp: new Date().toISOString(), correlationId: generateCorrelationId() });
        }
      });

      // Network switch handler
      socket.on('network:switch', (payload: any) => {
        socketLifecycleManager.handleNetworkSwitch(socket, payload?.ip || socket.handshake.address as string);
      });

      // Background / foreground
      socket.on('app:state', (payload: any) => {
        const isBackground = payload?.state === 'BACKGROUND';
        socketLifecycleManager.handleBackground(socket, isBackground);
      });

    } catch (err) {
      logger.error({ err: (err as Error).message, userId, socketId: socket.id }, 'Failed to register handlers');
    }

    // Disconnect handler
    socket.on('disconnect', async (reason: string) => {
      const rec = connectionManager.getConnection(socket.id);
      const duration = rec ? Date.now() - rec.connectedAt : 0;

      logger.info({ userId, socketId: socket.id, reason, durationMs: duration, correlationId }, 'Socket disconnect');

      await socketLifecycleManager.handleDisconnect(socket, reason);
      cleanupRateLimiters(socket.id, userId);

      // Remove presence if fully offline
      const remaining = connectionManager.getUserConnectionCount(userId);
      if (remaining === 0) {
        // Delay offline broadcast for grace period (user might be switching network)
        setTimeout(() => {
          if (connectionManager.getUserConnectionCount(userId) === 0) {
            io.emit('user:offline', {
              userId,
              status: 'OFFLINE',
              lastSeen: new Date().toISOString(),
              timestamp: new Date().toISOString(),
              correlationId: generateCorrelationId(),
            });
          }
        }, env.PRESENCE_OFFLINE_TIMEOUT_MS);
      }

      deviceValidatorService.removeDevice(userId, deviceId);
    });

    // Error handler
    socket.on('error', (err: any) => {
      logger.warn({ socketId: socket.id, userId, error: err?.message || err }, 'Socket error');
      metricsCollector.recordError('SOCKET_ERROR');
    });
  });

  logger.info({
    pingInterval: env.SOCKET_PING_INTERVAL_MS,
    pingTimeout: env.SOCKET_PING_TIMEOUT_MS,
    recovery: env.SOCKET_CONNECTION_STATE_RECOVERY,
    origins: env.ALLOWED_ORIGINS,
  }, 'Socket.IO server initialized');

  return io;
}
