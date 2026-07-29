import { ErrorCode, ErrorMessages } from './errorCodes.js';

export interface ErrorMeta {
  correlationId: string;
  userId?: string;
  deviceId?: string;
  roomId?: string;
  event?: string;
  details?: unknown;
  timestamp: string;
}

export class AppError extends Error {
  public readonly code: ErrorCode;
  public readonly correlationId: string;
  public readonly statusCode: number;
  public readonly developerMessage: string;
  public readonly recovery: string;
  public readonly meta?: Omit<ErrorMeta, 'correlationId' | 'timestamp'>;
  public readonly timestamp: string;

  constructor(params: {
    code: ErrorCode;
    correlationId: string;
    statusCode?: number;
    message?: string;
    developerMessage?: string;
    recovery?: string;
    meta?: Omit<ErrorMeta, 'correlationId' | 'timestamp'>;
  }) {
    const humanMessage = params.message || ErrorMessages[params.code] || 'Unknown error';
    super(humanMessage);
    this.name = 'AppError';
    this.code = params.code;
    this.correlationId = params.correlationId;
    this.statusCode = params.statusCode ?? mapCodeToStatus(params.code);
    this.developerMessage = params.developerMessage ?? humanMessage;
    this.recovery = params.recovery ?? mapCodeToRecovery(params.code);
    this.meta = params.meta;
    this.timestamp = new Date().toISOString();
  }

  toJSON() {
    return {
      code: this.code,
      message: this.message,
      developerMessage: this.developerMessage,
      correlationId: this.correlationId,
      recovery: this.recovery,
      timestamp: this.timestamp,
      meta: this.meta,
    };
  }
}

function mapCodeToStatus(code: ErrorCode): number {
  if (code.startsWith('AUTH_')) return 401;
  if (code === 'AUTH_FORBIDDEN' || code.includes('NOT_AUTHORIZED') || code === 'LOBBY_NOT_HOST' || code === 'LOBBY_BANNED') return 403;
  if (code.startsWith('ROOM_NOT_FOUND') || code.endsWith('_NOT_FOUND')) return 404;
  if (code.includes('RATE_LIMIT') || code.includes('THROTTLED') || code.includes('SPAM') || code.includes('FLOOD')) return 429;
  if (code.includes('TIMEOUT') || code === 'TIMEOUT') return 408;
  if (code === 'SERVICE_UNAVAILABLE') return 503;
  if (code.startsWith('VALIDATION') || code.startsWith('INVALID') || code.startsWith('MISSING')) return 400;
  return 400;
}

function mapCodeToRecovery(code: ErrorCode): string {
  const map: Partial<Record<ErrorCode, string>> = {
    AUTH_MISSING_TOKEN: 'Provide a valid Supabase JWT in handshake auth.token',
    AUTH_INVALID_TOKEN: 'Refresh your Supabase session and reconnect with new token',
    AUTH_EXPIRED_TOKEN: 'Refresh token via supabase.auth.refreshSession() and reconnect',
    AUTH_TOKEN_REVOKED: 'Re-authenticate via Supabase login',
    AUTH_DEVICE_MISMATCH: 'Ensure deviceId matches registered device or re-register device',
    AUTH_SESSION_INVALID: 'Re-authenticate and create new session',
    AUTH_TOO_MANY_DEVICES: 'Disconnect other devices or logout from existing sessions',
    CONNECTION_FLOOD: 'Wait and retry with exponential backoff',
    RATE_LIMIT_EXCEEDED: 'Slow down requests, respect throttling window',
    REPLAY_DETECTED: 'Do not replay same event id, generate new correlationId',
    CALL_BUSY: 'User is busy, try again later or leave voicemail',
    CALL_TIMED_OUT: 'User did not answer, trigger missed call notification',
    CALL_OFFLINE_TARGET: 'Target offline, send push notification via notification engine',
    ROOM_FULL: 'Room at capacity, try different room or request host to increase limit',
    PRESENCE_THROTTLED: 'Presence updates throttled, cache locally and batch updates',
  };
  return map[code] ?? 'Check error details and retry with corrected payload. If persists, contact support with correlationId.';
}

export function isAppError(err: unknown): err is AppError {
  return err instanceof AppError;
}
