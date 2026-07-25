package com.example.feature.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
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
import kotlin.math.abs

/**
 * CameraX capture pipeline for [CameraScreen].
 *
 * Covers the behaviours users expect from a flagship social camera:
 *  - device-orientation-aware capture (photos/videos save upright),
 *  - real optical/digital zoom driven by [androidx.camera.core.CameraControl],
 *  - real tap-to-focus metering,
 *  - torch for video (the flash mode on [ImageCapture] only fires for stills),
 *  - graceful handling of devices without a front camera or flash unit.
 */
class CameraCaptureController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var camera: Camera? = null

    @Volatile
    var isRecording: Boolean = false
        private set

    /**
     * Device rotation tracker.
     *
     * Without this, CameraX defaults to the display rotation captured at bind
     * time. Because the activity locks out configuration changes for
     * orientation, the display never reports a change and every photo taken
     * while holding the phone sideways was saved rotated 90 degrees.
     */
    private val orientationListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            val rotation = when {
                orientation >= 315 || orientation < 45 -> Surface.ROTATION_0
                orientation < 135 -> Surface.ROTATION_270
                orientation < 225 -> Surface.ROTATION_180
                else -> Surface.ROTATION_90
            }
            imageCapture?.targetRotation = rotation
            videoCapture?.targetRotation = rotation
        }
    }

    /** Zoom range actually supported by the bound camera. */
    var minZoomRatio: Float = 1f
        private set
    var maxZoomRatio: Float = 1f
        private set

    /** True when the bound camera has a usable flash unit. */
    var hasFlashUnit: Boolean = false
        private set

    suspend fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean,
        flashMode: Int,
    ): Result<Unit> = runCatching {
        val provider = cameraProvider ?: awaitCameraProvider().also { cameraProvider = it }

        val selector = resolveSelector(provider, useFrontCamera)

        val resolution = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolution)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(resolution)
            .setFlashMode(flashMode)
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.fromOrderedList(listOf(Quality.FHD, Quality.HD, Quality.SD))
            )
            .build()
        val video = VideoCapture.withOutput(recorder)

        provider.unbindAll()
        val boundCamera = provider.bindToLifecycle(
            lifecycleOwner, selector, preview, capture, video
        )

        imageCapture = capture
        videoCapture = video
        camera = boundCamera

        boundCamera.cameraInfo.zoomState.value?.let { zoom ->
            minZoomRatio = zoom.minZoomRatio
            maxZoomRatio = zoom.maxZoomRatio
        }
        hasFlashUnit = boundCamera.cameraInfo.hasFlashUnit()

        orientationListener.enable()
    }.onFailure {
        Log.e(TAG, "Failed to bind camera use cases", it)
    }

    /**
     * Falls back to whatever lens exists rather than throwing. Many tablets and
     * some budget devices have no front camera; requesting one that isn't there
     * previously failed the whole bind and left a black screen.
     */
    private fun resolveSelector(
        provider: ProcessCameraProvider,
        useFrontCamera: Boolean,
    ): CameraSelector {
        val desired =
            if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA
        if (runCatching { provider.hasCamera(desired) }.getOrDefault(false)) return desired

        val fallback =
            if (useFrontCamera) CameraSelector.DEFAULT_BACK_CAMERA
            else CameraSelector.DEFAULT_FRONT_CAMERA
        if (runCatching { provider.hasCamera(fallback) }.getOrDefault(false)) return fallback

        return desired
    }

    fun setFlashMode(mode: Int) {
        imageCapture?.flashMode = mode
    }

    /**
     * Torch stays on continuously — required for video, where [ImageCapture]'s
     * flash mode has no effect.
     */
    fun setTorch(enabled: Boolean) {
        val cam = camera ?: return
        if (!cam.cameraInfo.hasFlashUnit()) return
        runCatching { cam.cameraControl.enableTorch(enabled) }
            .onFailure { Log.w(TAG, "Unable to toggle torch", it) }
    }

    /**
     * Applies a real zoom ratio, clamped to what the sensor supports.
     * The zoom buttons previously only scaled the preview *view*, which
     * magnified the on-screen image but had no effect on captured output.
     *
     * @return the ratio actually applied.
     */
    fun setZoomRatio(ratio: Float): Float {
        val cam = camera ?: return 1f
        val clamped = ratio.coerceIn(minZoomRatio, maxZoomRatio)
        runCatching { cam.cameraControl.setZoomRatio(clamped) }
            .onFailure { Log.w(TAG, "Unable to set zoom", it) }
        return clamped
    }

    /** Relative zoom for pinch gestures. */
    fun scaleZoom(factor: Float): Float {
        val current = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
        return setZoomRatio(current * factor)
    }

    /**
     * Real autofocus + auto-exposure metering at the tapped point.
     * The tap previously drew a focus ring animation and nothing else.
     *
     * @param x,y coordinates within the preview, in pixels.
     */
    fun focusAt(previewView: PreviewView, x: Float, y: Float) {
        val cam = camera ?: return
        runCatching {
            val point = previewView.meteringPointFactory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                .addPoint(point, FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(AUTO_CANCEL_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            cam.cameraControl.startFocusAndMetering(action)
        }.onFailure { Log.w(TAG, "Focus/metering request failed", it) }
    }

    fun hasFrontCamera(): Boolean =
        runCatching {
            cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
        }.getOrDefault(false)

    suspend fun capturePhoto(): Result<Uri> {
        val capture = imageCapture
            ?: return Result.failure(IllegalStateException("Camera is not ready yet."))

        val name = "FOMO_${timestamp()}"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FOMO")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val metadata = ImageCapture.Metadata().apply {
            // Mirror selfies so the saved image matches the preview.
            isReversedHorizontal = isUsingFrontCamera()
        }

        val options = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).setMetadata(metadata).build()

        return suspendCancellableCoroutine { cont ->
            capture.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val uri = output.savedUri
                        if (uri == null) {
                            cont.resume(
                                Result.failure(IllegalStateException("Photo saved without a URI."))
                            )
                            return
                        }
                        publish(uri)
                        cont.resume(Result.success(uri))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed", exception)
                        cont.resume(Result.failure(exception))
                    }
                }
            )
        }
    }

    /** Clears IS_PENDING so the file becomes visible in the gallery. */
    private fun publish(uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(uri, done, null, null)
        }
    }

    private fun isUsingFrontCamera(): Boolean =
        runCatching {
            camera?.cameraInfo?.lensFacing == CameraSelector.LENS_FACING_FRONT
        }.getOrDefault(false)

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
                                event.cause
                                    ?: IllegalStateException("Recording failed (${event.error}).")
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

    fun stopRecording() {
        runCatching { activeRecording?.stop() }
            .onFailure { Log.e(TAG, "Unable to stop recording", it) }
        activeRecording = null
    }

    fun release() {
        runCatching {
            orientationListener.disable()
            activeRecording?.stop()
            activeRecording = null
            isRecording = false
            camera = null
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
        private const val AUTO_CANCEL_SECONDS = 4L

        val REQUIRED_PERMISSIONS: List<String> = listOf(Manifest.permission.CAMERA)

        fun hasCameraPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

        /**
         * Maps a rotation-degree reading onto the nearest [Surface] constant.
         * Extracted for testability.
         */
        internal fun surfaceRotationFor(degrees: Int): Int = when {
            degrees >= 315 || degrees < 45 -> Surface.ROTATION_0
            degrees < 135 -> Surface.ROTATION_270
            degrees < 225 -> Surface.ROTATION_180
            else -> Surface.ROTATION_90
        }

        /** Clamps a requested zoom ratio into the supported range. */
        internal fun clampZoom(requested: Float, min: Float, max: Float): Float =
            when {
                max <= min -> min
                requested.isNaN() -> min
                else -> requested.coerceIn(min, max)
            }

        /** True when two ratios are close enough to skip a redundant update. */
        internal fun isSameZoom(a: Float, b: Float): Boolean = abs(a - b) < 0.01f
    }
}
