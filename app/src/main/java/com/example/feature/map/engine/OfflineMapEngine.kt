package com.example.feature.map.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface RegionDownloadManager {
    fun download(region: FomoRegion)
    fun pause(region: FomoRegion)
    fun resume(region: FomoRegion)
    fun cancel(region: FomoRegion)
    fun delete(region: FomoRegion)
    fun observe(region: FomoRegion): Flow<DownloadState>
}

class DefaultRegionDownloadManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : RegionDownloadManager {

    private val _states = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val states = _states.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()

    override fun download(region: FomoRegion) {
        if (_states.value[region.id] is DownloadState.Downloading || _states.value[region.id] is DownloadState.Installed) return

        val job = scope.launch {
            val totalBytes = region.downloadSizeBytes
            var downloaded = 0L
            val chunkSize = totalBytes / 20

            while (downloaded < totalBytes) {
                delay(200)
                downloaded += chunkSize
                if (downloaded > totalBytes) downloaded = totalBytes

                val progress = downloaded.toFloat() / totalBytes.toFloat()
                _states.update { current ->
                    current + (region.id to DownloadState.Downloading(progress, downloaded, totalBytes))
                }
            }

            _states.update { current ->
                current + (region.id to DownloadState.Installed)
            }
            downloadJobs.remove(region.id)
        }

        downloadJobs[region.id] = job
    }

    override fun pause(region: FomoRegion) {
        downloadJobs[region.id]?.cancel()
        downloadJobs.remove(region.id)
        val current = _states.value[region.id]
        if (current is DownloadState.Downloading) {
            // Keep current progress in downloading state or pause
        }
    }

    override fun resume(region: FomoRegion) {
        download(region)
    }

    override fun cancel(region: FomoRegion) {
        downloadJobs[region.id]?.cancel()
        downloadJobs.remove(region.id)
        _states.update { it - region.id }
    }

    override fun delete(region: FomoRegion) {
        downloadJobs[region.id]?.cancel()
        downloadJobs.remove(region.id)
        _states.update { it + (region.id to DownloadState.NotDownloaded) }
    }

    override fun observe(region: FomoRegion): Flow<DownloadState> {
        return MutableStateFlow(_states.value[region.id] ?: DownloadState.NotDownloaded)
    }

    fun markAsInstalled(regionId: String) {
        _states.update { it + (regionId to DownloadState.Installed) }
    }
}

interface OfflineMapEngine {
    suspend fun isRegionInstalled(regionId: String): Boolean
    suspend fun installedRegions(): List<FomoRegion>
    suspend fun install(region: FomoRegion)
    suspend fun remove(region: FomoRegion)
    suspend fun getRegionForCoordinate(latitude: Double, longitude: Double): FomoRegion?
    fun availableCountries(): List<FomoCountry>
}

