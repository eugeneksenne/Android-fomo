package com.example.feature.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * - maintaining overlay/dialog selection state;
 * - dispatching navigation callbacks;
 * - emitting high-level overlay analytics.
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

    var selectedPreviewVenue by remember { mutableStateOf<com.example.core.data.ExploreVenue?>(null) }
    var isMyCircleHubOpen by remember { mutableStateOf(false) }
    var selectedStoryIndex by remember { mutableStateOf<Int?>(null) }
    var selectedFlashDropForClaim by remember { mutableStateOf<com.example.core.data.FlashDrop?>(null) }
    var selectedGlobalPlanTarget by remember { mutableStateOf<String?>(null) }
    var isPrepRoomsOpen by remember { mutableStateOf(false) }
    var isChannelsOpen by remember { mutableStateOf(false) }
    var isExploreTheCityOpen by remember { mutableStateOf(false) }
    var isFlashDropsHubOpen by remember { mutableStateOf(false) }
    var isSmartPlacesHubOpen by remember { mutableStateOf(false) }
    var selectedFlashDropForDetail by remember { mutableStateOf<com.example.core.data.FlashDrop?>(null) }
    var selectedFlashDropForRoute by remember { mutableStateOf<com.example.core.data.FlashDrop?>(null) }

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
                ClosingSoonSection(onSeeAllClick = { isSmartPlacesHubOpen = true })
            }
            item(key = "spacer_closing_soon", contentType = "spacer") { SectionSpacer() }
            item(key = "flash_drops", contentType = "section") {
                FlashDropsSection(
                    flashDrops = globalFlashDrops,
                    isOnline = isOnline,
                    onSeeAllClick = { isFlashDropsHubOpen = true },
                    onClaimClick = { selectedFlashDropForDetail = it },
                    onRetry = retryConnectivity
                )
            }
            item(key = "spacer_flash_drops", contentType = "spacer") { SectionSpacer() }
            item(key = "my_circle", contentType = "section") {
                MyCircleSection(
                    stories = storiesState,
                    isOnline = isOnline,
                    onSeeAllClick = { isMyCircleHubOpen = true },
                    onStoryClick = { selectedStoryIndex = it },
                    onRetry = retryConnectivity
                )
            }
            item(key = "spacer_my_circle", contentType = "spacer") { SectionSpacer() }
            item(key = "live_moments", contentType = "section") {
                LiveMomentsSection(onSeeAllClick = { isMyCircleHubOpen = true })
            }
            item(key = "spacer_live_moments", contentType = "spacer") { SectionSpacer() }
            item(key = "smart_places", contentType = "section") {
                SmartPlacesSection(
                    venues = exploreVenues,
                    isOnline = isOnline,
                    onSeeAllClick = { isSmartPlacesHubOpen = true },
                    onVenueClick = { selectedPreviewVenue = it },
                    onRetry = retryConnectivity
                )
            }
            item(key = "spacer_smart_places", contentType = "spacer") { SectionSpacer() }
            item(key = "trending_now", contentType = "section") {
                TrendingNowSection(onSeeAllClick = { isExploreTheCityOpen = true })
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
                    onVenueClick = { selectedPreviewVenue = it },
                    onLikeToggle = { id -> VenueRepository.toggleLikeVenue(id) },
                    onSeeAllClick = { isExploreTheCityOpen = true },
                    onRetry = retryConnectivity
                )
            }
            item(key = "spacer_explore_the_city", contentType = "spacer") { SectionSpacer() }
            item(key = "channels", contentType = "section") {
                ChannelsSection(onOpenClick = { isChannelsOpen = true })
            }
            item(key = "spacer_channels", contentType = "spacer") { SectionSpacer() }
            item(key = "prep_rooms", contentType = "section") {
                PrepRoomsSection(onOpenClick = { isPrepRoomsOpen = true })
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

    if (selectedGlobalPlanTarget != null) {
        // ModalBottomSheet provides its own dismiss animation and back handling.
        GlobalPlanContextSheet(
            targetName = selectedGlobalPlanTarget!!,
            onDismiss = { selectedGlobalPlanTarget = null }
        )
    }

    if (selectedPreviewVenue != null) {
        val currentVenue = exploreVenues.find { it.id == selectedPreviewVenue?.id } ?: selectedPreviewVenue!!
        DiscoverOverlay("venue_preview") {
            VenuePreviewOverlay(
                venue = currentVenue,
                onDismiss = { selectedPreviewVenue = null },
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> VenueRepository.toggleLikeVenue(id) }
            )
        }
    }

    if (selectedStoryIndex != null) {
        DiscoverOverlay("story_viewer") {
            ImmersiveStoryViewer(
                stories = storiesState,
                initialIndex = selectedStoryIndex!!,
                onDismiss = { selectedStoryIndex = null },
                onNavigateToLobby = onNavigateToLobby
            )
        }
    }

    if (isMyCircleHubOpen) {
        DiscoverOverlay("my_circle_hub") {
            MyCircleHubOverlay(
                onDismiss = { isMyCircleHubOpen = false },
                onStoryClick = { index -> selectedStoryIndex = index },
                onNavigateToLobby = onNavigateToLobby,
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }
    }

    if (selectedFlashDropForClaim != null) {
        // AlertDialog provides its own back handling.
        FlashDropClaimDialog(
            drop = selectedFlashDropForClaim!!,
            onDismiss = { selectedFlashDropForClaim = null },
            onConfirmClaim = { id -> VenueRepository.claimGlobalFlashDrop(id) }
        )
    }

    if (isPrepRoomsOpen) {
        DiscoverOverlay("prep_rooms") {
            PrepRoomsOverlay(
                onDismiss = { isPrepRoomsOpen = false },
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }
    }

    if (isChannelsOpen) {
        DiscoverOverlay("channels") {
            ChannelsOverlay(
                onDismiss = { isChannelsOpen = false },
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }
    }

    if (isExploreTheCityOpen) {
        DiscoverOverlay("explore_the_city") {
            ExploreTheCityOverlay(
                venues = exploreVenues,
                onDismiss = { isExploreTheCityOpen = false },
                onSelectVenue = { selectedPreviewVenue = it },
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> VenueRepository.toggleLikeVenue(id) }
            )
        }
    }

    if (isFlashDropsHubOpen) {
        DiscoverOverlay("flash_drops_hub") {
            FlashDropsHubOverlay(
                flashDrops = globalFlashDrops,
                onDismiss = { isFlashDropsHubOpen = false },
                onSelectDrop = { drop -> selectedFlashDropForDetail = drop },
                onOpenRoute = { drop -> selectedFlashDropForRoute = drop },
                onClaimDrop = { id -> VenueRepository.claimGlobalFlashDrop(id) }
            )
        }
    }

    if (isSmartPlacesHubOpen) {
        DiscoverOverlay("smart_places_hub") {
            SmartPlacesHubOverlay(
                venues = exploreVenues,
                onDismiss = { isSmartPlacesHubOpen = false },
                onSelectVenue = { selectedPreviewVenue = it },
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> VenueRepository.toggleLikeVenue(id) }
            )
        }
    }

    if (selectedFlashDropForDetail != null) {
        DiscoverOverlay("flash_drop_detail") {
            FlashDropDetailOverlay(
                drop = selectedFlashDropForDetail!!,
                onDismiss = { selectedFlashDropForDetail = null },
                onNavigateToLobby = onNavigateToLobby,
                onNavigateToEventDetails = onNavigateToEventDetails,
                onOpenRoute = { drop -> selectedFlashDropForRoute = drop },
                onClaimConfirm = { id -> VenueRepository.claimGlobalFlashDrop(id) }
            )
        }
    }

    if (selectedFlashDropForRoute != null) {
        // AlertDialog provides its own back handling.
        FlashDropRouteDialog(
            drop = selectedFlashDropForRoute!!,
            onDismiss = { selectedFlashDropForRoute = null }
        )
    }
}

/**
 * Shared wrapper for full-screen Discover overlays.
 *
 * Provides:
 * - `discover_overlay_opened` / `discover_overlay_dismissed` analytics;
 * - a consistent fade + slide enter transition.
 *
 * System back handling is implemented inside each overlay via `BackHandler`,
 * so the overlay can run any exit logic before calling `onDismiss`.
 */
@Composable
private fun DiscoverOverlay(name: String, content: @Composable () -> Unit) {
    LaunchedEffect(name) { DiscoverAnalytics.overlayOpened(name) }
    DisposableEffect(name) {
        onDispose { DiscoverAnalytics.overlayDismissed(name) }
    }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { fullHeight -> fullHeight / 14 }
    ) {
        content()
    }
}
