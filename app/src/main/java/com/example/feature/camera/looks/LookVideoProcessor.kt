package com.example.feature.camera.looks

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.RgbMatrix
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Bakes a [FomoLook] into a recorded video using Media3 Transformer.
 *
 * Transformer runs the colour transform on the GPU and re-encodes with the
 * hardware encoder, which is what makes this viable on-device. Attempting the
 * same with per-frame bitmap work would be far too slow for a full clip.
 *
 * As with stills, a failure returns the original clip: losing a user's
 * recording because a filter failed would be far worse than shipping it
 * ungraded.
 */
@UnstableApi
object LookVideoProcessor {

    private const val TAG = "LookVideoProcessor"

    /**
     * @param onProgress optional 0f..1f callback.
     * @return the graded file, or [source] if the grade is a no-op or export fails.
     */
    suspend fun applyToVideo(
        context: Context,
        source: Uri,
        grade: FomoLook.Grade,
    ): Uri {
        if (grade.isIdentity) return source

        val output = File(context.cacheDir, "look_${System.currentTimeMillis()}.mp4")

        return suspendCancellableCoroutine { cont ->
            val transformer = try {
                Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, result: ExportResult) {
                            if (cont.isActive) cont.resume(Uri.fromFile(output))
                        }

                        override fun onError(
                            composition: Composition,
                            result: ExportResult,
                            exception: ExportException,
                        ) {
                            Log.e(TAG, "Look export failed; keeping the original clip", exception)
                            if (cont.isActive) cont.resume(source)
                        }
                    })
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Unable to create Transformer", e)
                cont.resume(source)
                return@suspendCancellableCoroutine
            }

            val effects = buildEffects(grade)
            val item = EditedMediaItem.Builder(MediaItem.fromUri(source))
                .setEffects(Effects(emptyList(), effects))
                .build()

            try {
                transformer.start(item, output.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to start Look export", e)
                if (cont.isActive) cont.resume(source)
                return@suspendCancellableCoroutine
            }

            cont.invokeOnCancellation {
                runCatching { transformer.cancel() }
            }
        }
    }

    /**
     * Translates a [FomoLook.Grade] into Media3 video effects.
     *
     * Saturation is expressed through Media3's built-in greyscale filter when
     * fully desaturated; otherwise the tonal grade is applied as an RGB matrix.
     */
    internal fun buildEffects(grade: FomoLook.Grade): List<Effect> {
        val effects = mutableListOf<Effect>()

        if (grade.saturation <= 0.01f) {
            effects += RgbFilter.createGrayscaleFilter()
        }

        val matrix = rgbMatrixFor(grade)
        effects += RgbMatrix { _, _ -> matrix }

        return effects
    }

    /**
     * Builds the 4x4 column-major RGB matrix Media3 expects.
     *
     * Combines contrast about mid-grey, brightness and (when not fully
     * greyscaled) saturation via luminance weights.
     */
    internal fun rgbMatrixFor(grade: FomoLook.Grade): FloatArray {
        val c = grade.contrast.coerceAtLeast(0f)
        val s = grade.saturation.coerceIn(0f, 4f)
        val b = grade.brightness

        // Rec.709 luminance weights.
        val lr = 0.2126f
        val lg = 0.7152f
        val lb = 0.0722f

        // Saturation matrix entries.
        val sr = (1f - s) * lr
        val sg = (1f - s) * lg
        val sb = (1f - s) * lb

        // Contrast about 0.5 in normalised space, plus brightness.
        val offset = (0.5f - 0.5f * c) + b

        // Blend tint toward the grade colour by tintAlpha.
        val a = grade.tintAlpha.coerceIn(0f, 1f)
        val keep = 1f - a

        return floatArrayOf(
            (sr + s) * c * keep, sr * c * keep, sr * c * keep, 0f,
            sg * c * keep, (sg + s) * c * keep, sg * c * keep, 0f,
            sb * c * keep, sb * c * keep, (sb + s) * c * keep, 0f,
            offset + grade.tint.red * a, offset + grade.tint.green * a, offset + grade.tint.blue * a, 1f,
        )
    }
}
