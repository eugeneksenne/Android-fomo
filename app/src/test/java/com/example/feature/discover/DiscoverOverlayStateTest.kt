package com.example.feature.discover

import com.example.core.data.ExploreVenue
import com.example.core.data.FlashDrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DiscoverOverlayState].
 *
 * These exercise the state object directly (no Compose runtime / Robolectric
 * required) to lock in the "single source of truth for what's open" contract
 * that replaced the shell's dozen independent `mutableStateOf` flags.
 */
class DiscoverOverlayStateTest {

    private fun venue(id: String = "venue_1") = ExploreVenue(
        id = id,
        name = "Test Venue",
        category = "Nightlife",
        subcategory = "Club",
        imageUrl = "",
        rating = 4.5f,
        reviewCount = 10,
        address = "123 Test St",
        area = "Sandton",
        distanceText = "1.2km away",
        attributes = emptyList(),
        openDays = "Fri-Sat",
        startHour = 18,
        endHour = 2
    )

    private fun flashDrop(id: String = "drop_1") = FlashDrop(
        id = id,
        title = "Test Drop",
        subtitle = "Description",
        expiresMinutes = 30,
        initialStock = 10,
        currentStock = 10,
        imageRes = "",
        venueId = "venue_1",
        venueName = "Test Venue"
    )

    @Test
    fun initialState_hasNoOverlaysOpen() {
        val state = DiscoverOverlayState()
        assertNull(state.previewVenue)
        assertFalse(state.isMyCircleHubOpen)
        assertNull(state.storyViewerIndex)
        assertNull(state.flashDropForClaim)
        assertNull(state.globalPlanTarget)
        assertFalse(state.isPrepRoomsOpen)
        assertFalse(state.isChannelsOpen)
        assertFalse(state.isExploreTheCityOpen)
        assertFalse(state.isFlashDropsHubOpen)
        assertFalse(state.isSmartPlacesHubOpen)
        assertNull(state.flashDropForDetail)
        assertNull(state.flashDropForRoute)
    }

    @Test
    fun venuePreview_opensAndDismisses() {
        val state = DiscoverOverlayState()
        val target = venue()

        state.openVenuePreview(target)
        assertEquals(target, state.previewVenue)

        state.dismissVenuePreview()
        assertNull(state.previewVenue)
    }

    @Test
    fun myCircleHub_opensAndDismisses() {
        val state = DiscoverOverlayState()

        state.openMyCircleHub()
        assertTrue(state.isMyCircleHubOpen)

        state.dismissMyCircleHub()
        assertFalse(state.isMyCircleHubOpen)
    }

    @Test
    fun storyViewer_opensAtRequestedIndexAndDismisses() {
        val state = DiscoverOverlayState()

        state.openStoryViewer(3)
        assertEquals(3, state.storyViewerIndex)

        state.dismissStoryViewer()
        assertNull(state.storyViewerIndex)
    }

    @Test
    fun flashDropClaim_opensWithDropAndDismisses() {
        val state = DiscoverOverlayState()
        val drop = flashDrop()

        state.openFlashDropClaim(drop)
        assertEquals(drop, state.flashDropForClaim)

        state.dismissFlashDropClaim()
        assertNull(state.flashDropForClaim)
    }

    @Test
    fun globalPlanSheet_opensWithTargetNameAndDismisses() {
        val state = DiscoverOverlayState()

        state.openGlobalPlanSheet("The Rooftop")
        assertEquals("The Rooftop", state.globalPlanTarget)

        state.dismissGlobalPlanSheet()
        assertNull(state.globalPlanTarget)
    }

    @Test
    fun prepRooms_opensAndDismisses() {
        val state = DiscoverOverlayState()

        state.openPrepRooms()
        assertTrue(state.isPrepRoomsOpen)

        state.dismissPrepRooms()
        assertFalse(state.isPrepRoomsOpen)
    }

    @Test
    fun channels_opensAndDismisses() {
        val state = DiscoverOverlayState()

        state.openChannels()
        assertTrue(state.isChannelsOpen)

        state.dismissChannels()
        assertFalse(state.isChannelsOpen)
    }

    @Test
    fun exploreTheCity_opensAndDismisses() {
        val state = DiscoverOverlayState()

        state.openExploreTheCity()
        assertTrue(state.isExploreTheCityOpen)

        state.dismissExploreTheCity()
        assertFalse(state.isExploreTheCityOpen)
    }

    @Test
    fun flashDropsHub_opensAndDismisses() {
        val state = DiscoverOverlayState()

        state.openFlashDropsHub()
        assertTrue(state.isFlashDropsHubOpen)

        state.dismissFlashDropsHub()
        assertFalse(state.isFlashDropsHubOpen)
    }

    @Test
    fun smartPlacesHub_opensAndDismisses() {
        val state = DiscoverOverlayState()

        state.openSmartPlacesHub()
        assertTrue(state.isSmartPlacesHubOpen)

        state.dismissSmartPlacesHub()
        assertFalse(state.isSmartPlacesHubOpen)
    }

    @Test
    fun flashDropDetail_opensWithDropAndDismisses() {
        val state = DiscoverOverlayState()
        val drop = flashDrop("drop_2")

        state.openFlashDropDetail(drop)
        assertEquals(drop, state.flashDropForDetail)

        state.dismissFlashDropDetail()
        assertNull(state.flashDropForDetail)
    }

    @Test
    fun flashDropRoute_opensWithDropAndDismisses() {
        val state = DiscoverOverlayState()
        val drop = flashDrop("drop_3")

        state.openFlashDropRoute(drop)
        assertEquals(drop, state.flashDropForRoute)

        state.dismissFlashDropRoute()
        assertNull(state.flashDropForRoute)
    }

    @Test
    fun overlaySlots_areIndependent() {
        val state = DiscoverOverlayState()

        // Opening one overlay must not disturb another's state, otherwise
        // two independent flags could silently drift out of sync.
        state.openMyCircleHub()
        state.openChannels()
        state.openVenuePreview(venue())

        assertTrue(state.isMyCircleHubOpen)
        assertTrue(state.isChannelsOpen)
        assertEquals("venue_1", state.previewVenue?.id)

        state.dismissMyCircleHub()

        assertFalse(state.isMyCircleHubOpen)
        assertTrue("Dismissing one overlay should not affect others", state.isChannelsOpen)
        assertEquals("venue_1", state.previewVenue?.id)
    }

    @Test
    fun dismissAll_clearsEveryOverlayAndDialog() {
        val state = DiscoverOverlayState()

        state.openVenuePreview(venue())
        state.openMyCircleHub()
        state.openStoryViewer(1)
        state.openFlashDropClaim(flashDrop())
        state.openGlobalPlanSheet("Target")
        state.openPrepRooms()
        state.openChannels()
        state.openExploreTheCity()
        state.openFlashDropsHub()
        state.openSmartPlacesHub()
        state.openFlashDropDetail(flashDrop("drop_detail"))
        state.openFlashDropRoute(flashDrop("drop_route"))

        state.dismissAll()

        assertNull(state.previewVenue)
        assertFalse(state.isMyCircleHubOpen)
        assertNull(state.storyViewerIndex)
        assertNull(state.flashDropForClaim)
        assertNull(state.globalPlanTarget)
        assertFalse(state.isPrepRoomsOpen)
        assertFalse(state.isChannelsOpen)
        assertFalse(state.isExploreTheCityOpen)
        assertFalse(state.isFlashDropsHubOpen)
        assertFalse(state.isSmartPlacesHubOpen)
        assertNull(state.flashDropForDetail)
        assertNull(state.flashDropForRoute)
    }
}
