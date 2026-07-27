package com.example.core.config

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Centralized Firebase bootstrap configuration.
 *
 * Production builds should be configured through google-services.json. The fallback
 * path below exists for CI/dev sandboxes where google-services.json is intentionally
 * absent; values are supplied by the Gradle Secrets plugin from .env files.
 */
object FirebaseRuntimeConfig {
    private val invalidMarkers = listOf("placeholder", "your_", "change_me", "default")

    fun hasUsableFirebaseConfig(apiKey: String?, applicationId: String?, projectId: String?): Boolean {
        return listOf(apiKey, applicationId, projectId).all { value ->
            val normalized = value?.trim().orEmpty()
            normalized.isNotEmpty() && invalidMarkers.none { marker -> normalized.contains(marker, ignoreCase = true) }
        }
    }

    fun hasUsableOAuthClientId(webClientId: String?): Boolean {
        val normalized = webClientId?.trim().orEmpty()
        return normalized.isNotEmpty() && invalidMarkers.none { marker -> normalized.contains(marker, ignoreCase = true) }
    }

    fun ensureFirebaseApp(
        context: Context,
        apiKey: String?,
        applicationId: String?,
        projectId: String?,
        storageBucket: String? = null
    ): FirebaseApp? {
        FirebaseApp.getApps(context).firstOrNull()?.let { return it }

        if (!hasUsableFirebaseConfig(apiKey, applicationId, projectId)) {
            return null
        }

        val options = FirebaseOptions.Builder()
            .setApiKey(apiKey!!.trim())
            .setApplicationId(applicationId!!.trim())
            .setProjectId(projectId!!.trim())
            .apply {
                val bucket = storageBucket?.trim().orEmpty()
                if (bucket.isNotEmpty() && invalidMarkers.none { marker -> bucket.contains(marker, ignoreCase = true) }) {
                    setStorageBucket(bucket)
                }
            }
            .build()

        return FirebaseApp.initializeApp(context, options)
    }
}
