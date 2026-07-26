package com.example.core.data.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tests for the replay retention tiers.
 *
 * The spec defines 7 / 30 / 90 / 180 day tiers plus pinned replays capped at 10
 * per creator. None of it was implemented — replays had no expiry concept, so
 * storage would grow without bound.
 */
class ReplayRetentionTest {

    private val now = 1_700_000_000_000L
    private fun daysMs(d: Long) = TimeUnit.DAYS.toMillis(d)

    // ---- tier resolution ---------------------------------------------------

    @Test
    fun `the spec's four durations are correct`() {
        assertEquals(7, ReplayRetention.Tier.TEMPORARY.days)
        assertEquals(30, ReplayRetention.Tier.STANDARD.days)
        assertEquals(90, ReplayRetention.Tier.HIGH_MOMENTUM.days)
        assertEquals(180, ReplayRetention.Tier.VERIFIED.days)
    }

    @Test
    fun `a short quiet Live is temporary`() {
        assertEquals(
            ReplayRetention.Tier.TEMPORARY,
            ReplayRetention.tierFor(
                isPinned = false, isVerifiedAuthor = false,
                peakVelocity = 0.5f, durationMinutes = 2,
            )
        )
    }

    @Test
    fun `an ordinary Live is standard`() {
        assertEquals(
            ReplayRetention.Tier.STANDARD,
            ReplayRetention.tierFor(
                isPinned = false, isVerifiedAuthor = false,
                peakVelocity = 1f, durationMinutes = 45,
            )
        )
    }

    @Test
    fun `high momentum extends retention to ninety days`() {
        assertEquals(
            ReplayRetention.Tier.HIGH_MOMENTUM,
            ReplayRetention.tierFor(
                isPinned = false, isVerifiedAuthor = false,
                peakVelocity = 15f, durationMinutes = 45,
            )
        )
    }

    @Test
    fun `verification beats momentum`() {
        // A verified venue's replay is kept the full window even if it was quiet.
        assertEquals(
            ReplayRetention.Tier.VERIFIED,
            ReplayRetention.tierFor(
                isPinned = false, isVerifiedAuthor = true,
                peakVelocity = 0.1f, durationMinutes = 1,
            )
        )
    }

    @Test
    fun `pinning beats everything`() {
        assertEquals(
            ReplayRetention.Tier.PINNED,
            ReplayRetention.tierFor(
                isPinned = true, isVerifiedAuthor = false,
                peakVelocity = 0f, durationMinutes = 1,
            )
        )
    }

    // ---- expiry ------------------------------------------------------------

    @Test
    fun `a standard replay expires after thirty days`() {
        val tier = ReplayRetention.Tier.STANDARD
        assertFalse(ReplayRetention.isExpired(tier, now, now + daysMs(29)))
        assertTrue(ReplayRetention.isExpired(tier, now, now + daysMs(30)))
    }

    @Test
    fun `a pinned replay never expires`() {
        val tier = ReplayRetention.Tier.PINNED
        assertNull(ReplayRetention.expiresAtMs(tier, now))
        assertFalse(ReplayRetention.isExpired(tier, now, now + daysMs(3_650)))
        assertNull(ReplayRetention.daysRemaining(tier, now))
    }

    @Test
    fun `days remaining counts down and floors at zero`() {
        val tier = ReplayRetention.Tier.TEMPORARY
        assertEquals(7, ReplayRetention.daysRemaining(tier, now, now))
        assertEquals(2, ReplayRetention.daysRemaining(tier, now, now + daysMs(5)))
        assertEquals(0, ReplayRetention.daysRemaining(tier, now, now + daysMs(99)))
    }

    // ---- pinning cap -------------------------------------------------------

    @Test
    fun `a creator may pin up to ten replays`() {
        assertEquals(10, ReplayRetention.MAX_PINNED_PER_CREATOR)
        assertTrue(ReplayRetention.canPin(0))
        assertTrue(ReplayRetention.canPin(9))
        assertFalse(ReplayRetention.canPin(10))
        assertFalse(ReplayRetention.canPin(11))
    }

    // ---- copy --------------------------------------------------------------

    @Test
    fun `retention is described in plain language`() {
        assertTrue(
            ReplayRetention.describe(ReplayRetention.Tier.PINNED, now)
                .contains("Pinned", ignoreCase = true)
        )
        assertEquals(
            "Expires today",
            ReplayRetention.describe(ReplayRetention.Tier.TEMPORARY, now, now + daysMs(7))
        )
        assertEquals(
            "Expires tomorrow",
            ReplayRetention.describe(ReplayRetention.Tier.TEMPORARY, now, now + daysMs(6))
        )
    }
}
