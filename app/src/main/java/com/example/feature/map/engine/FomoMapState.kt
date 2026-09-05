package com.example.feature.map.engine

data class FomoMapState(
    val latitude: Double = -26.146, // Default Johannesburg (Rosebank)
    val longitude: Double = 28.043,
    val mode: MapDataMode = MapDataMode.HYBRID,
    val categoryFilter: MapCategoryFilter = MapCategoryFilter.ALL,
    val venues: List<FomoVenue> = emptyList(),
    val selectedVenue: FomoVenue? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<FomoVenue> = emptyList(),
    val isRegionDownloadOpen: Boolean = false,
    val activeRegion: FomoRegion? = null,
    val isNightguardActive: Boolean = true,
    val nightguardZones: List<NightguardZone> = emptyList(),
    val activeRoute: Route? = null,
    val isHeatmapEnabled: Boolean = false,
    val currentCityName: String = "Johannesburg (Gauteng)"
)
