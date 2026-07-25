package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Application entry point.
 *
 * Responsibilities:
 *  - Initialise Firebase exactly once, on the main thread, before any repository
 *    singleton touches [com.google.firebase.firestore.FirebaseFirestore.getInstance].
 *    Previously each screen/repository initialised Firebase ad-hoc inside a
 *    try/catch, which meant the very first repository to be touched would throw
 *    and silently fall back to seed data.
 *  - Register notification channels required on Android 8+.
 *
 * Configuration precedence:
 *  1. `google-services.json` (processed by the Google Services Gradle plugin).
 *     This is the supported production path.
 *  2. Values injected at build time from `.env` (see `.env.example`) via the
 *     Secrets Gradle plugin, used as a fallback so CI and local builds work
 *     without checking a `google-services.json` into source control.
 *
 * If neither is available the app still starts; every Firebase-backed repository
 * degrades to local seed data rather than crashing.
 */
class FomoApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        initialiseFirebase()
        createNotificationChannels()
    }

    /**
     * Application-wide Coil loader.
     *
     * [VideoFrameDecoder] must be registered explicitly — Coil 2.x cannot read
     * video at all without it, so every clip thumbnail (gallery button, publish
     * preview, replay library) silently rendered blank.
     *
     * Also sets a disk cache so remote feed avatars are not refetched on every
     * scroll, and enables hardware bitmaps for lower memory pressure.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(120L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()

    private fun initialiseFirebase() {
        // The Google Services plugin auto-initialises Firebase when
        // google-services.json is present. Nothing more to do in that case.
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            firebaseReady = true
            return
        }

        val apiKey = BuildConfig.FIREBASE_API_KEY
        val appId = BuildConfig.FIREBASE_APP_ID
        val projectId = BuildConfig.FIREBASE_PROJECT_ID

        if (apiKey.isBlank() || appId.isBlank() || projectId.isBlank() ||
            apiKey.startsWith("your_") || appId.startsWith("your_") || projectId.startsWith("your_")
        ) {
            Log.w(
                TAG,
                "Firebase is not configured. Add google-services.json or populate .env " +
                    "(FIREBASE_API_KEY / FIREBASE_APP_ID / FIREBASE_PROJECT_ID). " +
                    "Running in local-only mode."
            )
            firebaseReady = false
            return
        }

        firebaseReady = try {
            val options = FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId(appId)
                .setProjectId(projectId)
                .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET.takeIf { it.isNotBlank() })
                .build()
            FirebaseApp.initializeApp(this, options)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialisation failed; continuing in local-only mode.", e)
            false
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_SOCIAL,
                "Social",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Messages, invites and circle activity." },
            NotificationChannel(
                CHANNEL_SAFETY,
                "Safety",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Night Guard check-ins and safety alerts." }
        )
        channels.forEach { runCatching { manager.createNotificationChannel(it) } }
    }

    companion object {
        private const val TAG = "FomoApplication"

        const val CHANNEL_SOCIAL = "fomo_social"
        const val CHANNEL_SAFETY = "fomo_safety"

        /**
         * True when a [FirebaseApp] was successfully initialised. Repositories can
         * check this to skip network work entirely instead of relying on exceptions.
         */
        @Volatile
        var firebaseReady: Boolean = false
            private set
    }
}
