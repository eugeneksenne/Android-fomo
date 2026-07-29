import { z } from 'zod';
import dotenv from 'dotenv';
dotenv.config();

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  PORT: z.coerce.number().int().positive().default(3000),
  HOST: z.string().default('0.0.0.0'),
  LOG_LEVEL: z.enum(['trace', 'debug', 'info', 'warn', 'error', 'fatal']).default('info'),

  ALLOWED_ORIGINS: z.string().default('*'),
  CORS_CREDENTIALS: z.coerce.boolean().default(true),

  HELMET_ENABLED: z.coerce.boolean().default(true),
  RATE_LIMIT_GLOBAL_MAX: z.coerce.number().default(1000),
  RATE_LIMIT_GLOBAL_WINDOW_MS: z.coerce.number().default(60_000),

  SUPABASE_URL: z.string().url().or(z.string().length(0)).default(''),
  SUPABASE_ANON_KEY: z.string().default(''),
  SUPABASE_SERVICE_ROLE_KEY: z.string().default(''),
  SUPABASE_JWT_SECRET: z.string().min(1, 'SUPABASE_JWT_SECRET is required').default('dev_jwt_secret_change_me_32_chars_min'),
  SUPABASE_JWKS_URL: z.string().url().optional().or(z.literal('')),

  DEVICE_ID_HEADER: z.string().default('x-device-id'),
  SESSION_REPLAY_PROTECTION: z.coerce.boolean().default(true),
  JWT_CLOCK_TOLERANCE_SEC: z.coerce.number().default(10),
  MAX_DEVICES_PER_USER: z.coerce.number().default(5),

  REDIS_ENABLED: z.coerce.boolean().default(false),
  REDIS_HOST: z.string().default('127.0.0.1'),
  REDIS_PORT: z.coerce.number().default(6379),
  REDIS_PASSWORD: z.string().default(''),
  REDIS_DB: z.coerce.number().default(0),
  REDIS_TLS: z.coerce.boolean().default(false),
  REDIS_KEY_PREFIX: z.string().default('fomo:signal:'),
  REDIS_CLUSTER_ENABLED: z.coerce.boolean().default(false),
  REDIS_LATENCY_WARN_MS: z.coerce.number().default(100),

  SOCKET_PING_INTERVAL_MS: z.coerce.number().default(25_000),
  SOCKET_PING_TIMEOUT_MS: z.coerce.number().default(20_000),
  SOCKET_MAX_HTTP_BUFFER_SIZE: z.coerce.number().default(1_000_000),
  SOCKET_CONNECT_TIMEOUT_MS: z.coerce.number().default(45_000),
  SOCKET_CONNECTION_STATE_RECOVERY: z.coerce.boolean().default(true),
  SOCKET_RECOVERY_MAX_DISCONNECT_MS: z.coerce.number().default(120_000),

  MAX_CALL_PARTICIPANTS: z.coerce.number().default(8),
  MAX_LIVE_VIEWERS: z.coerce.number().default(5000),
  MAX_ROOMS_PER_USER: z.coerce.number().default(20),
  PRESENCE_THROTTLE_MS: z.coerce.number().default(2000),
  PRESENCE_OFFLINE_TIMEOUT_MS: z.coerce.number().default(60_000),
  CALL_RING_TIMEOUT_MS: z.coerce.number().default(45_000),
  CALL_IDLE_TIMEOUT_MS: z.coerce.number().default(300_000),
  LIVE_IDLE_TIMEOUT_MS: z.coerce.number().default(3_600_000),
  TYPING_THROTTLE_MS: z.coerce.number().default(1500),
  TYPING_TIMEOUT_MS: z.coerce.number().default(5000),

  CALL_RATE_LIMIT_WINDOW_MS: z.coerce.number().default(60_000),
  CALL_RATE_LIMIT_MAX: z.coerce.number().default(20),
  SIGNAL_RATE_LIMIT_WINDOW_MS: z.coerce.number().default(10_000),
  SIGNAL_RATE_LIMIT_MAX: z.coerce.number().default(100),
  ABUSE_THRESHOLD_CALLS_PER_MIN: z.coerce.number().default(10),
  SPAM_THRESHOLD_MESSAGES_PER_MIN: z.coerce.number().default(30),

  OTEL_ENABLED: z.coerce.boolean().default(false),
  OTEL_SERVICE_NAME: z.string().default('fomo-signaling'),
  OTEL_EXPORTER_OTLP_ENDPOINT: z.string().default('http://localhost:4317'),

  METRICS_ENABLED: z.coerce.boolean().default(true),

  RAILWAY_STATIC_URL: z.string().optional().or(z.literal('')),
  HEALTH_CHECK_PATH: z.string().default('/health'),
  READINESS_CHECK_PATH: z.string().default('/ready'),
  LIVENESS_CHECK_PATH: z.string().default('/live'),

  REPLAY_CACHE_SIZE: z.coerce.number().default(10_000),
  REPLAY_WINDOW_MS: z.coerce.number().default(300_000),
});

export type Env = z.infer<typeof envSchema>;

let parsed: Env;
try {
  parsed = envSchema.parse(process.env);
} catch (err) {
  if (err instanceof z.ZodError) {
    const issues = err.issues.map(i => `${i.path.join('.')}: ${i.message}`).join('\n');
    console.error(`❌ Invalid environment configuration:\n${issues}`);
    process.exit(1);
  }
  throw err;
}

export const env: Env = parsed;

export const isProduction = env.NODE_ENV === 'production';
export const isDevelopment = env.NODE_ENV === 'development';
export const isTest = env.NODE_ENV === 'test';
