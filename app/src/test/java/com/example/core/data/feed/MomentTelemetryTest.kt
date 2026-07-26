package com.example.core.data.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for watch telemetry.
 *
 * The Feed spec ranks on watch completion, but nothing in the app measured that
 * a Moment had even been seen. Ranking had no inputs and Creator Analytics
 * displayed fabricated values (views were `likesCount * 3 + 240`).
 */
@RunWith(RobolectricTestRunner::class)
class MomentTelemetryTest {

    private val t0 = 1_700_000_000_000L

    @Before
    fun setUp() {
        MomentTelemetry.reset()
    }

    @Test
    fun `a glance below the threshold does not count as a view`() {
        // Scrolling past must not inflate view counts.
        MomentTelemetry.onMomentVisible("m1", t0)
        MomentTelemetry.onMomentHidden("m1", t0 + 300)
        assertEquals(0, MomentTelemetry.statsFor("m1").views)
    }

    @Test
    fun `a sustained view is recorded`() {
        MomentTelemetry.onMomentVisible("m1", t0)
        MomentTelemetry.onMomentHidden("m1", t0 + 4_000)

        val stats = MomentTelemetry.statsFor("m1")
        assertEquals(1, stats.views)
        assertEquals(4_000L, stats.totalWatchMs)
    }

    @Test
    fun `watching most of a Moment counts as completed`() {
        MomentTelemetry.onMomentVisible("m1", t0)
        MomentTelemetry.onMomentHidden("m1", t0 + 8_000)

        val stats = MomentTelemetry.statsFor("m1")
        assertEquals(1, stats.completedViews)
        assertEquals(1f, stats.completionRate, 0.001f)
    }

    @Test
    fun `a partial view is not counted as completed`() {
        MomentTelemetry.onMomentVisible("m1", t0)
        MomentTelemetry.onMomentHidden("m1", t0 + 2_000)

        val stats = MomentTelemetry.statsFor("m1")
        assertEquals(1, stats.views)
        assertEquals(0, stats.completedViews)
        assertTrue(stats.averageWatchCompletion < 0.5f)
    }

    @Test
    fun `moving between Moments closes the previous session exactly once`() {
        // A fast scroll must not leave two sessions open and double-count.
        MomentTelemetry.onMomentVisible("m1", t0)
        MomentTelemetry.onMomentVisible("m2", t0 + 3_000)
        MomentTelemetry.onMomentHidden("m2", t0 + 9_000)

        assertEquals(1, MomentTelemetry.statsFor("m1").views)
        assertEquals(3_000L, MomentTelemetry.statsFor("m1").totalWatchMs)
        assertEquals(1, MomentTelemetry.statsFor("m2").views)
        assertEquals(6_000L, MomentTelemetry.statsFor("m2").totalWatchMs)
    }

    @Test
    fun `marking the same Moment visible twice does not restart the session`() {
        MomentTelemetry.onMomentVisible("m1", t0)
        MomentTelemetry.onMomentVisible("m1", t0 + 2_000)
        MomentTelemetry.onMomentHidden("m1", t0 + 5_000)

        assertEquals(1, MomentTelemetry.statsFor("m1").views)
        assertEquals(5_000L, MomentTelemetry.statsFor("m1").totalWatchMs)
    }

    @Test
    fun `repeat views accumulate`() {
        MomentTelemetry.onMomentVisible("m1", t0)
        MomentTelemetry.onMomentHidden("m1", t0 + 4_000)
        MomentTelemetry.onMomentVisible("m1", t0 + 10_000)
        MomentTelemetry.onMomentHidden("m1", t0 + 14_000)

        val stats = MomentTelemetry.statsFor("m1")
        assertEquals(2, stats.views)
        assertEquals(8_000L, stats.totalWatchMs)
        assertEquals(4_000L, stats.averageWatchMs)
    }

    @Test
    fun `flush closes an open session so time does not accrue off screen`() {
        MomentTelemetry.onMomentVisible("m1", t0)
        MomentTelemetry.flush(t0 + 5_000)
        assertEquals(1, MomentTelemetry.statsFor("m1").views)

        // A second flush must not double count.
        MomentTelemetry.flush(t0 + 20_000)
        assertEquals(1, MomentTelemetry.statsFor("m1").views)
    }

    @Test
    fun `hiding a Moment that never became visible is a no-op`() {
        MomentTelemetry.onMomentHidden("ghost", t0 + 5_000)
        assertEquals(0, MomentTelemetry.statsFor("ghost").views)
    }

    @Test
    fun `watch completion is bounded at one`() {
        MomentTelemetry.onMomentVisible("m1", t0)
        MomentTelemetry.onMomentHidden("m1", t0 + 600_000) // ten minutes
        assertEquals(1f, MomentTelemetry.statsFor("m1").averageWatchCompletion, 0.001f)
    }

    @Test
    fun `interaction counters increment independently`() {
        MomentTelemetry.recordVenueClick("m1")
        MomentTelemetry.recordVenueClick("m1")
        MomentTelemetry.recordRouteClick("m1")
        MomentTelemetry.recordClubLobbyOpen("m1")
        MomentTelemetry.recordShare("m1")
        MomentTelemetry.recordProfileVisit("m1")

        val stats = MomentTelemetry.statsFor("m1")
        assertEquals(2, stats.venueClicks)
        assertEquals(1, stats.routeClicks)
        assertEquals(1, stats.clubLobbyOpens)
        assertEquals(1, stats.shares)
        assertEquals(1, stats.profileVisits)
    }

    @Test
    fun `an unseen Moment reports zeroes rather than throwing`() {
        val stats = MomentTelemetry.statsFor("never-seen")
        assertEquals(0, stats.views)
        assertEquals(0f, stats.completionRate, 0.001f)
        assertEquals(0f, stats.averageWatchCompletion, 0.001f)
        assertEquals(0L, stats.averageWatchMs)
    }
}
