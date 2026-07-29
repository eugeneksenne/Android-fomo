import { Socket } from 'socket.io';
import { logger } from '../../core/logger/logger.js';
import { metricsCollector } from '../../core/monitoring/metrics.js';
import { getRedisClient, getRedisKey } from '../../core/redis/client.js';
import { env } from '../../config/env.js';
import { ConnectionQuality, DevicePriority } from '../../shared/types/enums.js';
import { SocketSession } from '../../shared/types/socket.js';
import { generateShortId } from '../../shared/utils/id.js';
import { nowMs, exponentialBackoff } from '../../shared/utils/time.js';

interface ConnectionRecord {
  socketId: string;
  userId: string;
  deviceId: string;
  sessionId: string;
  connectedAt: number;
  lastHeartbeatAt: number;
  lastActivityAt: number;
  connectionQuality: ConnectionQuality;
  reconnectAttempts: number;
  ip?: string;
  priority: DevicePriority;
  isOnline: boolean;
}

class ConnectionManager {
  private connections = new Map<string, ConnectionRecord>(); // socketId -> record
  private userConnections = new Map<string, Set<string>>(); // userId -> Set<socketId>
  private heartbeatInterval: NodeJS.Timeout | null = null;
  private qualityInterval: NodeJS.Timeout | null = null;

  start(): void {
    this.heartbeatInterval = setInterval(() => this.checkHeartbeats(), 30_000);
    this.qualityInterval = setInterval(() => this.assessQuality(), 60_000);
    logger.info('ConnectionManager started');
  }

  stop(): void {
    if (this.heartbeatInterval) clearInterval(this.heartbeatInterval);
    if (this.qualityInterval) clearInterval(this.qualityInterval);
  }

  addConnection(socket: Socket, session: SocketSession): void {
    const record: ConnectionRecord = {
      socketId: socket.id,
      userId: session.userId,
      deviceId: session.deviceId,
      sessionId: session.sessionId,
      connectedAt: session.connectedAt,
      lastHeartbeatAt: session.lastHeartbeatAt,
      lastActivityAt: session.lastActivityAt,
      connectionQuality: ConnectionQuality.EXCELLENT,
      reconnectAttempts: session.reconnectAttempts,
      ip: (socket.handshake.address as string) || undefined,
      priority: (socket.data as any)?.device?.priority || DevicePriority.PRIMARY,
      isOnline: true,
    };

    this.connections.set(socket.id, record);
    if (!this.userConnections.has(session.userId)) this.userConnections.set(session.userId, new Set());
    this.userConnections.get(session.userId)!.add(socket.id);

    metricsCollector.incConnection();
    metricsCollector.incAuthenticated();
    metricsCollector.trackUserOnline(session.userId);

    // Persist to Redis for distributed presence
    if (env.REDIS_ENABLED) {
      this.persistToRedis(record).catch(err => logger.warn({ err: err.message }, 'Failed to persist connection to Redis'));
    }

    logger.debug({ userId: session.userId, socketId: socket.id, deviceId: session.deviceId }, 'Connection added');
  }

  removeConnection(socketId: string): ConnectionRecord | null {
    const rec = this.connections.get(socketId);
    if (!rec) return null;

    this.connections.delete(socketId);
    this.userConnections.get(rec.userId)?.delete(socketId);
    if (this.userConnections.get(rec.userId)?.size === 0) {
      this.userConnections.delete(rec.userId);
    }

    metricsCollector.decConnection();
    metricsCollector.decAuthenticated();

    if (env.REDIS_ENABLED) {
      this.removeFromRedis(rec).catch(err => logger.warn({ err: err.message }, 'Failed to remove connection from Redis'));
    }

    return rec;
  }

  updateHeartbeat(socketId: string, quality?: ConnectionQuality): void {
    const rec = this.connections.get(socketId);
    if (!rec) return;
    rec.lastHeartbeatAt = nowMs();
    rec.lastActivityAt = nowMs();
    if (quality) rec.connectionQuality = quality;
  }

