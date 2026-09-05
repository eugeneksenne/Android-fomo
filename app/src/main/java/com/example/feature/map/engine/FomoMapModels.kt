package com.example.feature.map.engine

import com.example.core.data.ExploreVenue

enum class VenueCategory {
    NIGHTLIFE,
    PREP,
    FOOD_AND_DRINK,
    TRAVEL,
    WELLNESS
}

enum class VenueSource {
    OSM,
    FOMO,
    ENRICHED
}

data class FomoVenue(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: VenueCategory,
    val subcategory: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val openingHours: String? = null,
    val isOpenNow: Boolean? = true,
    val source: VenueSource = VenueSource.FOMO,
    val rating: Float = 4.5f,
    val distanceText: String = "0.5 km away",
    val imageUrl: String = "",
    val attributes: List<String> = emptyList(),
    val hasClubLobby: Boolean = false
)

/**
 * Hard FOMO Category Filter boundary.
 * Strictly restricts the visible venue layer to:
 * - NIGHTLIFE
 * - PREP
 * - FOOD & DRINK
 * - TRAVEL
 * - WELLNESS
 *
 * Generic OSM POIs (hospitals, schools, police, banks, ATMs, generic businesses)
 * are excluded from entering the FOMO venue layer.
 */
object FomoVenueFilter {
    private val allowedCategories = setOf(
        VenueCategory.NIGHTLIFE,
        VenueCategory.PREP,
        VenueCategory.FOOD_AND_DRINK,
        VenueCategory.TRAVEL,
        VenueCategory.WELLNESS
    )

    fun isAllowed(venue: FomoVenue): Boolean {
        return venue.category in allowedCategories
    }

    fun filter(venues: List<FomoVenue>): List<FomoVenue> {
        return venues.filter(::isAllowed)
    }
}

enum class MapCategoryFilter {
    ALL,
    NIGHTLIFE,
    FOOD_AND_DRINK,
    PREP,
    WELLNESS,
    TRAVEL
}

fun filterVenues(
    venues: List<FomoVenue>,
    filter: MapCategoryFilter
): List<FomoVenue> {
    val allowed = FomoVenueFilter.filter(venues)
    return when (filter) {
        MapCategoryFilter.ALL -> allowed
        MapCategoryFilter.NIGHTLIFE -> allowed.filter { it.category == VenueCategory.NIGHTLIFE }
        MapCategoryFilter.FOOD_AND_DRINK -> allowed.filter { it.category == VenueCategory.FOOD_AND_DRINK }
        MapCategoryFilter.PREP -> allowed.filter { it.category == VenueCategory.PREP }
        MapCategoryFilter.WELLNESS -> allowed.filter { it.category == VenueCategory.WELLNESS }
        MapCategoryFilter.TRAVEL -> allowed.filter { it.category == VenueCategory.TRAVEL }
    }
}

enum class MapDataMode {
    ONLINE,
    OFFLINE,
    HYBRID
}

data class FomoCountry(
    val id: String,
    val name: String,
    val isoCode: String,
    val regions: List<FomoRegion>
)

data class FomoRegion(
    val id: String,
    val countryId: String,
    val name: String,
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
    val downloadSizeBytes: Long,
    val version: String = "2026.1"
)

sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState
    data object Installed : DownloadState
    data class Failed(val message: String) : DownloadState
}

data class NightguardZone(
    val id: String,
    val name: String,
    val safetyScore: Int, // 0 to 100
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val status: String, // "PATROLLED", "VERIFIED_SAFE", "HIGH_ACTIVITY"
    val patrolUnitsCount: Int = 3
)

// Extension converter from ExploreVenue to FomoVenue
fun ExploreVenue.toFomoVenue(lat: Double, lng: Double): FomoVenue {
    val mappedCategory = when (category.lowercase()) {
        "nightlife" -> VenueCategory.NIGHTLIFE
        "food", "food & drink", "restaurant", "bar" -> VenueCategory.FOOD_AND_DRINK
        "prep", "grooming", "salon", "fashion" -> VenueCategory.PREP
        "wellness", "recover", "spa", "gym" -> VenueCategory.WELLNESS
        "travel", "hotel", "airport" -> VenueCategory.TRAVEL
        else -> VenueCategory.NIGHTLIFE
    }
    return FomoVenue(
        id = id,
        name = name,
        latitude = lat,
        longitude = lng,
        category = mappedCategory,
        subcategory = subcategory,
        address = address,
        phone = "+27 11 982 4000",
        website = websiteUrl,
        openingHours = "$openDays $startHour:00 - $endHour:00",
        isOpenNow = true,
        source = VenueSource.FOMO,
        rating = rating,
        distanceText = distanceText,
        imageUrl = imageUrl,
        attributes = attributes,
        hasClubLobby = hasClubLobby
    )
}
