import Fastify, { FastifyInstance } from 'fastify';
import fastifyHelmet from '@fastify/helmet';
import fastifyCors from '@fastify/cors';
import fastifyRateLimit from '@fastify/rate-limit';
import { env } from './config/env.js';
import { logger } from './core/logger/logger.js';
import { registerHealthRoutes, setReady } from './core/monitoring/health.js';
import { registerErrorHandler } from './core/middleware/errorHandler.js';
import { fastifyAuthPreHandler } from './core/middleware/auth.js';
import { metricsCollector } from './core/monitoring/metrics.js';
import { presenceEngine } from './modules/presence/presenceEngine.js';
import { callEngine } from './modules/calls/callEngine.js';
import { lobbyEngine } from './modules/club-lobby/lobbyEngine.js';

export async function buildApp(): Promise<FastifyInstance> {
  const app = Fastify({
    logger: false, // we use pino directly
    trustProxy: true,
    bodyLimit: 1 * 1024 * 1024, // 1MB
  });

  // Helmet
  if (env.HELMET_ENABLED) {
    await app.register(fastifyHelmet, {
      contentSecurityPolicy: false, // API, not HTML
      crossOriginEmbedderPolicy: false,
    });
  }

  // CORS
  await app.register(fastifyCors, {
    origin: env.ALLOWED_ORIGINS === '*' ? true : env.ALLOWED_ORIGINS.split(',').map(o => o.trim()),
    credentials: env.CORS_CREDENTIALS,
    methods: ['GET', 'POST', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'x-device-id', 'x-correlation-id'],
  });

  // Global rate limit
  await app.register(fastifyRateLimit, {
    global: true,
    max: env.RATE_LIMIT_GLOBAL_MAX,
    timeWindow: env.RATE_LIMIT_GLOBAL_WINDOW_MS,
    addHeaders: {
      'x-ratelimit-limit': true,
      'x-ratelimit-remaining': true,
      'x-ratelimit-reset': true,
    },
    keyGenerator: (req) => (req.headers['x-forwarded-for'] as string) || req.ip,
    errorResponseBuilder: (req, context) => ({
      success: false,
      error: {
        code: 'RATE_LIMIT_EXCEEDED',
        message: 'Too many requests, slow down',
        correlationId: (req.headers['x-correlation-id'] as string) || 'unknown',
        recovery: `Retry after ${Math.ceil(Number(context.after) / 1000)} seconds`,
        timestamp: new Date().toISOString(),
      },
    }),
  });

  // Health routes (no auth)
  registerHealthRoutes(app);

  // Decorate with auth hook for protected routes
  const protectedRoutes = async (fastify: FastifyInstance) => {
    fastify.addHook('preHandler', async (request, reply) => {
      // Allow health/readiness without auth, but stats requires auth
      if (request.url.startsWith('/health') || request.url.startsWith('/ready') || request.url.startsWith('/live') || request.url.startsWith('/metrics')) {
        return;
      }
      await fastifyAuthPreHandler(request, reply).catch(err => {
        throw err;
      });
    });

    // Example: presence online list via REST
    fastify.get('/presence/online', async (req, res) => {
      const query = req.query as any;
      const limit = Math.min(parseInt(query.limit) || 50, 200);
      const online = await presenceEngine.getOnlineUsers(limit);
      return {
        success: true,
        data: { onlineCount: online.length, users: online },
        correlationId: (req.headers['x-correlation-id'] as string) || 'none',
        timestamp: new Date().toISOString(),
      };
    });

    fastify.get('/calls/active', async (req, res) => {
      const calls = callEngine.getAllCalls().filter(c => c.state !== 'ENDED');
      return {
        success: true,
        data: { count: calls.length, calls: calls.slice(0, 50).map(c => ({ id: c.id, type: c.type, state: c.state, participantCount: c.participants.size, createdAt: c.createdAt })) },
        correlationId: (req.headers['x-correlation-id'] as string) || 'none',
        timestamp: new Date().toISOString(),
      };
    });

    fastify.get('/lobbies', async (req, res) => {
      const query = req.query as any;
      const lobbies = lobbyEngine.listLobbies({ venueId: query.venueId });
      return {
        success: true,
        data: { count: lobbies.length, lobbies: lobbies.slice(0, 50).map(l => ({ id: l.id, venueId: l.venueId, crowdCount: l.crowdCount, participantCount: l.participants.size })) },
        correlationId: (req.headers['x-correlation-id'] as string) || 'none',
        timestamp: new Date().toISOString(),
      };
    });

    fastify.get('/stats/detailed', async (req, res) => {
      const snapshot = metricsCollector.getSnapshot();
      return {
        success: true,
        data: snapshot,
        correlationId: (req.headers['x-correlation-id'] as string) || 'none',
        timestamp: new Date().toISOString(),
      };
    });
  };

  await app.register(protectedRoutes);

  // Root route
  app.get('/', async (req, reply) => {
    return {
      name: 'FOMO Signaling Server',
      version: '2.0.0',
      env: env.NODE_ENV,
      status: 'running',
      uptime: process.uptime(),
      timestamp: new Date().toISOString(),
      health: env.HEALTH_CHECK_PATH,
      readiness: env.READINESS_CHECK_PATH,
      metrics: env.METRICS_ENABLED ? '/metrics' : 'disabled',
      socket: {
        transports: ['websocket', 'polling'],
        recovery: env.SOCKET_CONNECTION_STATE_RECOVERY,
      },
      stats: {
        connections: metricsCollector.getSnapshot().connections.total,
        usersOnline: metricsCollector.getSnapshot().users.online,
        callsActive: metricsCollector.getSnapshot().calls.active,
      },
    };
  });

  registerErrorHandler(app);

  // Set ready after build
  setReady(true);

  return app;
}
