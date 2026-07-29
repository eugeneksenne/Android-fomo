/**
 * Presence throttling - battery efficient & prevents spam
 * Token bucket style with per-user cooldown
 */
import { env } from '../../config/env.js';
import { logger } from '../../core/logger/logger.js';
import { metricsCollector } from '../../core/monitoring/metrics.js';

interface ThrottleEntry {
  lastUpdate: number;
  pendingUpdate?: NodeJS.Timeout;
  lastStatus: string;
}

class PresenceThrottler {
  private entries = new Map<string, ThrottleEntry>();
  private throttledCount = 0;

  canUpdate(userId: string, newStatus: string): boolean {
    const entry = this.entries.get(userId);
    const now = Date.now();
    if (!entry) return true;

    // If same status as last, throttle more aggressively
    if (entry.lastStatus === newStatus) {
      if (now - entry.lastUpdate < env.PRESENCE_THROTTLE_MS * 2) {
        this.throttledCount++;
        metricsCollector.recordError('PRESENCE_THROTTLED');
        return false;
      }
    }

    // General throttle window
    if (now - entry.lastUpdate < env.PRESENCE_THROTTLE_MS) {
      return false;
    }

    return true;
  }

  recordUpdate(userId: string, status: string): void {
    const existing = this.entries.get(userId);
    if (existing?.pendingUpdate) clearTimeout(existing.pendingUpdate);

    this.entries.set(userId, {
      lastUpdate: Date.now(),
      lastStatus: status,
    });
  }

  // Debounced update - if throttled, schedule later
  scheduleUpdate(userId: string, status: string, callback: () => void): boolean {
    if (this.canUpdate(userId, status)) {
      this.recordUpdate(userId, status);
      callback();
      return true;
    }

    // Schedule pending update if not already scheduled
    const entry = this.entries.get(userId);
    if (entry && !entry.pendingUpdate) {
      const delay = env.PRESENCE_THROTTLE_MS - (Date.now() - entry.lastUpdate);
      entry.pendingUpdate = setTimeout(() => {
        const e = this.entries.get(userId);
        if (e) {
          e.pendingUpdate = undefined;
          e.lastUpdate = Date.now();
          e.lastStatus = status;
        }
        callback();
      }, Math.max(delay, 100));
      logger.debug({ userId, status, delay }, 'Presence update scheduled (throttled)');
    }

    return false;
  }

  getThrottledCount(): number {
    return this.throttledCount;
  }

  cleanup(): void {
    const now = Date.now();
    for (const [userId, entry] of this.entries.entries()) {
      if (now - entry.lastUpdate > 5 * 60 * 1000) {
        if (entry.pendingUpdate) clearTimeout(entry.pendingUpdate);
        this.entries.delete(userId);
      }
    }
  }
}

export const presenceThrottler = new PresenceThrottler();

// Cleanup every 5 min
setInterval(() => presenceThrottler.cleanup(), 5 * 60 * 1000);
