/**
 * Sanitize & validate string inputs - prevent XSS, injection, overflow
 */

export function sanitizeString(input: unknown, maxLength = 1000): string {
  if (typeof input !== 'string') return '';
  // Trim, remove null bytes, limit length, strip control chars except newline/tab? For chat we keep simple
  return input
    .replace(/\u0000/g, '')
    .replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '')
    .slice(0, maxLength)
    .trim();
}

export function sanitizeUrl(input: unknown): string | null {
  if (typeof input !== 'string') return null;
  try {
    const url = new URL(input);
    if (!['http:', 'https:'].includes(url.protocol)) return null;
    return url.toString().slice(0, 2048);
  } catch {
    return null;
  }
}

export function sanitizeObject<T extends Record<string, unknown>>(obj: T, maxKeys = 50): Partial<T> {
  const entries = Object.entries(obj).slice(0, maxKeys);
  const out: Record<string, unknown> = {};
  for (const [k, v] of entries) {
    const safeKey = sanitizeString(k, 100);
    if (!safeKey) continue;
    if (typeof v === 'string') out[safeKey] = sanitizeString(v, 2000);
    else if (typeof v === 'number' || typeof v === 'boolean') out[safeKey] = v;
    else if (v && typeof v === 'object' && !Array.isArray(v)) {
      out[safeKey] = sanitizeObject(v as Record<string, unknown>, 20);
    }
  }
  return out as Partial<T>;
}

export function isValidLatitude(lat: unknown): boolean {
  return typeof lat === 'number' && lat >= -90 && lat <= 90 && Number.isFinite(lat);
}

export function isValidLongitude(lng: unknown): boolean {
  return typeof lng === 'number' && lng >= -180 && lng <= 180 && Number.isFinite(lng);
}
