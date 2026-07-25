package com.example.feature.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Real CameraX capture pipeline for [CameraScreen].
 *
 * Replaces the previous simulation, in which the "viewfinder" was a static
 * Unsplash photo and the shutter simply waited ~3s before attaching a different
 * hardcoded stock image URL to the published moment. Nothing was ever captured
 * and no permission was ever requested.
 *
 * Captured media is written through [MediaStore], so on API 29+ no storage
 * permission is required.
 */
class CameraCaptureController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    /** True while a video recording is in progress. */
    @Volatile
    var isRecording: Boolean = false
        private set

    /**
     * Binds preview + capture use cases to [lifecycleOwner].
     *
     * Safe to call repeatedly (e.g. when flipping the camera or toggling
     * flash); previous bindings are released first.
     */
    suspend fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean,
        flashMode: Int,
    ): Result<Unit> = runCatching {
        val provider = cameraProvider ?: awaitCameraProvider().also { cameraProvider = it }

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.fromOrderedList(listOf(Quality.FHD, Quality.HD, Quality.SD))
            )
            .build()
        val video = VideoCapture.withOutput(recorder)

        val selector =
            if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA

        provider.unbindAll()
        // Preview + ImageCapture + VideoCapture is a guaranteed-supported
        // combination on LIMITED-level devices and above.
        provider.bindToLifecycle(lifecycleOwner, selector, preview, capture, video)

        imageCapture = capture
        videoCapture = video
    }.onFailure { Log.e(TAG, "Failed to bind camera use cases", it) }

    /** Updates flash without a full rebind. */
    fun setFlashMode(mode: Int) {
        imageCapture?.flashMode = mode
    }

    /** True when the device actually exposes a front-facing camera. */
    fun hasFrontCamera(): Boolean =
        runCatching {
            cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
        }.getOrDefault(false)

    /**
     * Captures a still image into the device gallery.
     *
     * @return the content [Uri] of the saved photo.
     */
    suspend fun capturePhoto(): Result<Uri> {
        val capture = imageCapture
            ?: return Result.failure(IllegalStateException("Camera is not ready yet."))

        val name = "FOMO_${timestamp()}"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FOMO")
            }
        }

        val options = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        return suspendCancellableCoroutine { cont ->
            capture.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val uri = output.savedUri
                        if (uri != null) {
                            cont.resume(Result.success(uri))
                        } else {
                            cont.resume(
                                Result.failure(IllegalStateException("Photo saved without a URI."))
                            )
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed", exception)
                        cont.resume(Result.failure(exception))
                    }
                }
            )
        }
    }

    /**
     * Starts recording video to the gallery.
     *
     * Audio is only enabled when RECORD_AUDIO has actually been granted, so a
     * user who declined the mic can still record silent video instead of
     * hitting a SecurityException.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(onFinalised: (Result<Uri>) -> Unit): Result<Unit> = runCatching {
        val video = videoCapture ?: error("Camera is not ready yet.")
        check(!isRecording) { "A recording is already in progress." }

        val name = "FOMO_${timestamp()}"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/FOMO")
            }
        }

        val output = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values)
            .build()

        var pending = video.output.prepareRecording(context, output)
        // Only request audio when the mic permission was actually granted;
        // calling withAudioEnabled() without it throws a SecurityException.
        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
            pending = pending.withAudioEnabled()
        }

        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> isRecording = true
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    activeRecording = null
                    if (event.hasError()) {
                        Log.e(TAG, "Video capture failed: ${event.error}", event.cause)
                        onFinalised(
                            Result.failure(
                                event.cause ?: IllegalStateException("Recording failed (${event.error}).")
                            )
                        )
                    } else {
                        onFinalised(Result.success(event.outputResults.outputUri))
                    }
                }
            }
        }
    }.onFailure {
        isRecording = false
        Log.e(TAG, "Unable to start recording", it)
    }

    /** Stops an in-flight recording; the URI arrives via the `onFinalised` callback. */
    fun stopRecording() {
        runCatching { activeRecording?.stop() }
            .onFailure { Log.e(TAG, "Unable to stop recording", it) }
        activeRecording = null
    }

    /** Releases the camera. Must be called when the screen leaves composition. */
    fun release() {
        runCatching {
            activeRecording?.stop()
            activeRecording = null
            isRecording = false
            cameraProvider?.unbindAll()
        }.onFailure { Log.e(TAG, "Error releasing camera", it) }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private suspend fun awaitCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        cont.resume(future.get())
                    } catch (e: Exception) {
                        cont.cancel(e)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())

    companion object {
        private const val TAG = "CameraCapture"

        val REQUIRED_PERMISSIONS: List<String> = listOf(Manifest.permission.CAMERA)

        fun hasCameraPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }
}
