package com.example.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class CountryPackType(val displayName: String, val emoji: String, val description: String) {
    CORE("Core Infrastructure", "🏛️", "Essential geographic boundaries, roads, and regional cell metadata"),
    NIGHTLIFE("Nightlife Pack", "🌙", "Clubs, lounges, bars, shisanyamas, taverns & realtime crowd overlays"),
    FOOD("Food Pack", "🍽️", "Trendy restaurants, date spots, brunch cafes, rooftop dining & speakeasies"),
    PREP("Prep Pack", "💈", "Barbershops, salons, tattoo studios, sneaker stores & fashion boutiques"),
    WELLNESS("Wellness Pack", "🧘", "Yoga studios, spas, recovery clubs, social coffee shops & healthy spots"),
    TRAVEL("Travel Pack", "✈️", "Airports, resorts, scenic viewpoints, tourist attractions & hotel pools")
}

enum class PackInstallStatus {
    NOT_INSTALLED,
    QUEUED,
    DOWNLOADING,
    INSTALLED,
    UPDATING
}

enum class CellScanStatus {
    PENDING,
    SCANNING,
    IMPORTING,
    NORMALIZING,
    DEDUPLICATING,
    VERIFYING,
    INDEXING,
    COMPLETE,
    MONITORING
}

data class CountryPackInfo(
    val id: String,
    val country: String,
    val packType: CountryPackType,
    val version: Int,
    val sizeMb: Double,
    val installedVersion: Int? = null,
    val status: PackInstallStatus = PackInstallStatus.NOT_INSTALLED,
    val downloadProgress: Float = 0f,
    val venueCount: Int = 0,
    val lastUpdated: String = "2026-07-20"
)

data class RegionalCell(
    val cellId: String,
    val country: String,
    val province: String,
    val metro: String,
    val city: String,
    val district: String,
    val suburb: String,
    val postalCode: String,
    val coverageScore: Double, // 0.0 to 100.0%
    val scanStatus: CellScanStatus,
    val venueCount: Int,
    val lastScanDate: String,
    val packCoverageMap: Map<CountryPackType, Double> = emptyMap()
)

data class PermanentVenueRecord(
    val fomoVenueId: String, // e.g. "FMO-ZA-GP-JHB-00000127"
    val name: String,
    val displayName: String,
    val slug: String,
    val category: String,
    val subcategory: String,
    val packType: CountryPackType,
    val country: String,
    val province: String,
    val metro: String,
    val city: String,
    val district: String,
    val suburb: String,
    val postalCode: String,
    val cellId: String,
    val latitude: Double,
    val longitude: Double,
    val streetAddress: String,
    val phone: String,
    val website: String,
    val instagram: String,
    val openingHours: String,
    val staticAmenities: List<String>,
    val completenessScore: Int, // 0 to 100
    val isVerified: Boolean,
    val googlePlaceId: String? = null,
    val openStreetMapId: String? = null,
    val foursquareId: String? = null,
    val imageRes: String = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=600&auto=format&fit=crop"
)

data class DeltaUpdatePatch(
    val patchId: String,
    val packType: CountryPackType,
    val fromVersion: Int,
    val toVersion: Int,
    val patchSizeBytes: Long,
    val newVenuesCount: Int,
    val updatedVenuesCount: Int,
    val closedVenuesCount: Int,
    val releaseNotes: String
)

data class CountryPackState(
    val currentCountry: String = "South Africa",
    val detectedLocation: String = "Johannesburg, GP (GPS Verified)",
    val countryPacks: List<CountryPackInfo> = emptyList(),
    val regionalCells: List<RegionalCell> = emptyList(),
    val permanentVenues: List<PermanentVenueRecord> = emptyList(),
    val activeCategoryFilters: Set<CountryPackType> = setOf(CountryPackType.CORE, CountryPackType.NIGHTLIFE),
    val expandedDistricts: Set<String> = setOf("Sandton", "Rosebank", "Soweto"),
    val isBackgroundSyncActive: Boolean = true,
    val activeDeltaPatch: DeltaUpdatePatch? = null,
    val totalCoveragePercent: Double = 99.8,
    val totalVenuesInCountry: Int = 14250,
    val sqliteDatabaseSizeMb: Double = 42.8
)

