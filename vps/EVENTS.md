# FOMO Signaling Server - Event Definitions (v2 Production)

**Version**: 2.0.0  
**Protocol**: Socket.IO 4.x, WebSocket primary, polling fallback (dev only)  
**Auth**: Supabase JWT in handshake `auth.token`  
**All events**: Zod validated, authz checked, ack with `{success, data, error, correlationId, timestamp}`, structured errors, versioned, correlationId for tracing & replay protection.

## Base Envelope

```ts
interface BaseEvent {
  correlationId?: string; // client gen UUID for replay protection & tracing
  version?: string; // default 2.0.0
  timestamp?: string; // ISO
  deviceId?: string;
}
```

All client→server payloads extend BaseEvent. All server→client payloads include `timestamp`, `correlationId`, `version`.

Error envelope:

```json
{
  "code": "AUTH_EXPIRED_TOKEN",
  "message": "Authentication token has expired",
  "developerMessage": "Token expired at 2026-...)",
  "correlationId": "corr_...",
  "recovery": "Refresh token via supabase.auth.refreshSession() and reconnect",
  "timestamp": "2026-...",
  "details": {}
}
```

## Connection Lifecycle

- Client connects with `auth: { token: <Supabase JWT>, deviceId: "...", deviceType: "ANDROID", appVersion: "1.0.0", correlationId: "..." }`
- Server middleware verifies JWT <100ms, device validation, session creation
- Server emits `connected` {socketId, userId, sessionId, serverTime, version, onlineCount, recoveryEnabled}
- Client should send `heartbeat` every 25s: {status, location, battery, correlationId} → server ack `heartbeat:ack` {serverTime, latencyMs}
- Reconnect: client sends `reconnect:attempt` {sessionId, lastEventId, deviceId} → server restores session if within 120s, emits `session:restored`
- Network switch: client emits `network:switch` {ip}
- App background: `app:state` {state: BACKGROUND|FOREGROUND}

## Presence

**Client → Server**

- `presence:update` {status: ONLINE|OFFLINE|AWAY|BUSY|IN_CALL|RECORDING_VOICE|UPLOADING|WATCHING_LIVE|WALKING_HOME|INSIDE_VENUE|SHARING_LOCATION, customMessage?, venueId?, correlationId} → ack {data: PresenceData}
  - Throttled 2000ms, same status 4000ms. If throttled server schedules pending update and ack `throttled: true`.
- `presence:get` {userIds?: string[], limit?: number} → ack {users: PresenceData[]}

**Server → Client**

- `presence:update` {userId, status, lastSeen, customMessage?, venueId?, displayName?, photoUrl?, timestamp, correlationId}
- `presence:list` {users: [], timestamp, correlationId}
- `user:online`, `user:offline` (compat) same payload
- `user:typing`, `chat:typing` {userId, roomId?, chatId?, lobbyId?, isTyping, displayName?, timestamp, correlationId} - throttled 1.5s, auto clear after 5s

## Call Signaling

### Types: VOICE, VIDEO, GROUP_VOICE, GROUP_VIDEO, CLUB_LOBBY, NIGHTGUARD, BUDDY_PAIR, EMERGENCY

**Client → Server**

- `call:initiate` {targetUserId?, targetUserIds?, callType, chatId?, lobbyId?, nightguardSessionId?, metadata?, correlationId} → ack {roomId, callId, state: RINGING, participants, offlineTargets}
  - Rate limited 20/min per user
  - Fails if caller already in call (CALL_ALREADY_ACTIVE)
- `call:accept` {roomId, reason?, correlationId} → ack {participants}
- `call:reject` {roomId, reason?, correlationId} → ack
- `call:cancel` {roomId, reason?} (caller only before accepted)
- `call:end` {roomId, reason?} → ends for all in room
- `call:leave` {roomId} → leave but keep call alive for others
- `call:join` {roomId} → for group calls
- `call:media_state` {roomId, audioEnabled?, videoEnabled?, isMuted?, isCameraOn?, isSpeakerOn?, isHandRaised?, correlationId}
- `call:escalate` {roomId, newType: VIDEO} VOICE→VIDEO only
- `call:group_action` {roomId, targetUserId?, action: MUTE|UNMUTE|REMOVE|PROMOTE|DEMOTE|RAISE_HAND|LOWER_HAND}

**Server → Client**

- `call:incoming` {roomId, callId, callerId, callerName?, callerPhoto?, callType, participants: string[], timestamp, correlationId, metadata?} → to target user(s)
- `call:ringing` {roomId, callId, state: RINGING, userId, participants, timestamp, correlationId} → to room
- `call:accepted` {roomId, callId, state: ACCEPTED, userId, participants, timestamp, correlationId}
- `call:rejected`, `call:busy`, `call:cancelled`, `call:ended`, `call:failed`, `call:timeout` same shape + reason?
- `call:participant_update` {roomId, participants: [{userId, role, isMuted, isCameraOn, isHandRaised, displayName?}], participantCount, timestamp, correlationId}
- `call:media_state` {roomId, userId, audioEnabled?, videoEnabled?, isMuted?, isCameraOn?, isSpeakerOn?, isHandRaised?, timestamp, correlationId}
- `call:escalated` {roomId, callId, state, userId, timestamp, correlationId}

