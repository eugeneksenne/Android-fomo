package com.example.feature.map

import com.example.core.data.CircleFriend
import com.example.core.data.ExploreVenue

data class MapScreenState(
    val selectedCategory: String = "All",
    val selectedMapItem: SelectedMapItem? = null,
    val isHeatmapEnabled: Boolean = true,
    val bottomTabSelection: String = "Venues",
    val isSearchOpen: Boolean = false,
    val isNotificationsOpen: Boolean = false,
    val isProfileOpen: Boolean = false,
    val isAddPlaceOpen: Boolean = false,
    val activeWebsiteUrl: String? = null,
    val activeWebsiteTitle: String? = null,
    val cityStatusIndex: Int = 0,
)

sealed interface SelectedMapItem {
    data class Venue(val venue: ExploreVenue) : SelectedMapItem
    data class Friend(val friend: CircleFriend) : SelectedMapItem
}

fun MapScreenState.openSearch(): MapScreenState = copy(isSearchOpen = true)
fun MapScreenState.closeSearch(): MapScreenState = copy(isSearchOpen = false)
fun MapScreenState.openNotifications(): MapScreenState = copy(isNotificationsOpen = true)
fun MapScreenState.closeNotifications(): MapScreenState = copy(isNotificationsOpen = false)
fun MapScreenState.openProfile(): MapScreenState = copy(isProfileOpen = true)
fun MapScreenState.closeProfile(): MapScreenState = copy(isProfileOpen = false)
fun MapScreenState.openAddPlace(): MapScreenState = copy(isAddPlaceOpen = true)
fun MapScreenState.closeAddPlace(): MapScreenState = copy(isAddPlaceOpen = false)
fun MapScreenState.selectCategory(category: String): MapScreenState = copy(selectedCategory = category)
fun MapScreenState.selectMapItem(item: SelectedMapItem?): MapScreenState = copy(selectedMapItem = item)
fun MapScreenState.toggleHeatmap(): MapScreenState = copy(isHeatmapEnabled = !isHeatmapEnabled)
fun MapScreenState.selectBottomTab(tab: String): MapScreenState = copy(bottomTabSelection = tab)
fun MapScreenState.openWebsite(url: String, title: String): MapScreenState = copy(activeWebsiteUrl = url, activeWebsiteTitle = title)
fun MapScreenState.closeWebsite(): MapScreenState = copy(activeWebsiteUrl = null, activeWebsiteTitle = null)
fun MapScreenState.advanceCityStatus(): MapScreenState = copy(cityStatusIndex = (cityStatusIndex + 1) % 4)
