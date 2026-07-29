import { PresenceStatus, ConnectionQuality } from '../../shared/types/enums.js';
import { PresenceData } from '../../shared/types/socket.js';
import { presenceStore } from './presenceStore.js';
import { presenceThrottler } from './presenceThrottler.js';
import { logger } from '../../core/logger/logger.js';
import { connectionManager } from '../socket/connectionManager.js';
import { metricsCollector } from '../../core/monitoring/metrics.js';
import { env } from '../../config/env.js';

class PresenceEngine {
  async updatePresence(params: {
    userId: string;
    status: PresenceStatus;
    customMessage?: string;
    venueId?: string;
    displayName?: string;
    photoUrl?: string;
    deviceId?: string;
  }): Promise<PresenceData> {
    const { userId, status, customMessage, venueId, displayName, photoUrl, deviceId } = params;

    // Validate status
    if (!Object.values(PresenceStatus).includes(status)) {
      throw new Error(`Invalid presence status: ${status}`);
    }

    const now = Date.now();
    const conns = connectionManager.getUserConnections(userId);
    const connectionQuality = conns.length > 0 ? conns[0]!.connectionQuality : ConnectionQuality.DISCONNECTED;
    const activeSessions = conns.length;

    const data: PresenceData = {
      userId,
      status,
      lastSeen: new Date().toISOString(),
      lastSeenMs: now,
      deviceId,
      venueId,
      customMessage,
      isOnline: status !== PresenceStatus.OFFLINE,
      activeSessions,
      connectionQuality,
      displayName,
      photoUrl,
    };

    // Throttle check
    if (!presenceThrottler.canUpdate(userId, status)) {
      logger.debug({ userId, status }, 'Presence throttled');
      metricsCollector.recordError('PRESENCE_THROTTLED');
      // Still update store but don't broadcast? For battery efficiency we throttle broadcast
      // Return existing presence
      const existing = await presenceStore.getPresence(userId);
      return existing || data;
    }

    await presenceStore.setPresence(data);
    presenceThrottler.recordUpdate(userId, status);

    logger.info({ userId, status, venueId, deviceId }, `Presence update ${userId} -> ${status}`);

    return data;
  }

  async getPresence(userId: string): Promise<PresenceData | null> {
    return presenceStore.getPresence(userId);
  }

  async getManyPresence(userIds: string[]): Promise<PresenceData[]> {
    return presenceStore.getMany(userIds);
  }

  async getOnlineUsers(limit = 100): Promise<PresenceData[]> {
    return presenceStore.getAllOnline(limit);
  }

  async setOffline(userId: string): Promise<PresenceData | null> {
    const data = await presenceStore.setOffline(userId);
    logger.info({ userId }, `User set offline ${userId}`);
    return data;
  }

  async handleHeartbeat(userId: string, deviceId?: string, status?: PresenceStatus, battery?: number): Promise<PresenceData | null> {
    const existing = await presenceStore.getPresence(userId);
    if (!existing) {
      // Create new online presence if not exists
      return this.updatePresence({
        userId,
        status: status || PresenceStatus.ONLINE,
        deviceId,
      });
    }

    existing.lastSeen = new Date().toISOString();
    existing.lastSeenMs = Date.now();
    if (deviceId) existing.deviceId = deviceId;
    if (status && Object.values(PresenceStatus).includes(status)) {
      // Don't throttle heartbeat status unless changing
      if (existing.status !== status) {
        existing.status = status;
      }
    }
    // Update connection quality from manager
    const conns = connectionManager.getUserConnections(userId);
    if (conns.length > 0) {
      existing.connectionQuality = conns[0]!.connectionQuality;
      existing.activeSessions = conns.length;
      existing.isOnline = true;
    }

    await presenceStore.setPresence(existing);
    return existing;
  }

  // For friends engine
  async getFriendsPresence(friendIds: string[]): Promise<PresenceData[]> {
    return this.getManyPresence(friendIds);
  }

  async cleanup(): Promise<number> {
    const now = Date.now();
    let cleaned = 0;
    const all = await presenceStore.getAllOnline(10000);
    for (const p of all) {
      if (now - p.lastSeenMs > env.PRESENCE_OFFLINE_TIMEOUT_MS * 3) {
        if (connectionManager.getUserConnectionCount(p.userId) === 0) {
          await presenceStore.setOffline(p.userId);
          cleaned++;
        }
      }
    }
    return cleaned;
  }
}

export const presenceEngine = new PresenceEngine();
