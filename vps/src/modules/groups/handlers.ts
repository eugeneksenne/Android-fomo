import { Server as SocketIOServer, Socket } from 'socket.io';
import { z } from 'zod';
import { groupManager } from './groupManager.js';
import { RoomType, ParticipantRole } from '../../shared/types/enums.js';
import { validatePayload } from '../../core/middleware/validation.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';

const groupCreateSchema = z.object({
  type: z.nativeEnum(RoomType),
  name: z.string().max(100).optional(),
  maxParticipants: z.number().min(2).max(1000).optional(),
  metadata: z.record(z.unknown()).optional(),
  correlationId: z.string().optional(),
});

const groupJoinSchema = z.object({
  groupId: z.string().min(3).max(128),
  correlationId: z.string().optional(),
});

export function groupHandlers(io: SocketIOServer, socket: Socket): void {
  const userId = (socket.data as any).user?.id as string;
  const displayName = (socket.data as any).user?.user_metadata?.display_name as string | undefined;

  socket.on('group:create', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(groupCreateSchema, payload, correlationId);
      const group = groupManager.createGroup({ type: data.type, name: data.name, createdBy: userId, maxParticipants: data.maxParticipants, metadata: data.metadata });
      groupManager.addParticipant(group.id, userId, ParticipantRole.HOST, socket.id, displayName);
      await socket.join(group.id);

      if (typeof ack === 'function') ack({ success: true, data: { groupId: group.id, group }, correlationId, timestamp: new Date().toISOString() });
      io.to(group.id).emit('group:created', { groupId: group.id, createdBy: userId, type: data.type, timestamp: new Date().toISOString(), correlationId });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.INTERNAL_ERROR, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  socket.on('group:join', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(groupJoinSchema, payload, correlationId);
      const group = groupManager.addParticipant(data.groupId, userId, ParticipantRole.PARTICIPANT, socket.id, displayName);
      await socket.join(data.groupId);

      io.to(data.groupId).emit('group:participant_joined', {
        groupId: data.groupId,
        userId,
        displayName,
        participantCount: group.participants.size,
        timestamp: new Date().toISOString(),
        correlationId,
      });

      if (typeof ack === 'function') ack({ success: true, data: { groupId: data.groupId, participants: Array.from(group.participants.values()) }, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.ROOM_NOT_FOUND, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });

  socket.on('group:leave', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(groupJoinSchema, payload, correlationId);
      const group = groupManager.removeParticipant(data.groupId, userId);
      await socket.leave(data.groupId);

      if (group) {
        io.to(data.groupId).emit('group:participant_left', { groupId: data.groupId, userId, participantCount: group.participants.size, timestamp: new Date().toISOString(), correlationId });
      }

      if (typeof ack === 'function') ack({ success: true, correlationId, timestamp: new Date().toISOString() });
    } catch (err) {
      if (typeof ack === 'function') ack({ success: false, error: (err as Error).message });
    }
  });
}
