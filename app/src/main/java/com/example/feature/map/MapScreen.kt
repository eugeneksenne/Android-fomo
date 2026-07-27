package com.example.feature.map

import android.annotation.SuppressLint
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.data.MyCircleRepository
import com.example.core.data.VenueRepository
import com.example.feature.map.cards.NearbyVenueCarousel
import com.example.feature.map.cards.NearestVenueCard
import com.example.feature.map.components.CountryPackChips
import com.example.feature.map.components.MapFloatingButtons
import com.example.feature.map.components.MapTopBar
import com.example.feature.map.dialogs.AddPlaceOverlayDialog
import com.example.feature.map.dialogs.NotificationsOverlayDialog
import com.example.feature.map.dialogs.SearchOverlayDialog
import com.example.feature.map.dialogs.UserProfileOverlayDialog
import com.example.feature.map.map.MarkerRenderer
import com.example.feature.map.map.VenueMapCanvas
import com.example.feature.map.overlays.VenuePreviewOverlay
import com.example.feature.map.overlays.WebsiteViewer
import com.example.feature.map.state.SelectedMapItem
import com.example.feature.map.state.rememberMapScreenState
import com.example.feature.map.util.VenueFilter
import com.example.feature.map.util.VenueRanking
import com.example.feature.map.util.getVenueCoordinates

