package com.example.feature.camera

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for capture-controller logic that is pure enough to verify off-device.
 *
 * Rotation mapping is covered because its absence was a launch-blocking bug:
 * every photo shot in landscape was saved rotated 90 degrees.
 */
@RunWith(RobolectricTestRunner::class)
class CameraCaptureControllerTest {

    // ---- device rotation ---------------------------------------------------

    @Test
    fun `portrait maps to rotation 0`() {
        assertEquals(Surface.ROTATION_0, CameraCaptureController.surfaceRotationFor(0))
        assertEquals(Surface.ROTATION_0, CameraCaptureController.surfaceRotationFor(10))
        assertEquals(Surface.ROTATION_0, CameraCaptureController.surfaceRotationFor(350))
    }

    @Test
    fun `landscape orientations map to the correct surface rotations`() {
        // Device rotated clockwise onto its left edge.
        assertEquals(Surface.ROTATION_270, CameraCaptureController.surfaceRotationFor(90))
        // Upside down.
        assertEquals(Surface.ROTATION_180, CameraCaptureController.surfaceRotationFor(180))
        // Rotated onto its right edge.
        assertEquals(Surface.ROTATION_90, CameraCaptureController.surfaceRotationFor(270))
    }

    @Test
    fun `rotation boundaries are handled consistently`() {
        // Exactly on the 45-degree quadrant boundaries.
        assertEquals(Surface.ROTATION_270, CameraCaptureController.surfaceRotationFor(45))
        assertEquals(Surface.ROTATION_180, CameraCaptureController.surfaceRotationFor(135))
        assertEquals(Surface.ROTATION_90, CameraCaptureController.surfaceRotationFor(225))
        assertEquals(Surface.ROTATION_0, CameraCaptureController.surfaceRotationFor(315))
    }

    @Test
    fun `every angle produces a valid surface rotation`() {
        val valid = setOf(
            Surface.ROTATION_0, Surface.ROTATION_90,
            Surface.ROTATION_180, Surface.ROTATION_270
        )
        (0..359).forEach { deg ->
            assertTrue(
                "angle $deg produced an invalid rotation",
                CameraCaptureController.surfaceRotationFor(deg) in valid
            )
        }
    }

    // ---- zoom clamping -----------------------------------------------------

    @Test
    fun `zoom is clamped to the sensor's supported range`() {
        assertEquals(1f, CameraCaptureController.clampZoom(0.1f, 1f, 10f), 0.001f)
        assertEquals(10f, CameraCaptureController.clampZoom(50f, 1f, 10f), 0.001f)
        assertEquals(5f, CameraCaptureController.clampZoom(5f, 1f, 10f), 0.001f)
    }

    @Test
    fun `an ultra-wide sensor permits sub-1x zoom`() {
        assertEquals(0.5f, CameraCaptureController.clampZoom(0.5f, 0.5f, 10f), 0.001f)
        assertEquals(0.5f, CameraCaptureController.clampZoom(0.1f, 0.5f, 10f), 0.001f)
    }

    @Test
    fun `a fixed-zoom camera collapses to its single ratio`() {
        // Some front cameras report min == max.
        assertEquals(1f, CameraCaptureController.clampZoom(3f, 1f, 1f), 0.001f)
    }

    @Test
    fun `a degenerate range does not produce NaN`() {
        val result = CameraCaptureController.clampZoom(Float.NaN, 1f, 10f)
        assertFalse("clamped zoom must never be NaN", result.isNaN())
        assertEquals(1f, result, 0.001f)
    }

    @Test
    fun `an inverted range falls back to the minimum`() {
        assertEquals(2f, CameraCaptureController.clampZoom(5f, 2f, 1f), 0.001f)
    }

    // ---- zoom equality -----------------------------------------------------

    @Test
    fun `near-identical ratios are treated as the same zoom`() {
        assertTrue(CameraCaptureController.isSameZoom(1.0f, 1.005f))
        assertTrue(CameraCaptureController.isSameZoom(2f, 2f))
    }

    @Test
    fun `distinct ratios are not conflated`() {
        assertFalse(CameraCaptureController.isSameZoom(1f, 2f))
        assertFalse(CameraCaptureController.isSameZoom(0.5f, 1f))
    }
}
