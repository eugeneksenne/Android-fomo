import { Server as SocketIOServer, Socket } from 'socket.io';
import { logger } from '../../core/logger/logger.js';
import { callEngine } from './callEngine.js';
import { metricsCollector } from '../../core/monitoring/metrics.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { CallState } from '../../shared/types/enums.js';

type SdpType = 'offer' | 'answer';

interface SdpData {
  type: SdpType;
  sdp: string;
}

export class WebRTCSignalingService {
  async handleOffer(io: SocketIOServer, socket: Socket, payload: { roomId: string; targetUserId?: string; sdp: SdpData; codecInfo?: any; correlationId?: string }): Promise<void> {
    const start = Date.now();
    const correlationId = payload.correlationId || generateCorrelationId();
    const userId = (socket.data as any).user?.id as string;

    const room = callEngine.getCall(payload.roomId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId });

    if (!room.participants.has(userId)) throw new AppError({ code: ErrorCodes.NOT_IN_ROOM, correlationId });

    // Track for metrics & history
    room.sdpHistory.push({ userId, type: 'offer', timestamp: Date.now() });
    room.lastActivityAt = Date.now();

    metricsCollector.recordOffer();
    metricsCollector.recordLatency(Date.now() - start);

    const latency = Date.now() - start;
    if (latency > 50) logger.warn({ roomId: payload.roomId, latency, correlationId }, 'Offer delivery exceeded 50ms target');

    const eventPayload = {
      roomId: payload.roomId,
      senderId: userId,
      targetUserId: payload.targetUserId || null,
      sdp: payload.sdp,
      codecInfo: payload.codecInfo,
      timestamp: new Date().toISOString(),
      correlationId,
    };

    if (payload.targetUserId) {
      // Targeted offer for mesh/group
      const targetParticipant = room.participants.get(payload.targetUserId);
      if (targetParticipant?.socketId) {
        io.to(targetParticipant.socketId).emit('webrtc:offer', eventPayload);
      } else {
        // Fallback via user room - emit to all sockets of target user
        io.to(`user:${payload.targetUserId}`).emit('webrtc:offer', eventPayload);
        // Also try direct via connection manager sockets
        const { connectionManager } = await import('../socket/connectionManager.js');
        const conns = connectionManager.getUserConnections(payload.targetUserId);
        conns.forEach(c => io.to(c.socketId).emit('webrtc:offer', eventPayload));
      }
    } else {
      // Broadcast to room except sender
      socket.to(payload.roomId).emit('webrtc:offer', eventPayload);
    }

