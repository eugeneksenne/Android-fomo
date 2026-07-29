import { createAdapter } from '@socket.io/redis-adapter';
import { Server as SocketIOServer } from 'socket.io';
import { createClient } from 'redis';
import { env } from '../../config/env.js';
import { logger } from '../logger/logger.js';

export interface RedisAdapterClients {
  pubClient: ReturnType<typeof createClient>;
  subClient: ReturnType<typeof createClient>;
}

let adapterClients: RedisAdapterClients | null = null;

export async function setupRedisAdapter(io: SocketIOServer): Promise<void> {
  if (!env.REDIS_ENABLED) {
    logger.info('Redis adapter disabled - using in-memory');
    return;
  }

  try {
    const pubClient = createClient({
      socket: {
        host: env.REDIS_HOST,
        port: env.REDIS_PORT,
        tls: env.REDIS_TLS,
      },
      password: env.REDIS_PASSWORD || undefined,
      database: env.REDIS_DB,
    });

    const subClient = pubClient.duplicate();

    pubClient.on('error', (err) => logger.error({ err: err.message }, 'Redis pub error'));
    subClient.on('error', (err) => logger.error({ err: err.message }, 'Redis sub error'));

    await Promise.all([pubClient.connect(), subClient.connect()]);

    io.adapter(createAdapter(pubClient as any, subClient as any));
    adapterClients = { pubClient, subClient };

    logger.info({ host: env.REDIS_HOST, port: env.REDIS_PORT }, 'Socket.IO Redis adapter enabled - horizontal scaling active');
  } catch (err) {
    logger.warn({ err: (err as Error).message }, 'Redis adapter setup failed - falling back to in-memory');
  }
}

export async function teardownRedisAdapter(): Promise<void> {
  if (!adapterClients) return;
  try {
    await adapterClients.pubClient.quit();
    await adapterClients.subClient.quit();
    logger.info('Redis adapter clients disconnected');
  } catch (err) {
    logger.warn({ err: (err as Error).message }, 'Error disconnecting adapter clients');
  }
  adapterClients = null;
}
