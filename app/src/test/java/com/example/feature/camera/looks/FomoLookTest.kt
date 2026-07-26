package com.example.feature.camera.looks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the Looks colour-grading model.
 *
 * Looks were previously translucent overlays drawn only on the preview, defined
 * by duplicated `when` blocks in three places. Nothing was applied to the saved
 * file, so there was no grading logic to test.
 */
class FomoLookTest {

    @Test
    fun `NONE is an identity grade at every intensity`() {
        assertTrue(FomoLook.NONE.isIdentity)
        assertTrue(FomoLook.NONE.scaled(1f).isIdentity)
        assertTrue(FomoLook.NONE.scaled(0.5f).isIdentity)
    }

    @Test
    fun `zero intensity neutralises any Look`() {
        FomoLook.carousel.forEach { look ->
            val grade = look.scaled(0f)
            assertTrue(
                "${look.displayName} at 0% must be a no-op",
                grade.isIdentity
            )
        }
    }

    @Test
    fun `full intensity reproduces the Look's own parameters`() {
        val look = FomoLook.NEON
        val grade = look.scaled(1f)
        assertEquals(look.contrast, grade.contrast, 0.0001f)
        assertEquals(look.saturation, grade.saturation, 0.0001f)
        assertEquals(look.brightness, grade.brightness, 0.0001f)
        assertEquals(look.tintAlpha, grade.tintAlpha, 0.0001f)
    }

    @Test
    fun `half intensity interpolates from neutral`() {
        val grade = FomoLook.NEON.scaled(0.5f)
        // contrast 1.18 -> halfway from 1.0 is 1.09
        assertEquals(1.09f, grade.contrast, 0.001f)
        // saturation 1.30 -> halfway from 1.0 is 1.15
        assertEquals(1.15f, grade.saturation, 0.001f)
        assertEquals(FomoLook.NEON.tintAlpha / 2f, grade.tintAlpha, 0.001f)
    }

    @Test
    fun `intensity is clamped to a valid range`() {
        val over = FomoLook.PULSE.scaled(5f)
        val under = FomoLook.PULSE.scaled(-2f)
        assertEquals(FomoLook.PULSE.contrast, over.contrast, 0.0001f)
        assertTrue("negative intensity must clamp to identity", under.isIdentity)
    }

    @Test
    fun `Noir fully desaturates`() {
        assertEquals(0f, FomoLook.NOIR.scaled(1f).saturation, 0.0001f)
    }

    @Test
    fun `every Look has sane parameters`() {
        FomoLook.carousel.forEach { look ->
            assertTrue("${look.displayName} contrast", look.contrast in 0f..3f)
            assertTrue("${look.displayName} saturation", look.saturation in 0f..3f)
            assertTrue("${look.displayName} brightness", look.brightness in -1f..1f)
            assertTrue("${look.displayName} tintAlpha", look.tintAlpha in 0f..1f)
            assertTrue("${look.displayName} needs a name", look.displayName.isNotBlank())
        }
    }

    @Test
    fun `lookup by display name is case insensitive and falls back safely`() {
        assertEquals(FomoLook.PULSE, FomoLook.fromDisplayName("Pulse"))
        assertEquals(FomoLook.PULSE, FomoLook.fromDisplayName("pulse"))
        assertEquals(FomoLook.VINTAGE_PARTY, FomoLook.fromDisplayName("Vintage Party"))
        assertEquals(FomoLook.NONE, FomoLook.fromDisplayName("NotARealLook"))
        assertEquals(FomoLook.NONE, FomoLook.fromDisplayName(""))
    }

    @Test
    fun `carousel exposes every Look including a clear option`() {
        assertTrue(FomoLook.carousel.contains(FomoLook.NONE))
        assertEquals(FomoLook.entries.size, FomoLook.carousel.size)
    }

    @Test
    fun `a near-neutral grade is treated as identity to skip processing`() {
        // Avoids paying for a full re-encode when the grade is imperceptible.
        val negligible = FomoLook.PULSE.scaled(0.0001f)
        assertTrue(negligible.isIdentity)

        val perceptible = FomoLook.PULSE.scaled(0.5f)
        assertFalse(perceptible.isIdentity)
    }
}
