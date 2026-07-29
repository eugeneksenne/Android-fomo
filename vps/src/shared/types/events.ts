/**
 * Strongly typed Socket.IO events for FOMO
 * Every event has versioning, correlationId, validation via Zod
 */
import { PresenceStatus, CallType, CallState, NightGuardEventType, LiveLocationEventType, ClubLobbyEventType, NotificationTrigger } from './enums.js';
import { LocationData } from './socket.js';

export const EVENT_VERSION = '2.0.0';

// Base envelope every event must use
export interface BaseEventPayload {
  correlationId?: string;
  version?: string; // default EVENT_VERSION
  timestamp?: string; // ISO
  deviceId?: string;
}

// === Client → Server Events ===

export namespace ClientToServerEvents {
  // Connection / lifecycle
  export interface HeartbeatPayload extends BaseEventPayload {
    status?: PresenceStatus;
    location?: LocationData;
    battery?: number;
    connectionQuality?: string;
  }

  export interface ReconnectPayload extends BaseEventPayload {
    sessionId: string;
    lastEventId?: string;
    deviceId: string;
  }

  // Presence
  export interface PresenceUpdatePayload extends BaseEventPayload {
    status: PresenceStatus;
    customMessage?: string;
    venueId?: string;
  }

  export interface PresenceGetPayload extends BaseEventPayload {
    userIds?: string[];
    limit?: number;
  }

  export interface TypingPayload extends BaseEventPayload {
    roomId?: string;
    chatId?: string;
    lobbyId?: string;
    isTyping: boolean;
  }

  // Calls
  export interface CallInitiatePayload extends BaseEventPayload {
    targetUserId?: string;
    targetUserIds?: string[]; // for group
    callType: CallType;
    chatId?: string;
    lobbyId?: string;
    nightguardSessionId?: string;
    metadata?: Record<string, unknown>;
  }

  export interface CallActionPayload extends BaseEventPayload {
    roomId: string;
    reason?: string;
  }

  export interface CallJoinPayload extends BaseEventPayload {
    roomId: string;
  }

  export interface CallEscalatePayload extends BaseEventPayload {
    roomId: string;
    newType: CallType; // VOICE -> VIDEO
  }

  export interface CallMediaStatePayload extends BaseEventPayload {
    roomId: string;
    audioEnabled?: boolean;
    videoEnabled?: boolean;
    isMuted?: boolean;
    isCameraOn?: boolean;
    isSpeakerOn?: boolean;
    isHandRaised?: boolean;
  }

  // WebRTC
  export interface SdpPayload extends BaseEventPayload {
    roomId: string;
    targetUserId?: string;
    sdp: {
      type: 'offer' | 'answer';
      sdp: string;
    };
    codecInfo?: {
      codecs?: string[];
      preferOpus?: boolean;
    };
  }

  export interface IceCandidatePayload extends BaseEventPayload {
    roomId: string;
    targetUserId?: string;
    candidate: {
      candidate?: string;
      sdpMid?: string | null;
      sdpMLineIndex?: number | null;
      usernameFragment?: string | null;
    } | null;
    isRestart?: boolean;
  }

  export interface RenegotiationPayload extends BaseEventPayload {
    roomId: string;
    targetUserId?: string;
    reason: string;
  }

  // Group calls
  export interface GroupCallActionPayload extends BaseEventPayload {
    roomId: string;
    targetUserId?: string;
    action: 'MUTE' | 'UNMUTE' | 'REMOVE' | 'PROMOTE' | 'DEMOTE' | 'RAISE_HAND' | 'LOWER_HAND';
  }

  // Friends
  export interface FriendsPresencePayload extends BaseEventPayload {
    friendIds?: string[];
  }

  // Club Lobby
  export interface LobbyJoinPayload extends BaseEventPayload {
    venueId: string;
    lobbyId?: string; // optional sub-lobby id
    channel?: string;
  }

