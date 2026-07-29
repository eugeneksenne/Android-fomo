import { LiveLocationEventType } from '../../shared/types/enums.js';
import { LocationData } from '../../shared/types/socket.js';
import { generateRoomId } from '../../shared/utils/id.js';
import { logger } from '../../core/logger/logger.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { getRedisClient, getRedisKey } from '../../core/redis/client.js';
import { env } from '../../config/env.js';

export interface LiveLocationSession {
  id: string;
  ownerId: string;
  createdAt: number;
  lastActivityAt: number;
  status: 'ACTIVE' | 'PAUSED' | 'ENDED';
  trustedContactIds: string[];
  participants: Map<string, { userId: string; joinedAt: number; socketId?: string }>;
  tripName?: string;
  destination?: { latitude: number; longitude: number; name?: string };
  lastLocation?: LocationData;
  locations: LocationData[]; // could be large, we cap
  battery?: number;
  etaSeconds?: number;
  metadata: Record<string, unknown>;
}

class LiveLocationEngine {
  private sessions = new Map<string, LiveLocationSession>();
  private userSessions = new Map<string, Set<string>>();

  createSession(params: {
    ownerId: string;
    trustedContactIds?: string[];
    tripName?: string;
    destination?: { latitude: number; longitude: number; name?: string };
    metadata?: Record<string, unknown>;
  }): LiveLocationSession {
    const id = generateRoomId('live_location', 'trip');
    const session: LiveLocationSession = {
      id,
      ownerId: params.ownerId,
      createdAt: Date.now(),
      lastActivityAt: Date.now(),
      status: 'ACTIVE',
      trustedContactIds: params.trustedContactIds || [],
      participants: new Map(),
      tripName: params.tripName,
      destination: params.destination,
      locations: [],
      metadata: params.metadata || {},
    };

    session.participants.set(params.ownerId, { userId: params.ownerId, joinedAt: Date.now() });

    this.sessions.set(id, session);
    this.trackUserSession(params.ownerId, id);
    for (const tid of session.trustedContactIds) this.trackUserSession(tid, id);

    logger.info({ sessionId: id, ownerId: params.ownerId }, 'Live location session started');

    this.persist(session).catch(() => {});
    return session;
  }

  getSession(sessionId: string): LiveLocationSession | null {
    return this.sessions.get(sessionId) || null;
  }

  updateLocation(sessionId: string, userId: string, location: LocationData, eventType: LiveLocationEventType, extra?: { battery?: number; etaSeconds?: number; accuracy?: number; isMoving?: boolean }): LiveLocationSession {
    const session = this.sessions.get(sessionId);
    if (!session) throw new AppError({ code: ErrorCodes.LIVE_LOCATION_SESSION_NOT_FOUND, correlationId: generateCorrelationId() });
    if (session.ownerId !== userId && !session.participants.has(userId)) throw new AppError({ code: ErrorCodes.LIVE_LOCATION_NOT_AUTHORIZED, correlationId: generateCorrelationId() });
    if (session.status === 'ENDED') throw new AppError({ code: ErrorCodes.LIVE_LOCATION_SESSION_NOT_FOUND, correlationId: generateCorrelationId(), message: 'Session ended' });

    session.lastLocation = location;
    session.lastActivityAt = Date.now();
    session.locations.push(location);
    if (session.locations.length > 1000) session.locations.shift(); // cap

    if (extra?.battery !== undefined) session.battery = extra.battery;
    if (extra?.etaSeconds !== undefined) session.etaSeconds = extra.etaSeconds;
    if (extra?.accuracy !== undefined) session.lastLocation.accuracy = extra.accuracy;

    this.persist(session).catch(() => {});
    return session;
  }

  pauseSession(sessionId: string, userId: string): LiveLocationSession {
    const session = this.sessions.get(sessionId);
    if (!session) throw new AppError({ code: ErrorCodes.LIVE_LOCATION_SESSION_NOT_FOUND, correlationId: generateCorrelationId() });
    if (session.ownerId !== userId) throw new AppError({ code: ErrorCodes.LIVE_LOCATION_NOT_AUTHORIZED, correlationId: generateCorrelationId() });

    session.status = 'PAUSED';
    session.lastActivityAt = Date.now();
    this.persist(session).catch(() => {});
    return session;
  }

  resumeSession(sessionId: string, userId: string): LiveLocationSession {
    const session = this.sessions.get(sessionId);
    if (!session) throw new AppError({ code: ErrorCodes.LIVE_LOCATION_SESSION_NOT_FOUND, correlationId: generateCorrelationId() });
    if (session.ownerId !== userId) throw new AppError({ code: ErrorCodes.LIVE_LOCATION_NOT_AUTHORIZED, correlationId: generateCorrelationId() });

    session.status = 'ACTIVE';
    session.lastActivityAt = Date.now();
    this.persist(session).catch(() => {});
    return session;
  }

  endSession(sessionId: string, userId: string, reason?: string): LiveLocationSession {
    const session = this.sessions.get(sessionId);
    if (!session) throw new AppError({ code: ErrorCodes.LIVE_LOCATION_SESSION_NOT_FOUND, correlationId: generateCorrelationId() });
    if (session.ownerId !== userId) throw new AppError({ code: ErrorCodes.LIVE_LOCATION_NOT_AUTHORIZED, correlationId: generateCorrelationId() });

    session.status = 'ENDED';
    session.lastActivityAt = Date.now();
    session.metadata.endReason = reason || 'USER_ENDED';

    setTimeout(() => this.cleanup(sessionId), 60_000);

    this.persist(session).catch(() => {});
    logger.info({ sessionId, ownerId: userId, reason }, 'Live location session ended');
    return session;
  }

  addParticipant(sessionId: string, userId: string, socketId?: string): LiveLocationSession {
    const session = this.sessions.get(sessionId);
    if (!session) throw new AppError({ code: ErrorCodes.LIVE_LOCATION_SESSION_NOT_FOUND, correlationId: generateCorrelationId() });
    session.participants.set(userId, { userId, joinedAt: Date.now(), socketId });
    this.trackUserSession(userId, sessionId);
    this.persist(session).catch(() => {});
    return session;
  }

  private cleanup(sessionId: string): void {
    const session = this.sessions.get(sessionId);
    if (!session) return;
    for (const pid of session.participants.keys()) this.userSessions.get(pid)?.delete(sessionId);
    for (const tid of session.trustedContactIds) this.userSessions.get(tid)?.delete(sessionId);
    this.sessions.delete(sessionId);
    this.removeFromRedis(sessionId).catch(() => {});
  }

  private trackUserSession(userId: string, sessionId: string): void {
    if (!this.userSessions.has(userId)) this.userSessions.set(userId, new Set());
    this.userSessions.get(userId)!.add(sessionId);
  }

  private async persist(session: LiveLocationSession): Promise<void> {
    if (!env.REDIS_ENABLED) return;
    try {
      const rc = await getRedisClient();
      if (!rc) return;
      await rc.set(getRedisKey('live_location', session.id), JSON.stringify({ ...session, participants: Array.from(session.participants.entries()) }), { EX: 3600 * 24 });
    } catch {}
  }

  private async removeFromRedis(sessionId: string): Promise<void> {
    if (!env.REDIS_ENABLED) return;
    try {
      const rc = await getRedisClient();
      if (!rc) return;
      await rc.del(getRedisKey('live_location', sessionId));
    } catch {}
  }
}

export const liveLocationEngine = new LiveLocationEngine();
