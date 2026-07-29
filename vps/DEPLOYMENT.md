# Deployment Guide - Railway + Docker

## Prerequisites

- Supabase project: get `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_JWT_SECRET` (Settings > API > JWT Secret)
- Railway account
- Domain for signaling (optional, Railway provides *.up.railway.app)
- Redis: Railway Redis plugin or Upstash

## Railway Deployment (Recommended)

### 1. Connect Repo

- Go to Railway.app > New Project > Deploy from GitHub repo > select `Android-fomo`
- Set Root Directory to `vps`
- Railway auto detects Dockerfile

### 2. Add Redis

- In Railway project, New > Database > Redis (or Add Plugin Redis)
- Copy connection vars: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- Set in service Variables:
  - `REDIS_ENABLED=true`
  - `REDIS_HOST=${{Redis.REDIS_HOST}}` (use Railway variable reference)
  - `REDIS_PORT=${{Redis.REDIS_PORT}}`
  - `REDIS_PASSWORD=${{Redis.REDIS_PASSWORD}}`
  - `REDIS_DB=0`

Alternatively use Upstash Redis - set same vars.

### 3. Environment Variables (Railway Dashboard > Variables)

```ini
NODE_ENV=production
PORT=3000  # Railway injects PORT, but set fallback
HOST=0.0.0.0
LOG_LEVEL=info
ALLOWED_ORIGINS=https://your-app.com,https://fomo.app
# Or * for testing, but set specific domains for prod

SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_ANON_KEY=ey...
SUPABASE_SERVICE_ROLE_KEY=ey...
SUPABASE_JWT_SECRET=super-secret-jwt-... (long)

REDIS_ENABLED=true
REDIS_HOST=...
REDIS_PORT=6379
REDIS_PASSWORD=...

# Socket tuning
SOCKET_PING_INTERVAL_MS=25000
SOCKET_PING_TIMEOUT_MS=20000

MAX_CALL_PARTICIPANTS=8
PRESENCE_THROTTLE_MS=2000
CALL_RING_TIMEOUT_MS=45000

# Observability
OTEL_ENABLED=false
METRICS_ENABLED=true

# Security
HELMET_ENABLED=true
RATE_LIMIT_GLOBAL_MAX=1000
MAX_DEVICES_PER_USER=5
```

### 4. Deploy

- Railway builds via Dockerfile (multi-stage, Node22, non-root, healthcheck)
- Healthcheck Path: `/health` (configured in railway.toml)
- Wait for build logs, check Deployments > View Logs
- Should see `✅ Socket.IO server attached` and `FOMO Signaling Server ready`

### 5. Custom Domain (Optional)

- Railway > Settings > Domains > Add Custom Domain `signaling.yourdomain.com`
- Add CNAME in DNS to Railway provided
- SSL auto provisioned

### 6. Test

```bash
curl https://your-app.up.railway.app/health
# Should return {status: "ok", checks: {redis: "ok", ...}}

# Stats (needs auth token)
curl -H "Authorization: Bearer <supabase_jwt>" https://your-app.up.railway.app/stats/detailed
```

### 7. Logs & Monitoring

- Railway > Deployments > Logs (Pino JSON)
- Metrics at `/metrics` (if public, protect with auth or internal)
- OTEL: Set `OTEL_ENABLED=true` and `OTEL_EXPORTER_OTLP_ENDPOINT` to your OTEL collector (e.g., New Relic, Datadog)

## Docker Local

```bash
cd vps
cp .env.example .env
# Edit .env
docker build -t fomo-signaling .
docker run -p 3000:3000 --env-file .env fomo-signaling

# With Redis
docker-compose up --build
```

## Docker Compose Production (VPS without Railway)

On Ubuntu VPS:

```bash
# Setup script
chmod +x scripts/setup-vps.sh
sudo ./scripts/setup-vps.sh  # installs Node22, nginx, pm2, ufw

# Deploy
git clone <repo> /opt/fomo
cd /opt/fomo/vps
cp .env.example .env
nano .env # fill secrets

# Option A: Docker Compose
docker-compose up -d --build
docker-compose logs -f

# Option B: PM2 (if not using Docker)
npm ci --omit=dev
npm run build
pm2 start ecosystem.config.js --env production
pm2 save
pm2 startup
```

Nginx reverse proxy:

```bash
sudo cp nginx.conf.example /etc/nginx/sites-available/fomo-signaling
sudo nano /etc/nginx/sites-available/fomo-signaling # edit server_name signaling.yourdomain.com
sudo ln -s /etc/nginx/sites-available/fomo-signaling /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d signaling.yourdomain.com
```

## Client Integration (Android)

1. Supabase Auth:

```kotlin
val supabase = createSupabaseClient(supabaseUrl, supabaseKey) { install(Auth); install(Realtime) }
val session = supabase.auth.signInWith(Password) { email = "..."; password = "..." }
val token = session?.accessToken // This is Supabase JWT
```

2. Socket.IO client (io.socket:socket.io-client:2.1.0):

