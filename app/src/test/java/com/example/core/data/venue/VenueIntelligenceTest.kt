package com.example.core.data.venue

import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for the Venue Intelligence confidence model.
 *
 * The previous implementation displayed a hardcoded "Confidence: 99%" for every
 * capture regardless of location, so there was nothing to test. These cover the
 * scoring rules the spec's pipeline depends on.
 */
@RunWith(RobolectricTestRunner::class)
class VenueIntelligenceTest {

    @Test
    fun `standing at the venue with a good fix is high confidence`() {
        val score = VenueIntelligence.confidenceFor(distanceMetres = 10.0, accuracyMetres = 8f)
        assertEquals(1.0f, score, 0.001f)
        assertTrue(score >= VenueIntelligence.LOW_CONFIDENCE_THRESHOLD)
    }

    @Test
    fun `confidence decreases as distance increases`() {
        val near = VenueIntelligence.confidenceFor(50.0, 10f)
        val mid = VenueIntelligence.confidenceFor(200.0, 10f)
        val far = VenueIntelligence.confidenceFor(380.0, 10f)
        assertTrue("near should beat mid", near > mid)
        assertTrue("mid should beat far", mid > far)
    }

    @Test
    fun `a poor GPS fix damps confidence even when close`() {
        val precise = VenueIntelligence.confidenceFor(10.0, 5f)
        val vague = VenueIntelligence.confidenceFor(10.0, 200f)
        assertTrue("an imprecise fix must not claim certainty", vague < precise)
        assertTrue(vague < VenueIntelligence.LOW_CONFIDENCE_THRESHOLD)
    }

    @Test
    fun `beyond the match radius confidence is zero`() {
        assertEquals(0f, VenueIntelligence.confidenceFor(400.0, 5f), 0.001f)
        assertEquals(0f, VenueIntelligence.confidenceFor(5000.0, 5f), 0.001f)
    }

    @Test
    fun `confidence is always a valid probability`() {
        val samples = listOf(0.0, 25.0, 60.0, 150.0, 399.0, 10_000.0)
        val accuracies = listOf(0f, 5f, 50f, 500f)
        for (d in samples) {
            for (a in accuracies) {
                val score = VenueIntelligence.confidenceFor(d, a)
                assertTrue("score $score out of range for d=$d a=$a", score in 0f..1f)
            }
        }
    }

    @Test
    fun `location on top of FOMO Club identifies it`() {
        val location = Location("test").apply {
            latitude = -26.1452
            longitude = 28.0472
            accuracy = 10f
        }
        val state = VenueIntelligence.matchAgainstPack(location)
        assertTrue(state is VenueIntelligence.VenueState.Identified)
        assertEquals(
            "fomo_club",
            (state as VenueIntelligence.VenueState.Identified).match.venueId
        )
    }

    @Test
    fun `location far from every venue reports none nearby`() {
        // Cape Town - ~1300 km from the Johannesburg venue pack.
        val location = Location("test").apply {
            latitude = -33.9249
            longitude = 18.4241
            accuracy = 10f
        }
        assertTrue(
            VenueIntelligence.matchAgainstPack(location)
                is VenueIntelligence.VenueState.NoVenueNearby
        )
    }

    @Test
    fun `an imprecise fix near a venue asks the user to confirm`() {
        val location = Location("test").apply {
            latitude = -26.1452
            longitude = 28.0472
            accuracy = 300f // very poor fix
        }
        val state = VenueIntelligence.matchAgainstPack(location)
        assertTrue(
            "a vague fix must not silently auto-attach a venue",
            state is VenueIntelligence.VenueState.LowConfidence
        )
    }
}
