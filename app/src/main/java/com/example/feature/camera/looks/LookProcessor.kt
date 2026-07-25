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
            val original = context.contentResolver.openInputStream(source)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return@withContext source

            val graded = applyGrade(original, grade)

            val output = File(context.cacheDir, "look_${System.currentTimeMillis()}.jpg")
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
