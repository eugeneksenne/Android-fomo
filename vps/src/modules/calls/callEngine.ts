import { CallType, CallState, ParticipantRole, ConnectionQuality } from '../../shared/types/enums.js';
import { CallRoom, CallParticipantState, createEmptyCall } from './callState.js';
import { logger, logCall } from '../../core/logger/logger.js';
import { AppError } from '../../shared/errors/appError.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { generateCorrelationId, generateRoomId } from '../../shared/utils/id.js';
import { env } from '../../config/env.js';
import { connectionManager } from '../socket/connectionManager.js';
import { metricsCollector } from '../../core/monitoring/metrics.js';
import { getRedisClient, getRedisKey } from '../../core/redis/client.js';

class CallEngine {
  private calls = new Map<string, CallRoom>();
  private userCalls = new Map<string, Set<string>>(); // userId -> Set<callId>
  private callTimeouts = new Map<string, NodeJS.Timeout>();

  createCall(params: {
    callType: CallType;
    createdBy: string;
    targetUserId?: string;
    targetUserIds?: string[];
    metadata?: Record<string, unknown>;
    correlationId?: string;
  }): CallRoom {
    const { callType, createdBy, targetUserIds, targetUserId, metadata, correlationId } = params;
    const targets = targetUserIds || (targetUserId ? [targetUserId] : []);

    // Check if caller already in another call (for 1-1, not for group? But enforce: user can be in max 1 call)
    const existingCalls = this.getUserCalls(createdBy).filter(c => c.state !== CallState.ENDED && c.state !== CallState.CANCELLED);
    if (existingCalls.length > 0) {
      throw new AppError({
        code: ErrorCodes.CALL_ALREADY_ACTIVE,
        correlationId: correlationId || generateCorrelationId(),
        message: 'User already in another call',
        meta: { userId: createdBy, details: { existingCallIds: existingCalls.map(c => c.id) } },
      });
    }

    const callId = generateRoomId('call', `${callType.toLowerCase()}`);
    const room = createEmptyCall({
      id: callId,
      type: callType,
      createdBy,
      maxParticipants: callType.includes('GROUP') ? env.MAX_CALL_PARTICIPANTS : 2,
      metadata: metadata as any,
    });

    // Add caller as participant
    room.participants.set(createdBy, {
      userId: createdBy,
      role: ParticipantRole.CALLER,
      joinedAt: Date.now(),
      lastSeen: Date.now(),
      isMuted: false,
      isCameraOn: callType === CallType.VIDEO || callType === CallType.GROUP_VIDEO,
      isSpeakerOn: true,
      isHandRaised: false,
      connectionQuality: ConnectionQuality.EXCELLENT,
    });

    room.state = CallState.RINGING;
    room.ringingStartedAt = Date.now();

    this.calls.set(callId, room);
    this.trackUserCall(createdBy, callId);

    // Ring timeout
    const timeout = setTimeout(() => this.handleRingTimeout(callId), env.CALL_RING_TIMEOUT_MS);
    this.callTimeouts.set(callId, timeout);

    metricsCollector.incActiveCall();

    logCall({ callId, type: callType, initiator: createdBy, participants: [createdBy, ...targets], state: CallState.RINGING });

    this.persistToRedis(room).catch(err => logger.warn({ err: err.message, callId }, 'Failed to persist call to Redis'));

    return room;
  }

  getCall(callId: string): CallRoom | null {
    return this.calls.get(callId) || null;
  }

  getUserCalls(userId: string): CallRoom[] {
    const ids = this.userCalls.get(userId);
    if (!ids) return [];
    return Array.from(ids).map(id => this.calls.get(id)).filter(Boolean) as CallRoom[];
  }

  addParticipant(callId: string, userId: string, role: ParticipantRole = ParticipantRole.PARTICIPANT, socketId?: string, displayName?: string): CallRoom {
    const room = this.calls.get(callId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId: generateCorrelationId() });
    if (room.isLocked) throw new AppError({ code: ErrorCodes.ROOM_LOCKED, correlationId: generateCorrelationId() });
    if (room.participants.size >= room.maxParticipants) throw new AppError({ code: ErrorCodes.CALL_PARTICIPANT_LIMIT, correlationId: generateCorrelationId() });

    const existing = room.participants.get(userId);
    if (existing) {
      existing.socketId = socketId || existing.socketId;
      existing.lastSeen = Date.now();
      return room;
    }

