import { ErrorCode } from '../errors/errorCodes.js';

export interface ApiSuccess<T = unknown> {
  success: true;
  data: T;
  correlationId: string;
  timestamp: string;
}

export interface ApiError {
  success: false;
  error: {
    code: ErrorCode;
    message: string;
    developerMessage: string;
    correlationId: string;
    recovery: string;
    timestamp: string;
    details?: unknown;
  };
}

export type ApiResponse<T> = ApiSuccess<T> | ApiError;

export interface HealthResponse {
  status: 'ok' | 'degraded' | 'down';
  uptime: number;
  version: string;
  env: string;
  timestamp: string;
  checks: {
    redis: 'ok' | 'fail' | 'disabled';
    supabase: 'ok' | 'fail' | 'not_configured';
    memory: 'ok' | 'warn' | 'critical';
  };
  stats?: {
    connections: number;
    users: number;
    rooms: number;
    calls: number;
  };
}

export interface MetricsSnapshot {
  timestamp: string;
  connections: {
    total: number;
    authenticated: number;
    peakLastHour: number;
  };
  users: {
    online: number;
    uniqueLastHour: number;
  };
  calls: {
    active: number;
    totalLastHour: number;
    failedLastHour: number;
  };
  latency: {
    p50: number;
    p95: number;
    p99: number;
    avg: number;
  };
  signaling: {
    offersPerSec: number;
    answersPerSec: number;
    icePerSec: number;
    eventsDropped: number;
  };
  reconnect: {
    rate: number;
    avgAttempts: number;
  };
  errors: {
    rate: number;
    byCode: Record<string, number>;
  };
  redis: {
    latencyMs: number;
    status: string;
  };
  system: {
    memoryUsedMb: number;
    memoryTotalMb: number;
    cpuUsagePercent: number;
    eventLoopLagMs: number;
  };
}
