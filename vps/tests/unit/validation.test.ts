import { describe, it, expect } from 'vitest';
import { z } from 'zod';
import { validatePayload, sdpSchema } from '../../src/core/middleware/validation.js';
import { callInitiateSchema } from '../../src/modules/calls/validators.js';
import { CallType } from '../../src/shared/types/enums.js';

describe('Validation', () => {
  it('should validate SDP schema', () => {
    const valid = { type: 'offer' as const, sdp: 'v=0\r\no=- 123 1 IN IP4 127.0.0.1\r\n' };
    const result = sdpSchema.safeParse(valid);
    expect(result.success).toBe(true);
  });

  it('should reject invalid SDP', () => {
    const invalid = { type: 'invalid' as any, sdp: 'short' };
    const result = sdpSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should validate call initiate', () => {
    const payload = {
      targetUserId: 'user_123',
      callType: CallType.VOICE,
      correlationId: 'corr_test',
    };
    const data = validatePayload(callInitiateSchema, payload, 'corr_test');
    expect(data.targetUserId).toBe('user_123');
    expect(data.callType).toBe(CallType.VOICE);
  });

  it('should reject call without target', () => {
    const payload = { callType: CallType.VOICE };
    expect(() => validatePayload(callInitiateSchema, payload, 'corr')).toThrow();
  });
});
