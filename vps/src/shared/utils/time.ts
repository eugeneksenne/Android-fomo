export function nowIso(): string {
  return new Date().toISOString();
}

export function nowMs(): number {
  return Date.now();
}

export function elapsedMs(start: number): number {
  return Date.now() - start;
}

export function exponentialBackoff(attempt: number, baseMs = 1000, maxMs = 30000, jitter = true): number {
  const exp = Math.min(baseMs * Math.pow(2, attempt), maxMs);
  if (!jitter) return exp;
  const jitterMs = Math.random() * baseMs;
  return exp + jitterMs;
}

export function isExpired(timestampMs: number, ttlMs: number): boolean {
  return Date.now() - timestampMs > ttlMs;
}
