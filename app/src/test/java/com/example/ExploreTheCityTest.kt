package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.feature.discover.ExploreTheCitySection
import com.example.feature.discover.VenuePreviewOverlay
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ExploreTheCityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_explore_city_section_loads() {
        val testVenues = com.example.core.data.VenueRepository.exploreVenuesState.value
        composeTestRule.setContent {
            MyApplicationTheme {
                ExploreTheCitySection(
                    venues = testVenues,
                    onVenueClick = {},
                    onLikeToggle = {}
                )
            }
        }

        // Verify the hero card is displayed
        composeTestRule.onNodeWithTag("explore_hero_card").assertIsDisplayed()
    }

    @Test
    fun test_world_chip_clicks_and_opens_preview() {
        val testVenues = com.example.core.data.VenueRepository.exploreVenuesState.value
        composeTestRule.setContent {
            MyApplicationTheme {
                var selectedPreviewVenue by remember { mutableStateOf<com.example.core.data.ExploreVenue?>(null) }
                Box(modifier = Modifier.fillMaxSize()) {
                    ExploreTheCitySection(
                        venues = testVenues,
                        onVenueClick = { selectedPreviewVenue = it },
                        onLikeToggle = {}
                    )
                    if (selectedPreviewVenue != null) {
                        VenuePreviewOverlay(
                            venue = selectedPreviewVenue!!,
                            onDismiss = { selectedPreviewVenue = null },
                            onNavigateToLobby = {},
                            onLikeToggle = {}
                        )
                    }
                }
            }
        }

        // Click the nightlife world chip
        composeTestRule.onNodeWithTag("world_chip_Nightlife").performClick()
        composeTestRule.waitForIdle()

        // Click the Konka venue card to open preview overlay
        composeTestRule.onNodeWithTag("venue_card_konka_soweto").performClick()
        composeTestRule.waitForIdle()

        // Verify preview card is displayed
        composeTestRule.onNodeWithTag("venue_preview_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preview_club_lobby_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preview_route_button").assertIsDisplayed()

        // Dismiss the preview overlay
        composeTestRule.onNodeWithTag("preview_close_button").performClick()
        composeTestRule.waitForIdle()

        // Verify overlay is gone
        composeTestRule.onNodeWithTag("venue_preview_card").assertDoesNotExist()
    }
}