object CountryPackRepository {

    private val initialPacks = listOf(
        CountryPackInfo("p_core", "South Africa", CountryPackType.CORE, version = 14, sizeMb = 12.4, installedVersion = 14, status = PackInstallStatus.INSTALLED, downloadProgress = 1.0f, venueCount = 0),
        CountryPackInfo("p_nightlife", "South Africa", CountryPackType.NIGHTLIFE, version = 18, sizeMb = 8.6, installedVersion = 18, status = PackInstallStatus.INSTALLED, downloadProgress = 1.0f, venueCount = 1420),
        CountryPackInfo("p_food", "South Africa", CountryPackType.FOOD, version = 12, sizeMb = 15.2, installedVersion = 12, status = PackInstallStatus.INSTALLED, downloadProgress = 1.0f, venueCount = 4850),
        CountryPackInfo("p_prep", "South Africa", CountryPackType.PREP, version = 9, sizeMb = 4.1, installedVersion = 9, status = PackInstallStatus.INSTALLED, downloadProgress = 1.0f, venueCount = 1820),
        CountryPackInfo("p_wellness", "South Africa", CountryPackType.WELLNESS, version = 7, sizeMb = 3.8, installedVersion = 7, status = PackInstallStatus.INSTALLED, downloadProgress = 1.0f, venueCount = 980),
        CountryPackInfo("p_travel", "South Africa", CountryPackType.TRAVEL, version = 11, sizeMb = 6.2, installedVersion = 11, status = PackInstallStatus.INSTALLED, downloadProgress = 1.0f, venueCount = 1180)
    )

    private val initialCells = listOf(
        RegionalCell(
            cellId = "ZA-GP-JHB-001",
            country = "South Africa",
            province = "Gauteng",
            metro = "City of Johannesburg",
            city = "Johannesburg",
            district = "Sandton Central",
            suburb = "Sandton",
            postalCode = "2196",
            coverageScore = 100.0,
            scanStatus = CellScanStatus.COMPLETE,
            venueCount = 342,
            lastScanDate = "2026-07-21",
            packCoverageMap = mapOf(
                CountryPackType.NIGHTLIFE to 100.0,
                CountryPackType.FOOD to 99.8,
                CountryPackType.PREP to 100.0,
                CountryPackType.WELLNESS to 100.0,
                CountryPackType.TRAVEL to 98.5
            )
        ),
        RegionalCell(
            cellId = "ZA-GP-JHB-002",
            country = "South Africa",
            province = "Gauteng",
            metro = "City of Johannesburg",
            city = "Johannesburg",
            district = "Rosebank Node",
            suburb = "Rosebank",
            postalCode = "2196",
            coverageScore = 99.5,
            scanStatus = CellScanStatus.COMPLETE,
            venueCount = 288,
            lastScanDate = "2026-07-21",
            packCoverageMap = mapOf(
                CountryPackType.NIGHTLIFE to 100.0,
                CountryPackType.FOOD to 99.2,
                CountryPackType.PREP to 98.8,
                CountryPackType.WELLNESS to 100.0,
                CountryPackType.TRAVEL to 100.0
            )
        ),
        RegionalCell(
            cellId = "ZA-GP-JHB-003",
            country = "South Africa",
            province = "Gauteng",
            metro = "City of Johannesburg",
            city = "Soweto",
            district = "Orlando West",
            suburb = "Soweto",
            postalCode = "1804",
            coverageScore = 98.2,
            scanStatus = CellScanStatus.MONITORING,
            venueCount = 195,
            lastScanDate = "2026-07-20",
            packCoverageMap = mapOf(
                CountryPackType.NIGHTLIFE to 100.0,
                CountryPackType.FOOD to 97.5,
                CountryPackType.PREP to 96.0,
                CountryPackType.WELLNESS to 95.0,
                CountryPackType.TRAVEL to 99.0
            )
        )
    )

