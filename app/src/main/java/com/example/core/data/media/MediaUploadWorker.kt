package com.example.core.data.media

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.feature.camera.live.LiveSessionStore
import java.util.concurrent.TimeUnit

/**
 * Background upload for captured moments and Live replays.
 *
 * The spec requires "Background upload", an upload queue with retry, and
 * user-selectable preferences (immediately / Wi-Fi only / charging only).
 * Previously uploads ran inline in a Composable's `coroutineScope`: navigating
 * away, backgrounding the app, or a dropped connection silently abandoned the
 * upload and the moment was never published.
 *
 * WorkManager survives process death and reboots, and retries with exponential
 * backoff.
 */
class MediaUploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_LOCAL_URI)
            ?: return Result.failure(errorData("No media URI supplied."))
        val isVideo = inputData.getBoolean(KEY_IS_VIDEO, false)
        val sessionId = inputData.getString(KEY_SESSION_ID)

        val store = if (sessionId != null) {
            LiveSessionStore.getInstance(applicationContext).also { it.markUploading(sessionId) }
        } else null

        val outcome = MediaUploader.uploadMoment(
            localUri = Uri.parse(uriString),
            isVideo = isVideo,
        )

        return outcome.fold(
            onSuccess = { downloadUrl ->
                sessionId?.let { store?.markPublished(it, downloadUrl) }
                Result.success(Data.Builder().putString(KEY_DOWNLOAD_URL, downloadUrl).build())
            },
            onFailure = { error ->
                Log.w(TAG, "Upload attempt ${runAttemptCount + 1} failed", error)

                // Auth/permission problems will never succeed on retry; a
                // transient network fault will. Distinguishing them avoids
                // burning the retry budget on a hopeless request.
                val permanent = error is IllegalStateException
                when {
                    permanent -> {
                        sessionId?.let { store?.markFailed(it, error.message ?: "Upload failed") }
                        Result.failure(errorData(error.message ?: "Upload failed"))
                    }

                    runAttemptCount >= MAX_ATTEMPTS -> {
                        sessionId?.let {
                            store?.markFailed(it, "Upload failed after $MAX_ATTEMPTS attempts")
                        }
                        Result.failure(errorData("Upload failed after $MAX_ATTEMPTS attempts"))
                    }

                    else -> Result.retry()
                }
            }
        )
    }

    private fun errorData(message: String): Data =
        Data.Builder().putString(KEY_ERROR, message).build()

    companion object {
        private const val TAG = "MediaUploadWorker"
        private const val MAX_ATTEMPTS = 5

        const val KEY_LOCAL_URI = "localUri"
        const val KEY_IS_VIDEO = "isVideo"
        const val KEY_SESSION_ID = "sessionId"
        const val KEY_DOWNLOAD_URL = "downloadUrl"
        const val KEY_ERROR = "error"

        /** Mirrors the spec's Upload Preferences. */
        enum class UploadPolicy { IMMEDIATE, WIFI_ONLY, CHARGING_ONLY }

        /**
         * Enqueues an upload. Unique per [sessionId] (or URI) so a retry or a
         * double-tap can't produce duplicate posts.
         */
        fun enqueue(
            context: Context,
            localUri: Uri,
            isVideo: Boolean,
            sessionId: String? = null,
            policy: UploadPolicy = UploadPolicy.IMMEDIATE,
        ): String {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    when (policy) {
                        UploadPolicy.WIFI_ONLY -> NetworkType.UNMETERED
                        else -> NetworkType.CONNECTED
                    }
                )
                .setRequiresCharging(policy == UploadPolicy.CHARGING_ONLY)
                .setRequiresStorageNotLow(false)
                .build()

            val request = OneTimeWorkRequestBuilder<MediaUploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_LOCAL_URI, localUri.toString())
                        .putBoolean(KEY_IS_VIDEO, isVideo)
                        .putString(KEY_SESSION_ID, sessionId)
                        .build()
                )
                .addTag(TAG)
                .build()

            val workName = uniqueNameFor(sessionId, localUri)
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.KEEP,
                request
            )
            return workName
        }

        /** Stable de-duplication key. */
        internal fun uniqueNameFor(sessionId: String?, localUri: Uri): String =
            "upload_${sessionId ?: localUri.toString().hashCode()}"
    }
}
