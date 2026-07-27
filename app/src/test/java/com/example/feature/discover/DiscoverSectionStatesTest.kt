package com.example.feature.discover

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Compose UI tests for the Discover section state contract:
 * offline / empty / populated rendering and the standard SectionHeader.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DiscoverSectionStatesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ---------------------------------------------------------------------
    // Flash Drops
    // ---------------------------------------------------------------------

    @Test
    fun flashDrops_offlineAndEmpty_rendersOfflineState() {
        composeTestRule.setContent {
            MyApplicationTheme {
                FlashDropsSection(flashDrops = emptyList(), isOnline = false, onClaimClick = {})
            }
        }
        composeTestRule.onNodeWithText("You're offline").assertIsDisplayed()
    }

    @Test
    fun flashDrops_onlineAndEmpty_rendersEmptyState() {
        composeTestRule.setContent {
            MyApplicationTheme {
                FlashDropsSection(flashDrops = emptyList(), isOnline = true, onClaimClick = {})
            }
        }
        composeTestRule.onNodeWithText("No Flash Drops right now").assertIsDisplayed()
    }

    @Test
    fun flashDrops_withData_rendersFirstCard() {
        val drops = com.example.core.data.VenueRepository.globalFlashDropsState.value
        assumeTrue("Seed flash drops expected", drops.isNotEmpty())
        composeTestRule.setContent {
            MyApplicationTheme {
                FlashDropsSection(flashDrops = drops, onClaimClick = {})
            }
        }
        composeTestRule.onNodeWithText(drops.first().title).assertIsDisplayed()
    }

    // ---------------------------------------------------------------------
    // Events
    // ---------------------------------------------------------------------

    @Test
    fun events_offlineAndEmpty_rendersOfflineState() {
        composeTestRule.setContent {
            MyApplicationTheme {
                EventsSection(
                    events = emptyList(),
                    isOnline = false,
                    onNavigateToEvents = {},
                    onNavigateToEventDetails = {}
                )
            }
        }
        composeTestRule.onNodeWithText("You're offline").assertIsDisplayed()
    }

    @Test
    fun events_onlineAndEmpty_rendersEmptyState() {
        composeTestRule.setContent {
            MyApplicationTheme {
                EventsSection(
                    events = emptyList(),
                    onNavigateToEvents = {},
                    onNavigateToEventDetails = {}
                )
            }
        }
        composeTestRule.onNodeWithText("No events loaded").assertIsDisplayed()
    }

    @Test
    fun events_withData_rendersEventTitle() {
        val events = com.example.core.data.EventRepository.eventsState.value
        assumeTrue("Seed events expected", events.isNotEmpty())
        composeTestRule.setContent {
            MyApplicationTheme {
                EventsSection(events = events, onNavigateToEvents = {}, onNavigateToEventDetails = {})
            }
        }
        composeTestRule.onNodeWithText(events.first().title).assertIsDisplayed()
    }

    // ---------------------------------------------------------------------
    // Smart Places
    // ---------------------------------------------------------------------

    @Test
    fun smartPlaces_offlineAndEmpty_rendersOfflineState() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SmartPlacesSection(venues = emptyList(), isOnline = false)
            }
        }
        composeTestRule.onNodeWithText("You're offline").assertIsDisplayed()
    }

    @Test
    fun smartPlaces_onlineAndEmpty_rendersCuratedFallbackCards() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SmartPlacesSection(venues = emptyList(), isOnline = true)
            }
        }
        composeTestRule.onNodeWithText("D48 Midrand").assertIsDisplayed()
    }

    // ---------------------------------------------------------------------
    // My Circle
    // ---------------------------------------------------------------------

    @Test
    fun myCircle_offlineAndEmpty_rendersOfflineStateAndYourStory() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MyCircleSection(
                    stories = emptyList(),
                    isOnline = false,
                    onSeeAllClick = {},
                    onStoryClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("You're offline").assertIsDisplayed()
        // The "Your Story" composer is always available, even offline.
        composeTestRule.onNodeWithText("Your Story").assertIsDisplayed()
    }

    // ---------------------------------------------------------------------
    // Section header standard
    // ---------------------------------------------------------------------

    @Test
    fun sectionHeader_rendersTitleSubtitleAndSeeAllArrow() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SectionHeader(
                    title = "Test Section",
                    subtitle = "A subtitle",
                    actionText = "See all"
                )
            }
        }
        composeTestRule.onNodeWithText("Test Section").assertIsDisplayed()
        composeTestRule.onNodeWithText("A subtitle").assertIsDisplayed()
        composeTestRule.onNodeWithText("See all →").assertIsDisplayed()
    }
}