    private val initialVenues = listOf(
        PermanentVenueRecord(
            fomoVenueId = "FMO-ZA-GP-JHB-00000127",
            name = "LIV Sandton",
            displayName = "LIV Nightclub & Lounge",
            slug = "liv-sandton",
            category = "Nightlife",
            subcategory = "Nightclub",
            packType = CountryPackType.NIGHTLIFE,
            country = "South Africa",
            province = "Gauteng",
            metro = "City of Johannesburg",
            city = "Johannesburg",
            district = "Sandton",
            suburb = "Sandton",
            postalCode = "2196",
            cellId = "ZA-GP-JHB-001",
            latitude = -26.1076,
            longitude = 28.0567,
            streetAddress = "24 Gwigwi Mrwebi St, Sandton",
            phone = "+27 11 784 1000",
            website = "https://livsandton.co.za",
            instagram = "@livsandton",
            openingHours = "Thu–Sat 21:00–04:00",
            staticAmenities = listOf("VIP Booths", "Valet Parking", "High-Tech Audio", "Outdoor Terrace"),
            completenessScore = 100,
            isVerified = true,
            googlePlaceId = "ChIJ_3f4x-89lR4R...",
            openStreetMapId = "osm_node_88219"
        ),
        PermanentVenueRecord(
            fomoVenueId = "FMO-ZA-GP-JHB-00000128",
            name = "Marble Restaurant",
            displayName = "Marble Rosebank Rooftop",
            slug = "marble-rosebank",
            category = "Food",
            subcategory = "Fine Dining & Steakhouse",
            packType = CountryPackType.FOOD,
            country = "South Africa",
            province = "Gauteng",
            metro = "City of Johannesburg",
            city = "Johannesburg",
            district = "Rosebank",
            suburb = "Rosebank",
            postalCode = "2196",
            cellId = "ZA-GP-JHB-002",
            latitude = -26.1462,
            longitude = 28.0436,
            streetAddress = "19 Keyes Ave, Rosebank",
            phone = "+27 10 594 5550",
            website = "https://marble.restaurant",
            instagram = "@marblerestaurant",
            openingHours = "Mon–Sun 12:00–22:30",
            staticAmenities = listOf("Rooftop Deck", "Sommelier Wine Bar", "Open Fire Grill", "Reservations Required"),
            completenessScore = 100,
            isVerified = true,
            googlePlaceId = "ChIJ_marble_9918",
            openStreetMapId = "osm_node_99214"
        ),
        PermanentVenueRecord(
            fomoVenueId = "FMO-ZA-GP-JHB-00000129",
            name = "Legends Barbershop Rosebank",
            displayName = "Legends Barber Flagship",
            slug = "legends-barber-rosebank",
            category = "Prep",
            subcategory = "Barbershop",
            packType = CountryPackType.PREP,
            country = "South Africa",
            province = "Gauteng",
            metro = "City of Johannesburg",
            city = "Johannesburg",
            district = "Rosebank",
            suburb = "Rosebank",
            postalCode = "2196",
            cellId = "ZA-GP-JHB-002",
            latitude = -26.1450,
            longitude = 28.0450,
            streetAddress = "Rosebank Mall Shop 312",
            phone = "+27 11 880 2311",
            website = "https://legendsbarber.com",
            instagram = "@legends_barber",
            openingHours = "Mon–Sat 09:00–18:00",
            staticAmenities = listOf("VIP Grooming", "Hot Towel Shave", "Beverage Bar", "Sneaker Care"),
            completenessScore = 98,
            isVerified = true,
            googlePlaceId = "ChIJ_legends_1289",
            openStreetMapId = "osm_node_12398"
        )
    )