  export interface LobbyLeavePayload extends BaseEventPayload {
    venueId: string;
    lobbyId?: string;
    channel?: string;
  }

  export interface LobbyMessagePayload extends BaseEventPayload {
    venueId: string;
    lobbyId?: string;
    channel?: string;
    text: string;
    type?: 'TEXT' | 'IMAGE' | 'SYSTEM' | 'ANNOUNCEMENT';
    replyTo?: string;
    metadata?: Record<string, unknown>;
  }

  export interface LobbyAnnouncementPayload extends BaseEventPayload {
    venueId: string;
    message: string;
    priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  }

  // NightGuard
  export interface NightGuardCreatePayload extends BaseEventPayload {
    type: 'WALK_ME_HOME' | 'BUDDY_PAIR' | 'NIGHTGUARD' | 'EMERGENCY';
    trustedContactIds: string[];
    durationMinutes?: number;
    destination?: {
      name?: string;
      latitude: number;
      longitude: number;
    };
    metadata?: Record<string, unknown>;
  }

  export interface NightGuardJoinPayload extends BaseEventPayload {
    sessionId: string;
  }

  export interface NightGuardLocationPayload extends BaseEventPayload {
    sessionId: string;
    location: LocationData;
    etaSeconds?: number;
    battery?: number;
  }

  export interface NightGuardStatusPayload extends BaseEventPayload {
    sessionId: string;
    eventType: NightGuardEventType;
    status?: string;
    note?: string;
    location?: LocationData;
    battery?: number;
    etaSeconds?: number;
  }

  export interface NightGuardSosPayload extends BaseEventPayload {
    sessionId?: string;
    location: LocationData;
    message?: string;
    contactsToAlert?: string[];
    triggerType: 'MANUAL' | 'SHAKE' | 'INACTIVITY' | 'BATTERY_LOW' | 'OUT_OF_ROUTE';
  }

  // Live Location
  export interface LiveLocationStartPayload extends BaseEventPayload {
    sessionId?: string; // if resuming
    trustedContactIds?: string[];
    tripName?: string;
    destination?: { latitude: number; longitude: number; name?: string };
    metadata?: Record<string, unknown>;
  }

  export interface LiveLocationUpdatePayload extends BaseEventPayload {
    sessionId: string;
    location: LocationData;
    eventType: LiveLocationEventType;
    etaSeconds?: number;
    battery?: number;
    accuracy?: number;
    isMoving?: boolean;
  }

  export interface LiveLocationActionPayload extends BaseEventPayload {
    sessionId: string;
    action: 'PAUSE' | 'RESUME' | 'STOP';
    reason?: string;
  }

  // Chat typing
  export interface ChatTypingPayload extends BaseEventPayload {
    chatId: string;
    isTyping: boolean;
  }

  // Map
  export interface MapJoinPayload extends BaseEventPayload {
    venueId?: string;
    city?: string;
    mode?: string;
  }

  export interface MapLocationPayload extends BaseEventPayload {
    latitude: number;
    longitude: number;
    accuracy?: number;
    heading?: number;
    speed?: number;
    venueId?: string;
    city?: string;
    isLive?: boolean;
  }
}

export namespace ServerToClientEvents {
  export interface ConnectedPayload {
    socketId: string;
    userId: string;
    sessionId: string;
    correlationId: string;
    serverTime: number;
    timestamp: string;
    version: string;
    onlineCount: number;
    recoveryEnabled: boolean;
  }

  export interface HeartbeatAckPayload {
    timestamp: string;
    serverTime: number;
    latencyMs?: number;
    correlationId: string;
  }

  export interface PresenceUpdatePayload {
    userId: string;
    status: PresenceStatus;
    lastSeen: string;
    customMessage?: string;
    venueId?: string;
    displayName?: string;
    photoUrl?: string;
    timestamp: string;
    correlationId: string;
  }

