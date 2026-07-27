package com.example.feature.map.util

import com.example.core.data.ExploreVenue

/**
 * Category filtering for the Map screen's venue list.
 *
 * Extracted from `MapScreen.kt` unchanged: the same category-name cleanup
 * (strip emoji/punctuation, lowercase) and the same "Wellness" -> "Recover"
 * and "Events" -> curated-id-list special cases as the original inline
 * `filteredVenues` derivation.
 */
object VenueFilter {

    /** Category chip label used to mean "no filter applied". */
    const val ALL_CATEGORY = "All"

    /**
     * Normalises a category chip label (which may contain an emoji prefix,
     * e.g. "🌙 Nightlife") down to a plain lowercase category key
     * (e.g. "nightlife") for comparison against [ExploreVenue.category].
     */
    fun normalizeCategory(categoryLabel: String): String =
        categoryLabel.replace(Regex("[^\\w\\s]"), "").trim().lowercase()

    /**
     * Filters [venues] down to the ones matching [categoryLabel].
     *
     * - "All" (or its normalised form) returns every venue.
     * - "Wellness" matches both "Recover" and "Wellness" category venues.
     * - "Events" matches a curated set of event-hosting venue IDs plus any
     *   dynamically added event venue (`evt_` prefixed id).
     * - Any other category matches venues whose category equals the
     *   normalised label.
     */
    fun filterByCategory(venues: List<ExploreVenue>, categoryLabel: String): List<ExploreVenue> {
        val clean = normalizeCategory(categoryLabel)
        return when {
            clean == "all" -> venues
            clean == "wellness" -> venues.filter {
                it.category.lowercase() == "recover" || it.category.lowercase() == "wellness"
            }
            clean == "events" -> venues.filter {
                it.id == "fomo_club" || it.id == "d48_midrand" || it.id == "konka_soweto" || it.id.startsWith("evt_")
            }
            else -> venues.filter { it.category.lowercase() == clean }
        }
    }

    /**
     * Whether [venue] would be considered an "event" venue for the purposes
     * of category filtering and marker generation (used by
     * [com.example.feature.map.util.MarkerGenerator] to flag the 🎫 event
     * badge). Mirrors the curated id list embedded in the pre-refactor
     * Javascript marker generator.
     */
    fun hasEvent(venue: ExploreVenue): Boolean =
        venue.id == "fomo_club" || venue.id == "d48_midrand" || venue.id == "konka_soweto" || venue.id.startsWith("evt_")
}