    private val _state = MutableStateFlow(
        CountryPackState(
            countryPacks = initialPacks,
            regionalCells = initialCells,
            permanentVenues = initialVenues,
            activeDeltaPatch = DeltaUpdatePatch(
                patchId = "patch_za_v18_to_v19",
                packType = CountryPackType.NIGHTLIFE,
                fromVersion = 18,
                toVersion = 19,
                patchSizeBytes = 245000,
                newVenuesCount = 14,
                updatedVenuesCount = 32,
                closedVenuesCount = 2,
                releaseNotes = "Added 14 new nightlife spots in Sandton & Rosebank, updated weekend DJ lineups."
            )
        )
    )
    val state: StateFlow<CountryPackState> = _state.asStateFlow()

    fun toggleCategoryFilter(packType: CountryPackType) {
        _state.update { current ->
            val updated = current.activeCategoryFilters.toMutableSet()
            if (updated.contains(packType)) {
                if (updated.size > 1) updated.remove(packType) // Keep at least one active
            } else {
                updated.add(packType)
            }
            current.copy(activeCategoryFilters = updated)
        }
    }

    fun installPack(packType: CountryPackType) {
        _state.update { current ->
            val updatedPacks = current.countryPacks.map { pack ->
                if (pack.packType == packType) {
                    pack.copy(
                        status = PackInstallStatus.INSTALLED,
                        installedVersion = pack.version,
                        downloadProgress = 1.0f
                    )
                } else pack
            }
            current.copy(countryPacks = updatedPacks)
        }
    }

    fun applyDeltaUpdate(packType: CountryPackType) {
        _state.update { current ->
            val patch = current.activeDeltaPatch
            val updatedPacks = current.countryPacks.map { pack ->
                if (pack.packType == packType && patch != null) {
                    pack.copy(
                        version = patch.toVersion,
                        installedVersion = patch.toVersion,
                        lastUpdated = "2026-07-22 (Delta Patch Applied)"
                    )
                } else pack
            }
            current.copy(countryPacks = updatedPacks, activeDeltaPatch = null)
        }
    }

    fun triggerCellScan(cellId: String) {
        _state.update { current ->
            val updatedCells = current.regionalCells.map { cell ->
                if (cell.cellId == cellId) {
                    cell.copy(
                        scanStatus = CellScanStatus.COMPLETE,
                        coverageScore = 100.0,
                        lastScanDate = "2026-07-22 (Just Scanned)"
                    )
                } else cell
            }
            current.copy(regionalCells = updatedCells)
        }
    }

    fun expandDistrictLayer(districtName: String) {
        _state.update { current ->
            val updated = current.expandedDistricts.toMutableSet()
            if (updated.contains(districtName)) {
                updated.remove(districtName)
            } else {
                updated.add(districtName)
            }
            current.copy(expandedDistricts = updated)
        }
    }

    fun createVenueRecord(
        name: String,
        category: String,
        packType: CountryPackType,
        district: String,
        address: String,
        phone: String
    ) {
        val newRecord = PermanentVenueRecord(
            fomoVenueId = "FMO-ZA-GP-JHB-0000${(1000..9999).random()}",
            name = name,
            displayName = name,
            slug = name.lowercase().replace(" ", "-"),
            category = category,
            subcategory = category,
            packType = packType,
            country = "South Africa",
            province = "Gauteng",
            metro = "City of Johannesburg",
            city = "Johannesburg",
            district = district.ifBlank { "Sandton" },
            suburb = district.ifBlank { "Sandton" },
            postalCode = "2196",
            cellId = "ZA-GP-JHB-001",
            latitude = -26.1076,
            longitude = 28.0567,
            streetAddress = address.ifBlank { "Johannesburg Central" },
            phone = phone.ifBlank { "+27 11 000 0000" },
            website = "https://fomoapp.com/venue/$name",
            instagram = "@$name",
            openingHours = "10:00 - 02:00",
            staticAmenities = listOf("Wi-Fi", "Card Payments", "Verified Venue"),
            completenessScore = 95,
            isVerified = true
        )

        _state.update { current ->
            current.copy(permanentVenues = listOf(newRecord) + current.permanentVenues)
        }
    }
}
