import { PresenceStatus, DeviceType, DevicePriority, ConnectionQuality } from './enums.js';

export interface AuthUser {
  id: string; // Supabase user id
  email?: string;
  phone?: string;
  role: string;
  aud: string;
  exp: number;
  iat: number;
  appMetadata?: Record<string, unknown>;
  userMetadata?: {
    display_name?: string;
    displayName?: string;
    avatar_url?: string;
    photoUrl?: string;
    [key: string]: unknown;
  };
}

export interface DeviceInfo {
  deviceId: string;
  deviceType: DeviceType;
  deviceModel?: string;
  osVersion?: string;
  appVersion?: string;
  pushToken?: string;
  priority: DevicePriority;
  ip?: string;
  userAgent?: string;
}

export interface SocketSession {
  userId: string;
  deviceId: string;
  device: DeviceInfo;
  socketId: string;
  connectedAt: number;
  lastHeartbeatAt: number;
  lastActivityAt: number;
  connectionQuality: ConnectionQuality;
  reconnectAttempts: number;
  sessionId: string;
  correlationId: string;
  authUser: AuthUser;
}

export interface SocketData {
  user: AuthUser;
  device: DeviceInfo;
  sessionId: string;
  correlationId: string;
  authenticatedAt: number;
}

export interface PresenceData {
  userId: string;
  status: PresenceStatus;
  lastSeen: string; // ISO
  lastSeenMs: number;
  deviceId?: string;
  venueId?: string;
  customMessage?: string;
  isOnline: boolean;
  activeSessions: number;
  connectionQuality: ConnectionQuality;
  displayName?: string;
  photoUrl?: string;
}

export interface CallParticipant {
  userId: string;
  socketId?: string;
  role: string;
  joinedAt: number;
  isMuted: boolean;
  isCameraOn: boolean;
  isSpeakerOn: boolean;
  isHandRaised: boolean;
  connectionQuality: ConnectionQuality;
  deviceId?: string;
  displayName?: string;
  photoUrl?: string;
}

export interface RoomMetadata {
  name?: string;
  venueId?: string;
  venueName?: string;
  createdBy: string;
  type: string;
  maxParticipants?: number;
  isLocked?: boolean;
  isLive?: boolean;
  custom?: Record<string, unknown>;
}

export interface LocationData {
  latitude: number;
  longitude: number;
  accuracy?: number;
  heading?: number;
  speed?: number;
  altitude?: number;
  battery?: number;
  timestamp: string;
  provider?: string;
}
