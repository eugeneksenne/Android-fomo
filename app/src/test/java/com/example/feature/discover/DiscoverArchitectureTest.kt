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
            "components/DiscoverStateContent.kt",
            "components/DiscoverImagePrefetcher.kt",
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
            "overlays/MyMovesHubOverlay.kt",
            "dialogs/FlashDropClaimDialog.kt",
            "dialogs/FlashDropRouteDialog.kt",
            "dialogs/GlobalPlanContextSheet.kt",
            "dialogs/TonightPlanDialogs.kt",
            "DiscoverAnalytics.kt"
        )

        expected.forEach { relativePath ->
            assertTrue("Missing Discover module $relativePath", File(discoverRoot, relativePath).exists())
        }
    }

    @Test
    fun legacyDiscoverSectionsMonolithIsRemoved() {
        val legacy = File(discoverRoot, "DiscoverSections.kt")
        assertFalse(
            "DiscoverSections.kt was replaced by the components/ and sections/ modules and must not return",
            legacy.exists()
        )
    }

    @Test
    fun discoverScreenDoesNotContainRemovedQuickBanners() {
        val source = File(discoverRoot, "DiscoverScreen.kt").readText()
        assertFalse(source.contains("NightGuardQuickBanner"))
        assertFalse(source.contains("CountryPackQuickBanner"))
    }

    @Test
    fun discoverShellUsesStableLazyColumnKeysAndContentTypes() {
        val source = File(discoverRoot, "DiscoverScreen.kt").readText()
        assertTrue(
            "Discover LazyColumn items should declare stable keys",
            source.contains("key = \"hero\"") && source.contains("key = \"flash_drops\"")
        )
        assertTrue(
            "Discover LazyColumn items should declare contentType values",
            source.contains("contentType = \"section\"")
        )
    }

    @Test
    fun fullScreenOverlaysHandleSystemBack() {
        val overlaySources = listOf(
            "overlays/VenuePreviewOverlay.kt",
            "overlays/StoryViewerOverlay.kt",
            "overlays/MyCircleOverlay.kt",
            "overlays/FlashDropsOverlay.kt",
            "overlays/SmartPlacesOverlay.kt",
            "overlays/MyMovesHubOverlay.kt",
            "ExploreTheCityScreen.kt",
            "ChannelsScreen.kt",
            "PrepRoomsScreen.kt"
        )
        overlaySources.forEach { relativePath ->
            val source = File(discoverRoot, relativePath).readText()
            assertTrue(
                "$relativePath should handle the system back gesture via BackHandler",
                source.contains("BackHandler")
            )
        }
    }

    @Test
    fun dataDrivenSectionsSupportOfflineAndEmptyStates() {
        val sectionSources = listOf(
            "sections/FlashDropsSection.kt",
            "sections/EventsSection.kt",
            "sections/SmartPlacesSection.kt",
            "sections/MyCircleSection.kt",
            "sections/ExploreCitySection.kt"
        )
        sectionSources.forEach { relativePath ->
            val source = File(discoverRoot, relativePath).readText()
            assertTrue(
                "$relativePath should render DiscoverOfflineState when offline",
                source.contains("DiscoverOfflineState")
            )
            assertTrue(
                "$relativePath should accept an isOnline flag",
                source.contains("isOnline")
            )
        }
    }

    @Test
    fun sectionModulesDeclareComposableRoots() {
        val sections = File(discoverRoot, "sections").listFiles { f -> f.name.endsWith("Section.kt") } ?: emptyArray()
        assertTrue(sections.isNotEmpty())
        sections.forEach { file ->
            val lines = file.readLines()
            val rootDeclaration = lines.indexOfFirst { it.startsWith("fun ") && it.contains("Section(") }
            assertTrue("${file.name} should declare a root section composable", rootDeclaration > 0)
            assertTrue(
                "${file.name} root section must be annotated @Composable",
                lines[rootDeclaration - 1].contains("@Composable") ||
                    lines[rootDeclaration - 2].contains("@Composable")
            )
        }
    }

    @Test
    fun sectionHeadersUseStandardSeeAllPattern() {
        // TonightSection is an intentionally branded "My Moves" panel with its
        // own CTA header, so it is exempt from the shared SectionHeader pattern.
        val sections = File(discoverRoot, "sections").listFiles { f ->
            f.name.endsWith("Section.kt") && f.name != "TonightSection.kt"
        } ?: emptyArray()
        assertTrue(sections.isNotEmpty())
        sections.forEach { file ->
            assertTrue(
                "${file.name} should use the shared SectionHeader pattern",
                file.readText().contains("SectionHeader(")
            )
        }
    }
}
