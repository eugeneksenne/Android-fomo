package com.example.feature.map.util

import com.example.core.data.CircleFriend
import com.example.core.data.ExploreVenue

/**
 * Generates the Javascript calls used to populate the Leaflet map inside
 * `VenueMapCanvas` with venue pins, friend pins and (for dynamically added
 * places) a single "just added" marker.
 *
 * Extracted verbatim from the pre-refactor `MapScreen.kt` inline
 * `venuesScriptBuilder` / `friendsScriptBuilder` / Add Place `jsCall`
 * construction - the badge flags (flash drop, live, event, sponsored,
 * trending, hot, closing soon) are still the same curated id lists as
 * before. A future pass can move that curation into real venue/repo fields;
 * for now this keeps behaviour pixel-for-pixel identical while giving it a
 * single, testable home.
 */
object MarkerGenerator {

    /**
     * Escapes a Kotlin string for safe interpolation inside a single-quoted
     * Javascript string literal.
     *
     * Every field spliced into the generated `addVenueMarker(...)` /
     * `addFriendMarker(...)` calls must go through this - venue name,
     * category, subcategory and image URL are all reachable from user input
     * (the "Add to FOMO" dialog lets a user type an arbitrary venue name and
     * subcategory), so an unescaped single quote, backslash, or newline in
     * any of those fields would break out of the JS string literal and let
     * arbitrary script run inside the WebView (which also exposes the
     * `AndroidBridge` Javascript interface). Backslashes are escaped first
     * so a trailing backslash can't neutralise the following quote escape,
     * and newlines are escaped since the generated calls are not always
     * wrapped across multiple JS statements safely otherwise.
     */
    private fun jsString(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")

    /**
     * Builds one `addVenueMarker(...)` Javascript call per venue, to be
     * concatenated into the Leaflet page's initial `<script>` block.
     */
    fun buildVenueMarkersScript(venues: List<ExploreVenue>): String {
        val builder = StringBuilder()
        venues.forEach { venue ->
            builder.append(buildAddVenueMarkerCall(venue))
        }
        return builder.toString()
    }

    /**
     * Builds one `addFriendMarker(...)` Javascript call per friend, to be
     * concatenated into the Leaflet page's initial `<script>` block.
     */
    fun buildFriendMarkersScript(friends: List<CircleFriend>): String {
        val builder = StringBuilder()
        friends.forEach { friend ->
            builder.append(
                """
            addFriendMarker('${jsString(friend.id)}', '${jsString(friend.name)}', ${friend.latitude}, ${friend.longitude}, '${jsString(friend.avatarUrl)}', '${jsString(friend.status)}', '${jsString(friend.currentActivity)}', ${friend.isCloseFriend});
        """.trimIndent()
            )
        }
        return builder.toString()
    }

    /**
     * A single `addVenueMarker(...)` call for [venue], including the same
     * Flash Drop / Live / Event / Sponsored / Trending / Hot / Closing Soon
     * badge-flag curation used at initial page load.
     */
    private fun buildAddVenueMarkerCall(venue: ExploreVenue): String {
        val coords = getVenueCoordinates(venue.id)
        val score = VenueRanking.vibeScore(venue)
        val hasFlashDrop = venue.id == "fomo_club" || venue.id == "d48_midrand"
        val isLive = venue.id == "fomo_club" || venue.id == "d48_midrand"
        val hasEvent = VenueFilter.hasEvent(venue)
        val friendsCount = if (venue.id == "fomo_club") 4 else if (venue.id == "konka_soweto") 1 else 0
        val isSponsored = venue.id == "fomo_club" || venue.id == "four_seasons_westcliff"
        val isTrending = venue.id == "fomo_club" || venue.id == "d48_midrand" || venue.id == "konka_soweto"
        val isHot = venue.id == "fomo_club" || venue.id == "konka_soweto"
        val isClosingSoon = venue.id == "taboo_sandton"

        return """
            addVenueMarker(
                '${jsString(venue.id)}', 
                '${jsString(venue.name)}', 
                ${coords.latitude}, 
                ${coords.longitude}, 
                '${jsString(venue.category)}', 
                '${jsString(venue.subcategory)}', 
                ${venue.rating}, 
                $score, 
                '${jsString(venue.imageUrl)}',
                $hasFlashDrop,
                $isLive,
                $hasEvent,
                $friendsCount,
                $isSponsored,
                $isTrending,
                $isHot,
                $isClosingSoon
            );
        """.trimIndent()
    }

    /**
     * Javascript to append a single newly-added venue marker (from the Add
     * Place dialog) to an already-loaded map, then fly the camera to it.
     *
     * Mirrors the pre-refactor inline `jsCall` exactly: a fixed vibe score of
     * 90, every badge flag `false` except "closing soon" (`true`), which was
     * used purely to give freshly-added pins a distinct highlight ring.
     *
     * Every user-suppliable field (`id` is a generated timestamp, but
     * `name`, `category`, `subcategory` and `imageUrl` can all trace back to
     * free-text entered in the Add Place dialog) is escaped via [jsString].
     */
    fun buildAddCustomVenueScript(venue: ExploreVenue): String {
        val coords = getVenueCoordinates(venue.id)
        return """
            addVenueMarker(
                '${jsString(venue.id)}', 
                '${jsString(venue.name)}', 
                ${coords.latitude}, 
                ${coords.longitude}, 
                '${jsString(venue.category)}', 
                '${jsString(venue.subcategory)}', 
                ${venue.rating}, 
                90, 
                '${jsString(venue.imageUrl)}',
                false,
                false,
                false,
                0,
                false,
                false,
                true,
                false
            );
            centerOn(${coords.latitude}, ${coords.longitude}, 15);
        """.trimIndent()
    }
}