/**
 * FOMO Map route shell - the "venue discovery engine" screen.
 *
 * Responsibilities are intentionally limited to:
 * - collecting repository state (venues, friends) and combining it with
 *   session-only added venues;
 * - deriving the category-filtered venue list and nearest-venue
 *   recommendation via `feature/map/util`;
 * - rendering the HUD layout (top bar, chips, nearest card, map canvas,
 *   floating buttons, bottom carousel) with each piece as a dedicated
 *   component/card;
 * - dispatching intents into `MapScreenState` and issuing `WebView` JS
 *   commands via `MarkerRenderer`;
 * - hosting dialogs/overlays.
 *
 * All WebView/Leaflet code lives in `feature/map/map/`, all card rendering
 * lives in `feature/map/cards/`, all dialogs live in `feature/map/dialogs/`,
 * and all filtering/ranking/marker-generation logic lives in
 * `feature/map/util/`. See `docs/MAP_ARCHITECTURE.md`.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(
    onNavigateToLobby: (String) -> Unit = {},
    onNavigateToNightGuard: () -> Unit = {}
) {
    val context = LocalContext.current
    val staticVenues by VenueRepository.exploreVenuesState.collectAsState()
    val friends by MyCircleRepository.friendsState.collectAsState()

    val state = rememberMapScreenState()

    // Custom session-added venues (from the Add Place dialog) layer on top
    // of the repository-backed venue list for the lifetime of this screen.
    val allVenues = remember(staticVenues, state.customAddedVenues) {
        staticVenues + state.customAddedVenues
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val filteredVenues = remember(state.selectedCategory, allVenues) {
        VenueFilter.filterByCategory(allVenues, state.selectedCategory)
    }
    val nearestVenue = remember(state.selectedCategory, filteredVenues) {
        VenueRanking.nearestVenue(filteredVenues)
    }

    val cityStatuses = remember {
        listOf(
            "🔥 Johannesburg Pulse",
            "⚡ Friday Night Vibes",
            "🌙 Sandton Nightlife",
            "🟢 NightGuard Active"
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0F19))) {

        // 1. Interactive OSM base layer.
        VenueMapCanvas(
            venues = allVenues,
            friends = friends,
            onVenueSelected = { venue ->
                state.selectMapItem(SelectedMapItem.Venue(venue))
                MarkerRenderer.centerOnVenue(webViewRef, venue)
            },
            onFriendSelected = { friend ->
                state.selectMapItem(SelectedMapItem.Friend(friend))
                MarkerRenderer.centerOn(webViewRef, friend.latitude, friend.longitude)
            },
            onOverpassResult = { count, category ->
                val message = if (count > 0) {
                    "🌐 OSM Overpass API: Live added $count $category spots!"
                } else {
                    "🌐 OpenStreetMap Overpass API synced."
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            onWebViewReady = { webViewRef = it },
            modifier = Modifier.fillMaxSize()
        )

        // 2. HUD overlay elements (top-down flow).
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0B0F19).copy(alpha = 0.95f), Color.Transparent)
                        )
                    )
            ) {
                MapTopBar(
                    avatarUrl = "https://i.pravatar.cc/150?img=12",
                    cityStatus = cityStatuses[state.cityStatusIndex],
                    onCityStatusClick = {
                        state.cycleCityStatus(cityStatuses.size)
                        Toast.makeText(context, "Vibe Context: ${cityStatuses[state.cityStatusIndex]}", Toast.LENGTH_SHORT).show()
                    },
                    onSearchClick = state::openSearch,
                    onNotificationsClick = state::openNotifications,
                    onAvatarClick = state::openProfile
                )

                CountryPackChips(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { category ->
                        state.selectCategory(category)
                        MarkerRenderer.filterCategoryAndFetchOverpass(webViewRef, category)
                    }
                )

                NearestVenueCard(
                    venue = nearestVenue,
                    categoryLabel = state.selectedCategory,
                    onNavigateToLobby = onNavigateToLobby,
                    onRouteClick = {
                        MarkerRenderer.drawRouteToVenue(webViewRef, nearestVenue)
                        Toast.makeText(context, "Simulating premium walking route to ${nearestVenue.name}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            VenuePreviewOverlay(
                selectedItem = state.selectedMapItem,
                onNavigateToLobby = onNavigateToLobby,
                onDismiss = {
                    state.clearSelection()
                    MarkerRenderer.clearRoute(webViewRef)
                },
                onRouteToVenue = { selected -> MarkerRenderer.drawRouteToVenue(webViewRef, selected.venue) }
            )

            NearbyVenueCarousel(
                bottomTab = state.bottomTab,
                onTabSelected = state::selectBottomTab,
                friends = friends,
                filteredVenues = filteredVenues,
                selectedMapItem = state.selectedMapItem,
                onSelectVenue = { venue ->
                    state.selectMapItem(SelectedMapItem.Venue(venue))
                    MarkerRenderer.centerOnVenue(webViewRef, venue)
                },
                onSelectFriend = { friend ->
                    state.selectMapItem(SelectedMapItem.Friend(friend))
                    MarkerRenderer.centerOn(webViewRef, friend.latitude, friend.longitude)
                },
                onNavigateToLobby = onNavigateToLobby,
                onRouteToVenue = { venue ->
                    MarkerRenderer.drawRouteToVenue(webViewRef, venue)
                    Toast.makeText(context, "Routing to ${venue.name}...", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 3. Floating action HUD.
        MapFloatingButtons(
            isHeatmapEnabled = state.isHeatmapEnabled,
            onQueryOverpass = {
                Toast.makeText(context, "Querying OpenStreetMap Overpass API...", Toast.LENGTH_SHORT).show()
                MarkerRenderer.fetchOverpassPOIs(webViewRef, state.selectedCategory)
            },
            onAddPlace = state::openAddPlace,
            onSosClick = onNavigateToNightGuard,
            onToggleHeatmap = {
                state.toggleHeatmap()
                MarkerRenderer.toggleHeatmap(webViewRef, state.isHeatmapEnabled)
            },
            onRecenter = { MarkerRenderer.recenterToCityDefault(webViewRef) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 295.dp, end = 16.dp)
        )

        // --- Dialogs & overlays ---

        if (state.isSearchOpen) {
            SearchOverlayDialog(
                venues = allVenues,
                friends = friends,
                onClose = state::closeSearch,
                onSelectItem = { item ->
                    state.closeSearch()
                    state.selectMapItem(item)
                    when (item) {
                        is SelectedMapItem.Venue -> {
                            val coords = getVenueCoordinates(item.venue.id)
                            MarkerRenderer.centerAndDrawRouteTo(webViewRef, coords.latitude, coords.longitude)
                        }
                        is SelectedMapItem.Friend -> {
                            MarkerRenderer.centerOn(webViewRef, item.friend.latitude, item.friend.longitude)
                        }
                    }
                }
            )
        }

        if (state.isNotificationsOpen) {
            NotificationsOverlayDialog(
                onClose = state::closeNotifications,
                onLocateVenue = { venueId ->
                    state.closeNotifications()
                    val matched = allVenues.find { it.id == venueId }
                    if (matched != null) {
                        state.selectMapItem(SelectedMapItem.Venue(matched))
                        val coords = getVenueCoordinates(venueId)
                        MarkerRenderer.centerAndDrawRouteTo(webViewRef, coords.latitude, coords.longitude)
                    }
                }
            )
        }

        if (state.isProfileOpen) {
            UserProfileOverlayDialog(onClose = state::closeProfile)
        }

        WebsiteViewer(
            url = state.activeWebsiteUrl,
            title = state.activeWebsiteTitle,
            onDismiss = state::closeWebsite
        )

        if (state.isAddPlaceOpen) {
            AddPlaceOverlayDialog(
                onClose = state::closeAddPlace,
                onSubmitVenue = { newVenue ->
                    state.addCustomVenue(newVenue)
                    state.closeAddPlace()
                    MarkerRenderer.addCustomVenueMarker(webViewRef, newVenue)
                    Toast.makeText(context, "📍 Successfully posted ${newVenue.name} live on FOMO Map!", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
