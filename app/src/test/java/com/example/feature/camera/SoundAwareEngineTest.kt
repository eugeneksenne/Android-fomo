package com.example.feature.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tests for the Sound Aware DSP.
 *
 * These cover the pure analysis functions. The previous implementation had
 * nothing to test — BPM was `(120..128).random()` and no audio was ever read.
 */
class SoundAwareEngineTest {

    // ---- BPM estimation ----------------------------------------------------

    @Test
    fun `needs several onsets before reporting a tempo`() {
        assertNull(SoundAwareEngine.estimateBpm(emptyList()))
        assertNull(SoundAwareEngine.estimateBpm(listOf(0L, 500L)))
        assertNull(SoundAwareEngine.estimateBpm(listOf(0L, 500L, 1000L)))
    }

    @Test
    fun `evenly spaced onsets at 120 BPM are detected`() {
        // 120 BPM = one beat every 500 ms.
        val onsets = (0..8).map { it * 500L }
        assertEquals(120, SoundAwareEngine.estimateBpm(onsets))
    }

    @Test
    fun `detects a typical amapiano tempo`() {
        // ~112 BPM -> 535 ms per beat.
        val onsets = (0..8).map { (it * 535L) }
        val bpm = SoundAwareEngine.estimateBpm(onsets)
        assertNotNull(bpm)
        assertTrue("expected ~112, got $bpm", bpm!! in 110..114)
    }

    @Test
    fun `a single missed beat does not skew the estimate`() {
        // 500 ms spacing, but one beat dropped (a 1000 ms gap).
        val onsets = listOf(0L, 500L, 1000L, 2000L, 2500L, 3000L, 3500L)
        val bpm = SoundAwareEngine.estimateBpm(onsets)
        assertEquals("median should reject the outlier", 120, bpm)
    }

    @Test
    fun `very fast detection is folded into a musical range`() {
        // 100 ms spacing = 600 BPM, which is really 150 BPM double-timed.
        val onsets = (0..8).map { it * 100L }
        val bpm = SoundAwareEngine.estimateBpm(onsets)!!
        assertTrue("expected a musical range, got $bpm", bpm in 70..180)
    }

    @Test
    fun `very slow detection is folded into a musical range`() {
        // 2 s spacing = 30 BPM, really 120 BPM half-timed.
        val onsets = (0..8).map { it * 2000L }
        val bpm = SoundAwareEngine.estimateBpm(onsets)!!
        assertTrue("expected a musical range, got $bpm", bpm in 70..180)
    }

    // ---- Level analysis ----------------------------------------------------

    @Test
    fun `silence reads as zero level`() {
        val silence = ShortArray(1024)
        assertEquals(0f, SoundAwareEngine.rms(silence, silence.size), 0.0001f)
    }

    @Test
    fun `a loud tone reads higher than a quiet one`() {
        val loud = tone(amplitude = 20000)
        val quiet = tone(amplitude = 2000)
        assertTrue(
            SoundAwareEngine.rms(loud, loud.size) > SoundAwareEngine.rms(quiet, quiet.size)
        )
    }

    @Test
    fun `rms stays within the normalised range`() {
        val full = ShortArray(512) { Short.MAX_VALUE }
        val value = SoundAwareEngine.rms(full, full.size)
        assertTrue("got $value", value in 0f..1f)
    }

    @Test
    fun `low band responds more to bass than to treble`() {
        val bass = tone(frequency = 60.0, amplitude = 16000)
        val treble = tone(frequency = 8000.0, amplitude = 16000)
        val bassEnergy = SoundAwareEngine.lowBandEnergy(bass, bass.size)
        val trebleEnergy = SoundAwareEngine.lowBandEnergy(treble, treble.size)
        assertTrue(
            "low-pass should favour bass ($bassEnergy) over treble ($trebleEnergy)",
            bassEnergy > trebleEnergy
        )
    }

    @Test
    fun `empty buffers are handled safely`() {
        assertEquals(0f, SoundAwareEngine.rms(ShortArray(0), 0), 0.0001f)
        assertEquals(0f, SoundAwareEngine.lowBandEnergy(ShortArray(0), 0), 0.0001f)
        assertEquals(0f, SoundAwareEngine.rms(ShortArray(10), 0), 0.0001f)
    }

    // ---- Energy classification --------------------------------------------

    @Test
    fun `energy escalates with loudness`() {
        assertEquals(SoundAwareEngine.Energy.QUIET, SoundAwareEngine.classifyEnergy(0.005f))
        assertEquals(SoundAwareEngine.Energy.BUILDING, SoundAwareEngine.classifyEnergy(0.05f))
        assertEquals(SoundAwareEngine.Energy.ACTIVE, SoundAwareEngine.classifyEnergy(0.15f))
        assertEquals(SoundAwareEngine.Energy.PEAK, SoundAwareEngine.classifyEnergy(0.5f))
    }

    private fun tone(
        frequency: Double = 440.0,
        amplitude: Int = 16000,
        sampleRate: Int = 44_100,
        samples: Int = 2048,
    ): ShortArray = ShortArray(samples) { i ->
        (amplitude * sin(2.0 * PI * frequency * i / sampleRate)).toInt().toShort()
    }
}
