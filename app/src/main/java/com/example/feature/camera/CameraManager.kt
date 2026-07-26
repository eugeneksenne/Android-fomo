package com.example.feature.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * FOMO Live Camera Engine: CameraX Configuration & Lifecycle-Aware Hardware Manager
 * 
 * Manages CameraX initialization, surface previews, hardware video capture bindings,
 * lens switching, torch/flash controls, zoom ratios, and real-time frame buffer analysis.
 */
class CameraManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    var lensFacing: Int = CameraSelector.LENS_FACING_BACK
        private set

    var isTorchEnabled: Boolean = false
        private set

    var currentZoomRatio: Float = 1.0f
        private set

    /**
     * Initializes CameraX ProcessCameraProvider asynchronously.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onInitialized: ((Boolean) -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val success = bindCameraUseCases(lifecycleOwner, previewView)
                onInitialized?.invoke(success)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CameraX provider", e)
                onInitialized?.invoke(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Public method to bind CameraX use cases directly to a LifecycleOwner with a custom or PreviewView SurfaceProvider.
     */
    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider? = null
    ): Boolean {
        val provider = cameraProvider ?: return false

        // Check CAMERA permission
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "CAMERA permission not granted")
            return false
        }

        var targetLensFacing = lensFacing
        var cameraSelector = CameraSelector.Builder().requireLensFacing(targetLensFacing).build()

        try {
            if (!provider.hasCamera(cameraSelector)) {
                // Try fallback lens facing
                targetLensFacing = if (targetLensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                cameraSelector = CameraSelector.Builder().requireLensFacing(targetLensFacing).build()
                if (!provider.hasCamera(cameraSelector)) {
                    Log.w(TAG, "No camera hardware available on device")
                    return false
                }
                lensFacing = targetLensFacing
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking camera availability", e)
            return false
        }

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .also { previewUseCase ->
                surfaceProvider?.let { provider ->
                    previewUseCase.surfaceProvider = provider
                }
            }

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeFrame(imageProxy)
                }
            }

        return try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )
            camera?.cameraControl?.enableTorch(isTorchEnabled)
            camera?.cameraControl?.setZoomRatio(currentZoomRatio)
            true
        } catch (exc: Exception) {
            Log.e(TAG, "Lifecycle binding with 3 use cases failed, trying fallback without ImageAnalysis", exc)
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                camera?.cameraControl?.enableTorch(isTorchEnabled)
                camera?.cameraControl?.setZoomRatio(currentZoomRatio)
                true
            } catch (exc2: Exception) {
                Log.e(TAG, "Fallback camera binding failed", exc2)
                false
            }
        }
    }

    /**
     * Binds preview, capture, and real-time analysis use-cases to the lifecycle owner.
     */
    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ): Boolean {
        return bindToLifecycle(lifecycleOwner, previewView.surfaceProvider)
    }

    /**
     * Switches between Back and Front camera lenses.
     */
    fun toggleCameraLens(lifecycleOwner: LifecycleOwner, previewView: PreviewView): Boolean {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        return bindCameraUseCases(lifecycleOwner, previewView)
    }

    /**
     * Toggles flashlight / torch mode.
     */
    fun setTorchEnabled(enabled: Boolean) {
        isTorchEnabled = enabled
        camera?.cameraControl?.enableTorch(enabled)
    }

    /**
     * Sets optical/digital zoom ratio.
     */
    fun setZoomRatio(zoomRatio: Float) {
        currentZoomRatio = zoomRatio
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    /**
     * Frame analyzer callback for real-time live render audio/scene telemetry.
     */
    private fun analyzeFrame(imageProxy: ImageProxy) {
        // Frame processing pipeline (e.g., lighting level calculation or gesture detection)
        imageProxy.close()
    }

    /**
     * Releases hardware resources.
     */
    fun release() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing CameraManager resources", e)
        }
    }

    companion object {
        private const val TAG = "FomoCameraManager"
    }
}
