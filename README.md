# FOMO

Place-centric nightlife social app for Android. Jetpack Compose, CameraX,
Media3, Firebase.

---

## Open it in Android Studio

**Requirements**

| Tool | Version |
|---|---|
| Android Studio | Ladybug (2024.2.1) or newer |
| JDK | 17 (bundled with Studio — no separate install needed) |
| Android SDK | API 35 |

**Steps**

1. `File → Open…` and select this folder (do **not** use "Import Project").
2. Studio will prompt to download the Gradle distribution and the Android SDK
   components. Accept — everything else resolves automatically from Maven.
3. Wait for the first Gradle sync to finish.
4. Press **Run**.

The app builds and launches with **no further code changes and no
configuration**. Firebase-backed features (sign-in, chat sync, cloud upload)
detect that they are unconfigured and fall back to local-only mode rather than
crashing. See *Enabling Firebase* below to switch them on.

### If the Gradle wrapper is missing

`gradle/wrapper/gradle-wrapper.jar` is a binary and is **not** committed here.
Android Studio regenerates it automatically on first sync, so opening the
project in the IDE just works.

You only need this if you want to build from the **command line** before ever
opening Studio:

```bash
gradle wrapper --gradle-version 8.11.1
```

(or simply open the project in Studio once, which creates the file for you).

---

## Version matrix

Pinned to a mature, mutually-compatible set rather than the newest of
everything, so that a first import succeeds without manual intervention.

| Component | Version | Why |
|---|---|---|
| Android Gradle Plugin | 8.7.3 | Widely deployed and stable. AGP 9.x is very new and several third-party plugins still break on it. |
| Gradle | 8.11.1 | Matches AGP 8.7.x. |
| Kotlin | 2.0.21 | |
| KSP | 2.0.21-1.0.28 | **Must** match the Kotlin version exactly. |
| Compose BOM | 2024.12.01 | |
| compileSdk / targetSdk | 35 | |
| minSdk | 24 | |
| Java / JVM target | 17 | |

Everything is declared in `gradle/libs.versions.toml`.

---

## Enabling Firebase (optional)

The app runs without this. To turn on auth, chat sync and media upload:

1. Create a Firebase project and register an Android app with the package name
   **`com.findlyts.fomo`**.
2. Download `google-services.json` into `app/`.
   The Google Services plugin is applied *only if that file exists*, so adding
   it is all that is required.
3. Deploy the security rules (they are default-deny; without them your data is
   world-readable):
   ```bash
   firebase deploy --only firestore:rules,storage
   ```
   Rules live in `firebase/firestore.rules` and `firebase/storage.rules`.
4. Create the Firestore composite index the chat list needs:
   collection `conversations`, field `participants` (array-contains).
   Firestore will also print a direct link the first time the query runs.

### Alternative: `.env`

If you prefer not to commit `google-services.json`, copy `.env.example` to
`.env` and fill it in. The build reads it and exposes the values through
`BuildConfig`.

```bash
cp .env.example .env
```

`.env` is git-ignored.

---

## Building for Play

```bash
./gradlew :app:bundleRelease
```

Release signing is driven by environment variables, so no keystore or password
is ever committed:

```bash
export KEYSTORE_PATH=/absolute/path/to/upload-key.jks
export STORE_PASSWORD=…
export KEY_PASSWORD=…
export KEY_ALIAS=upload        # optional, defaults to "upload"
```

If those are unset the release build still succeeds but produces an **unsigned**
artifact. The output is `app/build/outputs/bundle/release/app-release.aab`.

Release builds run R8 with resource shrinking; keep rules are in
`app/proguard-rules.pro`.

---

## Project layout

```
app/src/main/java/com/example/
├── FomoApplication.kt          Firebase init, Coil loader, notification channels
├── MainActivity.kt             Single activity host
├── FomoApp.kt                  Navigation graph
├── core/
│   ├── data/                   Repositories (feed, chat, venue, media…)
│   └── navigation/             Type-safe routes
├── feature/
│   ├── camera/                 CameraX capture, Looks, Sound Aware, Live
│   ├── chats/                  Conversations, stories, safety features
│   ├── discover/  feed/  map/  profile/  settings/  …
└── ui/theme/
```

Reference material:

| Path | Contents |
|---|---|
| `docs/LAUNCH_READINESS.md` | Full pre-launch audit and outstanding work |
| `docs/LIVE_ARCHITECTURE.md` | Live implementation status + backend contract |
| `docs/schemas/` | Supabase reference schemas (not used at runtime) |
| `docs/ci/android-ci.yml` | CI workflow — see below |
| `tools/ktbalance.py` | Kotlin bracket-balance checker used during review |

### Enabling CI

The workflow could not be committed to `.github/workflows/` from the
environment that produced it. Move it yourself:

```bash
mkdir -p .github/workflows
git mv docs/ci/android-ci.yml .github/workflows/android.yml
```

---

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

126 unit tests covering the camera pipeline (rotation, zoom clamping, colour
grading, downsampling bounds), Sound Aware DSP (BPM estimation, onset
detection), Live readiness and crash recovery, chat identity and offline
delivery, and venue confidence scoring.

---

## Known limitations

Documented honestly in `docs/LAUNCH_READINESS.md`. The significant ones:

- **Live streaming does not transmit.** Recording, replay, crash recovery and
  readiness checks are real; there is no RTMP/WebRTC backend yet. The UI does
  not claim to be broadcasting when it isn't.
- **Several camera subsystems are UI-only**: Moment Templates, AI Scene
  Recognition, AI Moment Processing, Event Intelligence, Dual Shot.
- **No privacy policy or Play Data Safety disclosure yet** — both are mandatory
  before publishing, as the app collects location, camera, mic and account data.
