import { Socket } from 'socket.io';
import jwt from 'jsonwebtoken';
import { z } from 'zod';
import { env } from '../../config/env.js';
import { logger, logAuth } from '../logger/logger.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { isValidDeviceId, normalizeDeviceId } from '../../shared/utils/device.js';
import { DeviceType } from '../../shared/types/enums.js';

export interface SupabaseJwtPayload {
  sub: string; // user id
  aud: string;
  exp: number;
  iat: number;
  iss?: string;
  role?: string;
  email?: string;
  phone?: string;
  app_metadata?: Record<string, unknown>;
  user_metadata?: Record<string, unknown>;
  session_id?: string;
  is_anonymous?: boolean;
}

const authPayloadSchema = z.object({
  token: z.string().min(10).optional(),
  idToken: z.string().min(10).optional(),
  deviceId: z.string().min(8).max(128).optional(),
  deviceType: z.string().optional(),
  appVersion: z.string().optional(),
});

/**
 * Device validation store - in production use Redis
 */
class DeviceStore {
  private devices = new Map<string, Set<string>>(); // userId -> Set<deviceId>

  addDevice(userId: string, deviceId: string): boolean {
    if (!this.devices.has(userId)) this.devices.set(userId, new Set());
    const set = this.devices.get(userId)!;
    if (set.size >= env.MAX_DEVICES_PER_USER && !set.has(deviceId)) {
      return false;
    }
    set.add(deviceId);
    return true;
  }

  removeDevice(userId: string, deviceId: string): void {
    this.devices.get(userId)?.delete(deviceId);
  }

  countDevices(userId: string): number {
    return this.devices.get(userId)?.size || 0;
  }
}

const deviceStore = new DeviceStore();

export async function verifySupabaseJwt(token: string): Promise<SupabaseJwtPayload> {
  const start = Date.now();
  try {
    // Supabase JWT is typically HS256 signed with JWT secret
    const payload = jwt.verify(token, env.SUPABASE_JWT_SECRET, {
      clockTolerance: env.JWT_CLOCK_TOLERANCE_SEC,
      algorithms: ['HS256'],
    }) as SupabaseJwtPayload;

    // Reject expired handled by verify, but double-check
    if (payload.exp * 1000 < Date.now()) {
      throw new AppError({
        code: ErrorCodes.AUTH_EXPIRED_TOKEN,
        correlationId: generateCorrelationId(),
        statusCode: 401,
        developerMessage: `Token expired at ${new Date(payload.exp * 1000).toISOString()}`,
      });
    }

    if (payload.aud !== 'authenticated' && payload.aud !== 'service_role') {
      // Supabase anon role? But we require authenticated
      if (payload.role === 'anon' || payload.aud === 'anon') {
        throw new AppError({
          code: ErrorCodes.AUTH_FORBIDDEN,
          correlationId: generateCorrelationId(),
          message: 'Anonymous tokens not allowed for signaling',
        });
      }
    }

    // Latency target <100ms
    const latency = Date.now() - start;
    if (latency > 100) {
      logger.warn({ latency, userId: payload.sub }, 'JWT verification exceeded 100ms target');
    }

    return payload;
  } catch (err) {
    if (err instanceof AppError) throw err;

    const jwtErr = err as Error;
    if (jwtErr.name === 'TokenExpiredError') {
      throw new AppError({
        code: ErrorCodes.AUTH_EXPIRED_TOKEN,
        correlationId: generateCorrelationId(),
        statusCode: 401,
        developerMessage: jwtErr.message,
      });
    }

    throw new AppError({
      code: ErrorCodes.AUTH_INVALID_TOKEN,
      correlationId: generateCorrelationId(),
      statusCode: 401,
      developerMessage: jwtErr.message,
    });
  }
}

export interface AuthenticatedSocketData {
  user: SupabaseJwtPayload & { id: string };
  device: {
    deviceId: string;
    deviceType: DeviceType;
    appVersion?: string;
  };
  sessionId: string;
  authenticatedAt: number;
  correlationId: string;
}

