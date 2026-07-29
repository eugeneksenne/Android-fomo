import { describe, it, expect, beforeEach, vi } from 'vitest';
import { CallType, CallState } from '../../src/shared/types/enums.js';

vi.mock('../../src/core/redis/client.js', () => ({
  getRedisClient: vi.fn().mockResolvedValue(null),
  getRedisKey: (k: string, ...parts: string[]) => `${k}:${parts.join(':')}`,
}));

vi.mock('../../src/core/logger/logger.js', () => ({
  logger: { info: vi.fn(), warn: vi.fn(), debug: vi.fn(), error: vi.fn() },
  logCall: vi.fn(),
}));

vi.mock('../../src/core/monitoring/metrics.js', () => ({
  metricsCollector: { incActiveCall: vi.fn(), decActiveCall: vi.fn(), recordError: vi.fn() },
}));

describe('CallEngine', () => {
  let callEngine: any;

  beforeEach(async () => {
    vi.resetModules();
    const mod = await import('../../src/modules/calls/callEngine.js');
    callEngine = mod.callEngine;
    // Clear internal state - recreate map via direct access if needed
    (callEngine as any).calls?.clear?.();
    (callEngine as any).userCalls?.clear?.();
  });

  it('should create a voice call', () => {
    const call = callEngine.createCall({
      callType: CallType.VOICE,
      createdBy: 'user_a',
      targetUserId: 'user_b',
      correlationId: 'test_corr_1',
    });

    expect(call).toBeDefined();
    expect(call.type).toBe(CallType.VOICE);
    expect(call.state).toBe(CallState.RINGING);
    expect(call.participants.has('user_a')).toBe(true);
  });

  it('should reject when caller already in call', () => {
    callEngine.createCall({
      callType: CallType.VOICE,
      createdBy: 'user_a',
      targetUserId: 'user_b',
      correlationId: 'corr_1',
    });

    expect(() => {
      callEngine.createCall({
        callType: CallType.VOICE,
        createdBy: 'user_a',
        targetUserId: 'user_c',
        correlationId: 'corr_2',
      });
    }).toThrow();
  });

  it('should accept a call', () => {
    const call = callEngine.createCall({
      callType: CallType.VOICE,
      createdBy: 'user_a',
      targetUserId: 'user_b',
      correlationId: 'corr_accept',
    });

    const accepted = callEngine.acceptCall(call.id, 'user_b');
    expect(accepted.state).toBe(CallState.ACCEPTED);
    expect(accepted.participants.has('user_b')).toBe(true);
  });

  it('should end a call', () => {
    const call = callEngine.createCall({
      callType: CallType.VIDEO,
      createdBy: 'user_a',
      targetUserId: 'user_b',
      correlationId: 'corr_end',
    });

    callEngine.acceptCall(call.id, 'user_b');
    const ended = callEngine.endCall(call.id, 'user_a', 'TEST_END');
    expect(ended.state).toBe(CallState.ENDED);
  });

  it('should update media state', () => {
    const call = callEngine.createCall({
      callType: CallType.VIDEO,
      createdBy: 'user_a',
      targetUserId: 'user_b',
      correlationId: 'corr_media',
    });

    const updated = callEngine.updateMediaState(call.id, 'user_a', { isMuted: true, isCameraOn: false });
    const participant = updated.participants.get('user_a');
    expect(participant?.isMuted).toBe(true);
    expect(participant?.isCameraOn).toBe(false);
  });
});
