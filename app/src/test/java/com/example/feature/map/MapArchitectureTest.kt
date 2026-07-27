package com.example.feature.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Structural guardrails for the Map feature module split, mirroring
 * `DiscoverArchitectureTest`'s role for the Discover feature.
 */
class MapArchitectureTest {
    private val mapRoot = File("src/main/java/com/example/feature/map")

    @Test
    fun mapScreenIsThinShellAfterModuleSplit() {
        val shell = File(mapRoot, "MapScreen.kt")
        assertTrue(shell.exists())
        assertTrue(
            "MapScreen.kt should stay a thin orchestration shell",
            shell.readLines().size <= 350
        )
    }

    @Test
    fun mapFeatureModulesExist() {
        val expected = listOf(
            "state/MapScreenState.kt",
            "components/MapTopBar.kt",
            "components/CountryPackChips.kt",
            "components/SectionHeader.kt",
            "components/MapFloatingButtons.kt",
            "components/CityStatusChip.kt",
            "cards/NearestVenueCard.kt",
            "cards/NearbyVenueCarousel.kt",
            "cards/VenuePreviewCard.kt",
            "map/VenueMapCanvas.kt",
            "map/MarkerRenderer.kt",
            "map/WebViewMap.kt",
            "overlays/VenuePreviewOverlay.kt",
            "overlays/WebsiteViewer.kt",
            "dialogs/AddPlaceDialog.kt",
            "util/VenueFilter.kt",
            "util/VenueRanking.kt",
            "util/MarkerGenerator.kt"
        )

        expected.forEach { relativePath ->
            assertTrue("Missing Map module $relativePath", File(mapRoot, relativePath).exists())
        }
    }

    @Test
    fun mapScreenStateHolderDeclaresExpectedIntents() {
        val source = File(mapRoot, "state/MapScreenState.kt").readText()
        val expectedIntents = listOf(
            "selectCategory", "selectMapItem", "clearSelection", "toggleHeatmap",
            "selectBottomTab", "openSearch", "closeSearch", "openNotifications",
            "closeNotifications", "openProfile", "closeProfile", "openAddPlace",
            "closeAddPlace", "openWebsite", "closeWebsite", "cycleCityStatus",
            "addCustomVenue", "dismissAll"
        )
        expectedIntents.forEach { intent ->
            assertTrue(
                "MapScreenState should declare a `$intent` intent function",
                source.contains("fun $intent(")
            )
        }
    }

    @Test
    fun mapScreenDoesNotOwnRawOverlayState() {
        val source = File(mapRoot, "MapScreen.kt").readText()
        // The shell may still hold the raw WebView reference (a platform
        // handle, not UI/dialog selection state) but must not reintroduce
        // the pre-refactor dialog/category/selection booleans directly.
        val forbiddenLocalState = listOf(
            "var selectedCategory by remember",
            "var selectedMapItem by remember",
            "var isSearchOpen by remember",
            "var isNotificationsOpen by remember",
            "var isProfileOpen by remember",
            "var isAddPlaceOpen by remember",
            "var isHeatmapEnabled by remember",
            "var bottomTabSelection by remember",
            "var activeWebsiteUrl by remember",
            "var cityStatusIndex by remember"
        )
        forbiddenLocalState.forEach { snippet ->
            assertFalse(
                "MapScreen.kt should not reintroduce local overlay state ('$snippet'); use MapScreenState",
                source.contains(snippet)
            )
        }
    }

    @Test
    fun mapScreenUsesMapScreenState() {
        val source = File(mapRoot, "MapScreen.kt").readText()
        assertTrue(
            "MapScreen.kt should obtain its state via rememberMapScreenState()",
            source.contains("rememberMapScreenState()")
        )
    }

    @Test
    fun leafletHtmlAndWebViewCodeLiveInMapPackage() {
        assertTrue(File(mapRoot, "map/LeafletHtml.kt").exists())
        assertTrue(File(mapRoot, "map/WebViewMap.kt").exists())
        assertTrue(File(mapRoot, "map/VenueMapCanvas.kt").exists())

        val screenSource = File(mapRoot, "MapScreen.kt").readText()
        assertFalse(
            "MapScreen.kt should not contain raw Leaflet HTML any more",
            screenSource.contains("<!DOCTYPE html>")
        )
        assertFalse(
            "MapScreen.kt should not construct a WebView directly any more",
            screenSource.contains("WebView(ctx)")
        )
    }

    @Test
    fun markerGenerationLivesInUtilPackage() {
        val generatorSource = File(mapRoot, "util/MarkerGenerator.kt").readText()
        assertTrue(generatorSource.contains("addVenueMarker"))
        assertTrue(generatorSource.contains("addFriendMarker"))

        val screenSource = File(mapRoot, "MapScreen.kt").readText()
        assertFalse(
            "MapScreen.kt should not build marker Javascript strings inline any more",
            screenSource.contains("addVenueMarker(")
        )
    }

    @Test
    fun venueFilteringLivesInUtilPackage() {
        val filterSource = File(mapRoot, "util/VenueFilter.kt").readText()
        assertTrue(filterSource.contains("fun filterByCategory"))

        val screenSource = File(mapRoot, "MapScreen.kt").readText()
        assertFalse(
            "MapScreen.kt should not inline the category-filter predicate any more",
            screenSource.contains("clean == \"wellness\"")
        )
    }

    @Test
    fun componentFilesStayUnderLineBudget() {
        val componentFiles = listOf(
            "components/MapTopBar.kt",
            "components/CountryPackChips.kt",
            "components/SectionHeader.kt",
            "components/MapFloatingButtons.kt",
            "components/CityStatusChip.kt"
        )
        componentFiles.forEach { relativePath ->
            val file = File(mapRoot, relativePath)
            assertTrue("Missing $relativePath", file.exists())
            assertTrue(
                "$relativePath should stay under 250 lines",
                file.readLines().size <= 250
            )
        }
    }
}