  export interface PresenceListPayload {
    users: Array<{
      userId: string;
      status: PresenceStatus;
      lastSeen: string;
      displayName?: string;
      photoUrl?: string;
      venueId?: string;
      isOnline: boolean;
    }>;
    timestamp: string;
    correlationId: string;
  }

  export interface TypingIndicatorPayload {
    userId: string;
    roomId?: string;
    chatId?: string;
    lobbyId?: string;
    isTyping: boolean;
    displayName?: string;
    timestamp: string;
    correlationId: string;
  }

  export interface CallIncomingPayload {
    roomId: string;
    callId: string;
    callerId: string;
    callerName?: string;
    callerPhoto?: string;
    callType: CallType;
    participants: string[];
    timestamp: string;
    correlationId: string;
    metadata?: Record<string, unknown>;
  }

  export interface CallStatePayload {
    roomId: string;
    callId: string;
    state: CallState;
    userId?: string;
    reason?: string;
    participants: string[];
    timestamp: string;
    correlationId: string;
  }

  export interface CallParticipantUpdatePayload {
    roomId: string;
    participants: Array<{
      userId: string;
      role: string;
      isMuted: boolean;
      isCameraOn: boolean;
      isHandRaised: boolean;
      displayName?: string;
      photoUrl?: string;
    }>;
    participantCount: number;
    timestamp: string;
    correlationId: string;
  }

  export interface CallMediaStatePayload {
    roomId: string;
    userId: string;
    audioEnabled?: boolean;
    videoEnabled?: boolean;
    isMuted?: boolean;
    isCameraOn?: boolean;
    isSpeakerOn?: boolean;
    isHandRaised?: boolean;
    timestamp: string;
    correlationId: string;
  }

  export interface SdpPayload {
    roomId: string;
    senderId: string;
    targetUserId?: string;
    sdp: { type: 'offer' | 'answer'; sdp: string };
    timestamp: string;
    correlationId: string;
  }

  export interface IceCandidatePayload {
    roomId: string;
    senderId: string;
    targetUserId?: string;
    candidate: {
      candidate?: string;
      sdpMid?: string | null;
      sdpMLineIndex?: number | null;
      usernameFragment?: string | null;
    } | null;
    isRestart?: boolean;
    timestamp: string;
    correlationId: string;
  }

  export interface NotificationPayload {
    trigger: NotificationTrigger;
    title: string;
    body: string;
    data: Record<string, unknown>;
    userId: string;
    timestamp: string;
    correlationId: string;
  }

  export interface NightGuardUpdatePayload {
    sessionId: string;
    eventType: NightGuardEventType;
    userId: string;
    status?: string;
    location?: LocationData;
    battery?: number;
    etaSeconds?: number;
    note?: string;
    timestamp: string;
    correlationId: string;
  }

  export interface LiveLocationUpdatePayload {
    sessionId: string;
    userId: string;
    eventType: LiveLocationEventType;
    location: LocationData;
    battery?: number;
    etaSeconds?: number;
    timestamp: string;
    correlationId: string;
  }

  export interface ClubLobbyUpdatePayload {
    venueId: string;
    lobbyId?: string;
    eventType: ClubLobbyEventType;
    userId?: string;
    data: Record<string, unknown>;
    participantCount?: number;
    timestamp: string;
    correlationId: string;
  }

  export interface FriendsPresencePayload {
    userId: string;
    status: PresenceStatus;
    lastSeen: string;
    displayName?: string;
    photoUrl?: string;
    isCalling?: boolean;
    isStreaming?: boolean;
    venueId?: string;
    timestamp: string;
    correlationId: string;
  }

  export interface ErrorPayload {
    code: string;
    message: string;
    developerMessage: string;
    correlationId: string;
    recovery: string;
    timestamp: string;
    details?: unknown;
  }
}

