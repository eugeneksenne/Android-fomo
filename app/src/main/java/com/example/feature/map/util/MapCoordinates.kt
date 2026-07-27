package com.example.feature.map.util

import com.example.core.data.ExploreVenue

/**
 * Lat/lon pair used to place a venue pin on the Leaflet map.
 */
data class MapCoordinates(val latitude: Double, val longitude: Double)

/**
 * Static (mock) coordinate lookup for seeded venues, plus a small pseudo-random
 * fallback for venues without a curated location (e.g. custom/temporary places
 * added at runtime via the Add Place flow, or Overpass POIs).
 *
 * This is the same lookup table `MapScreen.kt` used before the module split -
 * behaviour is unchanged, only the location moved.
 */
fun getVenueCoordinates(venueId: String): MapCoordinates {
    return when (venueId) {
        "fomo_club" -> MapCoordinates(-26.1452, 28.0472)
        "d48_midrand" -> MapCoordinates(-25.9981, 28.1263)
        "konka_soweto" -> MapCoordinates(-26.2561, 27.8542)
        "taboo_sandton" -> MapCoordinates(-26.1044, 28.0581)
        "marble_rosebank" -> MapCoordinates(-26.1461, 28.0432)
        "proud_mary" -> MapCoordinates(-26.1445, 28.0454)
        "legend_barber" -> MapCoordinates(-26.1456, 28.0421)
        "sorbet_salon" -> MapCoordinates(-26.1072, 28.0524)
        "sanctuary_spa" -> MapCoordinates(-26.1085, 28.0551)
        "four_seasons_westcliff" -> MapCoordinates(-26.1643, 28.0285)
        "shell_select_rosebank" -> MapCoordinates(-26.1481, 28.0376)
        else -> MapCoordinates(-26.1452 + (Math.random() - 0.5) * 0.02, 28.0472 + (Math.random() - 0.5) * 0.02)
    }
}

/**
 * Fallback "hero" venue used whenever nearest-venue ranking has nothing to
 * recommend (e.g. a category filter matches zero venues). Mirrors the
 * pre-refactor `defaultFomoClubVenue` constant exactly.
 */
val defaultFomoClubVenue = ExploreVenue(
    id = "fomo_club",
    name = "FOMO Club",
    category = "Nightlife",
    subcategory = "VIP Lounge & Nightclub",
    imageUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=600&auto=format&fit=crop",
    isVerified = true,
    rating = 4.9f,
    reviewCount = 2431,
    address = "Rosebank, Johannesburg",
    area = "Rosebank",
    distanceText = "0.2 km away",
    attributes = listOf("🔴 LIVE Now", "Amapiano", "3D Light Mapping", "Rooftop Glasshouse"),
    openDays = "Open Now",
    startHour = 21,
    endHour = 4,
    is24Hours = false,
    websiteUrl = "https://fomoapp.live",
    hasClubLobby = true
)
