import { Socket } from 'socket.io';
import { env } from '../../config/env.js';
import { logger } from '../logger/logger.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { generateCorrelationId } from '../../shared/utils/id.js';

interface RateLimitEntry {
  count: number;
  windowStart: number;
}

class InMemoryRateLimiter {
  private clients = new Map<string, RateLimitEntry>();
  private cleanupInterval: NodeJS.Timeout;

  constructor() {
    this.cleanupInterval = setInterval(() => this.cleanup(), 60_000);
  }

  isAllowed(key: string, max: number, windowMs: number): boolean {
    const now = Date.now();
    let entry = this.clients.get(key);
    if (!entry) {
      this.clients.set(key, { count: 1, windowStart: now });
      return true;
    }
    if (now - entry.windowStart > windowMs) {
      entry.count = 1;
      entry.windowStart = now;
      return true;
    }
    entry.count++;
    return entry.count <= max;
  }

  private cleanup(): void {
    const now = Date.now();
    for (const [k, v] of this.clients.entries()) {
      if (now - v.windowStart > 120_000) this.clients.delete(k);
    }
  }

  stop(): void {
    clearInterval(this.cleanupInterval);
  }

  remove(key: string): void {
    this.clients.delete(key);
  }
}

export const globalRateLimiter = new InMemoryRateLimiter();
export const callRateLimiter = new InMemoryRateLimiter();
export const signalRateLimiter = new InMemoryRateLimiter();
export const abuseLimiter = new InMemoryRateLimiter(); // for spam / call spam

export function socketRateLimitMiddleware(socket: Socket, next: (err?: Error) => void): void {
  const userId = (socket.data as any)?.user?.id || (socket.data as any)?.user?.sub || socket.id;

  // Wrap socket.use to intercept all events
  socket.use((packet, nextEvent) => {
    const eventName = packet[0] as string;
    const correlationId = (packet[1] as any)?.correlationId || generateCorrelationId();

    // Skip for heartbeat which is frequent but should still be limited gently
    if (eventName === 'heartbeat') {
      if (!globalRateLimiter.isAllowed(`hb:${userId}`, 60, 60_000)) {
        logger.warn({ userId, socketId: socket.id }, 'Heartbeat rate limited');
        return nextEvent(new Error(JSON.stringify({
          code: ErrorCodes.RATE_LIMIT_EXCEEDED,
          message: 'Heartbeat rate limited',
          correlationId,
        })));
      }
      return nextEvent();
    }

    // Global per-socket limit
    if (!globalRateLimiter.isAllowed(`global:${socket.id}`, env.RATE_LIMIT_GLOBAL_MAX, env.RATE_LIMIT_GLOBAL_WINDOW_MS)) {
      return nextEvent(new Error(JSON.stringify({
        code: ErrorCodes.RATE_LIMIT_EXCEEDED,
        message: 'Global rate limit exceeded',
        correlationId,
      })));
    }

    // Call-specific limits
    if (eventName.startsWith('call:') && eventName !== 'call:media_state') {
      if (!callRateLimiter.isAllowed(`call:${userId}`, env.CALL_RATE_LIMIT_MAX, env.CALL_RATE_LIMIT_WINDOW_MS)) {
        return nextEvent(new Error(JSON.stringify({
          code: ErrorCodes.CALL_SPAM_BLOCKED,
          message: 'Call rate limit exceeded',
          correlationId,
        })));
      }
    }

    // Signaling limits (offer/answer/ice)
    if (eventName.startsWith('webrtc:')) {
      if (!signalRateLimiter.isAllowed(`signal:${userId}`, env.SIGNAL_RATE_LIMIT_MAX, env.SIGNAL_RATE_LIMIT_WINDOW_MS)) {
        return nextEvent(new Error(JSON.stringify({
          code: ErrorCodes.RATE_LIMIT_EXCEEDED,
          message: 'Signaling rate limit exceeded',
          correlationId,
        })));
      }
    }

    // Abuse detection for message spam
    if (eventName === 'lobby:message' || eventName === 'chat:message' || eventName === 'live:comment') {
      const threshold = eventName === 'lobby:message' ? env.SPAM_THRESHOLD_MESSAGES_PER_MIN : 60;
      if (!abuseLimiter.isAllowed(`msg:${userId}`, threshold, 60_000)) {
        return nextEvent(new Error(JSON.stringify({
          code: ErrorCodes.SPAM_DETECTED,
          message: 'Message spam detected',
          correlationId,
        })));
      }
    }

    return nextEvent();
  });

  next();
}

export function cleanupRateLimiters(socketId: string, userId: string): void {
  globalRateLimiter.remove(`global:${socketId}`);
  globalRateLimiter.remove(`hb:${userId}`);
  callRateLimiter.remove(`call:${userId}`);
  signalRateLimiter.remove(`signal:${userId}`);
  abuseLimiter.remove(`msg:${userId}`);
}
