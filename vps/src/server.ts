import { buildApp } from './app.js';
import { env } from './config/env.js';
import { logger } from './core/logger/logger.js';
import { initTelemetry, shutdownTelemetry } from './core/telemetry/otel.js';
import { createSocketServer } from './modules/socket/socketServer.js';
import { getRedisClient, disconnectRedis } from './core/redis/client.js';
import { teardownRedisAdapter } from './core/redis/adapter.js';
import { setReady, setShuttingDown } from './core/monitoring/health.js';
import { metricsCollector } from './core/monitoring/metrics.js';
import { connectionManager } from './modules/socket/connectionManager.js';

async function main(): Promise<void> {
  logger.info({ env: env.NODE_ENV, port: env.PORT, version: '2.0.0' }, '🚀 Starting FOMO Signaling Server');

  // Init OpenTelemetry if enabled
  await initTelemetry();

  // Init Redis if enabled
  if (env.REDIS_ENABLED) {
    const rc = await getRedisClient();
    if (!rc) {
      logger.warn('Redis enabled but connection failed - continuing with in-memory (degraded)');
    }
  }

  // Build Fastify app
  const app = await buildApp();

  // Start HTTP server
  try {
    await app.listen({ port: env.PORT, host: env.HOST });
    logger.info({ port: env.PORT, host: env.HOST, health: env.HEALTH_CHECK_PATH }, `✅ HTTP server listening on http://${env.HOST}:${env.PORT}`);
  } catch (err) {
    logger.fatal({ err: (err as Error).message }, 'Failed to start HTTP server');
    process.exit(1);
  }

  // Attach Socket.IO to underlying http server
  const httpServer = app.server;
  const io = await createSocketServer(httpServer, app);

  logger.info({
    transports: ['websocket', 'polling'],
    pingInterval: env.SOCKET_PING_INTERVAL_MS,
    pingTimeout: env.SOCKET_PING_TIMEOUT_MS,
    recovery: env.SOCKET_CONNECTION_STATE_RECOVERY,
    maxCallParticipants: env.MAX_CALL_PARTICIPANTS,
    redis: env.REDIS_ENABLED,
    origins: env.ALLOWED_ORIGINS,
  }, '✅ Socket.IO server attached');

  // Graceful shutdown handler
  const shutdown = async (signal: string) => {
    logger.info({ signal }, `Received ${signal}, initiating graceful shutdown...`);
    setShuttingDown(true);
    setReady(false);

    // Stop accepting new connections
    io.engine.close();
    logger.info('Socket.IO engine closed - no new connections');

    // Give 10s for existing connections to close gracefully
    const sockets = await io.fetchSockets();
    logger.info({ count: sockets.length }, `Disconnecting ${sockets.length} active sockets gracefully`);

    // Notify clients of shutdown
    io.emit('server:shutdown', {
      message: 'Server shutting down, please reconnect',
      timestamp: new Date().toISOString(),
      reconnectAfterMs: 5000,
    });

    // Close sockets after short delay
    setTimeout(() => {
      for (const s of sockets) {
        try {
          (s as any).disconnect(true);
        } catch {}
      }
    }, 2000);

    // Wait for connections to drain (up to 10s)
    const drainStart = Date.now();
    while (connectionManager.getConnectionCount() > 0 && Date.now() - drainStart < 10_000) {
      await new Promise(r => setTimeout(r, 500));
      logger.info({ remaining: connectionManager.getConnectionCount() }, 'Waiting for connections to drain');
    }

    // Close Fastify
    try {
      await app.close();
      logger.info('Fastify server closed');
    } catch (err) {
      logger.warn({ err: (err as Error).message }, 'Error closing Fastify');
    }

    // Teardown Redis
    try {
      await teardownRedisAdapter();
      await disconnectRedis();
      logger.info('Redis disconnected');
    } catch (err) {
      logger.warn({ err: (err as Error).message }, 'Error disconnecting Redis');
    }

    // Metrics
    metricsCollector.stop();
    connectionManager.stop();

    // Telemetry
    await shutdownTelemetry();

    logger.info('Graceful shutdown complete');
    process.exit(0);
  };

  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));

  process.on('uncaughtException', (err) => {
    logger.fatal({ err: err.message, stack: err.stack }, 'Uncaught Exception');
    shutdown('uncaughtException').catch(() => process.exit(1));
  });

  process.on('unhandledRejection', (reason, promise) => {
    logger.fatal({ reason, promise }, 'Unhandled Rejection');
  });

  // Periodic cleanup tasks
  setInterval(() => {
    // Presence cleanup could be done here
    // Session cleanup etc.
  }, 60_000);

  logger.info('🎉 FOMO Signaling Server ready - waiting for connections');
}

main().catch(err => {
  logger.fatal({ err: (err as Error).message, stack: (err as Error).stack }, 'Failed to start server');
  process.exit(1);
});
