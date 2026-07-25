# FOMO — Launch Readiness Assessment

**Audit date:** 2026-07-25
**Verdict:** 🔴 **NOT ready to publish today.**

The app is a large, polished Compose UI (~50k lines) but a substantial portion of
the "full stack" is simulated. This document lists what was fixed in this pass,
and what still blocks a public release.

> **Note on verification:** this sandbox has no JDK, no Android SDK and no network
> access to `dl.google.com` / `services.gradle.org` / Maven Central, so **nothing
> here has been compiled or run.** All findings are from static review.
>
> A CI workflow is provided at **`docs/ci/android-ci.yml`**. It could not be
> committed to `.github/workflows/` because this session's GitHub token lacks the
> `workflows` permission — **move it yourself** to activate it:
> ```bash
> mkdir -p .github/workflows && git mv docs/ci/android-ci.yml .github/workflows/android.yml
> ```
> Treat its first green run as the real verification gate.

---

## 1. Fixed in this pass

### Blockers
| # | Issue | Fix |
|---|-------|-----|
| 1 | **No `INTERNET` permission.** Every feature (Firebase, Coil images, WebView map) is network-backed. The app was non-functional on a real device. | Added `INTERNET`, `ACCESS_NETWORK_STATE`, camera, mic, location and notification permissions to `AndroidManifest.xml`. |
| 2 | **No Gradle wrapper.** No `gradlew`, no `gradle-wrapper.properties`. CI and fresh clones could not build. | Added `gradlew` + wrapper properties pinned to **Gradle 9.3.1**. ⚠️ `gradle-wrapper.jar` still must be generated once — see `gradle/wrapper/README.md`. |
| 3 | **Wrong JDK/Gradle for AGP 9.1.1.** Project targeted Java 11; AGP 9.1 requires **Gradle ≥ 9.3.1 and JDK 17**. | Bumped `compileOptions` to 17 and pinned `kotlin { jvmToolchain(17) }`. |
| 4 | **Debug builds could not sign.** `debug` used a `debug.keystore` that is git-ignored and absent. | Removed the broken config; debug now uses the standard auto-generated keystore. |
| 5 | **Release build would fail configuration** when signing env vars were absent. | Signing config is registered only when the keystore *and* passwords are present. |
| 6 | **No `Application` class**; Firebase was initialised ad-hoc inside a Composable. | Added `FomoApplication` — single init point, plus notification channels. |
| 7 | **Camera was 100% fake** (see §2). | Implemented a real CameraX pipeline. |

### Security
| # | Issue | Fix |
|---|-------|-----|
| 8 | **WebView accepted invalid TLS certificates** (`handler.proceed()` on any SSL error) in a viewer explicitly built for payment gateways. Trivially MITM-able; automatic Play rejection. | Now calls `handler.cancel()` and shows a specific, human-readable error. |
| 9 | **WebView auto-granted geolocation** to any page without asking. | Now denies by default. |
| 10 | **WebView allowed file/content access + mixed content.** | `allowFileAccess`/`allowContentAccess` off; `MIXED_CONTENT_NEVER_ALLOW`. |
| 11 | **Map WebView used `MIXED_CONTENT_ALWAYS_ALLOW`** while exposing a `@JavascriptInterface` bridge. | Set to `MIXED_CONTENT_NEVER_ALLOW`. |
| 12 | **Hardcoded Firebase API key, app ID and project ID** in `WelcomeAuthScreen.kt`. | Moved to `.env` → BuildConfig via the Secrets plugin. **The exposed key must still be rotated — see §3.** |
| 13 | **No Firestore security rules.** Clients read the entire `conversations` collection and seeded it. | Added `firebase/firestore.rules` (default-deny). ⚠️ Requires client changes — see §3. |
| 14 | **No Storage rules.** | Added `firebase/storage.rules` with per-user ownership and size limits. |
| 15 | **Client-side DB seeding** would write demo data into the production database on first launch. | Gated behind `BuildConfig.DEBUG`. |
| 16 | Release builds were **unminified and unshrunk**, with a near-empty ProGuard file. | Enabled R8 + resource shrinking and wrote full keep rules (serialization, Firestore models, Moshi, CameraX, JS bridge). |