```kotlin
val opts = IO.Options().apply {
  auth = mapOf(
    "token" to token,
    "deviceId" to deviceId, // UUID from Settings.Secure.ANDROID_ID
    "deviceType" to "ANDROID",
    "appVersion" to BuildConfig.VERSION_NAME
  )
  transports = arrayOf("websocket")
  reconnection = true
  reconnectionAttempts = 10
  reconnectionDelay = 1000
}
val socket = IO.socket("https://signaling.yourdomain.com", opts)
socket.connect()
```

3. Handle events:

```kotlin
socket.on("connected") { args -> val data = args[0] as JSONObject; val sessionId = data.getString("sessionId") }
socket.on("call:incoming") { args -> showIncomingCallUI(args[0]) }
socket.on("webrtc:offer") { args -> peerConnection.setRemoteDescription(...); createAnswer() }
socket.on("nightguard:sos") { args -> showSosAlert(args[0]) }
socket.on("user:online") { ... }
```

4. Token refresh:

```kotlin
supabase.auth.onAuthStateChange { event, session ->
  if (event == AuthChangeEvent.TOKEN_REFRESHED) {
    val newToken = session?.accessToken
    socket.io().opts.auth = mapOf("token" to newToken, "deviceId" to deviceId)
    socket.disconnect().connect() // Or emit reconnect:attempt with sessionId
  }
}
```

5. Battery efficient:

- Throttle presence updates (client side too, 2s)
- Batch location updates (5s or distance filter 10m)
- Stop heartbeat when app background? Server handles background state via `app:state` event

## Push Notifications

Server triggers push via `pushAdapter` which logs and optionally inserts into Supabase `push_notifications` table.

To actually send FCM:

- Create Supabase Edge Function `send-push` that uses Firebase Admin SDK to send FCM using `recipientId -> fcm_tokens` lookup.
- In `pushAdapter.ts`, replace logging adapter with call to `supabase.functions.invoke('send-push', {body: payload})`
- Or have a separate worker that polls `push_notifications` table and sends FCM.

Push payload is platform independent:

```json
{
  "recipientId": "uuid",
  "trigger": "INCOMING_CALL",
  "title": "Incoming Video Call",
  "body": "Alex is calling you",
  "data": {"callId": "...", "callerId": "...", "roomId": "..."},
  "priority": "critical",
  "ttlSeconds": 45
}
```

Android side: FirebaseMessagingService receives data message, shows full-screen intent for calls, heads-up for SOS.

## Scaling

- Start with 1 Railway instance (supports ~5k concurrent with tuning, Node single-thread)
- Enable Redis adapter (already) for multi-instance
- In Railway, increase replicas to 2-4 (they auto join Redis pub/sub)
- For 10k+ concurrent, consider:
  - pm2 cluster mode? But Fastify + Socket.IO with Redis adapter, use `instances: max` only if sticky sessions via load balancer - Railway does round-robin without sticky? Need to use `transports: websocket` only (no polling fallback) to avoid sticky requirement.
  - Or move to separate SFU for media? But this server is signaling only, so bottleneck is socket connections memory per instance (~1-2MB per socket). 8GB RAM ~4k sockets.
  - Use auto-scaling based on CPU/memory in Railway.

## Environment Template

See `.env.example` - all vars documented with defaults.

## Health Checks

- `/health` - liveness, returns 200 if ok, 503 if down, includes checks redis, supabase, memory
- `/ready` - readiness, 503 if not ready or shutting down, used by Railway to route traffic
- `/live` - simple alive check

Railway uses these to restart unhealthy instances.

## Graceful Shutdown

- On SIGTERM/SIGINT: `setShuttingDown(true)`, `setReady(false)`, close engine (no new connections), emit `server:shutdown` to clients, wait 10s for drain, close Fastify, disconnect Redis, shutdown OTEL.
- Clients on `server:shutdown` should reconnect after 5s with backoff.

## Logs

- Pino JSON in prod, pretty in dev
- Never logs tokens, passwords (redaction)
- Railway captures stdout -> Logs
- For ELK: ship via OTEL or Railway log drains

## Common Issues

- **Auth fails**: Check SUPABASE_JWT_SECRET matches Supabase dashboard > API > JWT Secret. Ensure token aud=authenticated, not anon.
- **Redis fail**: If REDIS_ENABLED=true but Redis down, server continues degraded (logs warn). Set REDIS_ENABLED=false to use in-memory for single instance.
- **CORS fail**: ALLOWED_ORIGINS must include your app domain exact or * for dev. Check browser console for CORS error.
- **Socket not connecting**: Ensure transports websocket allowed, check nginx upgrade headers if using nginx.
- **Rate limited**: Check logs for RATE_LIMIT_EXCEEDED, increase limits or slow client.
- **Replay detected**: Client reusing same correlationId, generate new UUID per event.

## Maintenance

See MAINTENANCE.md for log rotation, Redis failover, metrics alerts, upgrade steps.
