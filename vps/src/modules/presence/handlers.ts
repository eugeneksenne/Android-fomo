import { Server as SocketIOServer, Socket } from 'socket.io';
import { z } from 'zod';
import { presenceEngine } from './presenceEngine.js';
import { presenceThrottler } from './presenceThrottler.js';
import { validatePayload } from '../../core/middleware/validation.js';
import { logger } from '../../core/logger/logger.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { PresenceStatus } from '../../shared/types/enums.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { metricsCollector } from '../../core/monitoring/metrics.js';
import { auditLog } from '../../core/middleware/audit.js';
import { connectionManager } from '../socket/connectionManager.js';

const presenceUpdateSchema = z.object({
  status: z.nativeEnum(PresenceStatus),
  customMessage: z.string().max(200).optional(),
  venueId: z.string().max(100).optional(),
  correlationId: z.string().optional(),
  version: z.string().optional(),
  timestamp: z.string().optional(),
  deviceId: z.string().optional(),
});

const presenceGetSchema = z.object({
  userIds: z.array(z.string().min(3).max(128)).max(200).optional(),
  limit: z.number().min(1).max(200).optional(),
  correlationId: z.string().optional(),
});

const typingSchema = z.object({
  roomId: z.string().max(128).optional(),
  chatId: z.string().max(128).optional(),
  lobbyId: z.string().max(128).optional(),
  isTyping: z.boolean(),
  correlationId: z.string().optional(),
});

export function presenceHandlers(io: SocketIOServer, socket: Socket): void {
  const userId = (socket.data as any).user?.id as string;
  const displayName = (socket.data as any).user?.user_metadata?.display_name || (socket.data as any).user?.user_metadata?.displayName;
  const photoUrl = (socket.data as any).user?.user_metadata?.avatar_url;

  // Initial presence set to ONLINE on connect
  presenceEngine.updatePresence({
    userId,
    status: PresenceStatus.ONLINE,
    displayName,
    photoUrl,
    deviceId: (socket.data as any).device?.deviceId,
  }).catch(err => logger.warn({ err: err.message }, 'Initial presence update failed'));

  socket.on('presence:update', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    const start = Date.now();
    try {
      const data = validatePayload(presenceUpdateSchema, payload, correlationId);

      // Throttle check with scheduler for battery efficiency
      const wasThrottled = !presenceThrottler.canUpdate(userId, data.status);
      if (wasThrottled) {
        presenceThrottler.scheduleUpdate(userId, data.status, async () => {
          const presence = await presenceEngine.updatePresence({
            userId,
            status: data.status,
            customMessage: data.customMessage,
            venueId: data.venueId,
            displayName,
            photoUrl,
            deviceId: data.deviceId || (socket.data as any).device?.deviceId,
          });

          io.emit('presence:update', {
            userId,
            status: presence.status,
            lastSeen: presence.lastSeen,
            customMessage: presence.customMessage,
            venueId: presence.venueId,
            displayName: presence.displayName,
            photoUrl: presence.photoUrl,
            timestamp: new Date().toISOString(),
            correlationId,
          });
        });

        if (typeof ack === 'function') {
          ack({
            success: true,
            data: { throttled: true, scheduled: true },
            correlationId,
            timestamp: new Date().toISOString(),
          });
        }
        return;
      }

      const presence = await presenceEngine.updatePresence({
        userId,
        status: data.status,
        customMessage: data.customMessage,
        venueId: data.venueId,
        displayName,
        photoUrl,
        deviceId: data.deviceId || (socket.data as any).device?.deviceId,
      });

      metricsCollector.recordLatency(Date.now() - start);

      // Broadcast to all (in production, you might broadcast only to friends/presence rooms)
      io.emit('presence:update', {
        userId,
        status: presence.status,
        lastSeen: presence.lastSeen,
        customMessage: presence.customMessage,
        venueId: presence.venueId,
        displayName: presence.displayName,
        photoUrl: presence.photoUrl,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      // Also specific user:online/offline events for compatibility
      if (presence.status === PresenceStatus.OFFLINE) {
        io.emit('user:offline', {
          userId,
          status: presence.status,
          lastSeen: presence.lastSeen,
          timestamp: new Date().toISOString(),
          correlationId,
        });
      } else if (data.status === PresenceStatus.ONLINE) {
        io.emit('user:online', {
          userId,
          status: presence.status,
          lastSeen: presence.lastSeen,
          displayName: presence.displayName,
          photoUrl: presence.photoUrl,
          timestamp: new Date().toISOString(),
          correlationId,
        });
      }

      auditLog(socket, 'presence:update', payload, 'success');

      if (typeof ack === 'function') {
        ack({
          success: true,
          data: presence,
          correlationId,
          timestamp: new Date().toISOString(),
        });
      }
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({
        code: ErrorCodes.PRESENCE_INVALID_STATUS,
        correlationId,
        developerMessage: (err as Error).message,
      });
      socket.emit('error', appErr.toJSON());
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  socket.on('presence:get', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(presenceGetSchema, payload || {}, correlationId);
      let presences: any[];

      if (data.userIds && data.userIds.length > 0) {
        presences = await presenceEngine.getManyPresence(data.userIds);
      } else {
        presences = await presenceEngine.getOnlineUsers(data.limit || 50);
      }

      const response = {
        users: presences,
        timestamp: new Date().toISOString(),
        correlationId,
      };

      if (typeof ack === 'function') {
        ack({ success: true, data: response, correlationId, timestamp: new Date().toISOString() });
      } else {
        socket.emit('presence:list', response);
      }
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({
        code: ErrorCodes.INTERNAL_ERROR,
        correlationId,
        developerMessage: (err as Error).message,
      });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  // Typing indicators - throttled per room
  const typingThrottle = new Map<string, number>();

  socket.on('typing', (payload: any) => {
    try {
      const data = validatePayload(typingSchema, payload, generateCorrelationId());
      const key = `${userId}:${data.roomId || data.chatId || data.lobbyId || 'global'}`;
      const last = typingThrottle.get(key);
      const now = Date.now();
      if (last && now - last < 1500) return; // throttle typing events
      typingThrottle.set(key, now);

      const eventPayload = {
        userId,
        roomId: data.roomId,
        chatId: data.chatId,
        lobbyId: data.lobbyId,
        isTyping: data.isTyping,
        displayName,
        timestamp: new Date().toISOString(),
        correlationId: data.correlationId || generateCorrelationId(),
      };

      // Emit to room or broadcast
      if (data.roomId) {
        socket.to(data.roomId).emit('user:typing', eventPayload);
      } else if (data.chatId) {
        socket.to(`chat_${data.chatId}`).emit('chat:typing', eventPayload);
        socket.to(`chat_${data.chatId}`).emit('user:typing', eventPayload);
      } else if (data.lobbyId) {
        socket.to(`lobby_${data.lobbyId}`).emit('user:typing', eventPayload);
      } else {
        socket.broadcast.emit('user:typing', eventPayload);
      }

      // Auto-clear typing after 5s
      if (data.isTyping) {
        setTimeout(() => {
          socket.to(data.roomId || '').emit('user:typing', { ...eventPayload, isTyping: false });
        }, 5000);
      }
    } catch {}
  });

  socket.on('chat:typing', (payload: any) => {
    // Compatibility wrapper
    const wrapped = { chatId: payload?.chatId, isTyping: payload?.isTyping, correlationId: payload?.correlationId };
    (socket as any).emit('typing', wrapped);
  });
}
