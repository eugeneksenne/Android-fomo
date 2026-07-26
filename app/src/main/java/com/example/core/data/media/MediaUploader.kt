package com.example.core.data.media

import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Uploads locally captured media to Firebase Storage and returns a shareable
 * download URL.
 *
 * Why this exists: the Camera screen saves captures to the device gallery and
 * gets back a `content://media/...` Uri. That Uri is meaningless on any other
 * device, so publishing it directly produced moments that only rendered for
 * their author. Feeds must reference an `https://` URL.
 */
object MediaUploader {

    private const val TAG = "MediaUploader"

    /** Progress callback receives a 0f..1f fraction. */
    suspend fun uploadMoment(
        localUri: Uri,
        isVideo: Boolean,
        onProgress: (Float) -> Unit = {},
    ): Result<String> = runCatching {
        // Throws if FomoApplication could not initialise Firebase.
        runCatching { FirebaseApp.getInstance() }.getOrElse {
            error("Firebase is not configured; cannot publish media.")
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: error("You must be signed in to publish a moment.")

        val extension = if (isVideo) "mp4" else "jpg"
        val path = "moments/$uid/${System.currentTimeMillis()}.$extension"

        val ref = FirebaseStorage.getInstance().reference.child(path)

        val task = ref.putFile(localUri)
        task.addOnProgressListener { snapshot ->
            val total = snapshot.totalByteCount
            if (total > 0) {
                onProgress(snapshot.bytesTransferred.toFloat() / total.toFloat())
            }
        }
        task.await()

        ref.downloadUrl.await().toString()
    }.onFailure { Log.e(TAG, "Media upload failed", it) }
}
