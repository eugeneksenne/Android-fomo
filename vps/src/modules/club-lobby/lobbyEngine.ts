import { RoomType, ParticipantRole } from '../../shared/types/enums.js';
import { generateRoomId } from '../../shared/utils/id.js';
import { logger } from '../../core/logger/logger.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { getRedisClient, getRedisKey } from '../../core/redis/client.js';
import { env } from '../../config/env.js';

export interface LobbyRoom {
  id: string;
  venueId: string;
  lobbyId?: string;
  channel: string;
  name?: string;
  createdBy: string;
  createdAt: number;
  lastActivityAt: number;
  participants: Map<string, { userId: string; role: ParticipantRole; joinedAt: number; socketId?: string; displayName?: string; isMuted?: boolean }>;
  maxParticipants: number;
  isLocked: boolean;
  crowdCount: number;
  metadata: Record<string, unknown>;
  bannedUsers: Set<string>;
}

class LobbyEngine {
  private lobbies = new Map<string, LobbyRoom>();
  private venueLobbies = new Map<string, Set<string>>(); // venueId -> Set<lobbyRoomId>

  createOrGetLobby(params: { venueId: string; lobbyId?: string; channel?: string; createdBy: string; name?: string }): LobbyRoom {
    const channel = params.channel || 'GENERAL';
    const lobbyKey = params.lobbyId ? `lobby_${params.venueId}_${params.lobbyId}_${channel}` : `lobby_${params.venueId}_${channel}`;
    const existing = this.lobbies.get(lobbyKey);
    if (existing) return existing;

    const room: LobbyRoom = {
      id: lobbyKey,
      venueId: params.venueId,
      lobbyId: params.lobbyId,
      channel,
      name: params.name || `${params.venueId} ${channel}`,
      createdBy: params.createdBy,
      createdAt: Date.now(),
      lastActivityAt: Date.now(),
      participants: new Map(),
      maxParticipants: 500,
      isLocked: false,
      crowdCount: 0,
      metadata: {},
      bannedUsers: new Set(),
    };

    this.lobbies.set(lobbyKey, room);
    if (!this.venueLobbies.has(params.venueId)) this.venueLobbies.set(params.venueId, new Set());
    this.venueLobbies.get(params.venueId)!.add(lobbyKey);

    logger.info({ lobbyId: lobbyKey, venueId: params.venueId, createdBy: params.createdBy }, 'Club lobby created');
    this.persist(room).catch(() => {});
    return room;
  }

  getLobby(lobbyId: string): LobbyRoom | null {
    return this.lobbies.get(lobbyId) || null;
  }

  getLobbyByVenue(venueId: string, channel = 'GENERAL'): LobbyRoom | null {
    const key = `lobby_${venueId}_${channel}`;
    return this.lobbies.get(key) || null;
  }

  addParticipant(lobbyId: string, userId: string, role: ParticipantRole = ParticipantRole.PARTICIPANT, socketId?: string, displayName?: string): LobbyRoom {
    const lobby = this.lobbies.get(lobbyId);
    if (!lobby) throw new AppError({ code: ErrorCodes.LOBBY_NOT_FOUND, correlationId: generateCorrelationId() });
    if (lobby.bannedUsers.has(userId)) throw new AppError({ code: ErrorCodes.LOBBY_BANNED, correlationId: generateCorrelationId() });
    if (lobby.isLocked && lobby.createdBy !== userId) throw new AppError({ code: ErrorCodes.ROOM_LOCKED, correlationId: generateCorrelationId() });
    if (lobby.participants.size >= lobby.maxParticipants) throw new AppError({ code: ErrorCodes.LOBBY_FULL, correlationId: generateCorrelationId() });

    lobby.participants.set(userId, { userId, role: lobby.createdBy === userId ? ParticipantRole.HOST : role, joinedAt: Date.now(), socketId, displayName, isMuted: false });
    lobby.lastActivityAt = Date.now();
    lobby.crowdCount = lobby.participants.size;

    this.persist(lobby).catch(() => {});
    return lobby;
  }

  removeParticipant(lobbyId: string, userId: string): LobbyRoom | null {
    const lobby = this.lobbies.get(lobbyId);
    if (!lobby) return null;
    lobby.participants.delete(userId);
    lobby.lastActivityAt = Date.now();
    lobby.crowdCount = lobby.participants.size;

    if (lobby.participants.size === 0) {
      // Keep lobby for some time? Delete after idle? For now keep
      this.persist(lobby).catch(() => {});
      return lobby;
    }

    this.persist(lobby).catch(() => {});
    return lobby;
  }

  banUser(lobbyId: string, requesterId: string, targetUserId: string): void {
    const lobby = this.lobbies.get(lobbyId);
    if (!lobby) throw new AppError({ code: ErrorCodes.LOBBY_NOT_FOUND, correlationId: generateCorrelationId() });
    const requester = lobby.participants.get(requesterId);
    if (!requester || (requester.role !== ParticipantRole.HOST && requester.role !== ParticipantRole.MODERATOR && lobby.createdBy !== requesterId)) {
      throw new AppError({ code: ErrorCodes.LOBBY_NOT_HOST, correlationId: generateCorrelationId() });
    }
    lobby.bannedUsers.add(targetUserId);
    lobby.participants.delete(targetUserId);
    lobby.crowdCount = lobby.participants.size;
    this.persist(lobby).catch(() => {});
  }

  updateCrowdCount(venueId: string, count: number): void {
    const lobbies = this.venueLobbies.get(venueId);
    if (!lobbies) return;
    for (const lid of lobbies) {
      const lobby = this.lobbies.get(lid);
      if (lobby) {
        lobby.crowdCount = count;
        lobby.lastActivityAt = Date.now();
      }
    }
  }

  listLobbies(filter?: { venueId?: string }): LobbyRoom[] {
    if (filter?.venueId) {
      const ids = this.venueLobbies.get(filter.venueId);
      if (!ids) return [];
      return Array.from(ids).map(id => this.lobbies.get(id)).filter(Boolean) as LobbyRoom[];
    }
    return Array.from(this.lobbies.values());
  }

  private async persist(lobby: LobbyRoom): Promise<void> {
    if (!env.REDIS_ENABLED) return;
    try {
      const rc = await getRedisClient();
      if (!rc) return;
      await rc.set(getRedisKey('lobby', lobby.id), JSON.stringify({ ...lobby, participants: Array.from(lobby.participants.entries()), bannedUsers: Array.from(lobby.bannedUsers) }), { EX: 3600 });
    } catch {}
  }
}

export const lobbyEngine = new LobbyEngine();
