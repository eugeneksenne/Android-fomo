import { z } from 'zod';
import { CallType } from '../../shared/types/enums.js';
import { baseEventSchema, sdpSchema, iceCandidateSchema } from '../../core/middleware/validation.js';

export const callInitiateSchema = baseEventSchema.extend({
  targetUserId: z.string().min(3).max(128).optional(),
  targetUserIds: z.array(z.string().min(3).max(128)).max(20).optional(),
  callType: z.nativeEnum(CallType),
  chatId: z.string().max(128).optional(),
  lobbyId: z.string().max(128).optional(),
  nightguardSessionId: z.string().max(128).optional(),
  metadata: z.record(z.unknown()).optional(),
}).refine(data => data.targetUserId || (data.targetUserIds && data.targetUserIds.length > 0), {
  message: 'Either targetUserId or targetUserIds required',
});

export const callActionSchema = baseEventSchema.extend({
  roomId: z.string().min(3).max(128),
  reason: z.string().max(100).optional(),
});

export const callJoinSchema = baseEventSchema.extend({
  roomId: z.string().min(3).max(128),
});

export const callEscalateSchema = baseEventSchema.extend({
  roomId: z.string().min(3).max(128),
  newType: z.nativeEnum(CallType),
});

export const callMediaStateSchema = baseEventSchema.extend({
  roomId: z.string().min(3).max(128),
  audioEnabled: z.boolean().optional(),
  videoEnabled: z.boolean().optional(),
  isMuted: z.boolean().optional(),
  isCameraOn: z.boolean().optional(),
  isSpeakerOn: z.boolean().optional(),
  isHandRaised: z.boolean().optional(),
});

export const groupCallActionSchema = baseEventSchema.extend({
  roomId: z.string().min(3).max(128),
  targetUserId: z.string().min(3).max(128).optional(),
  action: z.enum(['MUTE', 'UNMUTE', 'REMOVE', 'PROMOTE', 'DEMOTE', 'RAISE_HAND', 'LOWER_HAND']),
});

export const sdpPayloadSchema = baseEventSchema.extend({
  roomId: z.string().min(3).max(128),
  targetUserId: z.string().min(3).max(128).optional(),
  sdp: sdpSchema,
  codecInfo: z.object({
    codecs: z.array(z.string()).optional(),
    preferOpus: z.boolean().optional(),
  }).optional(),
});

export const iceCandidatePayloadSchema = baseEventSchema.extend({
  roomId: z.string().min(3).max(128),
  targetUserId: z.string().min(3).max(128).optional(),
  candidate: iceCandidateSchema,
  isRestart: z.boolean().optional(),
});

export const renegotiationSchema = baseEventSchema.extend({
  roomId: z.string().min(3).max(128),
  targetUserId: z.string().min(3).max(128).optional(),
  reason: z.string().max(200),
});