### Correctness / policy
| # | Issue | Fix |
|---|-------|-----|
| 17 | **Google Sign-In silently signed users in anonymously** and displayed "Google Sign-In successful!", inventing the email `google.explorer@fomoapp.com` and writing a Google-attributed record to Firestore. Deceptive; a Play policy risk. | Removed the fallback; failures now report honestly. |
| 18 | `FirebaseAuth.getInstance()` was called unguarded in 5 places — `IllegalStateException` if Firebase was unconfigured. | Added a `requireAuth()` helper that fails gracefully. |
| 19 | **OpenStreetMap attribution was hidden** via `attributionControl: false` and `display: none`. Breaches ODbL and the OSM tile usage policy. | Attribution restored and styled for the dark theme. |
| 20 | Placeholder `default_web_client_id` string resource would collide with the one generated from `google-services.json`. | Removed. |
| 21 | `.sql` schema files sat inside `app/src/main/java/`. | Moved to `docs/schemas/`. |

---

## 2. The Camera screen (reviewed in detail)

**Before this pass the camera captured nothing.** Specifically:

- The "viewfinder" was a remote Unsplash JPEG of an unrelated nightclub.
- There were **no CameraX dependencies** — all four were commented out in `build.gradle.kts`.
- There was **no `CAMERA` permission** and no runtime permission request.
- The shutter ran `delay(800)` four times, then set `capturedPhotoUrl` to a
  *different* hardcoded Unsplash URL. Photo, video and live each had their own
  hardcoded stock image.
- "Publish" pushed that stock URL into `FeedRepository` and `MyCircleRepository`.
  A user could "post a photo" with the lens cap on and see a stranger's picture
  appear in the feed.
- The flip-camera icon only toggled a decorative overlay.
- "Dual Shot" rendered a stock portrait of an unrelated person as the selfie feed.

**Now implemented:**
- `CameraCaptureController` — real CameraX `Preview` + `ImageCapture` + `VideoCapture`,
  bound to the composable lifecycle, saving via `MediaStore` (no storage permission
  needed on API 29+), with correct release on dispose.
- `CameraViewfinder` — live `PreviewView`, runtime permission flow, and a proper
  denied-state screen with a path to Settings.
- Real photo capture, real video record/stop, real front/back lens switching,
  flash mapped to `ImageCapture.FLASH_MODE_*`.
- `MediaUploader` — uploads the captured file to Firebase Storage and returns an
  `https://` download URL, so published moments resolve on **other** devices.
  The publish dialog now shows genuine upload percentage.
- Removed the fake "Dual Shot" selfie bubble.
- `cameraError` is now actually rendered (it was set in six places and never shown).

**Still simulated on this screen (clearly commented in code):**
- **Live streaming.** There is no RTMP/WebRTC ingest or CDN. "Go Live" now records
  a real local video and publishes it as a replay, but *no one can watch it live*.
- **Viewer count** (`watcherCount += (-15..20).random()`) — fabricated audience numbers.
- **Live comments** — a fixed array replayed at random intervals.
- **"SOUND AWARE: n BPM"** — a random integer; no audio is sampled.
- **Looks / effects / templates** are colour overlays on the preview only; they are
  **not baked into the saved file**. The published media will not match the preview.

---

## 2b. Camera spec conformance (`Full FOMO Camera`)

Audited against the supplied spec. "Real" = actually functional, not a UI shell.