    room.participants.set(userId, {
      userId,
      socketId,
      role,
      joinedAt: Date.now(),
      lastSeen: Date.now(),
      isMuted: false,
      isCameraOn: room.type === CallType.VIDEO || room.type === CallType.GROUP_VIDEO,
      isSpeakerOn: true,
      isHandRaised: false,
      connectionQuality: ConnectionQuality.GOOD,
      displayName,
    });

    room.lastActivityAt = Date.now();
    this.trackUserCall(userId, callId);
    this.persistToRedis(room).catch(() => {});

    logger.debug({ callId, userId, role }, 'Participant added to call');
    return room;
  }

  removeParticipant(callId: string, userId: string): CallRoom | null {
    const room = this.calls.get(callId);
    if (!room) return null;

    room.participants.delete(userId);
    this.untrackUserCall(userId, callId);
    room.lastActivityAt = Date.now();

    // If no participants left, end call
    if (room.participants.size === 0) {
      this.endCall(callId, userId, 'NO_PARTICIPANTS');
      return null;
    }

    // If only caller left and was ringing, cancel?
    if (room.participants.size === 1 && room.state === CallState.RINGING) {
      // Keep ringing, don't end yet - callee may still answer
    }

    this.persistToRedis(room).catch(() => {});
    return room;
  }

  acceptCall(callId: string, userId: string, socketId?: string): CallRoom {
    const room = this.calls.get(callId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId: generateCorrelationId() });

    // Clear ring timeout
    const timeout = this.callTimeouts.get(callId);
    if (timeout) {
      clearTimeout(timeout);
      this.callTimeouts.delete(callId);
    }

    if (room.state !== CallState.RINGING && room.state !== CallState.REQUESTING) {
      throw new AppError({ code: ErrorCodes.CALL_FAILED, correlationId: generateCorrelationId(), message: `Call not in ringing state, current: ${room.state}` });
    }

    // Check if callee is busy in another call
    const userOtherCalls = this.getUserCalls(userId).filter(c => c.id !== callId && c.state !== CallState.ENDED);
    if (userOtherCalls.length > 0) {
      throw new AppError({ code: ErrorCodes.CALL_BUSY, correlationId: generateCorrelationId(), meta: { userId } });
    }

    this.addParticipant(callId, userId, ParticipantRole.CALLEE, socketId);
    room.state = CallState.ACCEPTED;
    room.acceptedAt = Date.now();
    room.lastActivityAt = Date.now();

    logCall({ callId, type: room.type, initiator: room.createdBy, participants: Array.from(room.participants.keys()), state: CallState.ACCEPTED });

    this.persistToRedis(room).catch(() => {});
    return room;
  }

  rejectCall(callId: string, userId: string, reason?: string): CallRoom {
    const room = this.calls.get(callId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId: generateCorrelationId() });

    const timeout = this.callTimeouts.get(callId);
    if (timeout) {
      clearTimeout(timeout);
      this.callTimeouts.delete(callId);
    }

    room.state = CallState.REJECTED;
    room.endedAt = Date.now();
    room.endedBy = userId;
    room.endReason = reason || 'REJECTED';

    // Remove all tracking? Keep for history short time
    setTimeout(() => this.cleanupCall(callId), 30_000);

    metricsCollector.decActiveCall(false);

    logCall({ callId, type: room.type, initiator: room.createdBy, participants: Array.from(room.participants.keys()), state: CallState.REJECTED });

    return room;
  }

  cancelCall(callId: string, userId: string, reason?: string): CallRoom {
    const room = this.calls.get(callId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId: generateCorrelationId() });

    if (room.createdBy !== userId) {
      throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: 'Only caller can cancel' });
    }

    const timeout = this.callTimeouts.get(callId);
    if (timeout) {
      clearTimeout(timeout);
      this.callTimeouts.delete(callId);
    }

    room.state = CallState.CANCELLED;
    room.endedAt = Date.now();
    room.endedBy = userId;
    room.endReason = reason || 'CANCELLED';

    metricsCollector.decActiveCall(false);

    setTimeout(() => this.cleanupCall(callId), 30_000);

    logCall({ callId, type: room.type, initiator: room.createdBy, participants: Array.from(room.participants.keys()), state: CallState.CANCELLED });

    return room;
  }

  endCall(callId: string, userId: string, reason?: string): CallRoom {
    const room = this.calls.get(callId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId: generateCorrelationId() });

    const timeout = this.callTimeouts.get(callId);
    if (timeout) {
      clearTimeout(timeout);
      this.callTimeouts.delete(callId);
    }

    room.state = CallState.ENDED;
    room.endedAt = Date.now();
    room.endedBy = userId;
    room.endReason = reason || 'ENDED';

    // Untrack all participants
    for (const pid of room.participants.keys()) {
      this.untrackUserCall(pid, callId);
    }

    metricsCollector.decActiveCall(false);

    setTimeout(() => this.cleanupCall(callId), 5_000);

    logCall({ callId, type: room.type, initiator: room.createdBy, participants: Array.from(room.participants.keys()), state: CallState.ENDED });

    return room;
  }

  updateMediaState(callId: string, userId: string, state: Partial<Pick<CallParticipantState, 'isMuted' | 'isCameraOn' | 'isSpeakerOn' | 'isHandRaised'>>): CallRoom {
    const room = this.calls.get(callId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId: generateCorrelationId() });

    const participant = room.participants.get(userId);
    if (!participant) throw new AppError({ code: ErrorCodes.NOT_IN_ROOM, correlationId: generateCorrelationId() });

    if (state.isMuted !== undefined) participant.isMuted = state.isMuted;
    if (state.isCameraOn !== undefined) participant.isCameraOn = state.isCameraOn;
    if (state.isSpeakerOn !== undefined) participant.isSpeakerOn = state.isSpeakerOn;
    if (state.isHandRaised !== undefined) participant.isHandRaised = state.isHandRaised;

    participant.lastSeen = Date.now();
    room.lastActivityAt = Date.now();

    this.persistToRedis(room).catch(() => {});
    return room;
  }

  escalateCall(callId: string, userId: string, newType: CallType): CallRoom {
    const room = this.calls.get(callId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId: generateCorrelationId() });

    // Only allow VOICE -> VIDEO escalation
    if (room.type === CallType.VOICE && newType === CallType.VIDEO) {
      room.type = CallType.VIDEO;
      // Update participants camera state
      for (const p of room.participants.values()) {
        p.isCameraOn = true;
      }
      room.lastActivityAt = Date.now();
      this.persistToRedis(room).catch(() => {});
      return room;
    }

    throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: `Escalation from ${room.type} to ${newType} not allowed` });
  }

  private handleRingTimeout(callId: string): void {
    const room = this.calls.get(callId);
    if (!room) return;
    if (room.state !== CallState.RINGING) return;

    room.state = CallState.TIMEOUT;
    room.endedAt = Date.now();
    room.endReason = 'RING_TIMEOUT';

    metricsCollector.decActiveCall(true);

    // Notify via event? The handlers layer will poll? Better emit via callback in handler timeout checker?
    // For now we just mark state and cleanup
    logger.info({ callId, state: CallState.TIMEOUT }, `Call ring timeout ${callId}`);

    // Untrack
    for (const pid of room.participants.keys()) {
      this.untrackUserCall(pid, callId);
    }

    setTimeout(() => this.cleanupCall(callId), 10_000);
  }

  private cleanupCall(callId: string): void {
    const room = this.calls.get(callId);
    if (!room) return;
    for (const pid of room.participants.keys()) {
      this.untrackUserCall(pid, callId);
    }
    this.calls.delete(callId);
    this.callTimeouts.delete(callId);
    this.removeFromRedis(callId).catch(() => {});
    logger.debug({ callId }, 'Call cleaned up');
  }

  private trackUserCall(userId: string, callId: string): void {
    if (!this.userCalls.has(userId)) this.userCalls.set(userId, new Set());
    this.userCalls.get(userId)!.add(callId);
  }

  private untrackUserCall(userId: string, callId: string): void {
    this.userCalls.get(userId)?.delete(callId);
    if (this.userCalls.get(userId)?.size === 0) this.userCalls.delete(userId);
  }

  getActiveCallsCount(): number {
    return Array.from(this.calls.values()).filter(c => c.state === CallState.ACCEPTED || c.state === CallState.RINGING).length;
  }

  getAllCalls(): CallRoom[] {
    return Array.from(this.calls.values());
  }

  private async persistToRedis(room: CallRoom): Promise<void> {
    if (!env.REDIS_ENABLED) return;
    const rc = await getRedisClient();
    if (!rc) return;
    const key = getRedisKey('call', room.id);
    const serializable = {
      ...room,
      participants: Array.from(room.participants.entries()),
    };
    await rc.set(key, JSON.stringify(serializable), { EX: 3600 });
  }

  private async removeFromRedis(callId: string): Promise<void> {
    if (!env.REDIS_ENABLED) return;
    const rc = await getRedisClient();
    if (!rc) return;
    await rc.del(getRedisKey('call', callId));
  }
}

export const callEngine = new CallEngine();