### Timeout Handling

- Ring timeout 45s (env CALL_RING_TIMEOUT_MS) → server sets state TIMEOUT, emits `call:timeout` to room, cleans up, triggers missed call push.

## WebRTC Signaling (Media handled by WebRTC, server only relay)

**Client → Server** (all ack optional, measure latency <50ms target)

- `webrtc:offer` {roomId, targetUserId?, sdp: {type: 'offer', sdp: string}, codecInfo?: {codecs?, preferOpus?}, correlationId}
- `webrtc:answer` {roomId, targetUserId?, sdp: {type: 'answer', sdp: string}, correlationId}
- `webrtc:ice-candidate` {roomId, targetUserId?, candidate: RTCIceCandidateInit|null, isRestart?, correlationId}
- `webrtc:ice-restart` same as ice-candidate with isRestart true
- `webrtc:renegotiate` {roomId, targetUserId?, reason, correlationId}

**Server → Client**

- Same events relayed: `webrtc:offer`, `webrtc:answer`, `webrtc:ice-candidate`, `webrtc:renegotiate` with senderId, timestamp, correlationId.

**Codecs**

- Client can send codecInfo.preferOpus true for voice, server relays but doesn't enforce.
- Renegotiation for codec switch: emit webrtc:renegotiate then new offer.

**ICE Restart**

- On network switch, client emits ice-restart, server increments iceRestartCount, relay.

**Connection Recovery**

- WebRTC connection state failed → client does ICE restart, server rebroadcasts.
- If peerConnection fails, client can call:leave then call:join to rejoin group call.

## Group Call Engine

- Join/Leave via call:join/leave
- Participant updates via call:participant_update on each change
- Mute: only self or moderator can mute others (MUTE), unmute only self (UNMUTE)
- Remove: only HOST
- Promote/Demote: HOST only, host cannot be demoted
- Raise hand: self only
- Speaker detection: client side audioLevel >0.3 and not muted → client emits call:media_state with isSpeaking? Or server could relay speaker events (not implemented, client detects via audio tracks)

## Friends Presence

- `friends:presence:get` {friendIds?: string[], correlationId} → ack {friends: [{userId, status, lastSeen, displayName, photoUrl, isOnline, venueId}], categorized: {onlineCount, activeNowCount, inCallCount, watchingLiveCount, insideVenueCount, walkingHomeCount}}
- Server emits `friends:presence` {userId, status, lastSeen, displayName, photoUrl, isCalling?, isStreaming?, venueId?, timestamp, correlationId} on friend presence change (future: only to friends, currently broadcast filtered client side)

## Club Lobby

**Client → Server**

- `lobby:join` {venueId, lobbyId?, channel?: GENERAL, correlationId} → ack {lobbyId, participantCount, participants}
- `lobby:leave` {venueId, lobbyId?, channel?}
- `lobby:message` {venueId, lobbyId?, channel?, text, type?: TEXT|IMAGE|SYSTEM|ANNOUNCEMENT, replyTo?, metadata?}
- `lobby:announcement` {venueId, message, priority?: LOW|MEDIUM|HIGH|CRITICAL} → host/moderator only

**Server → Client**

- `lobby:joined` {venueId, lobbyId, eventType: MEMBER_JOINED, userId, data: {displayName, participantCount}, participantCount, timestamp, correlationId}
- `lobby:left` {venueId, lobbyId, eventType: MEMBER_LEFT, userId, data: {participantCount}, ...}
- `lobby:message` {venueId, lobbyId, eventType: LIVE_CHAT, userId, data: {text, type, displayName, replyTo, metadata, id}, timestamp, correlationId}
- `lobby:announcement` {venueId, lobbyId, eventType: HOST_ANNOUNCEMENT, userId, data: {message, priority, displayName}, timestamp, correlationId}
- `lobby:crowd_count` {venueId, lobbyId, eventType: CROWD_COUNT, data: {count}, participantCount, timestamp, correlationId}

## NightGuard

**Client → Server**

- `nightguard:create` {type: WALK_ME_HOME|BUDDY_PAIR|NIGHTGUARD|EMERGENCY, trustedContactIds: string[], durationMinutes?, destination?: {name?, latitude, longitude}, metadata?, correlationId} → ack {sessionId, session}
  - Fails if user already has active session (NIGHTGUARD_ALREADY_ACTIVE)
- `nightguard:join` {sessionId} → ack {participants, status}
- `nightguard:leave` {sessionId}
- `nightguard:location` {sessionId, location: {latitude, longitude, accuracy?, heading?, speed?, battery?, timestamp}, etaSeconds?, battery?}
- `nightguard:status` {sessionId, eventType: SOS|EMERGENCY_CALL|BUDDY_UPDATE|ETA_UPDATE|ARRIVAL|BATTERY_UPDATE|SAFETY_STATUS|LIVE_LOCATION|EMERGENCY_ACK, status?: string, note?, location?, battery?, etaSeconds?}
- `nightguard:sos` {sessionId?, location, message?, contactsToAlert?: string[], triggerType: MANUAL|SHAKE|INACTIVITY|BATTERY_LOW|OUT_OF_ROUTE} → ack {sessionId, alerted} ; if sessionId missing creates EMERGENCY session

