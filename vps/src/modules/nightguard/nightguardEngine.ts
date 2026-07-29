import { NightGuardEventType, ParticipantRole, RoomType } from '../../shared/types/enums.js';
import { generateRoomId } from '../../shared/utils/id.js';
import { logger } from '../../core/logger/logger.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { LocationData } from '../../shared/types/socket.js';
import { getRedisClient, getRedisKey } from '../../core/redis/client.js';
import { env } from '../../config/env.js';

export interface NightGuardSession {
  id: string;
  type: 'WALK_ME_HOME' | 'BUDDY_PAIR' | 'NIGHTGUARD' | 'EMERGENCY';
  createdBy: string;
  createdAt: number;
  lastActivityAt: number;
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'SOS_TRIGGERED' | 'ARRIVED';
  trustedContactIds: string[];
  participants: Map<string, { userId: string; role: ParticipantRole; joinedAt: number; socketId?: string; displayName?: string }>;
  destination?: { name?: string; latitude: number; longitude: number };
  lastLocation?: LocationData;
  battery?: number;
  etaSeconds?: number;
  metadata: Record<string, unknown>;
  sosTriggeredAt?: number;
  sosTriggerType?: string;
}

class NightGuardEngine {
  private sessions = new Map<string, NightGuardSession>();
  private userSessions = new Map<string, Set<string>>();

  createSession(params: {
    type: NightGuardSession['type'];
    createdBy: string;
    trustedContactIds: string[];
    destination?: { name?: string; latitude: number; longitude: number };
    metadata?: Record<string, unknown>;
    durationMinutes?: number;
  }): NightGuardSession {
    const id = generateRoomId('nightguard', params.type.toLowerCase());

    if (this.getUserActiveSessions(params.createdBy).length > 0) {
      throw new AppError({ code: ErrorCodes.NIGHTGUARD_ALREADY_ACTIVE, correlationId: generateCorrelationId(), message: 'User already has active NightGuard session' });
    }

    const session: NightGuardSession = {
      id,
      type: params.type,
      createdBy: params.createdBy,
      createdAt: Date.now(),
      lastActivityAt: Date.now(),
      status: 'ACTIVE',
      trustedContactIds: params.trustedContactIds,
      participants: new Map(),
      destination: params.destination,
      metadata: params.metadata || {},
    };

    session.participants.set(params.createdBy, { userId: params.createdBy, role: ParticipantRole.OWNER, joinedAt: Date.now() });

    this.sessions.set(id, session);
    this.trackUserSession(params.createdBy, id);
    for (const tid of params.trustedContactIds) this.trackUserSession(tid, id);

    logger.info({ sessionId: id, type: params.type, createdBy: params.createdBy, trustedCount: params.trustedContactIds.length }, 'NightGuard session created');

    this.persist(session).catch(() => {});

    return session;
  }

  getSession(sessionId: string): NightGuardSession | null {
    return this.sessions.get(sessionId) || null;
  }

  getUserActiveSessions(userId: string): NightGuardSession[] {
    const ids = this.userSessions.get(userId);
    if (!ids) return [];
    return Array.from(ids).map(id => this.sessions.get(id)).filter(s => s && s.status === 'ACTIVE') as NightGuardSession[];
  }

  addParticipant(sessionId: string, userId: string, role: ParticipantRole = ParticipantRole.GUARDIAN, socketId?: string, displayName?: string): NightGuardSession {
    const session = this.sessions.get(sessionId);
    if (!session) throw new AppError({ code: ErrorCodes.NIGHTGUARD_SESSION_NOT_FOUND, correlationId: generateCorrelationId() });

    const isTrusted = session.trustedContactIds.includes(userId) || session.createdBy === userId;
    if (!isTrusted) throw new AppError({ code: ErrorCodes.NIGHTGUARD_NOT_AUTHORIZED, correlationId: generateCorrelationId() });

    session.participants.set(userId, { userId, role, joinedAt: Date.now(), socketId, displayName });
    session.lastActivityAt = Date.now();
    this.trackUserSession(userId, sessionId);
    this.persist(session).catch(() => {});
    return session;
  }

