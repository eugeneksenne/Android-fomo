package com.example.feature.map.engine

class MapDataResolver(
    private val offlineMapEngine: OfflineMapEngine
) {
    suspend fun resolve(
        latitude: Double,
        longitude: Double,
        isNetworkOnline: Boolean = true
    ): MapDataMode {
        val region = offlineMapEngine.getRegionForCoordinate(latitude, longitude)
        val isInstalled = region != null && offlineMapEngine.isRegionInstalled(region.id)

        return when {
            isInstalled && isNetworkOnline -> MapDataMode.HYBRID
            isInstalled && !isNetworkOnline -> MapDataMode.OFFLINE
            isNetworkOnline -> MapDataMode.ONLINE
            else -> MapDataMode.OFFLINE
        }
    }
}