class DefaultOfflineMapEngine(
    val downloadManager: DefaultRegionDownloadManager
) : OfflineMapEngine {

    val sampleCountries = listOf(
        FomoCountry(
            id = "ZA",
            name = "South Africa",
            isoCode = "ZA",
            regions = listOf(
                FomoRegion(
                    id = "ZA-GP",
                    countryId = "ZA",
                    name = "Gauteng (Johannesburg & Pretoria)",
                    minLatitude = -26.8,
                    maxLatitude = -25.4,
                    minLongitude = 27.5,
                    maxLongitude = 28.8,
                    downloadSizeBytes = 1200000000L // 1.2 GB
                ),
                FomoRegion(
                    id = "ZA-WC",
                    countryId = "ZA",
                    name = "Western Cape (Cape Town & Winelands)",
                    minLatitude = -34.8,
                    maxLatitude = -31.0,
                    minLongitude = 17.8,
                    maxLongitude = 24.0,
                    downloadSizeBytes = 1800000000L // 1.8 GB
                ),
                FomoRegion(
                    id = "ZA-MP",
                    countryId = "ZA",
                    name = "Mpumalanga (Mbombela & Kruger)",
                    minLatitude = -27.5,
                    maxLatitude = -24.0,
                    minLongitude = 28.5,
                    maxLongitude = 32.0,
                    downloadSizeBytes = 640000000L // 640 MB
                ),
                FomoRegion(
                    id = "ZA-KZN",
                    countryId = "ZA",
                    name = "KwaZulu-Natal (Durban & North Coast)",
                    minLatitude = -31.1,
                    maxLatitude = -26.8,
                    minLongitude = 28.8,
                    maxLongitude = 33.0,
                    downloadSizeBytes = 1400000000L // 1.4 GB
                ),
                FomoRegion(
                    id = "ZA-LP",
                    countryId = "ZA",
                    name = "Limpopo (Polokwane & Waterberg)",
                    minLatitude = -25.0,
                    maxLatitude = -22.1,
                    minLongitude = 26.5,
                    maxLongitude = 31.8,
                    downloadSizeBytes = 720000000L // 720 MB
                )
            )
        ),
        FomoCountry(
            id = "NG",
            name = "Nigeria",
            isoCode = "NG",
            regions = listOf(
                FomoRegion(
                    id = "NG-LA",
                    countryId = "NG",
                    name = "Lagos State & Victoria Island",
                    minLatitude = 6.3,
                    maxLatitude = 6.7,
                    minLongitude = 3.1,
                    maxLongitude = 3.7,
                    downloadSizeBytes = 1100000000L // 1.1 GB
                ),
                FomoRegion(
                    id = "NG-FC",
                    countryId = "NG",
                    name = "Abuja Federal Capital Territory",
                    minLatitude = 8.8,
                    maxLatitude = 9.3,
                    minLongitude = 7.1,
                    maxLongitude = 7.7,
                    downloadSizeBytes = 850000000L // 850 MB
                )
            )
        ),
        FomoCountry(
            id = "KE",
            name = "Kenya",
            isoCode = "KE",
            regions = listOf(
                FomoRegion(
                    id = "KE-30",
                    countryId = "KE",
                    name = "Nairobi Metropolitan & Rift Valley",
                    minLatitude = -1.5,
                    maxLatitude = -1.1,
                    minLongitude = 36.6,
                    maxLongitude = 37.1,
                    downloadSizeBytes = 950000000L // 950 MB
                )
            )
        ),
        FomoCountry(
            id = "UK",
            name = "United Kingdom",
            isoCode = "GB",
            regions = listOf(
                FomoRegion(
                    id = "GB-LDN",
                    countryId = "UK",
                    name = "Greater London & Home Counties",
                    minLatitude = 51.2,
                    maxLatitude = 51.7,
                    minLongitude = -0.5,
                    maxLongitude = 0.3,
                    downloadSizeBytes = 2100000000L // 2.1 GB
                )
            )
        )
    )

    init {
        // Pre-install Gauteng (ZA-GP) for immediate demonstration
        downloadManager.markAsInstalled("ZA-GP")
    }

    override suspend fun isRegionInstalled(regionId: String): Boolean {
        return downloadManager.states.value[regionId] is DownloadState.Installed
    }

    override suspend fun installedRegions(): List<FomoRegion> {
        val installedIds = downloadManager.states.value.filterValues { it is DownloadState.Installed }.keys
        return sampleCountries.flatMap { it.regions }.filter { it.id in installedIds }
    }

    override suspend fun install(region: FomoRegion) {
        downloadManager.download(region)
    }

    override suspend fun remove(region: FomoRegion) {
        downloadManager.delete(region)
    }

    override fun availableCountries(): List<FomoCountry> = sampleCountries

    override suspend fun getRegionForCoordinate(latitude: Double, longitude: Double): FomoRegion? {
        return sampleCountries.flatMap { it.regions }.firstOrNull { r ->
            latitude in r.minLatitude..r.maxLatitude && longitude in r.minLongitude..r.maxLongitude
        } ?: sampleCountries.first().regions.first() // Default to GP if within general SA
    }
}
