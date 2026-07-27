package com.example.feature.discover

/**
 * Lightweight analytics facade for Discover.
 *
 * The facade intentionally avoids depending directly on a vendor SDK from UI code.
 * Production builds can bridge these events to Firebase Analytics, Segment or a
 * backend ingestion endpoint from this single object.
 */
object DiscoverAnalytics {
    private const val TAG = "DiscoverAnalytics"

    fun sectionViewed(section: String) {
        track("discover_section_viewed", mapOf("section" to section))
    }

    fun seeAllClicked(section: String) {
        track("discover_see_all_clicked", mapOf("section" to section))
    }

    fun cardOpened(section: String, cardId: String, cardType: String = "card") {
        track(
            "discover_card_opened",
            mapOf(
                "section" to section,
                "card_id" to cardId,
                "card_type" to cardType
            )
        )
    }

    fun overlayOpened(overlay: String, source: String? = null) {
        track("discover_overlay_opened", mapOf("overlay" to overlay, "source" to source.orEmpty()))
    }

    fun overlayDismissed(overlay: String) {
        track("discover_overlay_dismissed", mapOf("overlay" to overlay))
    }

    fun actionClicked(action: String, targetId: String? = null) {
        track("discover_action_clicked", mapOf("action" to action, "target_id" to targetId.orEmpty()))
    }

    private fun track(eventName: String, parameters: Map<String, String>) {
        android.util.Log.d(TAG, "$eventName $parameters")
    }
}