| Spec subsystem | Status | Notes |
|---|---|---|
| Camera Capture Engine (Photo/Video) | ✅ Real | CameraX; saves via MediaStore. |
| Three modes PHOTO/VIDEO/LIVE | ✅ Real | Tap or horizontal swipe (spec requirement); locked during capture. |
| Venue Intelligence | ✅ Real | New `VenueIntelligence`: GPS → offline pack → **computed** confidence. Was a hardcoded "Confidence: 99%". |
| Low-confidence fallback (Nearby/Search/Skip) | ⚠️ Partial | Nearby + Skip implemented; **venue search not built.** |
| Publish Settings (visibility/destination/venue) | ✅ Real | **Was a privacy bug** — settings were collected then discarded; "Private" posted publicly. Now enforced. |
| Upload Engine (per-destination progress) | ⚠️ Partial | Real Firebase Storage upload + real %; **per-destination checklist still cosmetic.** |
| Looks (13 profiles) | ✅ Real | `FomoLook` catalogue drives preview AND output. Stills graded via ColorMatrix (EXIF preserved); video graded on GPU via **Media3 Transformer**. Preview and saved file can no longer drift. |
| Looks long-press intensity | ✅ Real | Long-press opens a 0-100% intensity slider; interpolates from neutral. |
| Looks carousel placement | ⚠️ Deviates | Spec puts it full-width below capture; it is a 90dp column beside it. |
| Creative Effects (11) | ⚠️ Preview-only | Same as Looks — not applied to output. |
| Moment Templates (10) | ❌ Cosmetic | UI list only; no template is applied. |
| Sound Aware Engine | ⚠️ Partial | **Now real**: `SoundAwareEngine` opens the mic and runs energy-based onset detection (median inter-onset → BPM), low-band bass energy and crowd-energy classification, all on-device. BPM was `(120..128).random()`. Beat events are emitted; **beat-reactive visual effects are not yet bound to them**, and applause/vocal-peak/drop detection need a classifier. |
| Dual Shot Engine | ❌ Removed | Was a stock photo of an unrelated person. Real version needs CameraX concurrent camera (device-gated). |
| AI Scene Recognition | ❌ Missing | No classifier. |
| AI Moment Processing | ❌ Fake | The "best frame / stabilisation / noise reduction" steps are `delay()` calls. |
| AI Capture Suggestions | ❌ Fake | Static strings, not context-driven. |
| Event Intelligence | ❌ Missing | No event linking. |
| Offline Engine / drafts / queue | ❌ Missing | "Drafts" button shows a Toast. No queue, no retry, no offline capture path. |
| Ripple Integration | ⚠️ Cosmetic | "+25 Ripple Points" is a fixed literal. |
| Performance targets (250 ms, 60 FPS) | ❓ Unverified | Never profiled — nothing has been run. |
| Creator Tools | ❌ Missing | None of the 8 items exist. |

**Net:** roughly 10 of ~20 spec subsystems are genuinely implemented. The camera
now *captures and publishes correctly*, which it did not before, but the
differentiating engines (Sound Aware, Dual Shot, AI processing, Offline queue)
are still absent.


### Flagship-camera hardening (this pass)

| Issue | Impact | Fix |
|---|---|---|
| **No rotation handling** | Every photo/video taken in landscape saved **rotated 90°**. The activity locks orientation config changes, so CameraX never learned the device had turned. | `OrientationEventListener` drives `targetRotation` on both use cases. |
| **Zoom was fake** | Buttons scaled the preview with `graphicsLayer`; the captured file stayed at 1x. What you saw was never what you got. | Real `CameraControl.setZoomRatio`, clamped to the sensor's range, plus **pinch-to-zoom**. Unsupported ratios are hidden. |
| **Tap-to-focus was fake** | Drew a focus ring; no metering ever ran. | Real `startFocusAndMetering` (AF+AE) at the tapped point. |
| **Flash dead for video** | `ImageCapture.flashMode` only fires for stills, so the button did nothing while recording. | Torch via `CameraControl.enableTorch` for VIDEO/LIVE; auto-off on return to PHOTO. |
| **Selfies not mirrored** | Front-camera stills saved un-mirrored, not matching the preview. | `Metadata.isReversedHorizontal`. |
| **Crash on no front camera** | Requesting an absent lens failed the whole bind → black screen. | Falls back to an available lens. |
| **Gallery button was a Toast** | "Opening FOMO Local Moments Drafts..." | Real system photo picker (`PickVisualMedia`, no storage permission) + last-capture thumbnail. |
| No haptics | Felt unresponsive vs. flagship cameras. | Shutter, zoom and Look-selection haptics. |
| Photos invisible in gallery | Files written without clearing `IS_PENDING`. | Published after save. |


### Runtime-correctness pass (second review)

Bugs that static feature-completeness checks miss but that break on real hardware:

| Issue | Impact | Fix |
|---|---|---|
| **Microphone contention** | `SoundAwareEngine` held `AudioSource.MIC` continuously. CameraX's `withAudioEnabled()` needs the same input, and most devices allow one client — so **every recorded video was silent or failed outright**. The single worst regression introduced by adding Sound Aware. | Analysis pauses for the duration of any recording and resumes after. `stop()` now releases `AudioRecord` synchronously instead of relying on async coroutine cancellation, so CameraX can't lose the race. |
| **OOM on flagship sensors** | `LookProcessor` decoded at full resolution then allocated a second ARGB_8888 copy: **400 MB for a 50 MP photo, 864 MB for 108 MP**, against a typical 192–512 MB heap. The Look filter crashed on exactly the phones it targets. | Bounds-only decode + power-of-two `inSampleSize` capping the long edge at 4096 px. Peak is now ~100 MB regardless of sensor. |
| **Unbounded cache growth** | Every graded photo/video wrote a full-size intermediate to `cacheDir` that was never reclaimed — gigabytes over normal use. | 24 h TTL sweep before each write, scoped to this class's own `look_` files. |
| **Double-tap shutter** | Rapid presses queued concurrent `takePicture()` calls racing to set state and opening the publish sheet repeatedly. | Shutter disabled while `isProcessing`. |
| **Shutter deadlock** | If recording failed to *start*, `isProcessing` stayed `true` and the newly added guard left the button permanently disabled. | All failure paths reset capture state. |
| **Uploads died with the screen** | Publishing ran in the Composable's `coroutineScope`; backgrounding the app or losing signal abandoned the moment permanently. | `MediaUploadWorker` (WorkManager) with exponential backoff, unique-work de-duplication, and the spec's Wi-Fi-only / charging-only policies. Permanent vs. transient failures are distinguished so retries aren't wasted. |
| **Missing R8 keep rules** | Media3 Transformer resolves codecs and GL processors reflectively; WorkManager instantiates workers by class name. Both would fail **only in release builds**. | Keep rules added for Media3, WorkManager and ExifInterface. |


### Lifecycle & durability pass (fourth review)

| Issue | Impact | Fix |
|---|---|---|
| **Video thumbnails never rendered** | A code comment claimed "Coil renders a frame from a local video Uri". That is false for Coil 2.x — `coil-video` is a separate artifact and `VideoFrameDecoder` must be registered. Every clip thumbnail (gallery button, publish preview) was **blank**. | Added `coil-video`; registered `VideoFrameDecoder` in an app-wide `ImageLoaderFactory`, with a disk cache so feed avatars aren't refetched on every scroll. |
| **Screen slept mid-recording** | Filming a long set, the display timed out; that pauses the preview and can finalise the recording. Footage lost. | `keepScreenOn` scoped to the camera screen, cleared on exit. |
| **Backgrounding truncated the clip** | Taking a call or hitting recents abandoned the recording mid-write with no finalise. | `ON_PAUSE` stops recording cleanly so the muxer flushes and the normal replay flow runs; UI flags reset so the shutter isn't left stuck. |
| **Release raced finalisation** | `release()` nulled `activeRecording` immediately after `stop()`, but finalisation is async — the reference could be collected before the file was written. | Reference is retained until `Finalize` confirms the file is on disk. |
| **Recording until the disk filled** | No file-size cap. The muxer fails on a full volume and the entire clip is lost. | `setFileSizeLimit` with a 300 MB reserve (50 MB floor). CameraX finalises cleanly at the limit, keeping everything captured so far. |
| **Timer drifted from the clip** | On-screen duration was a `delay(1000)` UI counter, which drifts against the media timeline on long recordings. | Polled from `VideoRecordEvent.Status.recordingStats` — the recorder's own clock. |


## 2c. Chats screen audit

