package com.example.feature.feed

import com.example.core.data.feed.InvitationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the Moment Invitation lifecycle defined in the Feed spec.
 *
 * The previous implementation stored a duration as `initialHours`/
 * `initialMinutes` plus a mutable status string that nothing updated, rendered
 * a hardcoded "Available for 02:44:18" that never ticked, and changed state
 * only through a debug button that shipped in the production UI. State is now
 * derived from an absolute expiry timestamp, which is what these cover.
 */
class MomentInvitationTest {

    private val now = 1_700_000_000_000L

    private fun invitation(
        expiresAtMs: Long,
        endedEarlyAtMs: Long? = null,
        isVenueClosed: Boolean = false,
    ) = InvitationData(
        venueName = "Cocoon Nightclub",
        isVenueVerified = true,
        creatorName = "Alfred",
        expiresAtMs = expiresAtMs,
        endedEarlyAtMs = endedEarlyAtMs,
        isVenueClosed = isVenueClosed,
    )

    // ---- state 1: active ---------------------------------------------------

    @Test
    fun `an unexpired invitation is active`() {
        val inv = invitation(expiresAtMs = now + 10 * 60_000L)
        assertEquals(InvitationData.State.ACTIVE, inv.stateAt(now))
    }

    @Test
    fun `remaining time counts down`() {
        val inv = invitation(expiresAtMs = now + 5 * 60_000L)
        assertEquals(5 * 60_000L, inv.remainingMs(now))
        assertEquals(2 * 60_000L, inv.remainingMs(now + 3 * 60_000L))
    }

    // ---- state 2: ended ----------------------------------------------------

    @Test
    fun `an invitation expires on its own`() {
        val inv = invitation(expiresAtMs = now + 60_000L)
        assertEquals(InvitationData.State.ACTIVE, inv.stateAt(now))
        assertEquals(InvitationData.State.ENDED, inv.stateAt(now + 60_001L))
    }

    @Test
    fun `expiry is inclusive at the boundary`() {
        val inv = invitation(expiresAtMs = now)
        assertEquals(InvitationData.State.ENDED, inv.stateAt(now))
    }

    @Test
    fun `a creator can end sharing before expiry`() {
        // Spec: "The creator leaves, manually ends sharing, or it expires."
        val inv = invitation(
            expiresAtMs = now + 60 * 60_000L,
            endedEarlyAtMs = now - 1_000L,
        )
        assertEquals(InvitationData.State.ENDED, inv.stateAt(now))
    }

    @Test
    fun `remaining time never goes negative`() {
        val inv = invitation(expiresAtMs = now - 60_000L)
        assertEquals(0L, inv.remainingMs(now))
    }

    // ---- state 3: venue closed --------------------------------------------

    @Test
    fun `a closed venue takes precedence over an active invitation`() {
        val inv = invitation(expiresAtMs = now + 60 * 60_000L, isVenueClosed = true)
        assertEquals(InvitationData.State.VENUE_CLOSED, inv.stateAt(now))
    }

    // ---- durations ---------------------------------------------------------

    @Test
    fun `the spec's four fixed durations are offered`() {
        assertEquals(
            listOf(15, 30, 60, 120),
            InvitationData.DURATION_OPTIONS_MINUTES,
        )
    }

    @Test
    fun `expiry is computed from the chosen duration`() {
        assertEquals(now + 30 * 60_000L, InvitationData.expiryFor(30, now))
        assertEquals(now + 120 * 60_000L, InvitationData.expiryFor(120, now))
    }

    @Test
    fun `until I leave is treated as open ended`() {
        // Spec offers "Until I Leave" alongside the fixed durations.
        val inv = invitation(expiresAtMs = now + InvitationData.OPEN_ENDED_THRESHOLD_MS * 2)
        assertTrue(inv.isOpenEnded)
        assertEquals(InvitationData.State.ACTIVE, inv.stateAt(now))
    }

    @Test
    fun `a two hour invitation is not open ended`() {
        val inv = invitation(expiresAtMs = InvitationData.expiryFor(120, now))
        assertFalse(inv.isOpenEnded)
    }

    // ---- formatting --------------------------------------------------------

    @Test
    fun `countdown formats as MM SS under an hour`() {
        assertEquals("04:45", InvitationData.formatRemaining(4 * 60_000L + 45_000L))
        assertEquals("00:09", InvitationData.formatRemaining(9_000L))
        assertEquals("00:00", InvitationData.formatRemaining(0L))
    }

    @Test
    fun `countdown includes hours when over an hour`() {
        val ms = 2 * 3_600_000L + 44 * 60_000L + 18_000L
        assertEquals("02:44:18", InvitationData.formatRemaining(ms))
    }

    // ---- viewer counts -----------------------------------------------------

    @Test
    fun `viewer counts abbreviate the way the spec displays them`() {
        assertEquals("2.4K", formatViewers(2_400))
        assertEquals("1K", formatViewers(1_000))
        assertEquals("999", formatViewers(999))
        assertEquals("12K", formatViewers(12_000))
        assertEquals("2.5M", formatViewers(2_500_000))
    }

    @Test
    fun `viewer count handles zero`() {
        assertEquals("0", formatViewers(0))
    }
}
