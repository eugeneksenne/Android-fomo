import { Server as SocketIOServer, Socket } from 'socket.io';
import { callEngine } from './callEngine.js';
import { webrtcSignalingService } from './webrtcSignaling.js';
import { groupCallEngine } from './groupCallEngine.js';
import { validatePayload } from '../../core/middleware/validation.js';
import { callInitiateSchema, callActionSchema, callJoinSchema, callEscalateSchema, callMediaStateSchema, groupCallActionSchema, sdpPayloadSchema, iceCandidatePayloadSchema, renegotiationSchema } from './validators.js';
import { logger } from '../../core/logger/logger.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { CallState, ParticipantRole } from '../../shared/types/enums.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { connectionManager } from '../socket/connectionManager.js';
import { notificationEngine } from '../notifications/notificationEngine.js';
import { NotificationTrigger } from '../../shared/types/enums.js';
import { metricsCollector } from '../../core/monitoring/metrics.js';
import { auditLog } from '../../core/middleware/audit.js';

export function callHandlers(io: SocketIOServer, socket: Socket): void {
  const userId = (socket.data as any).user?.id as string;
  const displayName = (socket.data as any).user?.user_metadata?.display_name || userId;

  // Initiate call
  socket.on('call:initiate', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    const start = Date.now();
    try {
      const data = validatePayload(callInitiateSchema, payload, correlationId);
      const targets = data.targetUserIds || (data.targetUserId ? [data.targetUserId] : []);

      if (targets.includes(userId)) {
        throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId, message: 'Cannot call self' });
      }

      const room = callEngine.createCall({
        callType: data.callType,
        createdBy: userId,
        targetUserId: data.targetUserId,
        targetUserIds: data.targetUserIds,
        metadata: data.metadata,
        correlationId,
      });

      await socket.join(room.id);
      // Map caller socket
      const callerParticipant = room.participants.get(userId);
      if (callerParticipant) callerParticipant.socketId = socket.id;

      logger.info({ callId: room.id, type: data.callType, initiator: userId, targets, correlationId }, `Call initiated ${room.id}`);

      // Notify targets individually
      const offlineTargets: string[] = [];
      for (const targetId of targets) {
        const conns = connectionManager.getUserConnections(targetId);
        if (conns.length === 0) {
          offlineTargets.push(targetId);
          // Trigger push notification for offline
          notificationEngine.trigger({
            trigger: NotificationTrigger.INCOMING_CALL,
            recipientId: targetId,
            title: `Incoming ${data.callType} call`,
            body: `${displayName} is calling you`,
            data: { callId: room.id, callerId: userId, callType: data.callType, roomId: room.id },
            correlationId,
          }).catch(() => {});
        } else {
          conns.forEach(conn => {
            io.to(conn.socketId).emit('call:incoming', {
              roomId: room.id,
              callId: room.id,
              callerId: userId,
              callerName: displayName,
              callType: data.callType,
              participants: [userId, ...targets],
              timestamp: new Date().toISOString(),
              correlationId,
              metadata: data.metadata,
            });
          });
        }
      }

      // Also broadcast to user rooms for multi-device
      targets.forEach(tid => io.to(`user:${tid}`).emit('call:incoming', {
        roomId: room.id,
        callId: room.id,
        callerId: userId,
        callerName: displayName,
        callType: data.callType,
        participants: [userId, ...targets],
        timestamp: new Date().toISOString(),
        correlationId,
        metadata: data.metadata,
      }));

      metricsCollector.recordLatency(Date.now() - start);

      const response = {
        success: true,
        data: {
          roomId: room.id,
          callId: room.id,
          state: room.state,
          participants: Array.from(room.participants.values()),
          offlineTargets,
        },
        correlationId,
        timestamp: new Date().toISOString(),
      };

      if (typeof ack === 'function') ack(response);
      auditLog(socket, 'call:initiate', payload, 'success');

      // Emit ringing to room
      io.to(room.id).emit('call:ringing', {
        roomId: room.id,
        callId: room.id,
        state: CallState.RINGING,
        userId,
        participants: [userId, ...targets],
        timestamp: new Date().toISOString(),
        correlationId,
      });

      // If targets offline, also emit missed call timeout eventually handled by callEngine timeout
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.CALL_FAILED, correlationId, developerMessage: (err as Error).message });
      logger.warn({ callId: payload?.roomId, userId, error: appErr.code, correlationId }, `Call initiate failed ${appErr.message}`);
      metricsCollector.recordError(appErr.code);
      socket.emit('error', appErr.toJSON());
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
      auditLog(socket, 'call:initiate', payload, 'failure', { code: appErr.code });
    }
  });

  // Accept
  socket.on('call:accept', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(callActionSchema, payload, correlationId);
      const room = callEngine.acceptCall(data.roomId, userId, socket.id);
      await socket.join(data.roomId);

      io.to(data.roomId).emit('call:accepted', {
        roomId: data.roomId,
        callId: data.roomId,
        state: CallState.ACCEPTED,
        userId,
        participants: Array.from(room.participants.keys()),
        timestamp: new Date().toISOString(),
        correlationId,
      });

      io.to(data.roomId).emit('call:participant_update', {
        roomId: data.roomId,
        participants: Array.from(room.participants.values()).map(p => ({
          userId: p.userId,
          role: p.role,
          isMuted: p.isMuted,
          isCameraOn: p.isCameraOn,
          isHandRaised: p.isHandRaised,
          displayName: p.displayName,
        })),
        participantCount: room.participants.size,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (typeof ack === 'function') ack({ success: true, data: { roomId: data.roomId, participants: Array.from(room.participants.values()) }, correlationId, timestamp: new Date().toISOString() });
      auditLog(socket, 'call:accept', payload, 'success');
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.CALL_FAILED, correlationId, developerMessage: (err as Error).message });
      socket.emit('error', appErr.toJSON());
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  // Reject
  socket.on('call:reject', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(callActionSchema, payload, correlationId);
      const room = callEngine.rejectCall(data.roomId, userId, data.reason);

      io.to(data.roomId).emit('call:rejected', {
        roomId: data.roomId,
        callId: data.roomId,
        state: CallState.REJECTED,
        userId,
        reason: data.reason,
        participants: Array.from(room.participants.keys()),
        timestamp: new Date().toISOString(),
        correlationId,
      });

      // Notify caller via push if needed
      const callerId = room.createdBy;
      if (callerId !== userId) {
        notificationEngine.trigger({
          trigger: NotificationTrigger.CALL_REJECTED,
          recipientId: callerId,
          title: 'Call rejected',
          body: `${displayName} rejected your call`,
          data: { callId: data.roomId, userId, reason: data.reason },
          correlationId,
        }).catch(() => {});
      }

      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
      auditLog(socket, 'call:reject', payload, 'success');
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.CALL_FAILED, correlationId, developerMessage: (err as Error).message });
      socket.emit('error', appErr.toJSON());
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  // Cancel (caller only)
  socket.on('call:cancel', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(callActionSchema, payload, correlationId);
      const room = callEngine.cancelCall(data.roomId, userId, data.reason);

      io.to(data.roomId).emit('call:cancelled', {
        roomId: data.roomId,
        callId: data.roomId,
        state: CallState.CANCELLED,
        userId,
        reason: data.reason,
        participants: Array.from(room.participants.keys()),
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
      socket.leave(data.roomId);
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.CALL_FAILED, correlationId, developerMessage: (err as Error).message });
      socket.emit('error', appErr.toJSON());
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  // End
  socket.on('call:end', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(callActionSchema, payload, correlationId);
      const room = callEngine.endCall(data.roomId, userId, data.reason);

      io.to(data.roomId).emit('call:ended', {
        roomId: data.roomId,
        callId: data.roomId,
        state: CallState.ENDED,
        userId,
        reason: data.reason,
        participants: Array.from(room.participants.keys()),
        timestamp: new Date().toISOString(),
        correlationId,
      });

      // Force all sockets to leave
      const sockets = await io.in(data.roomId).fetchSockets();
      for (const s of sockets) s.leave(data.roomId);

      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
      auditLog(socket, 'call:end', payload, 'success');
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.CALL_FAILED, correlationId, developerMessage: (err as Error).message });
      socket.emit('error', appErr.toJSON());
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  socket.on('call:leave', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(callActionSchema, payload, correlationId);
      const room = callEngine.removeParticipant(data.roomId, userId);
      await socket.leave(data.roomId);

      if (room) {
        io.to(data.roomId).emit('call:participant_update', {
          roomId: data.roomId,
          participants: Array.from(room.participants.values()).map(p => ({
            userId: p.userId,
            role: p.role,
            isMuted: p.isMuted,
            isCameraOn: p.isCameraOn,
            isHandRaised: p.isHandRaised,
          })),
          participantCount: room.participants.size,
          timestamp: new Date().toISOString(),
          correlationId,
        });
      }

      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      if (typeof ack === 'function') ack({ success: false, error: (err as Error).message });
    }
  });

  // Join (for group calls)
  socket.on('call:join', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(callJoinSchema, payload, correlationId);
      const room = callEngine.addParticipant(data.roomId, userId, ParticipantRole.PARTICIPANT, socket.id, displayName);
      await socket.join(data.roomId);

      io.to(data.roomId).emit('call:participant_update', {
        roomId: data.roomId,
        participants: Array.from(room.participants.values()).map(p => ({
          userId: p.userId,
          role: p.role,
          isMuted: p.isMuted,
          isCameraOn: p.isCameraOn,
          isHandRaised: p.isHandRaised,
          displayName: p.displayName,
        })),
        participantCount: room.participants.size,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (typeof ack === 'function') ack({ success: true, data: { participants: Array.from(room.participants.values()) }, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.ROOM_NOT_FOUND, correlationId, developerMessage: (err as Error).message });
      socket.emit('error', appErr.toJSON());
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  // Media state
  socket.on('call:media_state', (payload: any) => {
    try {
      const correlationId = payload?.correlationId || generateCorrelationId();
      const data = validatePayload(callMediaStateSchema, payload, correlationId);
      const room = callEngine.updateMediaState(data.roomId, userId, {
        isMuted: data.isMuted ?? (data.audioEnabled !== undefined ? !data.audioEnabled : undefined),
        isCameraOn: data.isCameraOn ?? data.videoEnabled,
        isSpeakerOn: data.isSpeakerOn,
        isHandRaised: data.isHandRaised,
      });

      io.to(data.roomId).emit('call:media_state', {
        roomId: data.roomId,
        userId,
        audioEnabled: data.audioEnabled,
        videoEnabled: data.videoEnabled,
        isMuted: data.isMuted,
        isCameraOn: data.isCameraOn,
        isSpeakerOn: data.isSpeakerOn,
        isHandRaised: data.isHandRaised,
        timestamp: new Date().toISOString(),
        correlationId,
      });
    } catch (err) {
      // non-critical
    }
  });

  // Escalate voice → video
  socket.on('call:escalate', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(callEscalateSchema, payload, correlationId);
      const room = callEngine.escalateCall(data.roomId, userId, data.newType);

      io.to(data.roomId).emit('call:escalated', {
        roomId: data.roomId,
        callId: data.roomId,
        state: CallState.ACCEPTED,
        userId,
        participants: Array.from(room.participants.keys()),
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (typeof ack === 'function') ack({ success: true, data: { newType: data.newType }, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId, developerMessage: (err as Error).message });
      socket.emit('error', appErr.toJSON());
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  // Group actions
  socket.on('call:group_action', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(groupCallActionSchema, payload, correlationId);
      const room = groupCallEngine.handleParticipantAction(data.roomId, userId, data.action, data.targetUserId);

      io.to(data.roomId).emit('call:participant_update', {
        roomId: data.roomId,
        participants: Array.from(room.participants.values()).map(p => ({
          userId: p.userId,
          role: p.role,
          isMuted: p.isMuted,
          isCameraOn: p.isCameraOn,
          isHandRaised: p.isHandRaised,
        })),
        participantCount: room.participants.size,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (data.action === 'REMOVE' && data.targetUserId) {
        const targetConns = connectionManager.getUserConnections(data.targetUserId);
        targetConns.forEach(conn => io.to(conn.socketId).emit('call:ended', {
          roomId: data.roomId,
          callId: data.roomId,
          state: CallState.ENDED,
          userId,
          reason: 'REMOVED_BY_HOST',
          participants: [userId],
          timestamp: new Date().toISOString(),
          correlationId,
        }));
      }

      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId, developerMessage: (err as Error).message });
      socket.emit('error', appErr.toJSON());
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  // WebRTC signaling
  socket.on('webrtc:offer', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(sdpPayloadSchema, payload, correlationId);
      await webrtcSignalingService.handleOffer(io, socket, { roomId: data.roomId, targetUserId: data.targetUserId, sdp: data.sdp, codecInfo: data.codecInfo, correlationId });
      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.INTERNAL_ERROR, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  socket.on('webrtc:answer', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(sdpPayloadSchema, payload, correlationId);
      await webrtcSignalingService.handleAnswer(io, socket, { roomId: data.roomId, targetUserId: data.targetUserId, sdp: data.sdp, correlationId });
      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.INTERNAL_ERROR, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  socket.on('webrtc:ice-candidate', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(iceCandidatePayloadSchema, payload, correlationId);
      await webrtcSignalingService.handleIceCandidate(io, socket, { roomId: data.roomId, targetUserId: data.targetUserId, candidate: data.candidate, isRestart: data.isRestart, correlationId });
      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      // ICE failures are high frequency, don't spam error
      if (typeof ack === 'function') ack({ success: false, error: { code: ErrorCodes.INTERNAL_ERROR, correlationId } });
    }
  });

  socket.on('webrtc:renegotiate', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(renegotiationSchema, payload, correlationId);
      await webrtcSignalingService.handleRenegotiation(io, socket, { roomId: data.roomId, targetUserId: data.targetUserId, reason: data.reason, correlationId });
      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      if (typeof ack === 'function') ack({ success: false, error: (err as Error).message });
    }
  });

  socket.on('webrtc:ice-restart', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(iceCandidatePayloadSchema, payload, correlationId);
      await webrtcSignalingService.handleIceRestart(io, socket, { roomId: data.roomId, targetUserId: data.targetUserId, candidate: data.candidate, correlationId });
      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      if (typeof ack === 'function') ack({ success: false, error: (err as Error).message });
    }
  });
}
