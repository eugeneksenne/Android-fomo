import { FastifyInstance } from 'fastify';
import { env } from '../../config/env.js';
import { logger } from '../logger/logger.js';
import { getRedisClient, measureRedisLatency } from '../redis/client.js';
import { metricsCollector } from './metrics.js';
import { HealthResponse } from '../../shared/types/api.js';

let isReady = false;
let isShuttingDown = false;

export function setReady(ready: boolean): void {
  isReady = ready;
  logger.info({ ready }, `Readiness set to ${ready}`);
}

export function setShuttingDown(shuttingDown: boolean): void {
  isShuttingDown = shuttingDown;
}

export function registerHealthRoutes(app: FastifyInstance): void {
  // Health - basic liveness
  app.get(env.HEALTH_CHECK_PATH, async (req, reply) => {
    const redis = env.REDIS_ENABLED ? await getRedisClient() : null;
    const redisLatency = await measureRedisLatency();
    const mem = process.memoryUsage();
    const memOk = mem.heapUsed / mem.heapTotal < 0.9 ? 'ok' : mem.heapUsed / mem.heapTotal < 0.95 ? 'warn' : 'critical';

    const response: HealthResponse = {
      status: isShuttingDown ? 'down' : memOk === 'critical' ? 'degraded' : 'ok',
      uptime: process.uptime(),
      version: '2.0.0',
      env: env.NODE_ENV,
      timestamp: new Date().toISOString(),
      checks: {
        redis: !env.REDIS_ENABLED ? 'disabled' : redis && redis.isOpen ? 'ok' : 'fail',
        supabase: env.SUPABASE_URL ? 'ok' : 'not_configured',
        memory: memOk as any,
      },
      stats: {
        connections: metricsCollector.getSnapshot().connections.total,
        users: metricsCollector.getSnapshot().users.online,
        rooms: 0,
        calls: metricsCollector.getSnapshot().calls.active,
      },
    };

    if (redisLatency >= 0) metricsCollector.setRedisLatency(redisLatency);

    const statusCode = response.status === 'ok' ? 200 : response.status === 'degraded' ? 200 : 503;
    return reply.code(statusCode).send(response);
  });

  // Readiness - ready to receive traffic?
  app.get(env.READINESS_CHECK_PATH, async (req, reply) => {
    if (!isReady || isShuttingDown) {
      return reply.code(503).send({
        status: 'not_ready',
        ready: isReady,
        shuttingDown: isShuttingDown,
        timestamp: new Date().toISOString(),
      });
    }

    if (env.REDIS_ENABLED) {
      const rc = await getRedisClient();
      if (!rc || !rc.isOpen) {
        return reply.code(503).send({ status: 'redis_not_ready', timestamp: new Date().toISOString() });
      }
    }

    return reply.send({
      status: 'ready',
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
    });
  });

  // Liveness - for k8s/Railway liveness probe
  app.get(env.LIVENESS_CHECK_PATH, async (req, reply) => {
    if (isShuttingDown) {
      return reply.code(503).send({ status: 'shutting_down' });
    }
    return reply.send({ status: 'alive', timestamp: new Date().toISOString() });
  });

  // Metrics
  app.get('/metrics', async (req, reply) => {
    if (!env.METRICS_ENABLED) {
      return reply.code(404).send({ error: 'Metrics disabled' });
    }
    const snapshot = metricsCollector.getSnapshot();
    return reply.send(snapshot);
  });

  // Detailed stats (auth required - but allow if no auth header for internal)
  app.get('/stats', async (req, reply) => {
    const snapshot = metricsCollector.getSnapshot();
    return reply.send(snapshot);
  });
}
