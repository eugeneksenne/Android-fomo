package com.example.feature.map.util

/**
 * Category-driven copy for the map's primary venue action button (the one
 * that opens either a Club Lobby, an Event Lobby, a reservation flow, a
 * livestream, or a plain website).
 *
 * Three call sites in the pre-refactor `MapScreen.kt`
 * (`NearestVenueCard`, `HorizontalVenueCard`, `VenueDetailsPanel`) each
 * duplicated this same `when` mapping inline. Centralising it here keeps
 * the copy consistent and gives it one place to test/update.
 */
object VenueActionLabels {

    /** Primary button title, e.g. "Club Lobby", "Event Lobby", "Reserve", "Watch", "Website". */
    fun primaryActionTitle(normalizedCategory: String): String = when (normalizedCategory) {
        "NIGHTLIFE" -> "Club Lobby"
        "EVENTS" -> "Event Lobby"
        "TRAVEL", "LUXURY" -> "Reserve"
        "LIVE" -> "Watch"
        else -> "Website"
    }

    /** Secondary/subtitle copy shown under the primary action title on the Nearest Venue card. */
    fun primaryActionSubtitle(normalizedCategory: String): String = when (normalizedCategory) {
        "NIGHTLIFE" -> "Photos, Events & More"
        "EVENTS" -> "Get Tickets & Lineup"
        "TRAVEL", "LUXURY" -> "Book Tables & Rooms"
        "LIVE" -> "Watch Stream Feed"
        else -> "View Menu & Booking"
    }

    /** Whether the primary action should navigate in-app (Club/Event Lobby) vs. open a website. */
    fun opensInAppLobby(normalizedCategory: String): Boolean =
        normalizedCategory == "NIGHTLIFE" || normalizedCategory == "EVENTS"

    /** Normalises an [com.example.core.data.ExploreVenue.category] value for label lookups. */
    fun normalize(category: String): String = category.uppercase().trim()
}
