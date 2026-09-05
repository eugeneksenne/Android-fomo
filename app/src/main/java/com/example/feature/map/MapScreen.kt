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
import androidx.compose.runtime.LaunchedEffect
import com.example.feature.map.dialogs.RegionDownloadDialog
import com.example.feature.map.engine.FomoMapEngine
import com.example.feature.map.engine.toFomoVenue
import com.example.feature.map.overlays.NightguardOverlay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(
    onNavigateToLobby: (String) -> Unit = {},
    onNavigateToNightGuard: () -> Unit = {}
) {
    val context = LocalContext.current
    val staticVenues by VenueRepository.exploreVenuesState.collectAsState()
    val friends by MyCircleRepository.friendsState.collectAsState()

    val mapEngine = remember { FomoMapEngine() }
    val mapEngineState by mapEngine.state.collectAsState()

    val state = rememberMapScreenState()

    // Sync static venues to FOMO Map Engine
    LaunchedEffect(staticVenues) {
        val fomoVenues = staticVenues.mapIndexed { idx, venue ->
            val coords = getVenueCoordinates(venue.id)
            venue.toFomoVenue(coords.latitude, coords.longitude)
        }
        mapEngine.load(-26.146, 28.043, fomoVenues)
    }

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

        // 2. Top HUD Overlay: Top Bar, Nightguard, Categories, Nearest Venue Card
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0F19).copy(alpha = 0.95f),
                            Color(0xFF0B0F19).copy(alpha = 0.70f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            MapTopBar(
                avatarUrl = "https://i.pravatar.cc/150?img=12",
                fomoScore = 92,
                onSearchClick = state::openSearch,
                onNotificationsClick = state::openNotifications,
                onAvatarClick = state::openProfile
            )

            NightguardOverlay(
                zones = mapEngineState.nightguardZones,
                isNightguardActive = mapEngineState.isNightguardActive,
                onToggleNightguard = mapEngine::toggleNightguard,
                onSosClick = onNavigateToNightGuard
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
                    Toast.makeText(context, "Routing to ${nearestVenue.name}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 3. Floating action HUD positioned above the carousel on the right, below Nearest Venue Card
        MapFloatingButtons(
            onAddPlace = state::openAddPlace,
            onSosClick = onNavigateToNightGuard,
            onRecenter = { MarkerRenderer.recenterToCityDefault(webViewRef) },
            onDownloadMapClick = mapEngine::openRegionDownloadManager,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 220.dp, end = 12.dp)
        )

        // 4. Bottom HUD Overlay: Venue Preview (when selected) + Nearby Venues Carousel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF0B0F19).copy(alpha = 0.85f),
                            Color(0xFF0B0F19)
                        )
                    )
                )
        ) {
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

        if (mapEngineState.isRegionDownloadOpen) {
            RegionDownloadDialog(
                offlineMapEngine = mapEngine.offlineMapEngine,
                currentMode = mapEngineState.mode,
                onClose = mapEngine::closeRegionDownloadManager
            )
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
