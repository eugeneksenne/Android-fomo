package com.example.feature.camera.looks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Bakes a [FomoLook] into captured media.
 *
 * Until now Looks were preview-only: a translucent `Box` drawn over the
 * viewfinder. The saved JPEG/MP4 was always ungraded, so the shared post never
 * matched what the user framed — the single most visible correctness bug in the
 * camera.
 *
 * Stills are processed here with a ColorMatrix (hardware-accelerated by
 * `Canvas`), which is dependency-free and fast enough for a single frame.
 * Video is handled by [LookVideoProcessor] using Media3 Transformer, which
 * performs the grade on the GPU.
 *
 * EXIF orientation is preserved — dropping it would rotate every processed
 * photo.
 */
object LookProcessor {

    private const val TAG = "LookProcessor"
    private const val JPEG_QUALITY = 95

    /** Long-edge ceiling after downsampling (comfortably above 4K delivery). */
    internal const val MAX_EDGE_PX = 4096

    /** Graded intermediates older than this are swept on next use. */
    private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000

    internal const val CACHE_PREFIX = "look_"

    /**
     * Applies [grade] to the image at [source] and writes a new JPEG.
     *
     * @return the processed file, or the original [source] when the grade is a
     *   no-op or processing fails — a failed filter must never cost the user
     *   their capture.
     */
    suspend fun applyToImage(
        context: Context,
        source: Uri,
        grade: FomoLook.Grade,
    ): Uri = withContext(Dispatchers.Default) {
        if (grade.isIdentity) return@withContext source

        try {
            // Decode bounds first so we can downsample. A full-resolution
            // ARGB_8888 decode of a 50 MP capture is ~200 MB, and applyGrade
            // allocates a second copy of the same size - 400 MB peak, well past
            // the per-app heap limit on most devices. Without this the filter
            // OOMs on exactly the flagship phones this feature targets.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = false
            }

            val original = context.contentResolver.openInputStream(source)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: return@withContext source

            val graded = applyGrade(original, grade)

            // Sweep stale intermediates before writing a new one. These are
            // full-size media files; without this they accumulate in cacheDir
            // indefinitely and can consume gigabytes over normal use.
            purgeStaleCache(context)

            val output = File(context.cacheDir, "$CACHE_PREFIX${System.currentTimeMillis()}.jpg")
            FileOutputStream(output).use { out ->
                graded.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            copyExifOrientation(context, source, output)

            if (graded !== original) original.recycle()
            graded.recycle()

            Uri.fromFile(output)
        } catch (e: Throwable) {
            // OutOfMemoryError is a real possibility on large sensors, and it is
            // an Error rather than an Exception - catch Throwable so a filter
            // failure degrades to the ungraded original instead of crashing.
            Log.e(TAG, "Unable to apply Look to image; keeping the original", e)
            source
        }
    }

    /** Applies the colour grade to [source], returning a new bitmap. */
    internal fun applyGrade(source: Bitmap, grade: FomoLook.Grade): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        // NOTE: this is the second full-size allocation; the caller downsamples
        // first via calculateSampleSize so the pair stays within budget.

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                buildColorMatrix(grade)
            )
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Tint pass on top of the tonal grade.
        if (grade.tintAlpha > 0.001f) {
            val tintPaint = Paint().apply {
                colorFilter = PorterDuffColorFilter(
                    android.graphics.Color.argb(
                        (grade.tintAlpha * 255).toInt().coerceIn(0, 255),
                        (grade.tint.red * 255).toInt().coerceIn(0, 255),
                        (grade.tint.green * 255).toInt().coerceIn(0, 255),
                        (grade.tint.blue * 255).toInt().coerceIn(0, 255),
                    ),
                    PorterDuff.Mode.SRC_ATOP
                )
            }
            canvas.drawBitmap(result, 0f, 0f, tintPaint)
        }

        return result
    }

    /**
     * Deletes graded intermediates older than [CACHE_TTL_MS].
     *
     * Safe to call frequently: it only touches files this class created, and
     * never the user's originals in MediaStore.
     */
    internal fun purgeStaleCache(context: Context, nowMs: Long = System.currentTimeMillis()) {
        runCatching {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.isFile &&
                    file.name.startsWith(CACHE_PREFIX) &&
                    nowMs - file.lastModified() > CACHE_TTL_MS
                ) {
                    file.delete()
                }
            }
        }.onFailure { Log.w(TAG, "Unable to purge Look cache", it) }
    }

    /**
     * Chooses a power-of-two downsample factor keeping the long edge at or
     * below [MAX_EDGE_PX]. That is ample for social delivery (well above 1080p)
     * while bounding memory to a predictable ceiling.
     */
    internal fun calculateSampleSize(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (maxOf(width / sample, height / sample) > MAX_EDGE_PX) {
            sample *= 2
        }
        return sample
    }

    /**
     * Builds a combined saturation / contrast / brightness matrix.
     *
     * Contrast is applied about mid-grey (0.5) rather than about black, so
     * increasing it doesn't also brighten the whole frame.
     */
    internal fun buildColorMatrix(grade: FomoLook.Grade): android.graphics.ColorMatrix {
        val matrix = android.graphics.ColorMatrix().apply {
            setSaturation(grade.saturation.coerceAtLeast(0f))
        }

        val c = grade.contrast.coerceAtLeast(0f)
        // Offset keeps mid-grey fixed: t = (1 - c) * 128
        val t = (1f - c) * 128f
        val b = grade.brightness * 255f

        val contrastMatrix = android.graphics.ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t + b,
                0f, c, 0f, 0f, t + b,
                0f, 0f, c, 0f, t + b,
                0f, 0f, 0f, 1f, 0f,
            )
        )

        matrix.postConcat(contrastMatrix)
        return matrix
    }

    /**
     * Carries EXIF orientation from the original onto the processed file.
     * Without this every graded photo loses its rotation and displays sideways.
     */
    private fun copyExifOrientation(context: Context, source: Uri, target: File) {
        try {
            val orientation = context.contentResolver.openInputStream(source)?.use { input ->
                ExifInterface(input).getAttribute(ExifInterface.TAG_ORIENTATION)
            } ?: return

            ExifInterface(target.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation)
                saveAttributes()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to carry EXIF orientation across", e)
        }
    }
}
