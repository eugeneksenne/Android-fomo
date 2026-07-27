package com.example.feature.discover

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Behavioural companion to [DiscoverArchitectureTest].
 *
 * The architecture test only checks *structure* (does every section use
 * `SectionHeader`, does every overlay have `BackHandler`, etc). This suite
 * verifies *behaviour*: clicking each section's "See all" (or equivalent
 * primary) affordance must route to the destination the Discover spec
 * defines, per the "See all destinations" table in
 * `docs/DISCOVER_ARCHITECTURE.md`.
 *
 * Each test renders exactly one section in isolation and asserts the correct
 * callback fired - matching how `DiscoverScreen` wires `DiscoverOverlayState`
 * intents (`overlayState::openX`) to each section's `onSeeAllClick`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DiscoverSeeAllRoutingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun clickSeeAll() {
        composeTestRule.onNodeWithText("See all →").performClick()
    }

    // ---------------------------------------------------------------------
    // Closing Soon -> Smart Places hub
    // ---------------------------------------------------------------------
    @Test
    fun closingSoon_seeAll_opensSmartPlacesHub() {
        val overlayState = DiscoverOverlayState()
        composeTestRule.setContent {
            MyApplicationTheme {
                ClosingSoonSection(onSeeAllClick = overlayState::openSmartPlacesHub)
            }
        }
        clickSeeAll()
        assertTrue("Closing Soon's See all should open the Smart Places hub", overlayState.isSmartPlacesHubOpen)
    }

    // ---------------------------------------------------------------------
    // Flash Drops -> Flash Drops hub
    // ---------------------------------------------------------------------
    @Test
    fun flashDrops_seeAll_opensFlashDropsHub() {
        val overlayState = DiscoverOverlayState()
        composeTestRule.setContent {
            MyApplicationTheme {
                FlashDropsSection(
                    flashDrops = emptyList(),
                    isOnline = true,
                    onSeeAllClick = overlayState::openFlashDropsHub,
                    onClaimClick = {}
                )
            }
        }
        clickSeeAll()
        assertTrue("Flash Drops' See all should open the Flash Drops hub", overlayState.isFlashDropsHubOpen)
    }

    // ---------------------------------------------------------------------
    // My Circle -> My Circle hub
    // ---------------------------------------------------------------------
    @Test
    fun myCircle_seeAll_opensMyCircleHub() {
        val overlayState = DiscoverOverlayState()
        composeTestRule.setContent {
            MyApplicationTheme {
                MyCircleSection(
                    stories = emptyList(),
                    isOnline = true,
                    onSeeAllClick = overlayState::openMyCircleHub,
                    onStoryClick = {}
                )
            }
        }
        clickSeeAll()
        assertTrue("My Circle's See all should open the My Circle hub", overlayState.isMyCircleHubOpen)
    }

    // ---------------------------------------------------------------------
    // Live Moments -> My Circle hub (activity feed)
    // ---------------------------------------------------------------------
    @Test
    fun liveMoments_seeAll_opensMyCircleHub() {
        val overlayState = DiscoverOverlayState()
        composeTestRule.setContent {
            MyApplicationTheme {
                LiveMomentsSection(onSeeAllClick = overlayState::openMyCircleHub)
            }
        }
        clickSeeAll()
        assertTrue("Live Moments' See all should open the My Circle hub", overlayState.isMyCircleHubOpen)
    }

    // ---------------------------------------------------------------------
    // Smart Places -> Smart Places hub
    // ---------------------------------------------------------------------
    @Test
    fun smartPlaces_seeAll_opensSmartPlacesHub() {
        val overlayState = DiscoverOverlayState()
        composeTestRule.setContent {
            MyApplicationTheme {
                SmartPlacesSection(venues = emptyList(), isOnline = true, onSeeAllClick = overlayState::openSmartPlacesHub)
            }
        }
        clickSeeAll()
        assertTrue("Smart Places' See all should open the Smart Places hub", overlayState.isSmartPlacesHubOpen)
    }

    // ---------------------------------------------------------------------
    // Trending Now -> Explore The City hub
    // ---------------------------------------------------------------------
    @Test
    fun trendingNow_seeAll_opensExploreTheCityHub() {
        val overlayState = DiscoverOverlayState()
        composeTestRule.setContent {
            MyApplicationTheme {
                TrendingNowSection(onSeeAllClick = overlayState::openExploreTheCity)
            }
        }
        clickSeeAll()
        assertTrue("Trending Now's See all should open the Explore The City hub", overlayState.isExploreTheCityOpen)
    }

    // ---------------------------------------------------------------------
    // Events -> EventsHomeScreen navigation callback
    // ---------------------------------------------------------------------
    @Test
    fun events_seeAll_invokesNavigateToEvents() {
        var navigatedToEvents = false
        composeTestRule.setContent {
            MyApplicationTheme {
                EventsSection(
                    events = emptyList(),
                    isOnline = true,
                    onNavigateToEvents = { navigatedToEvents = true },
                    onNavigateToEventDetails = {}
                )
            }
        }
        clickSeeAll()
        assertTrue("Events' See all should invoke onNavigateToEvents", navigatedToEvents)
    }

    // ---------------------------------------------------------------------
    // Explore The City -> Explore The City hub
    // ---------------------------------------------------------------------
    @Test
    fun exploreTheCity_seeAll_opensExploreTheCityHub() {
        val overlayState = DiscoverOverlayState()
        composeTestRule.setContent {
            MyApplicationTheme {
                ExploreTheCitySection(
                    venues = emptyList(),
                    isOnline = true,
                    onVenueClick = {},
                    onLikeToggle = {},
                    onSeeAllClick = overlayState::openExploreTheCity
                )
            }
        }
        // ExploreTheCitySection renders "See all →" twice (the shared
        // SectionHeader plus an inline "Places near you" affordance), both
        // wired to the same onSeeAllClick - clicking either must route the
        // same place.
        composeTestRule.onAllNodesWithText("See all →")[0].performClick()
        assertTrue("Explore The City's See all should open the Explore The City hub", overlayState.isExploreTheCityOpen)
    }

    // ---------------------------------------------------------------------
    // Channels -> Channels overlay
    // ---------------------------------------------------------------------
    @Test
    fun channels_seeAll_opensChannelsOverlay() {
        val overlayState = DiscoverOverlayState()
        composeTestRule.setContent {
            MyApplicationTheme {
                ChannelsSection(onOpenClick = overlayState::openChannels)
            }
        }
        clickSeeAll()
        assertTrue("Channels' See all should open the Channels overlay", overlayState.isChannelsOpen)
    }

    // ---------------------------------------------------------------------
    // Prep Rooms -> Prep Rooms overlay
    // ---------------------------------------------------------------------
    @Test
    fun prepRooms_seeAll_opensPrepRoomsOverlay() {
        val overlayState = DiscoverOverlayState()
        composeTestRule.setContent {
            MyApplicationTheme {
                PrepRoomsSection(onOpenClick = overlayState::openPrepRooms)
            }
        }
        clickSeeAll()
        assertTrue("Prep Rooms' See all should open the Prep Rooms overlay", overlayState.isPrepRoomsOpen)
    }

    // ---------------------------------------------------------------------
    // Tonight -> Plans Workspace navigation callback
    // ---------------------------------------------------------------------
    @Test
    fun tonight_seeAll_invokesNavigateToPlansWorkspace() {
        var navigatedToPlansWorkspace = false
        composeTestRule.setContent {
            MyApplicationTheme {
                TonightSection(
                    onNavigateToNightGuard = {},
                    onNavigateToPlansWorkspace = { navigatedToPlansWorkspace = true }
                )
            }
        }
        clickSeeAll()
        assertTrue("Tonight's See all should invoke onNavigateToPlansWorkspace", navigatedToPlansWorkspace)
    }

    // ---------------------------------------------------------------------
    // Sanity check: only one overlay slot flips per interaction.
    // ---------------------------------------------------------------------
    @Test
    fun closingSoon_seeAll_doesNotOpenUnrelatedOverlays() {
        val overlayState = DiscoverOverlayState()
        composeTestRule.setContent {
            MyApplicationTheme {
                ClosingSoonSection(onSeeAllClick = overlayState::openSmartPlacesHub)
            }
        }
        clickSeeAll()
        assertEquals(true, overlayState.isSmartPlacesHubOpen)
        assertEquals(false, overlayState.isFlashDropsHubOpen)
        assertEquals(false, overlayState.isChannelsOpen)
        assertEquals(false, overlayState.isPrepRoomsOpen)
        assertEquals(false, overlayState.isExploreTheCityOpen)
        assertEquals(false, overlayState.isMyCircleHubOpen)
    }
}
