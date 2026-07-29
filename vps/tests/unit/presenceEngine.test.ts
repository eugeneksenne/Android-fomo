import { describe, it, expect, beforeEach, vi } from 'vitest';
import { PresenceStatus } from '../../src/shared/types/enums.js';

// Mock dependencies
vi.mock('../../src/core/redis/client.js', () => ({
  getRedisClient: vi.fn().mockResolvedValue(null),
  getRedisKey: (k: string, ...parts: string[]) => `${k}:${parts.join(':')}`,
}));

vi.mock('../../src/modules/socket/connectionManager.js', () => ({
  connectionManager: {
    getUserConnections: vi.fn().mockReturnValue([{ connectionQuality: 'EXCELLENT' }]),
    getUserConnectionCount: vi.fn().mockReturnValue(1),
  },
}));

describe('PresenceEngine', () => {
  let presenceEngine: any;

  beforeEach(async () => {
    vi.resetModules();
    const mod = await import('../../src/modules/presence/presenceEngine.js');
    presenceEngine = mod.presenceEngine;
  });

  it('should update presence to ONLINE', async () => {
    const data = await presenceEngine.updatePresence({
      userId: 'user_123',
      status: PresenceStatus.ONLINE,
      displayName: 'Test User',
    });
    expect(data.userId).toBe('user_123');
    expect(data.status).toBe(PresenceStatus.ONLINE);
    expect(data.isOnline).toBe(true);
  });

  it('should set presence offline', async () => {
    await presenceEngine.updatePresence({
      userId: 'user_123',
      status: PresenceStatus.ONLINE,
    });
    const offline = await presenceEngine.setOffline('user_123');
    expect(offline?.isOnline).toBe(false);
    expect(offline?.status).toBe(PresenceStatus.OFFLINE);
  });

  it('should get online users', async () => {
    await presenceEngine.updatePresence({ userId: 'u1', status: PresenceStatus.ONLINE });
    await presenceEngine.updatePresence({ userId: 'u2', status: PresenceStatus.AWAY });
    const online = await presenceEngine.getOnlineUsers(10);
    expect(online.length).toBeGreaterThan(0);
  });
});
