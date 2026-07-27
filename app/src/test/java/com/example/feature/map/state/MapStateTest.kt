package com.example.feature.map.state

import com.example.core.data.CircleFriend
import com.example.core.data.ExploreVenue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MapScreenState]: intents, selected venue/friend, overlays,
 * the website viewer, and city status cycling.
 */
class MapStateTest {

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

    private fun friend(id: String = "friend_1") = CircleFriend(
        id = id,
        name = "Test Friend",
        username = "testfriend",
        avatarUrl = "",
        mutualFriendsCount = 3,
        currentActivity = "At the club",
        status = "Online",
        latitude = -26.1,
        longitude = 28.0
    )

    // ---------------------------------------------------------------------
    // Initial state
    // ---------------------------------------------------------------------

    @Test
    fun initialState_hasSensibleDefaults() {
        val state = MapScreenState()
        assertEquals("All", state.selectedCategory)
        assertNull(state.selectedMapItem)
        assertTrue(state.isHeatmapEnabled)
        assertEquals(MapBottomTab.VENUES, state.bottomTab)
        assertFalse(state.isSearchOpen)
        assertFalse(state.isNotificationsOpen)
        assertFalse(state.isProfileOpen)
        assertFalse(state.isAddPlaceOpen)
        assertNull(state.activeWebsiteUrl)
        assertNull(state.activeWebsiteTitle)
        assertEquals(0, state.cityStatusIndex)
        assertTrue(state.customAddedVenues.isEmpty())
    }

    // ---------------------------------------------------------------------
    // Category + selection intents
    // ---------------------------------------------------------------------

    @Test
    fun selectCategory_updatesSelectedCategory() {
        val state = MapScreenState()
        state.selectCategory("🌙 Nightlife")
        assertEquals("🌙 Nightlife", state.selectedCategory)
    }

    @Test
    fun selectMapItem_venue_setsSelectedMapItem() {
        val state = MapScreenState()
        val target = venue()
        state.selectMapItem(SelectedMapItem.Venue(target))
        assertEquals(target, (state.selectedMapItem as SelectedMapItem.Venue).venue)
    }

    @Test
    fun selectMapItem_friend_setsSelectedMapItem() {
        val state = MapScreenState()
        val target = friend()
        state.selectMapItem(SelectedMapItem.Friend(target))
        assertEquals(target, (state.selectedMapItem as SelectedMapItem.Friend).friend)
    }

    @Test
    fun clearSelection_clearsSelectedMapItem() {
        val state = MapScreenState()
        state.selectMapItem(SelectedMapItem.Venue(venue()))
        state.clearSelection()
        assertNull(state.selectedMapItem)
    }

    // ---------------------------------------------------------------------
    // Heatmap + bottom tab
    // ---------------------------------------------------------------------

    @Test
    fun toggleHeatmap_flipsHeatmapFlag() {
        val state = MapScreenState()
        assertTrue(state.isHeatmapEnabled)
        state.toggleHeatmap()
        assertFalse(state.isHeatmapEnabled)
        state.toggleHeatmap()
        assertTrue(state.isHeatmapEnabled)
    }

    @Test
    fun selectBottomTab_switchesBetweenVenuesAndFriends() {
        val state = MapScreenState()
        state.selectBottomTab(MapBottomTab.FRIENDS)
        assertEquals(MapBottomTab.FRIENDS, state.bottomTab)
        state.selectBottomTab(MapBottomTab.VENUES)
        assertEquals(MapBottomTab.VENUES, state.bottomTab)
    }

    // ---------------------------------------------------------------------
    // Dialog / overlay intents
    // ---------------------------------------------------------------------

    @Test
    fun search_opensAndCloses() {
        val state = MapScreenState()
        state.openSearch()
        assertTrue(state.isSearchOpen)
        state.closeSearch()
        assertFalse(state.isSearchOpen)
    }

    @Test
    fun notifications_openAndClose() {
        val state = MapScreenState()
        state.openNotifications()
        assertTrue(state.isNotificationsOpen)
        state.closeNotifications()
        assertFalse(state.isNotificationsOpen)
    }

    @Test
    fun profile_opensAndCloses() {
        val state = MapScreenState()
        state.openProfile()
        assertTrue(state.isProfileOpen)
        state.closeProfile()
        assertFalse(state.isProfileOpen)
    }

    @Test
    fun addPlace_opensAndCloses() {
        val state = MapScreenState()
        state.openAddPlace()
        assertTrue(state.isAddPlaceOpen)
        state.closeAddPlace()
        assertFalse(state.isAddPlaceOpen)
    }

    @Test
    fun overlaySlots_areIndependent() {
        val state = MapScreenState()
        state.openSearch()
        state.openNotifications()
        assertTrue(state.isSearchOpen)
        assertTrue(state.isNotificationsOpen)

        state.closeSearch()
        assertFalse(state.isSearchOpen)
        assertTrue("Closing one dialog should not affect another", state.isNotificationsOpen)
    }

    // ---------------------------------------------------------------------
    // Website viewer
    // ---------------------------------------------------------------------

    @Test
    fun website_opensWithUrlAndOptionalTitle() {
        val state = MapScreenState()
        state.openWebsite("https://fomoapp.live", "FOMO")
        assertEquals("https://fomoapp.live", state.activeWebsiteUrl)
        assertEquals("FOMO", state.activeWebsiteTitle)
    }

    @Test
    fun website_closes_clearsUrlAndTitle() {
        val state = MapScreenState()
        state.openWebsite("https://fomoapp.live", "FOMO")
        state.closeWebsite()
        assertNull(state.activeWebsiteUrl)
        assertNull(state.activeWebsiteTitle)
    }

    // ---------------------------------------------------------------------
    // City status cycling
    // ---------------------------------------------------------------------

    @Test
    fun cycleCityStatus_wrapsAroundStatusCount() {
        val state = MapScreenState()
        assertEquals(0, state.cityStatusIndex)
        state.cycleCityStatus(4)
        assertEquals(1, state.cityStatusIndex)
        state.cycleCityStatus(4)
        state.cycleCityStatus(4)
        state.cycleCityStatus(4)
        assertEquals(0, state.cityStatusIndex)
    }

    @Test
    fun cycleCityStatus_zeroStatusCount_isNoOp() {
        val state = MapScreenState()
        state.cycleCityStatus(0)
        assertEquals(0, state.cityStatusIndex)
    }

    // ---------------------------------------------------------------------
    // Session-added venues
    // ---------------------------------------------------------------------

    @Test
    fun addCustomVenue_appendsToCustomAddedVenues() {
        val state = MapScreenState()
        val newVenue = venue("custom_1")
        state.addCustomVenue(newVenue)
        assertEquals(1, state.customAddedVenues.size)
        assertEquals(newVenue, state.customAddedVenues.first())
    }

    // ---------------------------------------------------------------------
    // dismissAll
    // ---------------------------------------------------------------------

    @Test
    fun dismissAll_clearsSelectionAndEveryDialogAndWebsiteViewer() {
        val state = MapScreenState()
        state.selectMapItem(SelectedMapItem.Venue(venue()))
        state.openSearch()
        state.openNotifications()
        state.openProfile()
        state.openAddPlace()
        state.openWebsite("https://fomoapp.live")

        state.dismissAll()

        assertNull(state.selectedMapItem)
        assertFalse(state.isSearchOpen)
        assertFalse(state.isNotificationsOpen)
        assertFalse(state.isProfileOpen)
        assertFalse(state.isAddPlaceOpen)
        assertNull(state.activeWebsiteUrl)
        assertNull(state.activeWebsiteTitle)
    }
}