| Issue | Impact | Fix |
|---|---|---|
| **Message ownership was globally broken** | Every client wrote `senderId = "me"`, and the UI decided ownership with `senderId == "me"`. The moment two people shared a conversation, **both saw every message as their own** — right-aligned, attributed to themselves. The single most severe defect found in the app. | `senderId` is the real Firebase uid; ownership resolves via `ChatRepository.isFromCurrentUser()`, with a legacy path for seed rows. |
| **Offline messages were silently destroyed** | Reconnecting only *marked* the queue delivered and cleared it. Nothing was ever written to Firestore. The sender saw a delivered tick; **the message reached nobody.** | `flushOfflineQueue()` actually transmits on reconnect, confirms delivery per-message, and re-queues on failure. Nothing is dropped. |
| **Offline state was a manual toggle** | A toolbar switch labelled "Simulate Offline Queue" was the only source of offline state. Real signal loss was undetectable. | New `ConnectivityObserver` (NetworkCallback + `NET_CAPABILITY_VALIDATED`, so captive portals count as offline) drives it. |
| **Every message stamped `"10:08 PM"`** | A hardcoded string literal. A whole day of conversation displayed the same time. | Real locale-aware wall-clock time. |
| **Read receipts were fabricated** | `isRead` was set `true` on send, so every message showed as read by the recipient instantly. Users treat this as a factual signal about another person. | Always `false` on send. |
| **Client violated its own security rules** | Conversations carried no `participants` field and the client listened to the entire collection. `firestore.rules` grants access only when `uid in participants` — so deploying the rules would have **denied every read and write**, bricking chat. | Conversations now carry `participants` + `createdBy`; the listener uses `.whereArrayContains("participants", uid)`. |
| Stale offline indicator | `remember { !isNetworkOnline }` snapshotted once at composition and never updated. | Derived from state on each recomposition. |

### Tooling

Added `tools/ktbalance.py` — a Kotlin-aware bracket checker handling string
templates, nested templates, raw strings, char literals, backtick identifiers
and nested comments. My previous ad-hoc regex counting produced false results
on both counts (it reported imbalances in known-good files, and would have
missed real ones). All 79 Kotlin files verified balanced. This is a lexical
check only — **it is not a substitute for compiling.**


## 2d. Feed screen audit

| Issue | Impact | Fix |
|---|---|---|
| **No moderation controls anywhere in the app** | Google Play's User Generated Content policy requires an in-app way to **report** objectionable content and **block** abusive users. There was no report, block, hide or mute affordance in the feed, chats or stories. This is one of the most common causes of outright Play rejection for social apps — the listing would very likely have been refused. | New `ModerationRepository` + `ModerationSheet`: report (9 categories), block, hide, optional block-on-report. Blocked authors and hidden posts are filtered from the feed immediately and the state persists across restarts. Reports write to a `content_reports` collection that is create-only from the client. |
| **Video posts never played** | Every moment rendered through `AsyncImage`, including `VIDEO`, `LIVE` and `REPLAY` types — so video showed one frozen frame. In a vertical, video-first feed this is the core interaction. | New `FeedVideoPlayer` (Media3/ExoPlayer): autoplay, loop, only the visible page plays, muted by default with a per-post sound toggle, player released on page dispose so decoders don't leak. |
| **Comments attributed to a fictional user** | `addComment` defaulted to author `"Me"` with a stock avatar, so every comment in a shared feed showed the same fake identity — the same class of bug found in chats. | Defaults resolve from the signed-in account. |

Notes:
- Reported content is hidden for the reporter **immediately**, without waiting
  for review, which is what Play expects.
- `ReportReason` is persisted by enum name, so a keep rule was added to stop R8
  obfuscating it and making historical reports unreadable.


## 2e. Feed spec conformance (`FOMO Feed System`)

