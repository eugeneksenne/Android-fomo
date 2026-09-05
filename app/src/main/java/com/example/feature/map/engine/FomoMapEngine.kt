package com.example.feature.map.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FomoMapEngine(
    val offlineMapEngine: OfflineMapEngine = DefaultOfflineMapEngine(DefaultRegionDownloadManager()),
    val mapDataResolver: MapDataResolver = MapDataResolver(offlineMapEngine),
    val searchEngine: FomoSearchEngine = FomoSearchEngine(),
    val routingEngine: RoutingEngine = RoutingEngine(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(FomoMapState())
    val state: StateFlow<FomoMapState> = _state.asStateFlow()

    init {
        setupNightguardZones()
    }

    private fun setupNightguardZones() {
        val zones = listOf(
            NightguardZone("nz1", "Rosebank Patrol Sector A", 98, -26.146, 28.043, 800.0, "PATROLLED", 4),
            NightguardZone("nz2", "Sandton Square Safe Haven", 96, -26.107, 28.056, 1200.0, "VERIFIED_SAFE", 6),
            NightguardZone("nz3", "Braamfontein Youth Hub", 92, -26.192, 28.034, 600.0, "HIGH_ACTIVITY", 3),
            NightguardZone("nz4", "Midrand Entertainment Zone", 95, -25.998, 28.126, 1500.0, "PATROLLED", 5)
        )
        _state.update { it.copy(nightguardZones = zones) }
    }

    fun load(latitude: Double, longitude: Double, initialVenues: List<FomoVenue>) {
        scope.launch {
            val mode = mapDataResolver.resolve(latitude, longitude, isNetworkOnline = true)
            val allowedVenues = FomoVenueFilter.filter(initialVenues)
            val region = offlineMapEngine.getRegionForCoordinate(latitude, longitude)

            _state.update {
                it.copy(
                    latitude = latitude,
                    longitude = longitude,
                    mode = mode,
                    venues = allowedVenues,
                    activeRegion = region
                )
            }
        }
    }

    fun setCategoryFilter(filter: MapCategoryFilter) {
        _state.update { it.copy(categoryFilter = filter) }
    }

    fun selectVenue(venue: FomoVenue?) {
        _state.update { current ->
            val route = if (venue != null) {
                routingEngine.calculateRoute(current.latitude, current.longitude, venue)
            } else null

            current.copy(
                selectedVenue = venue,
                activeRoute = route
            )
        }
    }

    fun search(query: String) {
        val results = searchEngine.search(query, _state.value.venues)
        _state.update {
            it.copy(
                searchQuery = query,
                isSearching = query.isNotBlank(),
                searchResults = results
            )
        }
    }

    fun clearSearch() {
        _state.update {
            it.copy(
                searchQuery = "",
                isSearching = false,
                searchResults = emptyList()
            )
        }
    }

    fun toggleNightguard() {
        _state.update { it.copy(isNightguardActive = !it.isNightguardActive) }
    }

    fun toggleHeatmap() {
        _state.update { it.copy(isHeatmapEnabled = !it.isHeatmapEnabled) }
    }

    fun openRegionDownloadManager() {
        _state.update { it.copy(isRegionDownloadOpen = true) }
    }

    fun closeRegionDownloadManager() {
        _state.update { it.copy(isRegionDownloadOpen = false) }
    }

    fun drawRouteTo(venue: FomoVenue) {
        val current = _state.value
        val route = routingEngine.calculateRoute(current.latitude, current.longitude, venue)
        _state.update { it.copy(activeRoute = route, selectedVenue = venue) }
    }

    fun clearRoute() {
        _state.update { it.copy(activeRoute = null) }
    }
}
