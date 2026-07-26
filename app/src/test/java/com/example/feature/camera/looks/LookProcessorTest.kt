package com.example.feature.camera.looks

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Tests for still-image grading support logic.
 *
 * Covers the two defects that made the first implementation unsafe on real
 * hardware: unbounded decode memory, and cache files that were never reclaimed.
 */
@RunWith(RobolectricTestRunner::class)
class LookProcessorTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    // ---- downsampling ------------------------------------------------------

    @Test
    fun `images within the ceiling are not downsampled`() {
        assertEquals(1, LookProcessor.calculateSampleSize(1920, 1080))
        assertEquals(1, LookProcessor.calculateSampleSize(4000, 3000))
    }

    @Test
    fun `a 50MP capture is downsampled`() {
        // 8160x6120 - a common 50 MP flagship sensor output.
        val sample = LookProcessor.calculateSampleSize(8160, 6120)
        assertEquals(2, sample)
        assertTrue("long edge must fall within budget", 8160 / sample <= LookProcessor.MAX_EDGE_PX)
    }

    @Test
    fun `a 108MP capture is downsampled further`() {
        val sample = LookProcessor.calculateSampleSize(12000, 9000)
        assertEquals(4, sample)
        assertTrue(12000 / sample <= LookProcessor.MAX_EDGE_PX)
    }

    @Test
    fun `downsampling bounds peak memory regardless of sensor size`() {
        // Two ARGB_8888 bitmaps (decode + graded copy) must stay well under a
        // typical 256 MB per-app heap.
        listOf(4000 to 3000, 8160 to 6120, 12000 to 9000, 16000 to 12000).forEach { (w, h) ->
            val s = LookProcessor.calculateSampleSize(w, h)
            val bytesPerBitmap = (w / s).toLong() * (h / s).toLong() * 4
            val peak = bytesPerBitmap * 2
            assertTrue(
                "peak ${peak / 1_000_000} MB too high for ${w}x$h",
                peak < 150_000_000
            )
        }
    }

    @Test
    fun `sample size is always a positive power of two`() {
        listOf(0 to 0, -1 to 100, 1 to 1, 99999 to 99999).forEach { (w, h) ->
            val s = LookProcessor.calculateSampleSize(w, h)
            assertTrue("sample must be >= 1", s >= 1)
            assertTrue("sample must be a power of two", s and (s - 1) == 0)
        }
    }

    @Test
    fun `degenerate dimensions do not crash`() {
        assertEquals(1, LookProcessor.calculateSampleSize(0, 0))
        assertEquals(1, LookProcessor.calculateSampleSize(-5, -5))
    }

    // ---- cache hygiene -----------------------------------------------------

    @Test
    fun `stale graded intermediates are purged`() {
        val old = File(context.cacheDir, "${LookProcessor.CACHE_PREFIX}old.jpg").apply {
            writeText("x")
            setLastModified(System.currentTimeMillis() - 48L * 60 * 60 * 1000)
        }
        assertTrue(old.exists())

        LookProcessor.purgeStaleCache(context)

        assertFalse("a 48h-old intermediate should be reclaimed", old.exists())
    }

    @Test
    fun `recent intermediates are kept`() {
        val fresh = File(context.cacheDir, "${LookProcessor.CACHE_PREFIX}fresh.jpg").apply {
            writeText("x")
        }
        LookProcessor.purgeStaleCache(context)
        assertTrue("a just-written intermediate must survive", fresh.exists())
        fresh.delete()
    }

    @Test
    fun `purging never touches unrelated cache files`() {
        val unrelated = File(context.cacheDir, "important_user_data.bin").apply {
            writeText("x")
            setLastModified(System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000)
        }

        LookProcessor.purgeStaleCache(context)

        assertTrue("must only reclaim its own files", unrelated.exists())
        unrelated.delete()
    }

    // ---- colour matrix -----------------------------------------------------

    @Test
    fun `an identity grade produces a neutral matrix`() {
        val matrix = LookProcessor.buildColorMatrix(FomoLook.NONE.scaled(1f))
        val expected = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        matrix.array.forEachIndexed { i, actual ->
            assertEquals("entry $i", expected[i], actual, 0.001f)
        }
    }

    @Test
    fun `contrast pivots about mid-grey rather than black`() {
        // With contrast c, the offset must be (1-c)*128 so mid-grey is fixed.
        val grade = FomoLook.Grade(
            tint = androidx.compose.ui.graphics.Color.Transparent,
            tintAlpha = 0f,
            contrast = 2f,
            saturation = 1f,
            brightness = 0f,
        )
        val matrix = LookProcessor.buildColorMatrix(grade)
        // Row 0: [c, 0, 0, 0, offset]
        assertEquals(2f, matrix.array[0], 0.001f)
        assertEquals(-128f, matrix.array[4], 0.001f)

        // 128 in -> 128 out.
        val out = matrix.array[0] * 128f + matrix.array[4]
        assertEquals(128f, out, 0.001f)
    }
}
