import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import jwt from 'jsonwebtoken';

describe('Supabase JWT Verification (integration logic)', () => {
  const JWT_SECRET = 'test_jwt_secret_32_chars_min_for_fomo_dev';

  it('should verify a valid JWT', () => {
    const payload = {
      sub: 'user_123',
      aud: 'authenticated',
      role: 'authenticated',
      exp: Math.floor(Date.now() / 1000) + 3600,
      iat: Math.floor(Date.now() / 1000),
      email: 'test@example.com',
    };
    const token = jwt.sign(payload, JWT_SECRET, { algorithm: 'HS256' });
    const decoded = jwt.verify(token, JWT_SECRET) as any;
    expect(decoded.sub).toBe('user_123');
    expect(decoded.aud).toBe('authenticated');
  });

  it('should reject expired JWT', () => {
    const payload = {
      sub: 'user_123',
      aud: 'authenticated',
      role: 'authenticated',
      exp: Math.floor(Date.now() / 1000) - 100, // expired
      iat: Math.floor(Date.now() / 1000) - 3700,
    };
    const token = jwt.sign(payload, JWT_SECRET, { algorithm: 'HS256' });
    expect(() => jwt.verify(token, JWT_SECRET)).toThrow();
  });

  it('should reject token with wrong secret', () => {
    const payload = { sub: 'user_123', aud: 'authenticated', exp: Math.floor(Date.now() / 1000) + 3600, role: 'authenticated' };
    const token = jwt.sign(payload, JWT_SECRET, { algorithm: 'HS256' });
    expect(() => jwt.verify(token, 'wrong_secret')).toThrow();
  });

  it('should handle correlationId generation', async () => {
    const { generateCorrelationId } = await import('../../src/shared/utils/id.js');
    const id1 = generateCorrelationId();
    const id2 = generateCorrelationId();
    expect(id1).not.toBe(id2);
    expect(id1.startsWith('corr_')).toBe(true);
  });

  it('should handle replay protection', async () => {
    const { ReplayCache } = await import('../../src/shared/utils/correlation.js');
    const cache = new ReplayCache(10, 1000);
    const cid = 'test_corr_replay';
    expect(cache.checkAndAdd(cid)).toBe(false); // first time, not replay
    expect(cache.checkAndAdd(cid)).toBe(true); // second time, replay detected
    cache.stop();
  });
});