  removeParticipant(sessionId: string, userId: string): NightGuardSession | null {
    const session = this.sessions.get(sessionId);
    if (!session) return null;
    session.participants.delete(userId);
    this.userSessions.get(userId)?.delete(sessionId);
    session.lastActivityAt = Date.now();
    this.persist(session).catch(() => {});
    return session;
  }

  updateLocation(sessionId: string, userId: string, location: LocationData, extra?: { etaSeconds?: number; battery?: number }): NightGuardSession {
    const session = this.sessions.get(sessionId);
    if (!session) throw new AppError({ code: ErrorCodes.NIGHTGUARD_SESSION_NOT_FOUND, correlationId: generateCorrelationId() });
    if (!session.participants.has(userId)) throw new AppError({ code: ErrorCodes.NIGHTGUARD_NOT_AUTHORIZED, correlationId: generateCorrelationId() });

    session.lastLocation = location;
    session.lastActivityAt = Date.now();
    if (extra?.etaSeconds !== undefined) session.etaSeconds = extra.etaSeconds;
    if (extra?.battery !== undefined) session.battery = extra.battery;

    this.persist(session).catch(() => {});
    return session;
  }

  updateStatus(sessionId: string, userId: string, status: NightGuardSession['status'], note?: string): NightGuardSession {
    const session = this.sessions.get(sessionId);
    if (!session) throw new AppError({ code: ErrorCodes.NIGHTGUARD_SESSION_NOT_FOUND, correlationId: generateCorrelationId() });
    if (!session.participants.has(userId)) throw new AppError({ code: ErrorCodes.NIGHTGUARD_NOT_AUTHORIZED, correlationId: generateCorrelationId() });

    session.status = status;
    session.lastActivityAt = Date.now();
    if (note) session.metadata.lastNote = note;

    this.persist(session).catch(() => {});

    if (status === 'COMPLETED' || status === 'CANCELLED' || status === 'ARRIVED') {
      setTimeout(() => this.cleanupSession(sessionId), 60_000);
    }

    logger.info({ sessionId, userId, status }, `NightGuard status ${status}`);
    return session;
  }

  triggerSos(sessionId: string | undefined, userId: string, location: LocationData, triggerType: string, message?: string, contactsToAlert?: string[]): NightGuardSession | null {
    let session: NightGuardSession | null = null;
    if (sessionId) session = this.sessions.get(sessionId) || null;

    if (!session) {
      // Create emergency session on the fly
      session = this.createSession({ type: 'EMERGENCY', createdBy: userId, trustedContactIds: contactsToAlert || [], metadata: { triggerType, message } });
    }

    session.status = 'SOS_TRIGGERED';
    session.sosTriggeredAt = Date.now();
    session.sosTriggerType = triggerType;
    session.lastLocation = location;
    session.lastActivityAt = Date.now();
    session.metadata.sosMessage = message;

    logger.warn({ sessionId: session.id, userId, triggerType, location }, '🚨 SOS TRIGGERED');

    this.persist(session).catch(() => {});
    return session;
  }

  private cleanupSession(sessionId: string): void {
    const session = this.sessions.get(sessionId);
    if (!session) return;
    for (const pid of session.participants.keys()) this.userSessions.get(pid)?.delete(sessionId);
    for (const tid of session.trustedContactIds) this.userSessions.get(tid)?.delete(sessionId);
    this.sessions.delete(sessionId);
    this.removeFromRedis(sessionId).catch(() => {});
    logger.info({ sessionId }, 'NightGuard session cleaned');
  }

  private trackUserSession(userId: string, sessionId: string): void {
    if (!this.userSessions.has(userId)) this.userSessions.set(userId, new Set());
    this.userSessions.get(userId)!.add(sessionId);
  }

  private async persist(session: NightGuardSession): Promise<void> {
    if (!env.REDIS_ENABLED) return;
    try {
      const rc = await getRedisClient();
      if (!rc) return;
      await rc.set(getRedisKey('nightguard', session.id), JSON.stringify({ ...session, participants: Array.from(session.participants.entries()) }), { EX: 3600 * 6 });
    } catch {}
  }

  private async removeFromRedis(sessionId: string): Promise<void> {
    if (!env.REDIS_ENABLED) return;
    try {
      const rc = await getRedisClient();
      if (!rc) return;
      await rc.del(getRedisKey('nightguard', sessionId));
    } catch {}
  }
}

export const nightguardEngine = new NightGuardEngine();
