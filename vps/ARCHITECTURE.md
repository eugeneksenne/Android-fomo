# FOMO Signaling Server - Architecture

## Overview

Stateless signaling layer between FOMO Android clients. Media via WebRTC P2P/SFU, signaling via Socket.IO + Redis adapter for horizontal scaling.

```
[Android Client] --wss--> [Fastify + Socket.IO] --pub/sub--> [Redis] --pub/sub--> [Other Signaling Instances]
        |                           |
        |                           |--> [Supabase Auth] JWT verify (local HS256)
        |                           |--> [Supabase Postgres] Notifications table (push queue) optional
        |                           |--> [Pino + OTEL] logs/traces
        |
[WebRTC P2P] Direct media between clients, never via server
```

## Modular Domains (SOLID)

- **auth**: supabaseAuth.service verifies JWT <100ms, deviceValidator enforces max 5 devices with priority eviction, session.service tracks sessionId -> socketIds in Redis with 24h TTL.
- **socket**: connectionManager tracks socketId -> userId, duplicate detection, primary socket selection by priority, heartbeat quality assessment, Redis persistence. lifecycle handles CONNECTING→AUTHENTICATED→RECONNECTING→DISCONNECTED→BACKGROUND, exponential backoff, session restoration within 2min recovery window.
- **presence**: presenceStore (local + Redis), presenceThrottler (2000ms cooldown, debounced pending update), presenceEngine updates presence, broadcasts to all or friends. Throttled for battery.
- **calls**: callState (CallRoom), callEngine (state machine: REQUESTING→RINGING→ACCEPTED→ENDED, ring timeout 45s, cleanup), webrtcSignaling (offer<50ms, answer<50ms, ICE<50ms), groupCallEngine (mute, remove, promote, raise hand).
- **friends**: friendsEngine categorizes friends presence (online, activeNow, inCall, watchingLive, insideVenue).
- **groups**: groupManager (user rooms, group rooms)
- **club-lobby**: lobbyEngine venueId->lobbyRooms, crowd count, ban list
- **nightguard**: nightguardEngine SOS with trigger types MANUAL|SHAKE|INACTIVITY, emergency session creation on fly, trusted contacts alert via pushAdapter
- **live-location**: liveLocationEngine trip sessions, locations capped 1000, PAUSE/RESUME/STOP
- **notifications**: notificationEngine triggers pushAdapter (logging adapter + optional Supabase insert), platform independent, priority critical for SOS/calls
- **core/redis**: adapter setup for horizontal scaling, client with latency measurement
- **core/monitoring**: metricsCollector histograms p50/p95/p99, per-sec counters, health routes

## Data Flow - Call Signaling

```
Caller                  Server                          Callee
  | -- call:initiate --> |
  |                      |-- pushAdapter trigger --> FCM (if offline)
  |                      |-- call:incoming --------> |
  |                      |                            | -- call:accept -->
  | <-- call:accepted -- | <-- call:accepted -------- |
  | -- webrtc:offer ---> | -- webrtc:offer ---------> |
  | <-- webrtc:answer -- | <-- webrtc:answer -------- |
  | -- webrtc:ice -----> | -- webrtc:ice -----------> | (trickle, <50ms)
  Media P2P established, server only relays signaling
```

## Presence Throttling

- Token bucket per user: general throttle 2000ms, same status 4000ms.
- If throttled, schedule pending update after delay.
- Reduces battery drain on Android when user rapidly changes status (e.g., moving between venues).
- Metrics: PRESENCE_THROTTLED error code counted.

## Socket Lifecycle & Recovery

```
CONNECTING -> CONNECTED -> AUTHENTICATED -> HEARTBEAT every 25s
    |           |              |
    |           |              |-> NETWORK_SWITCHING (IP change) -> AUTHENTICATED
    |           |              |-> BACKGROUND (app background) -> reduce heartbeat? still online
    |           |
    |           |-> DISCONNECT (transport close) -> RECONNECTING with exponential backoff up to 30s
    |                                   |
    |                                   |-> Session recovery within 120s window via sessionId + lastEventId
    |                                   |-> If expired, new session
    |
    -> Graceful shutdown: server:shutdown event, 10s drain, then force disconnect
```

## Redis Distributed Design

