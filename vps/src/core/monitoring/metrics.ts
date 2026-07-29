import { MetricsSnapshot } from '../../shared/types/api.js';
import { env } from '../../config/env.js';
import { logger } from '../logger/logger.js';

interface Histogram {
  values: number[];
  add: (v: number) => void;
  getPercentile: (p: number) => number;
  getAvg: () => number;
}

function createHistogram(maxValues = 1000): Histogram {
  const values: number[] = [];
  return {
    values,
    add(v: number) {
      values.push(v);
      if (values.length > maxValues) values.shift();
    },
    getPercentile(p: number) {
      if (values.length === 0) return 0;
      const sorted = [...values].sort((a, b) => a - b);
      const idx = Math.ceil((p / 100) * sorted.length) - 1;
      return sorted[Math.max(0, idx)] ?? 0;
    },
    getAvg() {
      if (values.length === 0) return 0;
      return values.reduce((a, b) => a + b, 0) / values.length;
    },
  };
}

class MetricsCollector {
  private connections = 0;
  private authenticatedConnections = 0;
  private peakConnections = 0;
  private uniqueUsersLastHour = new Set<string>();
  private callsActive = 0;
  private callsTotalLastHour = 0;
  private callsFailedLastHour = 0;
  private latencyHist: Histogram = createHistogram(5000);
  private errorCounts: Record<string, number> = {};
  private eventsDropped = 0;
  private reconnectRate = 0;
  private reconnectAttempts: number[] = [];
  private offersPerSec = 0;
  private answersPerSec = 0;
  private icePerSec = 0;
  private lastHourReset = Date.now();
  private redisLatency = 0;

  private offerCountWindow = 0;
  private answerCountWindow = 0;
  private iceCountWindow = 0;
  private perSecInterval: NodeJS.Timeout | null = null;

  constructor() {
    this.perSecInterval = setInterval(() => this.tickPerSec(), 1000);
    setInterval(() => this.resetHourlyIfNeeded(), 60_000);
  }

  private tickPerSec(): void {
    this.offersPerSec = this.offerCountWindow;
    this.answersPerSec = this.answerCountWindow;
    this.icePerSec = this.iceCountWindow;
    this.offerCountWindow = 0;
    this.answerCountWindow = 0;
    this.iceCountWindow = 0;
  }

  private resetHourlyIfNeeded(): void {
    if (Date.now() - this.lastHourReset > 3_600_000) {
      this.uniqueUsersLastHour.clear();
      this.callsTotalLastHour = 0;
      this.callsFailedLastHour = 0;
      this.lastHourReset = Date.now();
      logger.info('Hourly metrics reset');
    }
  }

  // Connection tracking
  incConnection(): void {
    this.connections++;
    if (this.connections > this.peakConnections) this.peakConnections = this.connections;
  }

  decConnection(): void {
    this.connections = Math.max(0, this.connections - 1);
  }

  incAuthenticated(): void {
    this.authenticatedConnections++;
  }

  decAuthenticated(): void {
    this.authenticatedConnections = Math.max(0, this.authenticatedConnections - 1);
  }

  trackUserOnline(userId: string): void {
    this.uniqueUsersLastHour.add(userId);
  }

  // Calls
  incActiveCall(): void {
    this.callsActive++;
    this.callsTotalLastHour++;
  }

  decActiveCall(failed = false): void {
    this.callsActive = Math.max(0, this.callsActive - 1);
    if (failed) this.callsFailedLastHour++;
  }

  // Latency
  recordLatency(ms: number): void {
    this.latencyHist.add(ms);
  }

  // Signaling
  recordOffer(): void {
    this.offerCountWindow++;
  }

  recordAnswer(): void {
    this.answerCountWindow++;
  }

  recordIce(): void {
    this.iceCountWindow++;
  }

  recordDroppedEvent(): void {
    this.eventsDropped++;
  }

  recordError(code: string): void {
    this.errorCounts[code] = (this.errorCounts[code] || 0) + 1;
  }

  recordReconnect(attempts: number): void {
    this.reconnectAttempts.push(attempts);
    if (this.reconnectAttempts.length > 1000) this.reconnectAttempts.shift();
    this.reconnectRate = this.reconnectAttempts.length;
  }

  setRedisLatency(ms: number): void {
    this.redisLatency = ms;
  }

  getSnapshot(): MetricsSnapshot {
    const mem = process.memoryUsage();
    const cpu = process.cpuUsage();

    return {
      timestamp: new Date().toISOString(),
      connections: {
        total: this.connections,
        authenticated: this.authenticatedConnections,
        peakLastHour: this.peakConnections,
      },
      users: {
        online: this.uniqueUsersLastHour.size,
        uniqueLastHour: this.uniqueUsersLastHour.size,
      },
      calls: {
        active: this.callsActive,
        totalLastHour: this.callsTotalLastHour,
        failedLastHour: this.callsFailedLastHour,
      },
      latency: {
        p50: this.latencyHist.getPercentile(50),
        p95: this.latencyHist.getPercentile(95),
        p99: this.latencyHist.getPercentile(99),
        avg: this.latencyHist.getAvg(),
      },
      signaling: {
        offersPerSec: this.offersPerSec,
        answersPerSec: this.answersPerSec,
        icePerSec: this.icePerSec,
        eventsDropped: this.eventsDropped,
      },
      reconnect: {
        rate: this.reconnectRate,
        avgAttempts: this.reconnectAttempts.length ? this.reconnectAttempts.reduce((a, b) => a + b, 0) / this.reconnectAttempts.length : 0,
      },
      errors: {
        rate: Object.values(this.errorCounts).reduce((a, b) => a + b, 0),
        byCode: { ...this.errorCounts },
      },
      redis: {
        latencyMs: this.redisLatency,
        status: env.REDIS_ENABLED ? (this.redisLatency >= 0 ? 'ok' : 'fail') : 'disabled',
      },
      system: {
        memoryUsedMb: Math.round(mem.heapUsed / 1024 / 1024),
        memoryTotalMb: Math.round(mem.heapTotal / 1024 / 1024),
        cpuUsagePercent: Math.round((cpu.user + cpu.system) / 1_000_000),
        eventLoopLagMs: 0, // could measure with perf_hooks
      },
    };
  }

  stop(): void {
    if (this.perSecInterval) clearInterval(this.perSecInterval);
  }
}

export const metricsCollector = new MetricsCollector();
