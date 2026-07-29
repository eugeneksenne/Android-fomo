# Maintenance Guide - FOMO Signaling Server

## Daily Checks

- `/health` endpoint 200, redis ok
- Railway logs: no spikes in `AUTH_INVALID_TOKEN` or `RATE_LIMIT_EXCEEDED`
- Metrics `/metrics`: connections, latency p95 <100ms, error rate <1%, redis latency <50ms

## Log Rotation

- Pino logs to stdout, Railway retains 7 days. For VPS pm2:
  - pm2 logs rotation: `pm2 install pm2-logrotate`, `pm2 set pm2-logrotate:max_size 10M`, `pm2 set pm2-logrotate:retain 7`
  - System logrotate: `/etc/logrotate.d/fomo-signaling` already in `scripts/setup-vps.sh`

## Redis Maintenance

- Monitor memory: `redis-cli info memory`
- Persistence: AOF enabled in docker-compose (`appendonly yes`)
- Backup: If using Railway Redis plugin, backup via Railway dashboard Snapshots
- Failover: If Redis down, server degrades to in-memory (single instance). Multi-instance requires Redis - set alerts for redis check fail.
- Key expiration: All keys have TTL (conn 300s, presence 3600s, call 3600s, session 24h). No manual cleanup needed, but periodic `presenceEngine.cleanup()` removes offline stale.

## Presence & Session Cleanup

- Presence: `presenceEngine.cleanup()` every 60s in lifecycle? Currently via periodic check in connectionManager. Stale presence >3x offline timeout marked offline.
- Sessions: `sessionService.cleanupExpired(24h)` - call via cron or interval. Add in `server.ts` periodic: `setInterval(() => sessionService.cleanupExpired(), 3600000)`
- Calls: `callEngine` auto cleans after ended (5-30s), ring timeout 45s.
- Lobbies: kept but could add idle timeout similar to calls if needed.

## Metrics Alerts (Suggested)

Set up alerts in your monitoring (Datadog, New Relic, Railway notifications):

- `connections.total` > 4000 per instance (scale up)
- `latency.p95` > 100ms for 5min (check CPU, Redis latency)
- `errors.rate` > 10/min (check logs for specific code)
- `redis.latencyMs` > 100ms (check Redis CPU, network)
- `system.memoryUsedMb` > 80% of instance RAM (scale or restart)
- Health check fails 3x consecutive (Railway auto restarts, but alert)

## Upgrades

### Node.js 22 -> 24

- Update Dockerfile `FROM node:24-alpine`
- Update `engines` in package.json
- Test: `npm run typecheck && npm run test`
- Deploy to staging Railway env first, then production with blue-green (Railway supports preview deploys per PR)

### Dependency Upgrades

- Check monthly: `npm outdated`
- Especially `@fastify/*`, `socket.io`, `redis`, `@supabase/supabase-js`, `zod`
- Run tests, check changelog for breaking changes (Fastify 4->5, Socket.IO 4->5)

### Env Changes

- When adding new env var, update `src/config/env.ts` Zod schema with default and fail-fast message
- Update `.env.example` and `DEPLOYMENT.md`
- Add to Railway variables

## Backups

- No DB in this service, but Redis holds ephemeral state. No backup needed for signaling, but if you persist push_notifications in Supabase, backup via Supabase daily backups (enabled by default).
- For pm2 VPS: backup `/opt/fomo/vps/.env` securely (e.g., Railway variables or Vault)

## Security Patches

- Helmet, CORS: keep `@fastify/helmet` updated
- JWT: Supabase JWT secret rotation: generate new secret in Supabase dashboard, update env var, rolling restart. Old tokens will fail until client refreshes (Supabase client auto refreshes with new secret? Check Supabase docs: secret rotation invalidates all tokens, clients will need re-login - schedule maintenance window).
- Rate limits: tune based on abuse detection logs. If seeing `CALL_SPAM_BLOCKED`, consider lowering `CALL_RATE_LIMIT_MAX` or adding CAPTCHA on client for call initiation.
- Audit logs: review weekly for `AUTH_FORBIDDEN`, `LOBBY_BANNED`, `NIGHTGUARD_NOT_AUTHORIZED` spikes.

## Scaling Steps

1. **Vertical**: Increase Railway instance memory/CPU (Settings > Resources)
2. **Horizontal**: Increase replicas (Settings > Deploy > Replicas) - requires `REDIS_ENABLED=true` and websocket transport only for no sticky session requirement. If using polling fallback, need sticky sessions (Railway doesn't provide sticky by default, so recommend `transports: ['websocket']` only in production client).
3. **If still bottleneck**: Move to Kubernetes with proper L4 load balancer with IP hash sticky, or use managed Socket.IO platform like Ably/Pusher? But custom is cheaper.
4. **Database**: Supabase Postgres for push queue - ensure connection pooling via Supavisor.

## Graceful Shutdown Testing

```bash
# Local
npm run build
PORT=3000 node dist/server.js &
PID=$!
sleep 2
curl http://localhost:3000/health
kill -SIGTERM $PID
# Should see graceful shutdown logs: engine closed, draining connections, Fastify closed, Redis disconnected

# Railway: trigger deploy, old instance should get SIGTERM and drain
```

## Troubleshooting

| Symptom | Check | Fix |
|---------|-------|-----|
| Socket auth 401 | Logs `AUTH_INVALID_TOKEN`, check SUPABASE_JWT_SECRET | Verify secret, check client token via jwt.io aud=authenticated exp not expired |
| High latency p95 >100ms | `/metrics` latency, Redis latency | Check CPU, scale up, check Redis host close to app region, enable OTEL to trace |
| Many reconnects | `reconnect.rate` high, check `ping timeout` logs | Check client network, increase `SOCKET_PING_TIMEOUT_MS`, check server memory GC pauses |
| Redis connection fail | Logs `Redis error`, health redis fail | Check Redis credentials, network, firewall, Railway Redis private network enabled |
| Call timeout always | Ring timeout 45s, callee not receiving `call:incoming` | Check callee online, connectionManager.getUserConnections, push trigger logs, client listening to event |
| Presence not updating | Throttled | Client should respect 2s throttle, server schedules pending update |
| Memory leak | `system.memoryUsedMb` increasing | Heap dump via `node --inspect`, check presenceStore maps growing, ensure cleanup intervals running |

## On-call Runbook

- **P1 Outage (all sockets disconnected)**: Check Railway status, health endpoint, Redis. If Redis down, set `REDIS_ENABLED=false` and redeploy to degraded mode single instance while fixing Redis.
- **P1 SOS not alerting**: Check `nightguard:global_sos` logs, pushAdapter logs, Supabase push table, FCM credentials in Edge Function.
- **P2 High error rate**: Check error by code via `/metrics` `errors.byCode`, correlate with deploy time, rollback if needed via Railway Deployments > Rollback.
- **P2 Rate limited legit users**: Increase limits temporarily via env var and redeploy, then investigate abuse IP via logs.

## End-of-Life Checklist for Decommission

- Set `setShuttingDown(true)`, emit `server:shutdown`, wait drain
- Remove Railway service, delete Redis plugin (after data export if needed)
- Revoke Supabase service role key if rotated
- Archive logs from Railway
