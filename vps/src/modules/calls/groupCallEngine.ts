import { CallRoom } from './callState.js';
import { ParticipantRole } from '../../shared/types/enums.js';
import { callEngine } from './callEngine.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { logger } from '../../core/logger/logger.js';

class GroupCallEngine {
  handleParticipantAction(roomId: string, requesterId: string, action: 'MUTE' | 'UNMUTE' | 'REMOVE' | 'PROMOTE' | 'DEMOTE' | 'RAISE_HAND' | 'LOWER_HAND', targetUserId?: string): CallRoom {
    const room = callEngine.getCall(roomId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId: generateCorrelationId() });

    const requester = room.participants.get(requesterId);
    if (!requester) throw new AppError({ code: ErrorCodes.NOT_IN_ROOM, correlationId: generateCorrelationId() });

    const targetId = targetUserId || requesterId;
    const target = room.participants.get(targetId);
    if (!target) throw new AppError({ code: ErrorCodes.ROOM_NOT_FOUND, correlationId: generateCorrelationId(), message: 'Target participant not found' });

    const isHost = requester.role === ParticipantRole.HOST || requester.role === ParticipantRole.CALLER || room.createdBy === requesterId;
    const isModerator = requester.role === ParticipantRole.MODERATOR || isHost;
    const isSelf = targetId === requesterId;

    switch (action) {
      case 'MUTE':
        if (!isSelf && !isModerator) throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: 'Only moderator can mute others' });
        target.isMuted = true;
        break;
      case 'UNMUTE':
        if (!isSelf) throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: 'Cannot unmute others' });
        target.isMuted = false;
        break;
      case 'REMOVE':
        if (!isHost) throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: 'Only host can remove' });
        if (isSelf) throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: 'Cannot remove self, leave instead' });
        callEngine.removeParticipant(roomId, targetId);
        logger.info({ roomId, requesterId, targetId, action }, 'Participant removed');
        return room;
      case 'PROMOTE':
        if (!isHost) throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: 'Only host can promote' });
        target.role = ParticipantRole.MODERATOR;
        break;
      case 'DEMOTE':
        if (!isHost) throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: 'Only host can demote' });
        if (target.role === ParticipantRole.HOST || room.createdBy === targetId) throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: 'Cannot demote host' });
        target.role = ParticipantRole.PARTICIPANT;
        break;
      case 'RAISE_HAND':
        if (!isSelf) throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: 'Can only raise own hand' });
        target.isHandRaised = true;
        break;
      case 'LOWER_HAND':
        if (!isSelf && !isModerator) throw new AppError({ code: ErrorCodes.CALL_NOT_ALLOWED, correlationId: generateCorrelationId(), message: 'Only self or moderator can lower hand' });
        target.isHandRaised = false;
        break;
    }

    target.lastSeen = Date.now();
    room.lastActivityAt = Date.now();

    logger.info({ roomId, requesterId, targetId, action }, `Group call action ${action}`);

    return room;
  }

  getParticipantCount(roomId: string): number {
    const room = callEngine.getCall(roomId);
    return room?.participants.size || 0;
  }

  isHost(roomId: string, userId: string): boolean {
    const room = callEngine.getCall(roomId);
    if (!room) return false;
    return room.createdBy === userId || room.participants.get(userId)?.role === ParticipantRole.HOST;
  }

  detectSpeaker(roomId: string, userId: string, audioLevel: number): boolean {
    // Simple speaker detection: if audioLevel > threshold and not muted
    const room = callEngine.getCall(roomId);
    if (!room) return false;
    const participant = room.participants.get(userId);
    if (!participant) return false;
    if (participant.isMuted) return false;
    return audioLevel > 0.3;
  }
}

export const groupCallEngine = new GroupCallEngine();