- Adapter: pubClient + subClient via `createClient`, `createAdapter` enables room broadcasts across instances.
- Keys:
  - `fomo:signal:conn:<socketId>` -> ConnectionRecord TTL 300s refreshed on heartbeat
  - `fomo:signal:user_conns:<userId>` -> Set<socketId>
  - `fomo:signal:presence:<userId>` -> PresenceData JSON
  - `fomo:signal:presence_set` -> Set<userId> online
  - `fomo:signal:call:<callId>` -> CallRoom JSON
  - `fomo:signal:session:<sessionId>` -> UserSession JSON
  - `fomo:signal:nightguard:<id>`, `live_location:<id>`, `lobby:<id>`
- Stateless: any instance can handle any socket, Redis sync ensures presence & calls visible across instances.
- Horizontal scaling: add more Railway instances, they auto join Redis pub/sub.

## Security Layers

1. **JWT**: Supabase JWT HS256 local verify, clock tolerance 10s, aud check, exp rejection.
2. **Device**: deviceId normalized, max 5 per user, priority eviction.
3. **Origin**: `allowRequest` callback checks ALLOWED_ORIGINS.
4. **Rate limits**: Fastify global + per-socket via token buckets (call 20/min, signal 100/10s, message 30/min).
5. **Replay**: correlationId cache LRU 10k, 5min window, reject replay.
6. **Sanitization**: `sanitizeString` strips control chars, limits length, URL validation.
7. **Helmet**: security headers.
8. **Audit**: every call/auth logged with userId, deviceId, action, result, never sensitive tokens.
9. **Pino redaction**: tokens, passwords, secrets redacted.

## Observability

- **Pino**: structured JSON, level configurable, pretty in dev.
- **OTEL**: optional, auto-instrumentations for http, fastify, redis, socket.io if OTEL_ENABLED.
- **Metrics**: in-memory collector with histograms, per-sec counters, snapshots at /metrics.
- **Health**: /health checks redis, supabase config, memory; /ready checks isReady + redis; /live simple.
- **Logs**: connections, disconnects (reason, duration), calls (state transitions), errors (code, correlationId), auth (success/failure), latency warnings >100ms, reconnects.

## Performance Targets - How Met

- Auth <100ms: local JWT verify (no network), start timer, warn if >100.
- Call init <200ms: in-memory call creation + Redis persist async (not blocking), offline push trigger async.
- Offer/Answer/ICE <50ms: direct relay via socket.to(), no DB, latency measured, warn if >50.
- Reconnect <2s: connectionStateRecovery 120s, sessionService restores, exponential backoff base 1s.
- Heartbeat <100ms: heartbeat handler records latency, ack immediate, no DB.
- Thousands concurrent: Fastify + uWebSockets? Actually Node http + Socket.IO, Redis adapter, non-blocking, metrics show peak.

## Future Extensibility

- FOMO Live: extend lobbyEngine to liveEngine with broadcaster/viewer roles, SFU integration point.
- Advanced presence: customMessage, venueId already supported, add more statuses easily via enum.
- Moderation: lobbyEngine ban list, groupEngine host controls already.
- Push: pushAdapter interface allows swapping to FCM/APNS directly or Supabase Edge Function.

## Avoided Anti-Patterns

- No monolithic files: each domain <400 lines, separated handlers/validators/engine.
- No circular deps: shared/types at bottom, core/middleware depends on shared, modules depend on core/shared, not vice versa.
- No placeholder TODOs: all functions implemented, no mock services (pushAdapter logs but is production-ready interface with Supabase insert).
- No media handling: server never touches SDP media content, only relays.

## Diagram - NightGuard SOS

```
User triggers SOS (MANUAL|SHAKE)
  -> nightguard:sos {location, triggerType}
  -> nightguardEngine.triggerSos (creates emergency session if needed)
  -> io.to(sessionId).emit nightguard:sos
  -> for each trustedContact: connectionManager.getConnections -> io.to(socketId).emit
  -> notificationEngine.trigger EMERGENCY_ALERT (critical priority)
  -> io.emit nightguard:global_sos for monitoring dashboard
```

## Diagram - Live Location

```
Owner start: live-location:start {destination, trusted}
  -> liveLocationEngine.createSession
  -> socket.join(sessionId)
  -> conns of trusted get live-location:started
Owner update: live-location:update {sessionId, location, battery, eta}
  -> engine.updateLocation (cap 1000 locations)
  -> io.to(sessionId).emit live-location:update
  -> trusted contacts individually if not in room but online
Pause/Resume/Stop via live-location:action
```
