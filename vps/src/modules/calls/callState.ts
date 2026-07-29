import { CallType, CallState, ParticipantRole, ConnectionQuality } from '../../shared/types/enums.js';

export interface CallParticipantState {
  userId: string;
  socketId?: string;
  role: ParticipantRole;
  joinedAt: number;
  lastSeen: number;
  isMuted: boolean;
  isCameraOn: boolean;
  isSpeakerOn: boolean;
  isHandRaised: boolean;
  isDeafened?: boolean;
  connectionQuality: ConnectionQuality;
  deviceId?: string;
  displayName?: string;
  photoUrl?: string;
}

export interface CallMetadata {
  chatId?: string;
  lobbyId?: string;
  venueId?: string;
  nightguardSessionId?: string;
  title?: string;
  isEmergency?: boolean;
  custom?: Record<string, unknown>;
}

export interface CallRoom {
  id: string;
  callId: string;
  type: CallType;
  state: CallState;
  createdBy: string;
  createdAt: number;
  lastActivityAt: number;
  participants: Map<string, CallParticipantState>; // userId -> state
  metadata: CallMetadata;
  maxParticipants: number;
  isLocked: boolean;
  sdpHistory: Array<{ userId: string; type: 'offer' | 'answer'; timestamp: number }>;
  iceRestartCount: number;
  ringingStartedAt?: number;
  acceptedAt?: number;
  endedAt?: number;
  endedBy?: string;
  endReason?: string;
}

export function createEmptyCall(params: {
  id: string;
  type: CallType;
  createdBy: string;
  maxParticipants: number;
  metadata?: CallMetadata;
}): CallRoom {
  const now = Date.now();
  return {
    id: params.id,
    callId: params.id,
    type: params.type,
    state: CallState.REQUESTING,
    createdBy: params.createdBy,
    createdAt: now,
    lastActivityAt: now,
    participants: new Map(),
    metadata: params.metadata || {},
    maxParticipants: params.maxParticipants,
    isLocked: false,
    sdpHistory: [],
    iceRestartCount: 0,
  };
}