  updateActivity(socketId: string): void {
    const rec = this.connections.get(socketId);
    if (rec) rec.lastActivityAt = nowMs();
  }

  getConnection(socketId: string): ConnectionRecord | null {
    return this.connections.get(socketId) || null;
  }

  getUserConnections(userId: string): ConnectionRecord[] {
    const socketIds = this.userConnections.get(userId);
    if (!socketIds) return [];
    return Array.from(socketIds).map(id => this.connections.get(id)).filter(Boolean) as ConnectionRecord[];
  }

  getUserConnectionCount(userId: string): number {
    return this.userConnections.get(userId)?.size || 0;
  }

  getAllConnections(): ConnectionRecord[] {
    return Array.from(this.connections.values());
  }

  getOnlineCount(): number {
    return this.userConnections.size;
  }

  getConnectionCount(): number {
    return this.connections.size;
  }

  // Duplicate detection
  hasDuplicateConnection(userId: string, deviceId: string, currentSocketId: string): boolean {
    const conns = this.getUserConnections(userId).filter(c => c.deviceId === deviceId && c.socketId !== currentSocketId);
    return conns.length > 0;
  }

  getPrimarySocketId(userId: string): string | null {
    const conns = this.getUserConnections(userId);
    if (conns.length === 0) return null;
    const sorted = conns.sort((a, b) => b.priority - a.priority || b.lastActivityAt - a.lastActivityAt);
    return sorted[0]?.socketId || null;
  }

  // Heartbeat checker - marks stale connections
  private checkHeartbeats(): void {
    const now = nowMs();
    const timeout = env.SOCKET_PING_INTERVAL_MS + env.SOCKET_PING_TIMEOUT_MS + 5000;
    for (const [socketId, rec] of this.connections.entries()) {
      if (now - rec.lastHeartbeatAt > timeout) {
        logger.warn({ socketId, userId: rec.userId, lastHeartbeat: rec.lastHeartbeatAt, timeout }, 'Heartbeat timeout - connection stale');
        rec.connectionQuality = ConnectionQuality.DISCONNECTED;
        rec.isOnline = false;
      }
    }
  }

  // Quality assessment based on heartbeat latency
  private assessQuality(): void {
    for (const rec of this.connections.values()) {
      const sinceLastHb = nowMs() - rec.lastHeartbeatAt;
      if (sinceLastHb < 30_000) rec.connectionQuality = ConnectionQuality.EXCELLENT;
      else if (sinceLastHb < 60_000) rec.connectionQuality = ConnectionQuality.GOOD;
      else if (sinceLastHb < 120_000) rec.connectionQuality = ConnectionQuality.POOR;
      else rec.connectionQuality = ConnectionQuality.DISCONNECTED;
    }
  }

  // Reconnect queue logic helper
  getReconnectDelay(attempt: number): number {
    return exponentialBackoff(attempt, 1000, 30000, true);
  }

  // Redis persistence for horizontal scaling
  private async persistToRedis(rec: ConnectionRecord): Promise<void> {
    const rc = await getRedisClient();
    if (!rc) return;
    const key = getRedisKey('conn', rec.socketId);
    await rc.set(key, JSON.stringify(rec), { EX: 300 }); // 5min TTL, refreshed on heartbeat
    const userKey = getRedisKey('user_conns', rec.userId);
    await rc.sAdd(userKey, rec.socketId);
    await rc.expire(userKey, 300);
  }

  private async removeFromRedis(rec: ConnectionRecord): Promise<void> {
    const rc = await getRedisClient();
    if (!rc) return;
    await rc.del(getRedisKey('conn', rec.socketId));
    await rc.sRem(getRedisKey('user_conns', rec.userId), rec.socketId);
  }

  // Session restoration helper
  findRestorableSession(userId: string, deviceId: string): ConnectionRecord | null {
    const conns = this.getUserConnections(userId).filter(c => c.deviceId === deviceId);
    if (conns.length === 0) return null;
    return conns[0] ?? null;
  }
}

export const connectionManager = new ConnectionManager();
