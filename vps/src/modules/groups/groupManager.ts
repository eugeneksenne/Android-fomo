import { RoomType, ParticipantRole } from '../../shared/types/enums.js';
import { generateRoomId } from '../../shared/utils/id.js';
import { logger } from '../../core/logger/logger.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { getRedisClient, getRedisKey } from '../../core/redis/client.js';
import { env } from '../../config/env.js';

export interface GroupRoom {
  id: string;
  type: RoomType.GROUP | RoomType.CHAT | RoomType.LIVE | RoomType.MAP;
  name?: string;
  createdBy: string;
  createdAt: number;
  lastActivityAt: number;
  participants: Map<string, { userId: string; role: ParticipantRole; joinedAt: number; socketId?: string; displayName?: string }>;
  maxParticipants: number;
  isLocked: boolean;
  metadata: Record<string, unknown>;
}

class GroupManager {
  private groups = new Map<string, GroupRoom>();
  private userGroups = new Map<string, Set<string>>();

  createGroup(params: { type: RoomType; name?: string; createdBy: string; maxParticipants?: number; metadata?: Record<string, unknown> }): GroupRoom {
    const id = generateRoomId('group', params.type.toLowerCase());
    const group: GroupRoom = {
      id,
      type: params.type as any,
      name: params.name,
      createdBy: params.createdBy,
      createdAt: Date.now(),
      lastActivityAt: Date.now(),
      participants: new Map(),
      maxParticipants: params.maxParticipants || 100,
      isLocked: false,
      metadata: params.metadata || {},
    };
    this.groups.set(id, group);
    logger.info({ groupId: id, type: params.type, createdBy: params.createdBy }, 'Group created');
    this.persist(group).catch(() => {});
    return group;
  }

  getGroup(groupId: string): GroupRoom | null {
    return this.groups.get(groupId) || null;
  }

  addParticipant(groupId: string, userId: string, role: ParticipantRole = ParticipantRole.PARTICIPANT, socketId?: string, displayName?: string): GroupRoom {
    const group = this.groups.get(groupId);
    if (!group) throw new AppError({ code: ErrorCodes.ROOM_NOT_FOUND, correlationId: generateCorrelationId() });
    if (group.isLocked) throw new AppError({ code: ErrorCodes.ROOM_LOCKED, correlationId: generateCorrelationId() });
    if (group.participants.size >= group.maxParticipants) throw new AppError({ code: ErrorCodes.ROOM_FULL, correlationId: generateCorrelationId() });

    group.participants.set(userId, { userId, role, joinedAt: Date.now(), socketId, displayName });
    group.lastActivityAt = Date.now();

    if (!this.userGroups.has(userId)) this.userGroups.set(userId, new Set());
    this.userGroups.get(userId)!.add(groupId);

    this.persist(group).catch(() => {});
    return group;
  }

  removeParticipant(groupId: string, userId: string): GroupRoom | null {
    const group = this.groups.get(groupId);
    if (!group) return null;
    group.participants.delete(userId);
    this.userGroups.get(userId)?.delete(groupId);
    group.lastActivityAt = Date.now();

    if (group.participants.size === 0) {
      this.groups.delete(groupId);
      this.removeFromRedis(groupId).catch(() => {});
      logger.info({ groupId }, 'Group deleted (empty)');
      return null;
    }

    this.persist(group).catch(() => {});
    return group;
  }

  listGroups(filter?: { type?: RoomType; createdBy?: string }): GroupRoom[] {
    let groups = Array.from(this.groups.values());
    if (filter?.type) groups = groups.filter(g => g.type === filter.type);
    if (filter?.createdBy) groups = groups.filter(g => g.createdBy === filter.createdBy);
    return groups;
  }

  private async persist(group: GroupRoom): Promise<void> {
    if (!env.REDIS_ENABLED) return;
    try {
      const rc = await getRedisClient();
      if (!rc) return;
      await rc.set(getRedisKey('group', group.id), JSON.stringify({ ...group, participants: Array.from(group.participants.entries()) }), { EX: 3600 });
    } catch {}
  }

  private async removeFromRedis(groupId: string): Promise<void> {
    if (!env.REDIS_ENABLED) return;
    try {
      const rc = await getRedisClient();
      if (!rc) return;
      await rc.del(getRedisKey('group', groupId));
    } catch {}
  }
}

export const groupManager = new GroupManager();
