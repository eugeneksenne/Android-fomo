import { z } from 'zod';
import { baseEventSchema } from '../../core/middleware/validation.js';

export const lobbyJoinSchema = baseEventSchema.extend({
  venueId: z.string().min(2).max(100),
  lobbyId: z.string().max(100).optional(),
  channel: z.string().max(50).optional(),
});

export const lobbyLeaveSchema = baseEventSchema.extend({
  venueId: z.string().min(2).max(100),
  lobbyId: z.string().max(100).optional(),
  channel: z.string().max(50).optional(),
});

export const lobbyMessageSchema = baseEventSchema.extend({
  venueId: z.string().min(2).max(100),
  lobbyId: z.string().max(100).optional(),
  channel: z.string().max(50).optional(),
  text: z.string().min(1).max(2000),
  type: z.enum(['TEXT', 'IMAGE', 'SYSTEM', 'ANNOUNCEMENT']).optional(),
  replyTo: z.string().max(100).optional(),
  metadata: z.record(z.unknown()).optional(),
});

export const lobbyAnnouncementSchema = baseEventSchema.extend({
  venueId: z.string().min(2).max(100),
  message: z.string().min(1).max(1000),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']).optional(),
});