// Combined type for Socket.IO generics
export interface ClientToServerEventMap {
  'heartbeat': (payload: ClientToServerEvents.HeartbeatPayload, ack?: (res: unknown) => void) => void;
  'presence:update': (payload: ClientToServerEvents.PresenceUpdatePayload, ack?: (res: unknown) => void) => void;
  'presence:get': (payload: ClientToServerEvents.PresenceGetPayload, ack?: (res: unknown) => void) => void;
  'typing': (payload: ClientToServerEvents.TypingPayload) => void;
  'chat:typing': (payload: ClientToServerEvents.ChatTypingPayload) => void;

  'call:initiate': (payload: ClientToServerEvents.CallInitiatePayload, ack?: (res: unknown) => void) => void;
  'call:accept': (payload: ClientToServerEvents.CallActionPayload, ack?: (res: unknown) => void) => void;
  'call:reject': (payload: ClientToServerEvents.CallActionPayload, ack?: (res: unknown) => void) => void;
  'call:cancel': (payload: ClientToServerEvents.CallActionPayload, ack?: (res: unknown) => void) => void;
  'call:end': (payload: ClientToServerEvents.CallActionPayload, ack?: (res: unknown) => void) => void;
  'call:leave': (payload: ClientToServerEvents.CallActionPayload, ack?: (res: unknown) => void) => void;
  'call:escalate': (payload: ClientToServerEvents.CallEscalatePayload, ack?: (res: unknown) => void) => void;
  'call:media_state': (payload: ClientToServerEvents.CallMediaStatePayload) => void;
  'call:group_action': (payload: ClientToServerEvents.GroupCallActionPayload, ack?: (res: unknown) => void) => void;

  'webrtc:offer': (payload: ClientToServerEvents.SdpPayload, ack?: (res: unknown) => void) => void;
  'webrtc:answer': (payload: ClientToServerEvents.SdpPayload, ack?: (res: unknown) => void) => void;
  'webrtc:ice-candidate': (payload: ClientToServerEvents.IceCandidatePayload, ack?: (res: unknown) => void) => void;
  'webrtc:renegotiate': (payload: ClientToServerEvents.RenegotiationPayload, ack?: (res: unknown) => void) => void;
  'webrtc:ice-restart': (payload: ClientToServerEvents.IceCandidatePayload, ack?: (res: unknown) => void) => void;

  'lobby:join': (payload: ClientToServerEvents.LobbyJoinPayload, ack?: (res: unknown) => void) => void;
  'lobby:leave': (payload: ClientToServerEvents.LobbyLeavePayload, ack?: (res: unknown) => void) => void;
  'lobby:message': (payload: ClientToServerEvents.LobbyMessagePayload) => void;
  'lobby:announcement': (payload: ClientToServerEvents.LobbyAnnouncementPayload) => void;

  'nightguard:create': (payload: ClientToServerEvents.NightGuardCreatePayload, ack?: (res: unknown) => void) => void;
  'nightguard:join': (payload: ClientToServerEvents.NightGuardJoinPayload, ack?: (res: unknown) => void) => void;
  'nightguard:leave': (payload: ClientToServerEvents.NightGuardJoinPayload, ack?: (res: unknown) => void) => void;
  'nightguard:location': (payload: ClientToServerEvents.NightGuardLocationPayload) => void;
  'nightguard:status': (payload: ClientToServerEvents.NightGuardStatusPayload) => void;
  'nightguard:sos': (payload: ClientToServerEvents.NightGuardSosPayload, ack?: (res: unknown) => void) => void;

  'live-location:start': (payload: ClientToServerEvents.LiveLocationStartPayload, ack?: (res: unknown) => void) => void;
  'live-location:update': (payload: ClientToServerEvents.LiveLocationUpdatePayload) => void;
  'live-location:action': (payload: ClientToServerEvents.LiveLocationActionPayload, ack?: (res: unknown) => void) => void;

  'map:join': (payload: ClientToServerEvents.MapJoinPayload, ack?: (res: unknown) => void) => void;
  'map:location': (payload: ClientToServerEvents.MapLocationPayload) => void;
}

