import { generateCorrelationId } from './id.js';

/**
 * Replay protection cache - LRU-like with TTL using Map
 * Prevents same correlationId/event replay within window
 */
export class ReplayCache {
  private cache = new Map<string, number>();
  private maxSize: number;
  private windowMs: number;
  private cleanupInterval: NodeJS.Timeout;

  constructor(maxSize = 10000, windowMs = 300_000) {
    this.maxSize = maxSize;
    this.windowMs = windowMs;
    this.cleanupInterval = setInterval(() => this.cleanup(), 60_000);
  }

  has(id: string): boolean {
    const ts = this.cache.get(id);
    if (!ts) return false;
    if (Date.now() - ts > this.windowMs) {
      this.cache.delete(id);
      return false;
    }
    return true;
  }

  add(id: string): void {
    if (this.cache.size >= this.maxSize) {
      // Evict oldest
      const oldest = this.cache.keys().next().value;
      if (oldest) this.cache.delete(oldest);
    }
    this.cache.set(id, Date.now());
  }

  checkAndAdd(id: string): boolean {
    if (!id) return false;
    if (this.has(id)) return true; // replay detected
    this.add(id);
    return false;
  }

  private cleanup(): void {
    const now = Date.now();
    for (const [k, v] of this.cache.entries()) {
      if (now - v > this.windowMs) this.cache.delete(k);
    }
  }

  stop(): void {
    clearInterval(this.cleanupInterval);
  }
}

export function ensureCorrelationId(provided?: string): string {
  return provided && typeof provided === 'string' && provided.length > 5 ? provided : generateCorrelationId();
}
