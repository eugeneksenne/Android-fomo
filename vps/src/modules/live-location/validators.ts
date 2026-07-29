import { z } from 'zod';
import { baseEventSchema, locationSchema } from '../../core/middleware/validation.js';
import { LiveLocationEventType } from '../../shared/types/enums.js';

export const liveLocationStartSchema = baseEventSchema.extend({
  sessionId: z.string().max(128).optional(),
  trustedContactIds: z.array(z.string().min(3).max(128)).max(20).optional(),
  tripName: z.string().max(100).optional(),
  destination: z.object({
    latitude: z.number().min(-90).max(90),
    longitude: z.number().min(-180).max(180),
    name: z.string().max(200).optional(),
  }).optional(),
  metadata: z.record(z.unknown()).optional(),
});

export const liveLocationUpdateSchema = baseEventSchema.extend({
  sessionId: z.string().min(3).max(128),
  location: locationSchema,
  eventType: z.nativeEnum(LiveLocationEventType),
  etaSeconds: z.number().min(0).max(86400).optional(),
  battery: z.number().min(0).max(100).optional(),
  accuracy: z.number().optional(),
  isMoving: z.boolean().optional(),
});

export const liveLocationActionSchema = baseEventSchema.extend({
  sessionId: z.string().min(3).max(128),
  action: z.enum(['PAUSE', 'RESUME', 'STOP']),
  reason: z.string().max(200).optional(),
});