export interface ServerToClientEventMap {
  'connected': (payload: ServerToClientEvents.ConnectedPayload) => void;
  'heartbeat:ack': (payload: ServerToClientEvents.HeartbeatAckPayload) => void;

  'presence:update': (payload: ServerToClientEvents.PresenceUpdatePayload) => void;
  'presence:list': (payload: ServerToClientEvents.PresenceListPayload) => void;
  'user:online': (payload: ServerToClientEvents.PresenceUpdatePayload) => void;
  'user:offline': (payload: ServerToClientEvents.PresenceUpdatePayload) => void;
  'user:typing': (payload: ServerToClientEvents.TypingIndicatorPayload) => void;
  'chat:typing': (payload: ServerToClientEvents.TypingIndicatorPayload) => void;

  'call:incoming': (payload: ServerToClientEvents.CallIncomingPayload) => void;
  'call:ringing': (payload: ServerToClientEvents.CallStatePayload) => void;
  'call:accepted': (payload: ServerToClientEvents.CallStatePayload) => void;
  'call:rejected': (payload: ServerToClientEvents.CallStatePayload) => void;
  'call:busy': (payload: ServerToClientEvents.CallStatePayload) => void;
  'call:cancelled': (payload: ServerToClientEvents.CallStatePayload) => void;
  'call:ended': (payload: ServerToClientEvents.CallStatePayload) => void;
  'call:failed': (payload: ServerToClientEvents.CallStatePayload) => void;
  'call:timeout': (payload: ServerToClientEvents.CallStatePayload) => void;
  'call:participant_update': (payload: ServerToClientEvents.CallParticipantUpdatePayload) => void;
  'call:media_state': (payload: ServerToClientEvents.CallMediaStatePayload) => void;
  'call:escalated': (payload: ServerToClientEvents.CallStatePayload) => void;

  'webrtc:offer': (payload: ServerToClientEvents.SdpPayload) => void;
  'webrtc:answer': (payload: ServerToClientEvents.SdpPayload) => void;
  'webrtc:ice-candidate': (payload: ServerToClientEvents.IceCandidatePayload) => void;
  'webrtc:renegotiate': (payload: { roomId: string; reason: string; timestamp: string; correlationId: string }) => void;

  'lobby:joined': (payload: ServerToClientEvents.ClubLobbyUpdatePayload) => void;
  'lobby:left': (payload: ServerToClientEvents.ClubLobbyUpdatePayload) => void;
  'lobby:message': (payload: ServerToClientEvents.ClubLobbyUpdatePayload) => void;
  'lobby:announcement': (payload: ServerToClientEvents.ClubLobbyUpdatePayload) => void;
  'lobby:crowd_count': (payload: ServerToClientEvents.ClubLobbyUpdatePayload) => void;

  'nightguard:created': (payload: ServerToClientEvents.NightGuardUpdatePayload) => void;
  'nightguard:joined': (payload: ServerToClientEvents.NightGuardUpdatePayload) => void;
  'nightguard:location': (payload: ServerToClientEvents.NightGuardUpdatePayload) => void;
  'nightguard:status': (payload: ServerToClientEvents.NightGuardUpdatePayload) => void;
  'nightguard:sos': (payload: ServerToClientEvents.NightGuardUpdatePayload) => void;

  'live-location:started': (payload: ServerToClientEvents.LiveLocationUpdatePayload) => void;
  'live-location:update': (payload: ServerToClientEvents.LiveLocationUpdatePayload) => void;
  'live-location:ended': (payload: ServerToClientEvents.LiveLocationUpdatePayload) => void;

  'friends:presence': (payload: ServerToClientEvents.FriendsPresencePayload) => void;

  'notification:trigger': (payload: ServerToClientEvents.NotificationPayload) => void;

  'error': (payload: ServerToClientEvents.ErrorPayload) => void;
}
