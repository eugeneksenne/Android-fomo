/**
 * Structured error codes for all socket events.
 * Format: DOMAIN_SUBDOMAIN_REASON
 */
export const ErrorCodes = {
  // Auth
  AUTH_MISSING_TOKEN: 'AUTH_MISSING_TOKEN',
  AUTH_INVALID_TOKEN: 'AUTH_INVALID_TOKEN',
  AUTH_EXPIRED_TOKEN: 'AUTH_EXPIRED_TOKEN',
  AUTH_TOKEN_REVOKED: 'AUTH_TOKEN_REVOKED',
  AUTH_DEVICE_MISMATCH: 'AUTH_DEVICE_MISMATCH',
  AUTH_SESSION_INVALID: 'AUTH_SESSION_INVALID',
  AUTH_TOO_MANY_DEVICES: 'AUTH_TOO_MANY_DEVICES',
  AUTH_FORBIDDEN: 'AUTH_FORBIDDEN',

  // Connection
  CONNECTION_FLOOD: 'CONNECTION_FLOOD',
  CONNECTION_DUPLICATE: 'CONNECTION_DUPLICATE',
  CONNECTION_RATE_LIMITED: 'CONNECTION_RATE_LIMITED',
  CONNECTION_UNAUTHORIZED: 'CONNECTION_UNAUTHORIZED',

  // Validation
  VALIDATION_FAILED: 'VALIDATION_FAILED',
  INVALID_PAYLOAD: 'INVALID_PAYLOAD',
  MISSING_FIELD: 'MISSING_FIELD',

  // Rate / Abuse
  RATE_LIMIT_EXCEEDED: 'RATE_LIMIT_EXCEEDED',
  SPAM_DETECTED: 'SPAM_DETECTED',
  CALL_SPAM_BLOCKED: 'CALL_SPAM_BLOCKED',
  REPLAY_DETECTED: 'REPLAY_DETECTED',

  // Rooms
  ROOM_NOT_FOUND: 'ROOM_NOT_FOUND',
  ROOM_FULL: 'ROOM_FULL',
  ROOM_LOCKED: 'ROOM_LOCKED',
  NOT_IN_ROOM: 'NOT_IN_ROOM',
  ALREADY_IN_ROOM: 'ALREADY_IN_ROOM',

  // Calls
  CALL_NOT_FOUND: 'CALL_NOT_FOUND',
  CALL_ALREADY_ACTIVE: 'CALL_ALREADY_ACTIVE',
  CALL_BUSY: 'CALL_BUSY',
  CALL_TIMED_OUT: 'CALL_TIMED_OUT',
  CALL_REJECTED: 'CALL_REJECTED',
  CALL_FAILED: 'CALL_FAILED',
  CALL_NOT_ALLOWED: 'CALL_NOT_ALLOWED',
  CALL_PARTICIPANT_LIMIT: 'CALL_PARTICIPANT_LIMIT',
  CALL_OFFLINE_TARGET: 'CALL_OFFLINE_TARGET',

  // Presence
  PRESENCE_THROTTLED: 'PRESENCE_THROTTLED',
  PRESENCE_INVALID_STATUS: 'PRESENCE_INVALID_STATUS',

  // NightGuard
  NIGHTGUARD_SESSION_NOT_FOUND: 'NIGHTGUARD_SESSION_NOT_FOUND',
  NIGHTGUARD_NOT_AUTHORIZED: 'NIGHTGUARD_NOT_AUTHORIZED',
  NIGHTGUARD_ALREADY_ACTIVE: 'NIGHTGUARD_ALREADY_ACTIVE',

  // Live Location
  LIVE_LOCATION_SESSION_NOT_FOUND: 'LIVE_LOCATION_SESSION_NOT_FOUND',
  LIVE_LOCATION_NOT_AUTHORIZED: 'LIVE_LOCATION_NOT_AUTHORIZED',

  // Club Lobby
  LOBBY_NOT_FOUND: 'LOBBY_NOT_FOUND',
  LOBBY_FULL: 'LOBBY_FULL',
  LOBBY_NOT_HOST: 'LOBBY_NOT_HOST',
  LOBBY_BANNED: 'LOBBY_BANNED',

  // Generic
  INTERNAL_ERROR: 'INTERNAL_ERROR',
  SERVICE_UNAVAILABLE: 'SERVICE_UNAVAILABLE',
  TIMEOUT: 'TIMEOUT',
  NOT_IMPLEMENTED: 'NOT_IMPLEMENTED',
} as const;

