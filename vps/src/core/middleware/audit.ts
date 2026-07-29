import { Socket } from 'socket.io';
import { logger } from '../logger/logger.js';
import { generateCorrelationId } from '../../shared/utils/id.js';

export function auditLog(socket: Socket, event: string, payload: unknown, result: 'success' | 'failure', details?: unknown): void {
  const userId = (socket.data as any)?.user?.id || 'unknown';
  const deviceId = (socket.data as any)?.device?.deviceId;
  const correlationId = (payload as any)?.correlationId || generateCorrelationId();

  // Never log sensitive payload directly - sanitize
  const safePayload = typeof payload === 'object' && payload !== null
    ? { ...payload as Record<string, unknown>, token: undefined, sdp: undefined, candidate: undefined }
    : payload;

  logger.info({
    event: 'audit',
    userId,
    deviceId,
    socketId: socket.id,
    action: event,
    result,
    correlationId,
    timestamp: new Date().toISOString(),
    details: details ? JSON.stringify(details).slice(0, 500) : undefined,
  }, `AUDIT ${userId} ${event} ${result}`);
}

export function auditMiddleware(socket: Socket, next: (err?: Error) => void): void {
  // Hook for audit - we wrap emit? Actually we audit in handlers
  next();
}
