import { z } from 'zod';
import { baseEventSchema, locationSchema } from '../../core/middleware/validation.js';
import { NightGuardEventType } from '../../shared/types/enums.js';

export const nightguardCreateSchema = baseEventSchema.extend({
  type: z.enum(['WALK_ME_HOME', 'BUDDY_PAIR', 'NIGHTGUARD', 'EMERGENCY']),
  trustedContactIds: z.array(z.string().min(3).max(128)).min(1).max(20),
  durationMinutes: z.number().min(5).max(600).optional(),
  destination: z.object({
    name: z.string().max(200).optional(),
    latitude: z.number().min(-90).max(90),
    longitude: z.number().min(-180).max(180),
  }).optional(),
  metadata: z.record(z.unknown()).optional(),
});

export const nightguardJoinSchema = baseEventSchema.extend({
  sessionId: z.string().min(3).max(128),
});

export const nightguardLocationSchema = baseEventSchema.extend({
  sessionId: z.string().min(3).max(128),
  location: locationSchema,
  etaSeconds: z.number().min(0).max(86400).optional(),
  battery: z.number().min(0).max(100).optional(),
});

export const nightguardStatusSchema = baseEventSchema.extend({
  sessionId: z.string().min(3).max(128),
  eventType: z.nativeEnum(NightGuardEventType),
  status: z.string().max(50).optional(),
  note: z.string().max(500).optional(),
  location: locationSchema.optional(),
  battery: z.number().min(0).max(100).optional(),
  etaSeconds: z.number().optional(),
});

export const nightguardSosSchema = baseEventSchema.extend({
  sessionId: z.string().min(3).max(128).optional(),
  location: locationSchema,
  message: z.string().max(500).optional(),
  contactsToAlert: z.array(z.string().min(3).max(128)).max(20).optional(),
  triggerType: z.enum(['MANUAL', 'SHAKE', 'INACTIVITY', 'BATTERY_LOW', 'OUT_OF_ROUTE']),
});
