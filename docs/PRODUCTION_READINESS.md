# FOMO Production Readiness Notes

## Mobile client configuration

FOMO prefers Firebase configuration through `google-services.json` for Android release builds. Local and CI builds can also provide public Firebase client values through `.env` using the Gradle Secrets plugin:

```properties
FIREBASE_API_KEY=...
FIREBASE_APPLICATION_ID=...
FIREBASE_PROJECT_ID=...
FIREBASE_STORAGE_BUCKET=...
MAPS_API_KEY=...
GEMINI_API_KEY=...
```

Do not commit private signing keys, service-account JSON files, or backend credentials.

## Authentication hardening

- Firebase initialization is centralized in `FirebaseRuntimeConfig`.
- The app no longer hard-codes Firebase client values in Kotlin source.
- Google Sign-In no longer silently falls back to anonymous sign-in when OAuth configuration is missing. Users receive a clear recovery message and can use email or guest access instead.
- Email/password, password reset, and guest access fail gracefully when Firebase is not configured.

## Runtime permissions

- Android location permission requests use `ActivityResultContracts.RequestMultiplePermissions` and request both fine and coarse location.
- Android 13+ notification permission requests use `ActivityResultContracts.RequestPermission` and `POST_NOTIFICATIONS`.
- Denials are non-blocking: users can continue onboarding and enable permissions later in Settings.

## Release checklist additions

Before Play Store release:

1. Add production `google-services.json` or inject `.env` Firebase public client config.
2. Configure `default_web_client_id` for Google Sign-In.
3. Verify Firebase Auth providers enabled in the production Firebase project.
4. Confirm App Check enforcement and Firestore security rules are enabled.
5. Confirm permission copy matches the Play Data safety declarations.
