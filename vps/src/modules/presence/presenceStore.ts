import { PresenceStatus, ConnectionQuality } from '../../shared/types/enums.js';
import { PresenceData } from '../../shared/types/socket.js';
import { getRedisClient, getRedisKey } from '../../core/redis/client.js';
import { env } from '../../config/env.js';
import { logger } from '../../core/logger/logger.js';

class PresenceStore {
  private local = new Map<string, PresenceData>();

  async setPresence(data: PresenceData): Promise<void> {
    this.local.set(data.userId, data);

    if (env.REDIS_ENABLED) {
      try {
        const rc = await getRedisClient();
        if (!rc) return;
        const key = getRedisKey('presence', data.userId);
        await rc.set(key, JSON.stringify(data), { EX: 3600 }); // 1h TTL
        // Add to global presence set for distributed queries
        await rc.sAdd(getRedisKey('presence_set'), data.userId);
      } catch (err) {
        logger.warn({ err: (err as Error).message, userId: data.userId }, 'Failed to persist presence to Redis');
      }
    }
  }

  async getPresence(userId: string): Promise<PresenceData | null> {
    const local = this.local.get(userId);
    if (local) return local;

    if (env.REDIS_ENABLED) {
      try {
        const rc = await getRedisClient();
        if (!rc) return null;
        const key = getRedisKey('presence', userId);
        const data = await rc.get(key);
        if (data) {
          const parsed = JSON.parse(data) as PresenceData;
          this.local.set(userId, parsed);
          return parsed;
        }
      } catch (err) {
        logger.warn({ err: (err as Error).message, userId }, 'Failed to get presence from Redis');
      }
    }
    return null;
  }

  async getMany(userIds: string[]): Promise<PresenceData[]> {
    const results: PresenceData[] = [];
    for (const id of userIds) {
      const p = await this.getPresence(id);
      if (p) results.push(p);
    }
    return results;
  }

  async getAllOnline(limit = 100): Promise<PresenceData[]> {
    // Local fast path
    const online = Array.from(this.local.values()).filter(p => p.isOnline).slice(0, limit);
    if (online.length >= limit) return online;

    if (env.REDIS_ENABLED) {
      try {
        const rc = await getRedisClient();
        if (rc) {
          const members = await rc.sMembers(getRedisKey('presence_set'));
          for (const userId of members) {
            if (resultsLength(online) >= limit) break;
            if (online.find(p => p.userId === userId)) continue;
            const p = await this.getPresence(userId);
            if (p && p.isOnline) online.push(p);
          }
        }
      } catch {}
    }
    return online;
  }

  async removePresence(userId: string): Promise<void> {
    // We don't delete, we mark offline for last seen
    const existing = this.local.get(userId);
    if (existing) {
      existing.isOnline = false;
      existing.status = PresenceStatus.OFFLINE;
      existing.lastSeen = new Date().toISOString();
      existing.lastSeenMs = Date.now();
      await this.setPresence(existing);
    }

    if (env.REDIS_ENABLED) {
      try {
        const rc = await getRedisClient();
        if (rc) {
          await rc.sRem(getRedisKey('presence_set'), userId);
        }
      } catch {}
    }
  }

  async setOffline(userId: string): Promise<PresenceData | null> {
    const existing = await this.getPresence(userId);
    if (!existing) return null;
    existing.isOnline = false;
    existing.status = PresenceStatus.OFFLINE;
    existing.lastSeen = new Date().toISOString();
    existing.lastSeenMs = Date.now();
    await this.setPresence(existing);
    return existing;
  }

  localCount(): number {
    return this.local.size;
  }
}

function resultsLength(arr: any[]): number {
  return arr.length;
}

export const presenceStore = new PresenceStore();
