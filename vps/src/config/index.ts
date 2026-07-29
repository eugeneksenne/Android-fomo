export { env, isProduction, isDevelopment, isTest } from './env.js';

import { env } from './env.js';

export const config = {
  env: env.NODE_ENV,
  port: env.PORT,
  host: env.HOST,
  logLevel: env.LOG_LEVEL,

  cors: {
    allowedOrigins: env.ALLOWED_ORIGINS.split(',').map(o => o.trim()).filter(Boolean),
    credentials: env.CORS_CREDENTIALS,
  },

  security: {
    helmetEnabled: env.HELMET_ENABLED,
    rateLimit: {
      globalMax: env.RATE_LIMIT_GLOBAL_MAX,
      globalWindowMs: env.RATE_LIMIT_GLOBAL_WINDOW_MS,
    },
    deviceHeader: env.DEVICE_ID_HEADER,
    maxDevicesPerUser: env.MAX_DEVICES_PER_USER,
    replayProtection: env.SESSION_REPLAY_PROTECTION,
    jwtClockToleranceSec: env.JWT_CLOCK_TOLERANCE_SEC,
  },

  supabase: {
    url: env.SUPABASE_URL,
    anonKey: env.SUPABASE_ANON_KEY,
    serviceRoleKey: env.SUPABASE_SERVICE_ROLE_KEY,
    jwtSecret: env.SUPABASE_JWT_SECRET,
    jwksUrl: env.SUPABASE_JWKS_URL,
  },

  redis: {
    enabled: env.REDIS_ENABLED,
    host: env.REDIS_HOST,
    port: env.REDIS_PORT,
    password: env.REDIS_PASSWORD || undefined,
    db: env.REDIS_DB,
    tls: env.REDIS_TLS,
    keyPrefix: env.REDIS_KEY_PREFIX,
    clusterEnabled: env.REDIS_CLUSTER_ENABLED,
    latencyWarnMs: env.REDIS_LATENCY_WARN_MS,
  },

  socket: {
    pingIntervalMs: env.SOCKET_PING_INTERVAL_MS,
    pingTimeoutMs: env.SOCKET_PING_TIMEOUT_MS,
    maxHttpBufferSize: env.SOCKET_MAX_HTTP_BUFFER_SIZE,
    connectTimeoutMs: env.SOCKET_CONNECT_TIMEOUT_MS,
    connectionStateRecovery: env.SOCKET_CONNECTION_STATE_RECOVERY,
    recoveryMaxDisconnectMs: env.SOCKET_RECOVERY_MAX_DISCONNECT_MS,
  },

  limits: {
    maxCallParticipants: env.MAX_CALL_PARTICIPANTS,
    maxLiveViewers: env.MAX_LIVE_VIEWERS,
    maxRoomsPerUser: env.MAX_ROOMS_PER_USER,
  },

  timeouts: {
    presenceThrottleMs: env.PRESENCE_THROTTLE_MS,
    presenceOfflineMs: env.PRESENCE_OFFLINE_TIMEOUT_MS,
    callRingMs: env.CALL_RING_TIMEOUT_MS,
    callIdleMs: env.CALL_IDLE_TIMEOUT_MS,
    liveIdleMs: env.LIVE_IDLE_TIMEOUT_MS,
    typingThrottleMs: env.TYPING_THROTTLE_MS,
    typingTimeoutMs: env.TYPING_TIMEOUT_MS,
  },

  rateLimit: {
    callWindowMs: env.CALL_RATE_LIMIT_WINDOW_MS,
    callMax: env.CALL_RATE_LIMIT_MAX,
    signalWindowMs: env.SIGNAL_RATE_LIMIT_WINDOW_MS,
    signalMax: env.SIGNAL_RATE_LIMIT_MAX,
    abuseThresholdCallsPerMin: env.ABUSE_THRESHOLD_CALLS_PER_MIN,
    spamThresholdMessagesPerMin: env.SPAM_THRESHOLD_MESSAGES_PER_MIN,
  },

  observability: {
    otelEnabled: env.OTEL_ENABLED,
    otelServiceName: env.OTEL_SERVICE_NAME,
    otelEndpoint: env.OTEL_EXPORTER_OTLP_ENDPOINT,
    metricsEnabled: env.METRICS_ENABLED,
  },

  railway: {
    staticUrl: env.RAILWAY_STATIC_URL,
    healthPath: env.HEALTH_CHECK_PATH,
    readinessPath: env.READINESS_CHECK_PATH,
    livenessPath: env.LIVENESS_CHECK_PATH,
  },

  replay: {
    cacheSize: env.REPLAY_CACHE_SIZE,
    windowMs: env.REPLAY_WINDOW_MS,
  },
} as const;

export default config;
