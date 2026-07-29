import { presenceEngine } from '../presence/presenceEngine.js';
import { PresenceData } from '../../shared/types/socket.js';
import { logger } from '../../core/logger/logger.js';
import { PresenceStatus } from '../../shared/types/enums.js';

class FriendsEngine {
  async getFriendsPresence(friendIds: string[]): Promise<PresenceData[]> {
    return presenceEngine.getManyPresence(friendIds);
  }

  async getOnlineFriends(friendIds: string[]): Promise<PresenceData[]> {
    const presences = await presenceEngine.getManyPresence(friendIds);
    return presences.filter(p => p.isOnline);
  }

  categorizeFriends(presences: PresenceData[]): {
    online: PresenceData[];
    offline: PresenceData[];
    activeNow: PresenceData[];
    inCall: PresenceData[];
    watchingLive: PresenceData[];
    insideVenue: PresenceData[];
    walkingHome: PresenceData[];
  } {
    return {
      online: presences.filter(p => p.isOnline),
      offline: presences.filter(p => !p.isOnline),
      activeNow: presences.filter(p => p.isOnline && Date.now() - p.lastSeenMs < 5 * 60 * 1000),
      inCall: presences.filter(p => p.status === PresenceStatus.IN_CALL),
      watchingLive: presences.filter(p => p.status === PresenceStatus.WATCHING_LIVE),
      insideVenue: presences.filter(p => p.status === PresenceStatus.INSIDE_VENUE),
      walkingHome: presences.filter(p => p.status === PresenceStatus.WALKING_HOME),
    };
  }
}

export const friendsEngine = new FriendsEngine();