**Server → Client**

- `nightguard:created`, `nightguard:joined`, `nightguard:location`, `nightguard:status`, `nightguard:sos` all shape {sessionId, eventType, userId, status?, location?, battery?, etaSeconds?, note?, timestamp, correlationId}
- `nightguard:global_sos` → all (for monitoring service) {sessionId, userId, location, triggerType, timestamp, correlationId}

## Live Location

**Client → Server**

- `live-location:start` {sessionId? (resume), trustedContactIds?, tripName?, destination?: {latitude, longitude, name?}, metadata?, correlationId} → ack {sessionId}
  - If sessionId provided tries resume, else creates new
- `live-location:update` {sessionId, location, eventType: START|STOP|PAUSE|RESUME|ETA_UPDATE|MOVEMENT|BATTERY_UPDATE|ACCURACY_UPDATE|SESSION_END|LOCATION_UPDATE, etaSeconds?, battery?, accuracy?, isMoving?}
- `live-location:action` {sessionId, action: PAUSE|RESUME|STOP, reason?} → ack

**Server → Client**

- `live-location:started` {sessionId, userId, eventType: START|RESUME, location, timestamp, correlationId}
- `live-location:update` {sessionId, userId, eventType, location, battery?, etaSeconds?, timestamp, correlationId}
- `live-location:ended` {sessionId, userId, eventType: SESSION_END, location, timestamp, correlationId}

## Notifications (Server internal + Server→Client push triggers)

Server internally triggers `notificationEngine.trigger({trigger, recipientId, title, body, data, correlationId})` which via pushAdapter logs and inserts into Supabase `push_notifications` table.

**Server → Client** `notification:trigger` {trigger: INCOMING_CALL|MISSED_CALL|CALL_BUSY|CALL_REJECTED|GROUP_INVITATION|NIGHTGUARD_ALERT|EMERGENCY_ALERT|BUDDY_PING|SOS_TRIGGERED, title, body, data, userId, timestamp, correlationId}

Client should show notification based on trigger.

## Error Handling Every Event

- Ack always includes `{success: true, data, correlationId, timestamp}` or `{success: false, error: {code, message, developerMessage, correlationId, recovery, timestamp, details?}}`
- If no ack callback provided, server emits `error` event with same error object.
- Errors have structured code from `ErrorCodes` enum.
- Never silent failure: every catch emits error or ack error.

## Versioning

- `EVENT_VERSION = 2.0.0` in `src/shared/types/events.ts`
- Client sends version in base envelope (optional). Server logs version, and includes version in `connected` ack.
- Future breaking changes bump major version, server can handle backward compat by checking payload.version.

## Retry & Timeout

- Client should implement retry with exponential backoff for failed acks (except validation errors 4xx)
- Server has per-event timeouts: ring 45s, presence throttle 2s, typing auto clear 5s, heartbeat 25s interval + 20s timeout = 45s total, disconnect if no heartbeat.
- For WebRTC, client should retry offer if no answer within 5s (ICE restart).

## Example Flow - Voice Call

1. A: `call:initiate` {targetUserId: B, callType: VOICE} → server creates callId, joins A to room, emits `call:incoming` to B, triggers push if B offline
2. B: receives `call:incoming`, shows ringing UI, B: `call:accept` {roomId}
3. Server: `call:accepted` to room, `call:participant_update` to room
4. A: creates RTCPeerConnection, createOffer → `webrtc:offer` {roomId, targetUserId: B, sdp}
5. Server: relay offer to B (direct socketId or user:room)
6. B: setRemoteDescription, createAnswer → `webrtc:answer`
7. Server: relay answer
8. Both trickle ICE via `webrtc:ice-candidate`
9. Media flows P2P, server not involved
10. Either: `call:end` → server emits `call:ended` to room, cleans up

## Example Flow - SOS

1. User: `nightguard:sos` {location, triggerType: SHAKE, contactsToAlert: [friend1, friend2]}
2. Server: creates emergency session if sessionId missing, sets SOS_TRIGGERED, io.to(session).emit sos, for each contact: emit to their sockets + push trigger EMERGENCY_ALERT critical
3. Server: emit `nightguard:global_sos` for monitoring
4. Contacts: receive sos, can `nightguard:join` to track, `nightguard:status` with EMERGENCY_ACK

## Rate Limits

- Global: 1000 req/min per IP (Fastify), per socket global via token bucket
- Call: 20/min per user
- Signal: 100/10s per user
- Message (lobby, chat, live comment): 30/min per user
- Heartbeat: 60/min per user (every second max)
- Exceed → RATE_LIMIT_EXCEEDED error with recovery retry after.

## Security Notes

- All payloads sanitized: control chars stripped, length capped, URLs validated
- DeviceId validated, max 5 per user
- Origin check in Socket.IO allowRequest
- JWT verified every connection, exp rejected
- Audit logs for call:initiate, accept, reject, end, nightguard:sos
