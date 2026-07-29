import { logger } from '../../core/logger/logger.js';
import { getRedisClient, getRedisKey } from '../../core/redis/client.js';
import { DeviceType } from '../../shared/types/enums.js';
import { env } from '../../config/env.js';

export interface UserSession {
  sessionId: string;
  userId: string;
  deviceId: string;
  deviceType: DeviceType;
  socketIds: string[];
  createdAt: number;
  lastActiveAt: number;
  ip?: string;
  isRevoked: boolean;
  correlationId: string;
}

class SessionService {
  private localSessions = new Map<string, UserSession>(); // sessionId -> session
  private userSessions = new Map<string, Set<string>>(); // userId -> Set<sessionId>

  async createSession(session: UserSession): Promise<void> {
    this.localSessions.set(session.sessionId, session);
    if (!this.userSessions.has(session.userId)) this.userSessions.set(session.userId, new Set());
    this.userSessions.get(session.userId)!.add(session.sessionId);

    // Persist to Redis if enabled for horizontal scaling
    if (env.REDIS_ENABLED) {
      try {
        const rc = await getRedisClient();
        if (rc) {
          const key = getRedisKey('session', session.sessionId);
          await rc.set(key, JSON.stringify(session), { EX: 24 * 60 * 60 }); // 24h TTL
          const userKey = getRedisKey('user_sessions', session.userId);
          await rc.sAdd(userKey, session.sessionId);
          await rc.expire(userKey, 24 * 60 * 60);
        }
      } catch (err) {
        logger.warn({ err: (err as Error).message, sessionId: session.sessionId }, 'Failed to persist session to Redis');
      }
    }

    logger.debug({ sessionId: session.sessionId, userId: session.userId }, 'Session created');
  }

  async getSession(sessionId: string): Promise<UserSession | null> {
    const local = this.localSessions.get(sessionId);
    if (local) return local;

    if (env.REDIS_ENABLED) {
      try {
        const rc = await getRedisClient();
        if (rc) {
          const key = getRedisKey('session', sessionId);
          const data = await rc.get(key);
          if (data) {
            const parsed = JSON.parse(data) as UserSession;
            this.localSessions.set(sessionId, parsed);
            return parsed;
          }
        }
      } catch (err) {
        logger.warn({ err: (err as Error).message, sessionId }, 'Failed to get session from Redis');
      }
    }
    return null;
  }

  async updateActivity(sessionId: string): Promise<void> {
    const session = this.localSessions.get(sessionId);
    if (session) {
      session.lastActiveAt = Date.now();
    }
    if (env.REDIS_ENABLED) {
      try {
        const rc = await getRedisClient();
        if (rc) {
          const key = getRedisKey('session', sessionId);
          const existing = await rc.get(key);
          if (existing) {
            const parsed = JSON.parse(existing) as UserSession;
            parsed.lastActiveAt = Date.now();
            await rc.set(key, JSON.stringify(parsed), { EX: 24 * 60 * 60 });
          }
        }
      } catch {}
    }
  }

  async addSocketToSession(sessionId: string, socketId: string): Promise<void> {
    const session = await this.getSession(sessionId);
    if (session && !session.socketIds.includes(socketId)) {
      session.socketIds.push(socketId);
      await this.createSession(session); // re-persist
    }
  }

  async removeSocketFromSession(sessionId: string, socketId: string): Promise<UserSession | null> {
    const session = await this.getSession(sessionId);
    if (!session) return null;
    session.socketIds = session.socketIds.filter(id => id !== socketId);
    session.lastActiveAt = Date.now();
    await this.createSession(session);
    if (session.socketIds.length === 0) {
      // Keep session for recovery window
      logger.debug({ sessionId, userId: session.userId }, 'Session has no sockets, keeping for recovery');
    }
    return session;
  }

  async revokeSession(sessionId: string): Promise<void> {
    const session = await this.getSession(sessionId);
    if (session) {
      session.isRevoked = true;
      await this.createSession(session);
      this.localSessions.delete(sessionId);
      this.userSessions.get(session.userId)?.delete(sessionId);
      if (env.REDIS_ENABLED) {
        try {
          const rc = await getRedisClient();
          if (rc) {
            await rc.del(getRedisKey('session', sessionId));
            await rc.sRem(getRedisKey('user_sessions', session.userId), sessionId);
          }
        } catch {}
      }
      logger.info({ sessionId, userId: session.userId }, 'Session revoked');
    }
  }

  async revokeAllUserSessions(userId: string, exceptSessionId?: string): Promise<void> {
    const sessionIds = this.userSessions.get(userId);
    if (!sessionIds) return;
    for (const sid of Array.from(sessionIds)) {
      if (exceptSessionId && sid === exceptSessionId) continue;
      await this.revokeSession(sid);
    }
  }

  getUserSessionIds(userId: string): string[] {
    return Array.from(this.userSessions.get(userId) || []);
  }

  async cleanupExpired(ttlMs = 24 * 60 * 60 * 1000): Promise<number> {
    let cleaned = 0;
    const now = Date.now();
    for (const [sid, sess] of this.localSessions.entries()) {
      if (now - sess.lastActiveAt > ttlMs) {
        this.localSessions.delete(sid);
        this.userSessions.get(sess.userId)?.delete(sid);
        cleaned++;
        if (env.REDIS_ENABLED) {
          try {
            const rc = await getRedisClient();
            if (rc) await rc.del(getRedisKey('session', sid));
          } catch {}
        }
      }
    }
    if (cleaned > 0) logger.info({ cleaned }, 'Expired sessions cleaned');
    return cleaned;
  }
}

export const sessionService = new SessionService();
