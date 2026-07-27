package com.example.feature.map.util

import com.example.core.data.ExploreVenue

/**
 * Ranking / recommendation helpers for the Map screen.
 *
 * `VenueRanking` is intentionally small today (rating-based nearest venue and
 * the FOMO "vibe score" percentage used across the map cards). It's the
 * natural home for future popularity/recommendation scoring so that logic
 * doesn't creep back into composables.
 */
object VenueRanking {

    /**
     * Picks the "nearest hotspot" to feature in [com.example.feature.map.cards.NearestVenueCard].
     *
     * Today this is a simple highest-rating pick within the already
     * category-filtered venue list (unchanged from the pre-refactor
     * behaviour); falls back to [defaultFomoClubVenue] when the filtered
     * list is empty.
     */
    fun nearestVenue(filteredVenues: List<ExploreVenue>): ExploreVenue {
        return filteredVenues.maxByOrNull { it.rating } ?: defaultFomoClubVenue
    }

    /**
     * FOMO "vibe score" percentage shown on venue cards and map badges.
     * Derived from the 0-5 star rating, scaled to a 0-100 percentage.
     */
    fun vibeScore(venue: ExploreVenue): Int = (venue.rating * 20).toInt()
}
