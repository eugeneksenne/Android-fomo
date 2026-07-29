import { z } from 'zod';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { generateCorrelationId } from '../../shared/utils/id.js';

export function validatePayload<T>(schema: z.ZodSchema<T>, payload: unknown, correlationId?: string): T {
  const cid = correlationId || generateCorrelationId();
  const result = schema.safeParse(payload);
  if (!result.success) {
    throw new AppError({
      code: ErrorCodes.VALIDATION_FAILED,
      correlationId: cid,
      statusCode: 400,
      message: 'Validation failed',
      developerMessage: result.error.issues.map(i => `${i.path.join('.')}: ${i.message}`).join('; '),
      meta: { details: result.error.flatten() },
    });
  }
  return result.data;
}

// Common validators
export const commonValidators = {
  roomId: z.string().min(3).max(128),
  userId: z.string().min(3).max(128),
  deviceId: z.string().min(8).max(128).optional(),
  correlationId: z.string().min(5).max(128).optional(),
  version: z.string().optional(),
  timestamp: z.string().optional(),
};

export const sdpSchema = z.object({
  type: z.enum(['offer', 'answer']),
  sdp: z.string().min(10).max(20000),
});

export const iceCandidateSchema = z.object({
  candidate: z.string().optional(),
  sdpMid: z.string().nullable().optional(),
  sdpMLineIndex: z.number().nullable().optional(),
  usernameFragment: z.string().optional().nullable(),
}).passthrough().nullable();

export const locationSchema = z.object({
  latitude: z.number().min(-90).max(90),
  longitude: z.number().min(-180).max(180),
  accuracy: z.number().optional(),
  heading: z.number().optional(),
  speed: z.number().optional(),
  altitude: z.number().optional(),
  battery: z.number().min(0).max(100).optional(),
  timestamp: z.string().optional(),
  provider: z.string().optional(),
});

export const baseEventSchema = z.object({
  correlationId: z.string().optional(),
  version: z.string().optional(),
  timestamp: z.string().optional(),
  deviceId: z.string().optional(),
});
