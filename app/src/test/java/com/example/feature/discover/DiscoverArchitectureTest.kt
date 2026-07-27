package com.example.feature.discover

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DiscoverArchitectureTest {
    private val discoverRoot = File("src/main/java/com/example/feature/discover")

    @Test
    fun discoverScreenIsThinShellAfterModuleSplit() {
        val shell = File(discoverRoot, "DiscoverScreen.kt")
        assertTrue(shell.exists())
        assertTrue(
            "DiscoverScreen.kt should stay a thin orchestration shell",
            shell.readLines().size <= 360
        )
    }

    @Test
    fun discoverFeatureModulesExist() {
        val expected = listOf(
            "components/DiscoverTopBar.kt",
            "components/HeroSection.kt",
            "components/SectionHeader.kt",
            "components/SectionSpacer.kt",
            "sections/ClosingSoonSection.kt",
            "sections/FlashDropsSection.kt",
            "sections/MyCircleSection.kt",
            "sections/LiveMomentsSection.kt",
            "sections/SmartPlacesSection.kt",
            "sections/TrendingSection.kt",
            "sections/EventsSection.kt",
            "sections/ExploreCitySection.kt",
            "sections/ChannelsSection.kt",
            "sections/PrepRoomsSection.kt",
            "sections/TonightSection.kt",
            "overlays/VenuePreviewOverlay.kt",
            "overlays/StoryViewerOverlay.kt",
            "overlays/MyCircleOverlay.kt",
            "overlays/FlashDropsOverlay.kt",
            "overlays/SmartPlacesOverlay.kt",
            "dialogs/FlashDropClaimDialog.kt",
            "dialogs/FlashDropRouteDialog.kt",
            "dialogs/GlobalPlanContextSheet.kt"
        )

        expected.forEach { relativePath ->
            assertTrue("Missing Discover module $relativePath", File(discoverRoot, relativePath).exists())
        }
    }

    @Test
    fun discoverScreenDoesNotContainRemovedQuickBanners() {
        val source = File(discoverRoot, "DiscoverScreen.kt").readText()
        assertFalse(source.contains("NightGuardQuickBanner"))
        assertFalse(source.contains("CountryPackQuickBanner"))
    }
}