export async function socketAuthMiddleware(socket: Socket, next: (err?: Error) => void): Promise<void> {
  const correlationId = (socket.handshake.auth?.correlationId as string) || generateCorrelationId();
  const start = Date.now();

  try {
    // Parse auth payload
    const rawAuth = socket.handshake.auth || {};
    const parsed = authPayloadSchema.safeParse(rawAuth);
    if (!parsed.success) {
      throw new AppError({
        code: ErrorCodes.AUTH_MISSING_TOKEN,
        correlationId,
        statusCode: 401,
        developerMessage: `Invalid auth payload: ${parsed.error.message}`,
      });
    }

    const token = parsed.data.token || parsed.data.idToken || (socket.handshake.headers.authorization?.replace('Bearer ', '') as string);
    if (!token) {
      // Allow anonymous only in non-production for testing, but log
      if (env.NODE_ENV !== 'production') {
        const anonId = `anon_${socket.id.slice(0, 8)}`;
        (socket.data as AuthenticatedSocketData) = {
          user: {
            id: anonId,
            sub: anonId,
            aud: 'authenticated',
            exp: Math.floor(Date.now() / 1000) + 3600,
            iat: Math.floor(Date.now() / 1000),
            role: 'authenticated',
          },
          device: {
            deviceId: normalizeDeviceId(parsed.data.deviceId, socket.id),
            deviceType: DeviceType.UNKNOWN,
            appVersion: parsed.data.appVersion,
          },
          sessionId: `sess_${socket.id}`,
          authenticatedAt: Date.now(),
          correlationId,
        };
        logAuth({ deviceId: parsed.data.deviceId, success: true });
        return next();
      }

      throw new AppError({
        code: ErrorCodes.AUTH_MISSING_TOKEN,
        correlationId,
        statusCode: 401,
      });
    }

    // Verify JWT
    const jwtPayload = await verifySupabaseJwt(token);

    // Device validation
    const deviceId = normalizeDeviceId(parsed.data.deviceId || (socket.handshake.headers[env.DEVICE_ID_HEADER] as string), socket.id);
    if (!isValidDeviceId(deviceId) && env.NODE_ENV === 'production') {
      throw new AppError({
        code: ErrorCodes.AUTH_DEVICE_MISMATCH,
        correlationId,
        meta: { deviceId, userId: jwtPayload.sub },
      });
    }

    // Device limit check
    const canAdd = deviceStore.addDevice(jwtPayload.sub, deviceId);
    if (!canAdd) {
      throw new AppError({
        code: ErrorCodes.AUTH_TOO_MANY_DEVICES,
        correlationId,
        statusCode: 403,
        meta: { userId: jwtPayload.sub, deviceId },
      });
    }

    // Determine device type
    const ua = socket.handshake.headers['user-agent'];
    const deviceTypeStr = parsed.data.deviceType || '';
    let deviceType: DeviceType = DeviceType.UNKNOWN;
    if (deviceTypeStr) {
      const upper = deviceTypeStr.toUpperCase();
      if (upper.includes('ANDROID')) deviceType = DeviceType.ANDROID;
      else if (upper.includes('IOS')) deviceType = DeviceType.IOS;
      else if (upper.includes('WEB')) deviceType = DeviceType.WEB;
    } else if (ua) {
      if (ua.toLowerCase().includes('android')) deviceType = DeviceType.ANDROID;
      else if (ua.toLowerCase().includes('iphone') || ua.toLowerCase().includes('ipad')) deviceType = DeviceType.IOS;
      else deviceType = DeviceType.WEB;
    }

    const sessionId = `sess_${jwtPayload.sub}_${deviceId}_${Date.now()}`;

    (socket.data as AuthenticatedSocketData) = {
      user: { ...jwtPayload, id: jwtPayload.sub },
      device: {
        deviceId,
        deviceType,
        appVersion: parsed.data.appVersion,
      },
      sessionId,
      authenticatedAt: Date.now(),
      correlationId,
    };

    // Store for cleanup tracking
    socket.data._deviceId = deviceId;

    logAuth({ userId: jwtPayload.sub, deviceId, success: true });

    const latency = Date.now() - start;
    if (latency > 100) {
      logger.warn({ userId: jwtPayload.sub, latency, correlationId }, 'Auth exceeded 100ms');
    }

    return next();
  } catch (err) {
    const appErr = err instanceof AppError ? err : new AppError({
      code: ErrorCodes.AUTH_INVALID_TOKEN,
      correlationId,
      developerMessage: (err as Error).message,
    });
    logAuth({ success: false, reason: appErr.code });
    return next(new Error(JSON.stringify(appErr.toJSON())));
  }
}

export function cleanupDeviceOnDisconnect(userId: string, deviceId: string): void {
  deviceStore.removeDevice(userId, deviceId);
}

// Express / Fastify middleware for REST auth
export async function fastifyAuthPreHandler(request: any, reply: any): Promise<void> {
  const authHeader = request.headers.authorization as string | undefined;
  const token = authHeader?.replace('Bearer ', '').trim() || (request.query as any)?.token;
  if (!token) {
    throw new AppError({ code: ErrorCodes.AUTH_MISSING_TOKEN, correlationId: generateCorrelationId(), statusCode: 401 });
  }
  try {
    const payload = await verifySupabaseJwt(token);
    (request as any).user = payload;
  } catch (err) {
    throw err;
  }
}
