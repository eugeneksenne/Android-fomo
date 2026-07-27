package com.example.feature.discover

import org.junit.Test

/**
 * Smoke tests for the Discover analytics facade.
 *
 * The facade currently forwards events to Logcat; these tests guard the
 * public contract (method names, null tolerance, no-crash behaviour) so a
 * future Firebase/Segment bridge cannot silently break Discover callers.
 */
class DiscoverAnalyticsTest {

    @Test
    fun sectionLifecycleEventsDoNotThrow() {
        DiscoverAnalytics.sectionViewed("Flash Drops")
        DiscoverAnalytics.seeAllClicked("Flash Drops")
    }

    @Test
    fun cardEventsDoNotThrow() {
        DiscoverAnalytics.cardOpened("events", "evt_123", "event")
        DiscoverAnalytics.cardOpened("smart_places", "d48_midrand")
    }

    @Test
    fun overlayEventsTolerateNullSource() {
        DiscoverAnalytics.overlayOpened("venue_preview")
        DiscoverAnalytics.overlayOpened("flash_drops_hub", source = null)
        DiscoverAnalytics.overlayDismissed("venue_preview")
    }

    @Test
    fun actionEventsTolerateNullTarget() {
        DiscoverAnalytics.actionClicked("claim_entry")
        DiscoverAnalytics.actionClicked("share_plan", targetId = null)
    }
}
