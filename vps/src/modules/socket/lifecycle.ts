import { Socket } from 'socket.io';
import { logger } from '../../core/logger/logger.js';
import { connectionManager } from './connectionManager.js';
import { SocketSession } from '../../shared/types/socket.js';
import { ConnectionQuality } from '../../shared/types/enums.js';
import { env } from '../../config/env.js';
import { sessionService } from '../auth/session.service.js';
import { deviceValidatorService } from '../auth/deviceValidator.service.js';
import { metricsCollector } from '../../core/monitoring/metrics.js';
import { nowMs } from '../../shared/utils/time.js';

export enum SocketLifecycleState {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  AUTHENTICATED = 'AUTHENTICATED',
  RECONNECTING = 'RECONNECTING',
  DISCONNECTED = 'DISCONNECTED',
  BACKGROUND = 'BACKGROUND',
  NETWORK_SWITCHING = 'NETWORK_SWITCHING',
}

interface LifecycleContext {
  socket: Socket;
  session: SocketSession;
  state: SocketLifecycleState;
  connectedAt: number;
  lastStateChange: number;
}

class SocketLifecycleManager {
  private contexts = new Map<string, LifecycleContext>();

  createContext(socket: Socket, session: SocketSession): LifecycleContext {
    const ctx: LifecycleContext = {
      socket,
      session,
      state: SocketLifecycleState.AUTHENTICATED,
      connectedAt: nowMs(),
      lastStateChange: nowMs(),
    };
    this.contexts.set(socket.id, ctx);
    return ctx;
  }

  getContext(socketId: string): LifecycleContext | null {
    return this.contexts.get(socketId) || null;
  }

  setState(socketId: string, state: SocketLifecycleState): void {
    const ctx = this.contexts.get(socketId);
    if (!ctx) return;
    const prev = ctx.state;
    ctx.state = state;
    ctx.lastStateChange = nowMs();
    logger.debug({ socketId, prevState: prev, newState: state }, 'Lifecycle state change');
  }

  async handleDisconnect(socket: Socket, reason: string): Promise<void> {
    const ctx = this.contexts.get(socket.id);
    if (!ctx) {
      connectionManager.removeConnection(socket.id);
      return;
    }

    const duration = nowMs() - ctx.connectedAt;
    logger.info({ socketId: socket.id, userId: ctx.session.userId, reason, durationMs: duration }, 'Socket disconnect');

    // Store for session recovery
    if (env.SOCKET_CONNECTION_STATE_RECOVERY) {
      // Keep session active for recovery window
      logger.debug({ sessionId: ctx.session.sessionId, recoveryWindow: env.SOCKET_RECOVERY_MAX_DISCONNECT_MS }, 'Session kept for recovery');
    }

    // Remove from connection manager
    connectionManager.removeConnection(socket.id);

    // Update session service
    await sessionService.removeSocketFromSession(ctx.session.sessionId, socket.id).catch(err => {
      logger.warn({ err: err.message, sessionId: ctx.session.sessionId }, 'Failed to remove socket from session');
    });

    // Track reconnect attempts
    if (reason === 'transport close' || reason === 'ping timeout') {
      metricsCollector.recordReconnect(ctx.session.reconnectAttempts);
    }

    this.contexts.delete(socket.id);
    this.setState(socket.id, SocketLifecycleState.DISCONNECTED);
  }

  async handleReconnect(socket: Socket, sessionId: string, lastEventId?: string): Promise<boolean> {
    const existingSession = await sessionService.getSession(sessionId);
    if (!existingSession) {
      logger.warn({ sessionId, socketId: socket.id }, 'Reconnect failed - session not found');
      return false;
    }

    if (existingSession.isRevoked) {
      logger.warn({ sessionId, userId: existingSession.userId }, 'Reconnect failed - session revoked');
      return false;
    }

    // Check recovery window
    if (nowMs() - existingSession.lastActiveAt > env.SOCKET_RECOVERY_MAX_DISCONNECT_MS) {
      logger.warn({ sessionId, lastActiveAt: existingSession.lastActiveAt }, 'Reconnect failed - recovery window expired');
      return false;
    }

    // Restore session
    const session: SocketSession = {
      userId: existingSession.userId,
      deviceId: existingSession.deviceId,
      device: {
        deviceId: existingSession.deviceId,
        deviceType: existingSession.deviceType,
        priority: deviceValidatorService.getDevicePriority(existingSession.userId, existingSession.deviceId),
        ip: existingSession.ip,
      } as any,
      socketId: socket.id,
      connectedAt: existingSession.createdAt,
      lastHeartbeatAt: nowMs(),
      lastActivityAt: nowMs(),
      connectionQuality: ConnectionQuality.GOOD,
      reconnectAttempts: (existingSession as any).reconnectAttempts ? (existingSession as any).reconnectAttempts + 1 : 1,
      sessionId: existingSession.sessionId,
      correlationId: existingSession.correlationId,
      authUser: (socket.data as any).user,
    };

    this.createContext(socket, session);
    connectionManager.addConnection(socket, session);
    await sessionService.addSocketToSession(sessionId, socket.id);

    logger.info({ sessionId, userId: session.userId, socketId: socket.id, lastEventId }, 'Session restored via reconnect');

    // Here you would replay missed events using lastEventId if you implement event store
    // For now, we just signal restored

    return true;
  }

  handleHeartbeat(socket: Socket, latencyMs?: number): void {
    const ctx = this.contexts.get(socket.id);
    if (!ctx) return;

    connectionManager.updateHeartbeat(socket.id);
    ctx.session.lastHeartbeatAt = nowMs();
    ctx.session.lastActivityAt = nowMs();

    sessionService.updateActivity(ctx.session.sessionId).catch(() => {});

    if (latencyMs && latencyMs > 100) {
      logger.debug({ socketId: socket.id, latencyMs }, 'High heartbeat latency');
    }
  }

  handleNetworkSwitch(socket: Socket, newIp?: string): void {
    const ctx = this.contexts.get(socket.id);
    if (!ctx) return;
    this.setState(socket.id, SocketLifecycleState.NETWORK_SWITCHING);
    logger.info({ socketId: socket.id, userId: ctx.session.userId, newIp, oldIp: (socket.handshake.address as string) }, 'Network switching detected');
    // Update session IP
    ctx.session.device.ip = newIp;
    setTimeout(() => this.setState(socket.id, SocketLifecycleState.AUTHENTICATED), 1000);
  }

  handleBackground(socket: Socket, isBackground: boolean): void {
    const socketId = socket.id;
    if (isBackground) this.setState(socketId, SocketLifecycleState.BACKGROUND);
    else this.setState(socketId, SocketLifecycleState.AUTHENTICATED);
  }

  getActiveCount(): number {
    return this.contexts.size;
  }
}

export const socketLifecycleManager = new SocketLifecycleManager();
