package com.example.feature.discover

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.data.EventRepository
import com.example.core.data.MyCircleRepository
import com.example.core.data.NetworkMonitor
import com.example.core.data.VenueRepository
import com.example.core.data.notification.NotificationRepository

/**
 * Discover orchestration shell.
 *
 * Responsibilities are intentionally limited to:
 * - collecting repository state;
 * - rendering the 12-section ordering with stable keys;
 * - dispatching section intents into [DiscoverOverlayState];
 * - delegating overlay/dialog presentation to [DiscoverOverlayHost].
 *
 * All overlay/dialog *selection* state lives in [DiscoverOverlayState] so this
 * file stays a thin renderer: sections emit intents (`overlayState::openX`)
 * instead of the shell owning a dozen independent `mutableStateOf` flags.
 *
 * All card rendering, business rules and state visuals live in the
 * `components/`, `sections/`, `overlays/` and `dialogs/` modules.
 */
@Composable
fun DiscoverScreen(
    onProfileClick: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToEventDetails: (String) -> Unit = {},
    onNavigateToLobby: (String) -> Unit = {},
    onNavigateToNightGuard: () -> Unit = {},
    onNavigateToCountryPackHub: () -> Unit = {},
    onNavigateToPlansWorkspace: () -> Unit = {}
) {
    val context = LocalContext.current

    // Initialise the process-wide connectivity monitor once and observe it.
    LaunchedEffect(Unit) { NetworkMonitor.init(context) }
    val isOnline by NetworkMonitor.isOnline.collectAsState()
    val retryConnectivity: () -> Unit = remember(context) { { NetworkMonitor.refresh(context) } }

    val eventsState by EventRepository.eventsState.collectAsState()
    val exploreVenues by VenueRepository.exploreVenuesState.collectAsState()
    val storiesState by MyCircleRepository.storiesState.collectAsState()
    val globalFlashDrops by VenueRepository.globalFlashDropsState.collectAsState()

    val notificationRepository = remember { NotificationRepository.getInstance(context) }
    val unreadCount by notificationRepository.unreadCount.collectAsState(initial = 0)

    // Warm Coil caches for hero + first-viewport imagery.
    DiscoverImagePrefetcher(venues = exploreVenues, events = eventsState, stories = storiesState)

    val overlayState = rememberDiscoverOverlayState()

    Scaffold(
        topBar = { DiscoverTopBar(unreadCount = unreadCount, onProfileClick = onProfileClick) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "hero", contentType = "hero") { HeroSection() }
            item(key = "spacer_hero", contentType = "spacer") { SectionSpacer() }
            item(key = "closing_soon", contentType = "section") {
                ClosingSoonSection(onSeeAllClick = overlayState::openSmartPlacesHub)
            }
            item(key = "spacer_closing_soon", contentType = "spacer") { SectionSpacer() }
            item(key = "flash_drops", contentType = "section") {
                FlashDropsSection(
                    flashDrops = globalFlashDrops,
                    isOnline = isOnline,
                    onSeeAllClick = overlayState::openFlashDropsHub,
                    onClaimClick = overlayState::openFlashDropDetail,
                    onRetry = retryConnectivity
                )
            }
            item(key = "spacer_flash_drops", contentType = "spacer") { SectionSpacer() }
            item(key = "my_circle", contentType = "section") {
                MyCircleSection(
                    stories = storiesState,
                    isOnline = isOnline,
                    onSeeAllClick = overlayState::openMyCircleHub,
                    onStoryClick = overlayState::openStoryViewer,
                    onRetry = retryConnectivity
                )
            }
            item(key = "spacer_my_circle", contentType = "spacer") { SectionSpacer() }
            item(key = "live_moments", contentType = "section") {
                LiveMomentsSection(onSeeAllClick = overlayState::openMyCircleHub)
            }
            item(key = "spacer_live_moments", contentType = "spacer") { SectionSpacer() }
            item(key = "smart_places", contentType = "section") {
                SmartPlacesSection(
                    venues = exploreVenues,
                    isOnline = isOnline,
                    onSeeAllClick = overlayState::openSmartPlacesHub,
                    onVenueClick = overlayState::openVenuePreview,
                    onRetry = retryConnectivity
                )
            }
            item(key = "spacer_smart_places", contentType = "spacer") { SectionSpacer() }
            item(key = "trending_now", contentType = "section") {
                TrendingNowSection(onSeeAllClick = overlayState::openExploreTheCity)
            }
            item(key = "spacer_trending_now", contentType = "spacer") { SectionSpacer() }
            item(key = "events", contentType = "section") {
                EventsSection(
                    events = eventsState,
                    isOnline = isOnline,
                    onNavigateToEvents = onNavigateToEvents,
                    onNavigateToEventDetails = onNavigateToEventDetails,
                    onRetry = retryConnectivity
                )
            }
            item(key = "spacer_events", contentType = "spacer") { SectionSpacer() }
            item(key = "explore_the_city", contentType = "section") {
                ExploreTheCitySection(
                    venues = exploreVenues,
                    isOnline = isOnline,
                    onVenueClick = overlayState::openVenuePreview,
                    onLikeToggle = { id -> VenueRepository.toggleLikeVenue(id) },
                    onSeeAllClick = overlayState::openExploreTheCity,
                    onRetry = retryConnectivity
                )
            }
            item(key = "spacer_explore_the_city", contentType = "spacer") { SectionSpacer() }
            item(key = "channels", contentType = "section") {
                ChannelsSection(onOpenClick = overlayState::openChannels)
            }
            item(key = "spacer_channels", contentType = "spacer") { SectionSpacer() }
            item(key = "prep_rooms", contentType = "section") {
                PrepRoomsSection(onOpenClick = overlayState::openPrepRooms)
            }
            item(key = "spacer_prep_rooms", contentType = "spacer") { SectionSpacer() }
            item(key = "tonight", contentType = "section") {
                TonightSection(
                    onNavigateToNightGuard = onNavigateToNightGuard,
                    onNavigateToPlansWorkspace = onNavigateToPlansWorkspace
                )
            }
        }
    }

    DiscoverOverlayHost(
        overlayState = overlayState,
        exploreVenues = exploreVenues,
        storiesState = storiesState,
        globalFlashDrops = globalFlashDrops,
        onNavigateToLobby = onNavigateToLobby,
        onNavigateToEventDetails = onNavigateToEventDetails
    )
}
