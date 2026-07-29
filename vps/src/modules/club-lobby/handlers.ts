import { Server as SocketIOServer, Socket } from 'socket.io';
import { lobbyEngine } from './lobbyEngine.js';
import { validatePayload } from '../../core/middleware/validation.js';
import { lobbyJoinSchema, lobbyLeaveSchema, lobbyMessageSchema, lobbyAnnouncementSchema } from './validators.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { ClubLobbyEventType, ParticipantRole } from '../../shared/types/enums.js';
import { AppError } from '../../shared/errors/appError.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { logger } from '../../core/logger/logger.js';

export function clubLobbyHandlers(io: SocketIOServer, socket: Socket): void {
  const userId = (socket.data as any).user?.id as string;
  const displayName = (socket.data as any).user?.user_metadata?.display_name as string | undefined;

  socket.on('lobby:join', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(lobbyJoinSchema, payload, correlationId);
      const lobby = lobbyEngine.createOrGetLobby({ venueId: data.venueId, lobbyId: data.lobbyId, channel: data.channel, createdBy: userId });
      const updated = lobbyEngine.addParticipant(lobby.id, userId, ParticipantRole.PARTICIPANT, socket.id, displayName);
      await socket.join(lobby.id);
      await socket.join(`venue:${data.venueId}`);

      io.to(lobby.id).emit('lobby:joined', {
        venueId: data.venueId,
        lobbyId: lobby.id,
        eventType: ClubLobbyEventType.MEMBER_JOINED,
        userId,
        data: { displayName, participantCount: updated.participants.size },
        participantCount: updated.participants.size,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      io.to(lobby.id).emit('lobby:crowd_count', {
        venueId: data.venueId,
        lobbyId: lobby.id,
        eventType: ClubLobbyEventType.CROWD_COUNT,
        data: { count: updated.crowdCount },
        participantCount: updated.crowdCount,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (typeof ack === 'function') ack({ success: true, data: { lobbyId: lobby.id, participantCount: updated.participants.size, participants: Array.from(updated.participants.values()) }, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.LOBBY_NOT_FOUND, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
      else socket.emit('error', appErr.toJSON());
    }
  });

  socket.on('lobby:leave', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(lobbyLeaveSchema, payload, correlationId);
      const lobbyId = data.lobbyId || `lobby_${data.venueId}_${data.channel || 'GENERAL'}`;
      const lobby = lobbyEngine.removeParticipant(lobbyId, userId);
      await socket.leave(lobbyId);

      if (lobby) {
        io.to(lobbyId).emit('lobby:left', {
          venueId: data.venueId,
          lobbyId,
          eventType: ClubLobbyEventType.MEMBER_LEFT,
          userId,
          data: { participantCount: lobby.participants.size },
          participantCount: lobby.participants.size,
          timestamp: new Date().toISOString(),
          correlationId,
        });
      }

      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      if (typeof ack === 'function') ack({ success: false, error: (err as Error).message });
    }
  });

  socket.on('lobby:message', async (payload: any) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(lobbyMessageSchema, payload, correlationId);
      const lobbyId = data.lobbyId || `lobby_${data.venueId}_${data.channel || 'GENERAL'}`;
      const lobby = lobbyEngine.getLobby(lobbyId);
      if (!lobby || !lobby.participants.has(userId)) throw new AppError({ code: ErrorCodes.NOT_IN_ROOM, correlationId });

      const msg = {
        venueId: data.venueId,
        lobbyId,
        eventType: ClubLobbyEventType.LIVE_CHAT,
        userId,
        data: {
          text: data.text,
          type: data.type || 'TEXT',
          displayName,
          replyTo: data.replyTo,
          metadata: data.metadata,
          id: `msg_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
        },
        timestamp: new Date().toISOString(),
        correlationId,
      };

      io.to(lobbyId).emit('lobby:message', msg);
    } catch (err) {
      logger.warn({ err: (err as Error).message, userId }, 'lobby:message failed');
    }
  });

  socket.on('lobby:announcement', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(lobbyAnnouncementSchema, payload, correlationId);
      const lobbyId = `lobby_${data.venueId}_GENERAL`;
      const lobby = lobbyEngine.getLobby(lobbyId);
      if (!lobby) throw new AppError({ code: ErrorCodes.LOBBY_NOT_FOUND, correlationId });

      const requester = lobby.participants.get(userId);
      if (!requester || (requester.role !== ParticipantRole.HOST && requester.role !== ParticipantRole.MODERATOR && lobby.createdBy !== userId)) {
        throw new AppError({ code: ErrorCodes.LOBBY_NOT_HOST, correlationId });
      }

      io.to(lobbyId).emit('lobby:announcement', {
        venueId: data.venueId,
        lobbyId,
        eventType: ClubLobbyEventType.HOST_ANNOUNCEMENT,
        userId,
        data: { message: data.message, priority: data.priority || 'MEDIUM', displayName },
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.INTERNAL_ERROR, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });
}