export type ErrorCode = typeof ErrorCodes[keyof typeof ErrorCodes];

export const ErrorMessages: Record<ErrorCode, string> = {
  AUTH_MISSING_TOKEN: 'Authentication token is required',
  AUTH_INVALID_TOKEN: 'Invalid authentication token',
  AUTH_EXPIRED_TOKEN: 'Authentication token has expired',
  AUTH_TOKEN_REVOKED: 'Authentication token has been revoked',
  AUTH_DEVICE_MISMATCH: 'Device validation failed',
  AUTH_SESSION_INVALID: 'Session is invalid or expired',
  AUTH_TOO_MANY_DEVICES: 'Too many devices connected for this user',
  AUTH_FORBIDDEN: 'You do not have permission to perform this action',

  CONNECTION_FLOOD: 'Too many connection attempts',
  CONNECTION_DUPLICATE: 'Duplicate connection detected',
  CONNECTION_RATE_LIMITED: 'Connection rate limited',
  CONNECTION_UNAUTHORIZED: 'Connection unauthorized',

  VALIDATION_FAILED: 'Payload validation failed',
  INVALID_PAYLOAD: 'Invalid payload',
  MISSING_FIELD: 'Required field is missing',

  RATE_LIMIT_EXCEEDED: 'Rate limit exceeded, please slow down',
  SPAM_DETECTED: 'Spam detected, action blocked',
  CALL_SPAM_BLOCKED: 'Call spam protection triggered',
  REPLAY_DETECTED: 'Replay attack detected',

  ROOM_NOT_FOUND: 'Room not found',
  ROOM_FULL: 'Room has reached maximum participants',
  ROOM_LOCKED: 'Room is locked',
  NOT_IN_ROOM: 'You are not in this room',
  ALREADY_IN_ROOM: 'You are already in this room',

  CALL_NOT_FOUND: 'Call not found',
  CALL_ALREADY_ACTIVE: 'User already in another call',
  CALL_BUSY: 'User is busy in another call',
  CALL_TIMED_OUT: 'Call timed out, no answer',
  CALL_REJECTED: 'Call was rejected',
  CALL_FAILED: 'Call failed to establish',
  CALL_NOT_ALLOWED: 'Call not allowed',
  CALL_PARTICIPANT_LIMIT: 'Group call participant limit reached',
  CALL_OFFLINE_TARGET: 'Target user is offline',

  PRESENCE_THROTTLED: 'Presence updates throttled',
  PRESENCE_INVALID_STATUS: 'Invalid presence status',

  NIGHTGUARD_SESSION_NOT_FOUND: 'NightGuard session not found',
  NIGHTGUARD_NOT_AUTHORIZED: 'Not authorized for NightGuard session',
  NIGHTGUARD_ALREADY_ACTIVE: 'NightGuard session already active',

  LIVE_LOCATION_SESSION_NOT_FOUND: 'Live location session not found',
  LIVE_LOCATION_NOT_AUTHORIZED: 'Not authorized for live location session',

  LOBBY_NOT_FOUND: 'Club lobby not found',
  LOBBY_FULL: 'Club lobby is full',
  LOBBY_NOT_HOST: 'Only host can perform this action',
  LOBBY_BANNED: 'You are banned from this lobby',

  INTERNAL_ERROR: 'Internal server error',
  SERVICE_UNAVAILABLE: 'Service temporarily unavailable',
  TIMEOUT: 'Operation timed out',
  NOT_IMPLEMENTED: 'Feature not implemented',
};
