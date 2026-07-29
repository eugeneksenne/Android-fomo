import { createClient, RedisClientType } from 'redis';
import { env } from '../../config/env.js';
import { logger } from '../logger/logger.js';

export type RedisClient = RedisClientType;

let client: RedisClient | null = null;
let isConnecting = false;

export async function getRedisClient(): Promise<RedisClient | null> {
  if (!env.REDIS_ENABLED) return null;
  if (client && client.isOpen) return client;
  if (isConnecting) {
    // Wait for connection
    for (let i = 0; i < 10; i++) {
      await new Promise(r => setTimeout(r, 200));
      if (client && client.isOpen) return client;
    }
  }

  isConnecting = true;
  try {
    const redisClient = createClient({
      socket: {
        host: env.REDIS_HOST,
        port: env.REDIS_PORT,
        tls: env.REDIS_TLS,
        reconnectStrategy: (retries) => {
          const delay = Math.min(retries * 100, 3000);
          logger.warn({ retries, delay }, 'Redis reconnecting');
          return delay;
        },
      },
      password: env.REDIS_PASSWORD || undefined,
      database: env.REDIS_DB,
    }) as RedisClient;

    redisClient.on('error', (err) => {
      logger.error({ event: 'redis_error', err: err.message }, 'Redis error');
    });
    redisClient.on('connect', () => logger.info('Redis connecting'));
    redisClient.on('ready', () => logger.info('Redis ready'));
    redisClient.on('end', () => logger.info('Redis connection ended'));

    await redisClient.connect();
    client = redisClient;
    logger.info({ host: env.REDIS_HOST, port: env.REDIS_PORT }, 'Redis connected');
    return client;
  } catch (err) {
    logger.error({ err: (err as Error).message }, 'Failed to connect to Redis');
    return null;
  } finally {
    isConnecting = false;
  }
}

export async function disconnectRedis(): Promise<void> {
  if (client) {
    try {
      await client.quit();
      logger.info('Redis disconnected');
    } catch {
      await client.disconnect().catch(() => {});
    }
    client = null;
  }
}

export function getRedisKey(...parts: string[]): string {
  return `${env.REDIS_KEY_PREFIX}${parts.join(':')}`;
}

export async function measureRedisLatency(): Promise<number> {
  const rc = await getRedisClient();
  if (!rc) return -1;
  const start = Date.now();
  try {
    await rc.ping();
    return Date.now() - start;
  } catch {
    return -1;
  }
}
