package com.example.feature.map.engine

class FomoSearchEngine {

    /**
     * Searches FOMO venues while strictly enforcing [FomoVenueFilter].
     * Query strings matching forbidden non-lifestyle categories (hospital, police, bank, etc.)
     * are filtered out at the engine boundary.
     */
    fun search(
        query: String,
        venues: List<FomoVenue>
    ): List<FomoVenue> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        // Forbidden POI keywords check
        val forbiddenKeywords = setOf("hospital", "police", "bank", "atm", "school", "clinic", "court")
        if (forbiddenKeywords.any { trimmed.contains(it) }) {
            return emptyList()
        }

        val filteredVenues = FomoVenueFilter.filter(venues)
        return filteredVenues.filter { venue ->
            venue.name.lowercase().contains(trimmed) ||
            venue.category.name.lowercase().contains(trimmed) ||
            (venue.subcategory?.lowercase()?.contains(trimmed) == true) ||
            (venue.address?.lowercase()?.contains(trimmed) == true) ||
            venue.attributes.any { it.lowercase().contains(trimmed) }
        }
    }
}
