import { Server as SocketIOServer, Socket } from 'socket.io';
import { liveLocationEngine } from './liveLocationEngine.js';
import { validatePayload } from '../../core/middleware/validation.js';
import { liveLocationStartSchema, liveLocationUpdateSchema, liveLocationActionSchema } from './validators.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { LiveLocationEventType } from '../../shared/types/enums.js';
import { AppError } from '../../shared/errors/appError.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { logger } from '../../core/logger/logger.js';
import { connectionManager } from '../socket/connectionManager.js';

export function liveLocationHandlers(io: SocketIOServer, socket: Socket): void {
  const userId = (socket.data as any).user?.id as string;
  const displayName = (socket.data as any).user?.user_metadata?.display_name as string | undefined;

  socket.on('live-location:start', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(liveLocationStartSchema, payload, correlationId);

      // If sessionId provided, treat as resume? But start should create new
      if (data.sessionId) {
        try {
          const resumed = liveLocationEngine.resumeSession(data.sessionId, userId);
          await socket.join(resumed.id);
          io.to(resumed.id).emit('live-location:started', {
            sessionId: resumed.id,
            userId,
            eventType: LiveLocationEventType.RESUME,
            location: resumed.lastLocation!,
            timestamp: new Date().toISOString(),
            correlationId,
          });
          if (typeof ack === 'function') ack({ success: true, data: { sessionId: resumed.id }, correlationId, timestamp: new Date().toISOString() });
          return;
        } catch {}
      }

      const session = liveLocationEngine.createSession({
        ownerId: userId,
        trustedContactIds: data.trustedContactIds,
        tripName: data.tripName,
        destination: data.destination as any,
        metadata: data.metadata,
      });

      await socket.join(session.id);

      // Invite trusted
      if (data.trustedContactIds) {
        for (const tid of data.trustedContactIds) {
          const conns = connectionManager.getUserConnections(tid);
          conns.forEach(conn => {
            io.to(conn.socketId).emit('live-location:started', {
              sessionId: session.id,
              userId,
              eventType: LiveLocationEventType.START,
              location: session.lastLocation || { latitude: 0, longitude: 0, timestamp: new Date().toISOString() },
              timestamp: new Date().toISOString(),
              correlationId,
            });
          });
        }
      }

      if (typeof ack === 'function') ack({ success: true, data: { sessionId: session.id }, correlationId, timestamp: new Date().toISOString() });

      io.to(session.id).emit('live-location:started', {
        sessionId: session.id,
        userId,
        eventType: LiveLocationEventType.START,
        location: session.lastLocation || { latitude: 0, longitude: 0, timestamp: new Date().toISOString() } as any,
        timestamp: new Date().toISOString(),
        correlationId,
      });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.INTERNAL_ERROR, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  socket.on('live-location:update', (payload: any) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(liveLocationUpdateSchema, payload, correlationId);
      const session = liveLocationEngine.updateLocation(data.sessionId, userId, data.location as any, data.eventType, {
        battery: data.battery,
        etaSeconds: data.etaSeconds,
        accuracy: data.accuracy,
        isMoving: data.isMoving,
      });

      io.to(data.sessionId).emit('live-location:update', {
        sessionId: data.sessionId,
        userId,
        eventType: data.eventType,
        location: data.location,
        battery: data.battery,
        etaSeconds: data.etaSeconds,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      // Also emit to trusted contacts individually if they are not in room but online
      session.trustedContactIds.forEach(tid => {
        if (tid === userId) return;
        const conns = connectionManager.getUserConnections(tid);
        conns.forEach(conn => {
          io.to(conn.socketId).emit('live-location:update', {
            sessionId: data.sessionId,
            userId,
            eventType: data.eventType,
            location: data.location,
            battery: data.battery,
            etaSeconds: data.etaSeconds,
            timestamp: new Date().toISOString(),
            correlationId,
          });
        });
      });
    } catch (err) {
      logger.warn({ err: (err as Error).message, userId, sessionId: payload?.sessionId }, 'live-location:update failed');
    }
  });

  socket.on('live-location:action', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(liveLocationActionSchema, payload, correlationId);
      let session;
      if (data.action === 'PAUSE') session = liveLocationEngine.pauseSession(data.sessionId, userId);
      else if (data.action === 'RESUME') session = liveLocationEngine.resumeSession(data.sessionId, userId);
      else if (data.action === 'STOP') session = liveLocationEngine.endSession(data.sessionId, userId, data.reason);

      if (!session) throw new AppError({ code: ErrorCodes.LIVE_LOCATION_SESSION_NOT_FOUND, correlationId });

      const eventType = data.action === 'PAUSE' ? LiveLocationEventType.PAUSE : data.action === 'RESUME' ? LiveLocationEventType.RESUME : LiveLocationEventType.SESSION_END;

      io.to(data.sessionId).emit(eventType === LiveLocationEventType.SESSION_END ? 'live-location:ended' : 'live-location:update', {
        sessionId: data.sessionId,
        userId,
        eventType,
        location: session.lastLocation || { latitude: 0, longitude: 0, timestamp: new Date().toISOString() } as any,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (eventType === LiveLocationEventType.SESSION_END) {
        const sockets = await io.in(data.sessionId).fetchSockets();
        for (const s of sockets) s.leave(data.sessionId);
      }

      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.INTERNAL_ERROR, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });
}