    logger.debug({ roomId: payload.roomId, senderId: userId, target: payload.targetUserId, correlationId }, 'Offer relayed');
  }

  async handleAnswer(io: SocketIOServer, socket: Socket, payload: { roomId: string; targetUserId?: string; sdp: SdpData; correlationId?: string }): Promise<void> {
    const start = Date.now();
    const correlationId = payload.correlationId || generateCorrelationId();
    const userId = (socket.data as any).user?.id as string;

    const room = callEngine.getCall(payload.roomId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId });
    if (!room.participants.has(userId)) throw new AppError({ code: ErrorCodes.NOT_IN_ROOM, correlationId });

    room.sdpHistory.push({ userId, type: 'answer', timestamp: Date.now() });
    room.lastActivityAt = Date.now();

    metricsCollector.recordAnswer();
    metricsCollector.recordLatency(Date.now() - start);

    const latency = Date.now() - start;
    if (latency > 50) logger.warn({ roomId: payload.roomId, latency, correlationId }, 'Answer delivery exceeded 50ms target');

    const eventPayload = {
      roomId: payload.roomId,
      senderId: userId,
      targetUserId: payload.targetUserId || null,
      sdp: payload.sdp,
      timestamp: new Date().toISOString(),
      correlationId,
    };

    if (payload.targetUserId) {
      const targetParticipant = room.participants.get(payload.targetUserId);
      if (targetParticipant?.socketId) {
        io.to(targetParticipant.socketId).emit('webrtc:answer', eventPayload);
      } else {
        io.to(`user:${payload.targetUserId}`).emit('webrtc:answer', eventPayload);
        const { connectionManager } = await import('../socket/connectionManager.js');
        const conns = connectionManager.getUserConnections(payload.targetUserId);
        conns.forEach(c => io.to(c.socketId).emit('webrtc:answer', eventPayload));
      }
    } else {
      socket.to(payload.roomId).emit('webrtc:answer', eventPayload);
    }

    logger.debug({ roomId: payload.roomId, senderId: userId, correlationId }, 'Answer relayed');
  }

  async handleIceCandidate(io: SocketIOServer, socket: Socket, payload: { roomId: string; targetUserId?: string; candidate: any; isRestart?: boolean; correlationId?: string }): Promise<void> {
    const start = Date.now();
    const correlationId = payload.correlationId || generateCorrelationId();
    const userId = (socket.data as any).user?.id as string;

    const room = callEngine.getCall(payload.roomId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId });
    if (!room.participants.has(userId)) throw new AppError({ code: ErrorCodes.NOT_IN_ROOM, correlationId });

    if (payload.isRestart) {
      room.iceRestartCount++;
      logger.info({ roomId: payload.roomId, userId, iceRestartCount: room.iceRestartCount }, 'ICE restart');
    }

    metricsCollector.recordIce();
    metricsCollector.recordLatency(Date.now() - start);

    const latency = Date.now() - start;
    if (latency > 50) logger.debug({ roomId: payload.roomId, latency, correlationId }, 'ICE delivery exceeded 50ms target (debug)');

    const eventPayload = {
      roomId: payload.roomId,
      senderId: userId,
      targetUserId: payload.targetUserId || null,
      candidate: payload.candidate,
      isRestart: payload.isRestart,
      timestamp: new Date().toISOString(),
      correlationId,
    };

    if (payload.targetUserId) {
      const targetParticipant = room.participants.get(payload.targetUserId);
      if (targetParticipant?.socketId) {
        io.to(targetParticipant.socketId).emit('webrtc:ice-candidate', eventPayload);
      } else {
        const { connectionManager } = await import('../socket/connectionManager.js');
        const conns = connectionManager.getUserConnections(payload.targetUserId);
        conns.forEach(c => io.to(c.socketId).emit('webrtc:ice-candidate', eventPayload));
      }
    } else {
      socket.to(payload.roomId).emit('webrtc:ice-candidate', eventPayload);
    }
  }

  async handleRenegotiation(io: SocketIOServer, socket: Socket, payload: { roomId: string; targetUserId?: string; reason: string; correlationId?: string }): Promise<void> {
    const correlationId = payload.correlationId || generateCorrelationId();
    const userId = (socket.data as any).user?.id as string;

    const room = callEngine.getCall(payload.roomId);
    if (!room) throw new AppError({ code: ErrorCodes.CALL_NOT_FOUND, correlationId });
    if (!room.participants.has(userId)) throw new AppError({ code: ErrorCodes.NOT_IN_ROOM, correlationId });

    logger.info({ roomId: payload.roomId, userId, reason: payload.reason, correlationId }, 'Renegotiation requested');

    io.to(payload.roomId).emit('webrtc:renegotiate', {
      roomId: payload.roomId,
      reason: payload.reason,
      timestamp: new Date().toISOString(),
      correlationId,
    });
  }

  async handleIceRestart(io: SocketIOServer, socket: Socket, payload: { roomId: string; targetUserId?: string; candidate: any; correlationId?: string }): Promise<void> {
    // ICE restart is essentially a new candidate with restart flag
    return this.handleIceCandidate(io, socket, { ...payload, isRestart: true });
  }
}

export const webrtcSignalingService = new WebRTCSignalingService();
