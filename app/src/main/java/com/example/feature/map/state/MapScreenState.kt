package com.example.feature.map.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.core.data.CircleFriend
import com.example.core.data.ExploreVenue

/**
 * Whatever is currently selected on the map: a venue pin or a friend pin.
 *
 * Moved from the top level of `MapScreen.kt` unchanged (same two variants,
 * same wrapped types) so every call site in the split-out modules can keep
 * referencing `SelectedMapItem.Venue` / `SelectedMapItem.Friend` exactly as
 * before.
 */
sealed interface SelectedMapItem {
    data class Venue(val venue: ExploreVenue) : SelectedMapItem
    data class Friend(val friend: CircleFriend) : SelectedMapItem
}

/** Which list the bottom carousel sheet is currently showing. */
enum class MapBottomTab(val label: String) {
    VENUES("🔥 Live Spots"),
    FRIENDS("👥 Friends Map")
}

/**
 * Centralised, declarative state holder for the Map screen.
 *
 * Before this type existed, `MapScreen` held ~14 independent
 * `mutableStateOf` fields directly in the composable body (selected
 * category, selected pin, heatmap toggle, bottom tab, four dialog/overlay
 * booleans, the website viewer url/title pair, and the city-status cycle
 * index) plus a `mutableStateListOf` for session-added venues. That mirrors
 * exactly the problem `DiscoverOverlayState` solved for the Discover screen:
 * a shell that's hard to scan and easy to leave inconsistent.
 *
 * `MapScreenState` collects all of that behind a small intent-style API
 * (`selectCategory`, `selectMapItem`, `openSearch`/`closeSearch`, etc). The
 * Map shell simply renders whatever this holds and dispatches intents;
 * business logic (filtering, ranking, marker generation) lives in
 * `feature/map/util`.
 */
class MapScreenState {

    // ------------------------------------------------------------------
    // Category filter + map selection
    // ------------------------------------------------------------------

    var selectedCategory by mutableStateOf("All")
        private set

    var selectedMapItem by mutableStateOf<SelectedMapItem?>(null)
        private set

    fun selectCategory(category: String) {
        selectedCategory = category
    }

    fun selectMapItem(item: SelectedMapItem?) {
        selectedMapItem = item
    }

    fun clearSelection() {
        selectedMapItem = null
    }

    // ------------------------------------------------------------------
    // Heatmap + bottom carousel tab
    // ------------------------------------------------------------------

    var isHeatmapEnabled by mutableStateOf(true)
        private set

    fun toggleHeatmap() {
        isHeatmapEnabled = !isHeatmapEnabled
    }

    var bottomTab by mutableStateOf(MapBottomTab.VENUES)
        private set

    fun selectBottomTab(tab: MapBottomTab) {
        bottomTab = tab
    }

    // ------------------------------------------------------------------
    // Dialog / overlay visibility
    // ------------------------------------------------------------------

    var isSearchOpen by mutableStateOf(false)
        private set

    fun openSearch() { isSearchOpen = true }
    fun closeSearch() { isSearchOpen = false }

    var isNotificationsOpen by mutableStateOf(false)
        private set

    fun openNotifications() { isNotificationsOpen = true }
    fun closeNotifications() { isNotificationsOpen = false }

    var isProfileOpen by mutableStateOf(false)
        private set

    fun openProfile() { isProfileOpen = true }
    fun closeProfile() { isProfileOpen = false }

    var isAddPlaceOpen by mutableStateOf(false)
        private set

    fun openAddPlace() { isAddPlaceOpen = true }
    fun closeAddPlace() { isAddPlaceOpen = false }

    // ------------------------------------------------------------------
    // Universal in-app website viewer sheet
    // ------------------------------------------------------------------

    var activeWebsiteUrl by mutableStateOf<String?>(null)
        private set

    var activeWebsiteTitle by mutableStateOf<String?>(null)
        private set

    fun openWebsite(url: String, title: String? = null) {
        activeWebsiteUrl = url
        activeWebsiteTitle = title
    }

    fun closeWebsite() {
        activeWebsiteUrl = null
        activeWebsiteTitle = null
    }

    // ------------------------------------------------------------------
    // Dynamic top-bar "city status" cycling badge
    // ------------------------------------------------------------------

    var cityStatusIndex by mutableIntStateOf(0)
        private set

    /** Advances the city status badge to the next entry in a status list of size [statusCount]. */
    fun cycleCityStatus(statusCount: Int) {
        if (statusCount <= 0) return
        cityStatusIndex = (cityStatusIndex + 1) % statusCount
    }

    // ------------------------------------------------------------------
    // Session-only venues added via the Add Place dialog
    // ------------------------------------------------------------------

    val customAddedVenues = mutableStateListOf<ExploreVenue>()

    fun addCustomVenue(venue: ExploreVenue) {
        customAddedVenues.add(venue)
    }

    /** Dismiss every dialog/overlay at once, e.g. before navigating away from Map. */
    fun dismissAll() {
        selectedMapItem = null
        isSearchOpen = false
        isNotificationsOpen = false
        isProfileOpen = false
        isAddPlaceOpen = false
        activeWebsiteUrl = null
        activeWebsiteTitle = null
    }
}

@Composable
fun rememberMapScreenState(): MapScreenState = remember { MapScreenState() }
