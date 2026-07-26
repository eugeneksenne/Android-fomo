# FOMO Live — Implementation Status & Backend Contract

Audited against **FOMO Live Engine — Billion-Dollar Scale Live Experience Platform**.

---

## 1. The central architectural point

The spec's most important line is:

> Every live is recorded locally before being uploaded.

and

> Even if internet disconnects, viewers disconnect, or server fails — the recording continues safely.

This is what makes FOMO Live buildable **today**, without streaming infrastructure.
The local-first half of the architecture — recording, readiness, crash recovery,
replay, upload queue — has no dependency on RTMP/WebRTC. That half is now real.

The remaining half — *transmitting to viewers* — cannot be faked. Anything that
displays viewers, comments or "LIVE" without a transport is lying to the
broadcaster about their own broadcast.

---

## 2. What is now implemented

| Spec engine | Status | Notes |
|---|---|---|
| Live Camera Engine | ✅ Real | CameraX `VideoCapture`. |
| **Local Recording Engine** | ✅ Real | Records to MediaStore MP4 the moment Live starts. |
| **Live Readiness Check** | ✅ Real | All 8 spec checks query the device: camera, mic, internet, GPS, venue, storage, battery, temperature. |
| **Storage Manager** | ✅ Real | Free space + estimated recording time; blocks below 500 MB. |
| **Crash Recovery Engine** | ✅ Real | Session marker written before recording; "Recovered Live Found → Recover / Delete" on next launch. |
| Replay Engine (basic) | ✅ Real | Ended session becomes a genuine replay from the recorded file. |
| Upload Queue (basic) | ⚠️ Partial | Session states QUEUED→UPLOADING→PUBLISHED/FAILED exist; **no WorkManager background retry yet**. |
| Local Replay Library | ⚠️ Partial | Data layer complete (`LiveSessionStore`); **no library UI screen yet**. |
| Venue / Event Intelligence | ✅ / ❌ | Venue real (GPS + confidence). Event linking absent. |
| Sound Aware Live | ⚠️ Partial | Real BPM/bass/energy detection on-device; beat events emitted but **not yet bound to visual effects**. |
| 3-second countdown | ✅ Real | Matches spec. |

### Fabrications removed

These actively misled the broadcaster and are gone:

| Was | Now |
|---|---|
| `watcherCount = 2410`, then `+= (-15..20).random()` every 3s | Starts at **0**; only moves when a backend reports real viewers. |
| 10 invented comments replayed every 2.5–5s ("Amanda: This is absolutely crazy! 🔥") | Removed. Only genuine local events shown. |
| Poll seeded at 142 / 84 votes with no audience | Starts at **0 / 0**. |
| Red **LIVE** badge while nothing transmits | Shows **REC** until real viewers exist. |
| `👥 2.4K` HUD | Shows **REC · LOCAL** until real viewers exist. |
| 5 readiness rows hardcoded `to true` | 8 real device checks with PASS/WARN/BLOCK. |

---

## 3. Not implemented — needs your backend

These are **blocked on infrastructure**, not on app work. Listed with exactly
what I need from you.

### 3.1 Live Broadcast Engine (transmit)
Android has **no platform RTMP support** and Jetpack provides none. A third-party
encoder is required — this is a dependency and licence decision for you:

| Option | Licence | Notes |
|---|---|---|
| `pedroSG94/RootEncoder` (rtmp-rtsp-stream-client-java) | Apache-2.0 | Most common RTMP(S) choice on Android. |
| `io.getstream:stream-webrtc-android` | MIT | WebRTC/WHIP; sub-second latency, heavier. |
| Managed SDK (Mux / LiveKit / IVS / Agora) | Commercial | Fastest path; they own scaling. |

**I need:** ingest protocol (RTMPS vs WHIP), ingest URL, and whether you want
me to pull in one of the above.

### 3.2 Auth model
**I need:** how a broadcast is authorised — stream key vs signed token, TTL,
and the endpoint the app calls to obtain it. Stream keys must never be
hardcoded; they should be minted per-session by your backend against the
Firebase ID token.

### 3.3 Playback
**I need:** the playback URL format (HLS / LL-HLS / DASH) and target latency.
Viewer-side playback also needs a player — Media3/ExoPlayer is the natural fit
and is not yet a dependency.

### 3.4 Adaptive Streaming
Spec lists 240p→1080p ladder. This is driven by the encoder SDK's bitrate
callbacks once 3.1 is chosen. Local recording stays at max quality regardless,
which is already how the recorder is configured.

### 3.5 Viewer Engine / Comments / Reactions / Polls
Currently zeroed out rather than faked.
**I need to know:** do these run over your streaming infrastructure's data
channel, or over Firestore? Firestore is straightforward and I can build it
immediately — realtime listeners per `live_sessions/{id}/comments` — if you
confirm that's the intended path.

### 3.6 Live Link
The spec's most distinctive feature: multiple independent broadcasters, each
owning their stream, linked into one venue experience with Manual / AI Director
/ PiP / Quad viewer modes.

This is **entirely a backend-coordination problem**: discovery of nearby
broadcasters, invitation handshake, session grouping, and synchronised
playback. Client UI is the easy part. **I need the session-grouping API**
before this is meaningful.

### 3.7 AI Live Director
Applause / fireworks / birthday-song / DJ-transition detection needs a trained
audio-event classifier (e.g. YAMNet via TFLite). The Sound Aware engine already
provides the audio pipeline to feed it. Energy heuristics alone cannot do this
honestly — I did not fake it.

---

## 4. Recommended order

1. **Decide 3.1 + 3.2** (encoder + auth). Everything else in Live unblocks from there.
2. Confirm **3.5** transport (Firestore vs data channel) — I can build comments,
   reactions and polls immediately on Firestore.
3. Add **WorkManager** background upload with retry to complete the Upload Queue.
4. Build the **Replay Library UI** over the existing `LiveSessionStore`.
5. Bind **Sound Aware beat events** to the visual effects (needs the shader work
   tracked in `LAUNCH_READINESS.md`, since effects aren't baked into output yet).
6. **Live Link** once session grouping exists.

---

## 5. Verification caveat

Nothing in this repository has been compiled — this environment has no JDK,
Android SDK, or network access to Google/Maven. The CI workflow at
`docs/ci/android-ci.yml` (move to `.github/workflows/`) is the real gate.
