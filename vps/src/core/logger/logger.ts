import pino, { Logger } from 'pino';
import { env } from '../../config/env.js';

const isProd = env.NODE_ENV === 'production';

export const logger: Logger = pino({
  level: env.LOG_LEVEL,
  formatters: {
    level(label) {
      return { level: label };
    },
  },
  redact: {
    paths: [
      'req.headers.authorization',
      'req.headers.cookie',
      'password',
      'token',
      'idToken',
      'access_token',
      'refresh_token',
      'supabaseKey',
      'serviceRoleKey',
      '*.token',
      '*.password',
      '*.secret',
      'auth.token',
      'headers.authorization',
    ],
    censor: '[REDACTED]',
  },
  timestamp: pino.stdTimeFunctions.isoTime,
  ...(isProd
    ? {}
    : {
        transport: {
          target: 'pino-pretty',
          options: {
            colorize: true,
            translateTime: 'HH:MM:ss.l',
            ignore: 'pid,hostname',
            singleLine: false,
          },
        },
      }),
});

export function childLogger(bindings: Record<string, unknown>): Logger {
  return logger.child(bindings);
}

export function logConnection(params: { userId: string; socketId: string; deviceId?: string; ip?: string }) {
  logger.info({ event: 'connection', ...params }, `Socket connected user=${params.userId} socket=${params.socketId}`);
}

export function logDisconnection(params: { userId: string; socketId: string; reason: string; durationMs: number }) {
  logger.info({ event: 'disconnection', ...params }, `Socket disconnected user=${params.userId} reason=${params.reason}`);
}

export function logCall(params: { callId: string; type: string; initiator: string; participants: string[]; state: string }) {
  logger.info({ event: 'call', ...params }, `Call ${params.callId} ${params.state} type=${params.type}`);
}

export function logAuth(params: { userId?: string; deviceId?: string; success: boolean; reason?: string }) {
  if (params.success) {
    logger.info({ event: 'auth', ...params }, `Auth success user=${params.userId}`);
  } else {
    logger.warn({ event: 'auth_failed', ...params }, `Auth failed reason=${params.reason}`);
  }
}

export function logError(params: { code: string; message: string; correlationId: string; userId?: string; error?: unknown }) {
  logger.error({ event: 'error', ...params }, `${params.code}: ${params.message} corr=${params.correlationId}`);
}

export function logAudit(params: { userId: string; action: string; resource: string; result: 'success' | 'failure'; details?: unknown }) {
  logger.info({ event: 'audit', ...params, timestamp: new Date().toISOString() }, `AUDIT user=${params.userId} action=${params.action} resource=${params.resource} result=${params.result}`);
}
