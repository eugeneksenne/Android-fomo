import { Server as SocketIOServer, Socket } from 'socket.io';
import { nightguardEngine } from './nightguardEngine.js';
import { validatePayload } from '../../core/middleware/validation.js';
import { nightguardCreateSchema, nightguardJoinSchema, nightguardLocationSchema, nightguardStatusSchema, nightguardSosSchema } from './validators.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { NightGuardEventType, ParticipantRole } from '../../shared/types/enums.js';
import { AppError } from '../../shared/errors/appError.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { notificationEngine } from '../notifications/notificationEngine.js';
import { NotificationTrigger } from '../../shared/types/enums.js';
import { logger } from '../../core/logger/logger.js';
import { connectionManager } from '../socket/connectionManager.js';

export function nightguardHandlers(io: SocketIOServer, socket: Socket): void {
  const userId = (socket.data as any).user?.id as string;
  const displayName = (socket.data as any).user?.user_metadata?.display_name as string | undefined;

  socket.on('nightguard:create', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(nightguardCreateSchema, payload, correlationId);
      const session = nightguardEngine.createSession({
        type: data.type,
        createdBy: userId,
        trustedContactIds: data.trustedContactIds,
        destination: data.destination as any,
        metadata: data.metadata,
        durationMinutes: data.durationMinutes,
      });

      await socket.join(session.id);
      const participant = session.participants.get(userId);
      if (participant) participant.socketId = socket.id;

      // Invite trusted contacts if online
      for (const contactId of data.trustedContactIds) {
        const conns = connectionManager.getUserConnections(contactId);
        conns.forEach(conn => {
          io.to(conn.socketId).emit('nightguard:created', {
            sessionId: session.id,
            eventType: NightGuardEventType.BUDDY_UPDATE,
            userId,
            status: session.status,
            location: session.lastLocation,
            battery: session.battery,
            etaSeconds: session.etaSeconds,
            timestamp: new Date().toISOString(),
            correlationId,
          });
        });

        if (conns.length === 0) {
          notificationEngine.trigger({
            trigger: NotificationTrigger.NIGHTGUARD_ALERT,
            recipientId: contactId,
            title: `NightGuard: ${displayName || userId} needs you`,
            body: `${displayName || 'Someone'} started a ${data.type} session`,
            data: { sessionId: session.id, type: data.type, ownerId: userId, destination: data.destination },
            correlationId,
          }).catch(() => {});
        }
      }

      if (typeof ack === 'function') ack({ success: true, data: { sessionId: session.id, session: { ...session, participants: Array.from(session.participants.values()) } }, correlationId, timestamp: new Date().toISOString() });

      io.to(session.id).emit('nightguard:created', {
        sessionId: session.id,
        eventType: NightGuardEventType.BUDDY_UPDATE,
        userId,
        status: session.status,
        timestamp: new Date().toISOString(),
        correlationId,
      });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.INTERNAL_ERROR, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
      else socket.emit('error', appErr.toJSON());
    }
  });

  socket.on('nightguard:join', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(nightguardJoinSchema, payload, correlationId);
      const session = nightguardEngine.addParticipant(data.sessionId, userId, ParticipantRole.GUARDIAN, socket.id, displayName);
      await socket.join(data.sessionId);

      io.to(data.sessionId).emit('nightguard:joined', {
        sessionId: data.sessionId,
        eventType: NightGuardEventType.BUDDY_UPDATE,
        userId,
        status: session.status,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (typeof ack === 'function') ack({ success: true, data: { sessionId: data.sessionId, participants: Array.from(session.participants.values()), status: session.status }, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.NIGHTGUARD_SESSION_NOT_FOUND, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  socket.on('nightguard:leave', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(nightguardJoinSchema, payload, correlationId);
      nightguardEngine.removeParticipant(data.sessionId, userId);
      await socket.leave(data.sessionId);
      socket.to(data.sessionId).emit('nightguard:status', {
        sessionId: data.sessionId,
        eventType: NightGuardEventType.BUDDY_UPDATE,
        userId,
        status: 'LEFT',
        timestamp: new Date().toISOString(),
        correlationId,
      });
      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      if (typeof ack === 'function') ack({ success: false, error: (err as Error).message });
    }
  });

  socket.on('nightguard:location', (payload: any) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(nightguardLocationSchema, payload, correlationId);
      const session = nightguardEngine.updateLocation(data.sessionId, userId, data.location as any, { etaSeconds: data.etaSeconds, battery: data.battery });

      io.to(data.sessionId).emit('nightguard:location', {
        sessionId: data.sessionId,
        eventType: NightGuardEventType.LIVE_LOCATION,
        userId,
        location: data.location,
        battery: data.battery,
        etaSeconds: data.etaSeconds,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      // Also forward to trusted contacts individually if not in session?
    } catch (err) {
      logger.warn({ err: (err as Error).message, userId, sessionId: payload?.sessionId }, 'nightguard:location failed');
    }
  });

  socket.on('nightguard:status', (payload: any) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(nightguardStatusSchema, payload, correlationId);
      const session = nightguardEngine.updateStatus(data.sessionId, userId, (data.status as any) || 'ACTIVE', data.note);

      io.to(data.sessionId).emit('nightguard:status', {
        sessionId: data.sessionId,
        eventType: data.eventType,
        userId,
        status: data.status || session.status,
        note: data.note,
        location: data.location,
        battery: data.battery,
        etaSeconds: data.etaSeconds,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if ([NightGuardEventType.ARRIVAL, 'ARRIVED', 'COMPLETED'].includes(data.eventType as any) || data.status === 'ARRIVED') {
        // Notify trusted contacts of arrival
        session.trustedContactIds.forEach(tid => {
          if (tid === userId) return;
          notificationEngine.trigger({
            trigger: NotificationTrigger.NIGHTGUARD_ALERT,
            recipientId: tid,
            title: 'NightGuard: Arrived safely',
            body: `${displayName || userId} arrived safely`,
            data: { sessionId: data.sessionId, type: 'ARRIVAL' },
            correlationId,
          }).catch(() => {});
        });
      }
    } catch (err) {
      logger.warn({ err: (err as Error).message, userId }, 'nightguard:status failed');
    }
  });

  socket.on('nightguard:sos', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(nightguardSosSchema, payload, correlationId);
      const session = nightguardEngine.triggerSos(data.sessionId, userId, data.location as any, data.triggerType, data.message, data.contactsToAlert);

      if (!session) throw new AppError({ code: ErrorCodes.NIGHTGUARD_SESSION_NOT_FOUND, correlationId });

      // Emit to session participants
      io.to(session.id).emit('nightguard:sos', {
        sessionId: session.id,
        eventType: NightGuardEventType.SOS,
        userId,
        location: data.location,
        battery: undefined,
        note: data.message,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      // Determine who to alert
      const toAlert = data.contactsToAlert || session.trustedContactIds;
      for (const contactId of toAlert) {
        if (contactId === userId) continue;
        const conns = connectionManager.getUserConnections(contactId);
        conns.forEach(conn => {
          io.to(conn.socketId).emit('nightguard:sos', {
            sessionId: session.id,
            eventType: NightGuardEventType.SOS,
            userId,
            location: data.location,
            note: data.message,
            timestamp: new Date().toISOString(),
            correlationId,
          });
        });

        // Push notification for SOS (critical)
        notificationEngine.trigger({
          trigger: NotificationTrigger.EMERGENCY_ALERT,
          recipientId: contactId,
          title: '🚨 SOS Emergency',
          body: `${displayName || userId} triggered SOS - needs help now!`,
          data: { sessionId: session.id, location: data.location, triggerType: data.triggerType, ownerId: userId, emergency: true },
          correlationId,
        }).catch(() => {});
      }

      // Global SOS for monitoring service
      io.emit('nightguard:global_sos', {
        sessionId: session.id,
        userId,
        location: data.location,
        triggerType: data.triggerType,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (typeof ack === 'function') ack({ success: true, data: { sessionId: session.id, alerted: toAlert.length }, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.INTERNAL_ERROR, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
      else socket.emit('error', appErr.toJSON());
    }
  });
}