| Spec area | Status | Notes |
|---|---|---|
| Four tabs (For You / Following / Nearby / Live) | ✅ Real | Live correctly filters to active broadcasts only. |
| Full-screen media + right action rail | ✅ Real | |
| **Moment Invitation — 3 states** | ✅ **Fixed** | Was driven by a **debug button shipping in the production UI** ("Invite State: N", cycling on tap). State now derives from an absolute expiry timestamp. |
| **Invitation countdown** | ✅ **Fixed** | Was the hardcoded literal `"Available for 02:44:18"` — frozen forever. Now ticks from real expiry, and stops ticking once ended so it doesn't burn a coroutine. |
| Invitation durations (15/30/60/120 min, Until I Leave) | ✅ Real | Modelled; "Until I Leave" as an open-ended expiry. |
| Creator ends sharing early | ✅ Real | `endInvitation()` records *when*, so state can't contradict the clock. |
| **Metadata context line** | ✅ **Fixed** | Only `timeAgo` rendered. Now tab-aware per spec: venue on For You/Following, distance on Nearby, `LIVE • 2.4K watching` on Live, `Replay • Ended …` on replays. |
| **"Who's Here" privacy default** | ✅ **Fixed** | Defaulted to `"PUBLIC"` while the dialog told the user "DEFAULT IS PRIVATE". Tapping Share Presence without changing anything would have **broadcast the user's real-time venue location publicly**. Now defaults to Private, per spec. |
| Ripple states (Idle→Viral) | ✅ Real | |
| Verification badges | ✅ Real | |
| Caption 3 lines + translation | ✅ Real | |
| Friend Intelligence line | ⚠️ Static | Rendered from a stored string; not computed from real friend activity. |
| Follow button hides once following | ⚠️ Partial | Toggles, but is not hidden for own content. |
| Search (creators/venues/events/sounds/hashtags) | ⚠️ Partial | Filters loaded moments by text only. |
| Feed ranking engine (FOMO Score) | ❌ Missing | Tabs filter; there is no ranking by velocity, watch completion, distance or trust. |
| Watch completion / view telemetry | ❌ Missing | Nothing is measured, so ranking has no inputs. |
| Creator analytics (14 metrics) | ❌ Cosmetic | Sheet exists; values are not real. |
| Replay retention tiers (7/30/90/180 days) | ❌ Missing | No retention policy implemented. |
| FlashList virtualization / preloading | ⚠️ Partial | `VerticalPager` virtualizes; no next-item preloading. |

---

## 3. Remaining blockers before you can publish

1. **Rotate the leaked Firebase credentials.** API key `AIzaSyBWw5-…UghLQ`, project
   `findlyts-4116c`, app ID `1:191228918156:…` are in git history. Restrict the key
   in Google Cloud Console (Android app + SHA-1), enable App Check, and rotate.
2. **Generate and commit `gradle/wrapper/gradle-wrapper.jar`** (see `gradle/wrapper/README.md`).
3. **Get one green CI run.** Nothing in this repo has ever been compiled here.
   Expect iteration on the first build.
4. **Update the chat client to match the new Firestore rules.** `ChatRepository`
   listens to the whole `conversations` collection; under default-deny rules it must
   write a `participants` array and query with `whereArrayContains("participants", uid)`.
   Move seed data to the Admin SDK.
5. **`applicationId` is `com.findlyts.fomo` but `namespace` is `com.example`.**
   `com.example` is a placeholder Google Play will reject in some flows; rename the
   package properly.
6. **`versionCode = 1` / `versionName = "1.0"`** — wire these to CI.
7. **Bake filters into captured media**, or remove the filter UI. Shipping a preview
   that doesn't match the saved file is a bug users will notice immediately.
8. **Remove fabricated engagement** (fake viewer counts, fake comments, random BPM)
   or clearly label it as demo content.
9. **Legal + Play Data safety.** No privacy policy or ToS is present, yet the app
   collects location, camera, mic and account data, and has a **Night Guard** feature
   handling safety-critical live location. A Data Safety form is mandatory.
10. **No real test coverage.** The suite is 4 repository tests plus `assertEquals(4, 2+2)`
    and a screenshot of `Greeting("Robolectric")` — a composable not used in the app.
11. **Decide on Live transport.** The local-first half (recording, readiness, crash
    recovery, replay) is now real — see `docs/LIVE_ARCHITECTURE.md`. Transmission
    is blocked on your RTMP/WebRTC decision; the UI no longer claims to be
    broadcasting when it isn't.
12. **`minSdk 24` + `enableOnBackInvokedCallback`** — verify predictive-back behaviour,
    and test on an API 24 device.

---

## 4. Suggested order of work

1. Rotate credentials; generate the wrapper JAR; get CI green.
2. Deploy `firestore.rules` + `storage.rules`; fix `ChatRepository` to match.
3. Rename `com.example` → a real package.
4. Resolve the Live/BPM/viewer-count honesty items.
5. Privacy policy, ToS, Play Data Safety.
6. Real tests around auth, capture/publish and the safety features.
