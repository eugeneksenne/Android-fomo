package com.example.feature.discover

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

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
    val eventsState by com.example.core.data.EventRepository.eventsState.collectAsState()
    val exploreVenues by com.example.core.data.VenueRepository.exploreVenuesState.collectAsState()
    val storiesState by com.example.core.data.MyCircleRepository.storiesState.collectAsState()
    val globalFlashDrops by com.example.core.data.VenueRepository.globalFlashDropsState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationRepository = remember { com.example.core.data.notification.NotificationRepository.getInstance(context) }
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
            item { HeroSection() }
            item { SectionSpacer() }
            item { ClosingSoonSection(onSeeAllClick = { isSmartPlacesHubOpen = true }) }
            item { SectionSpacer() }
            item {
                FlashDropsSection(
                    flashDrops = globalFlashDrops,
                    onSeeAllClick = { isFlashDropsHubOpen = true },
                    onClaimClick = { selectedFlashDropForDetail = it }
                )
            }
            item { SectionSpacer() }
            item {
                MyCircleSection(
                    stories = storiesState,
                    onSeeAllClick = { isMyCircleHubOpen = true },
                    onStoryClick = { selectedStoryIndex = it }
                )
            }
            item { SectionSpacer() }
            item { LiveMomentsSection(onSeeAllClick = { isMyCircleHubOpen = true }) }
            item { SectionSpacer() }
            item {
                SmartPlacesSection(
                    venues = exploreVenues,
                    onSeeAllClick = { isSmartPlacesHubOpen = true },
                    onVenueClick = { selectedPreviewVenue = it }
                )
            }
            item { SectionSpacer() }
            item { TrendingNowSection(onSeeAllClick = { isExploreTheCityOpen = true }) }
            item { SectionSpacer() }
            item { EventsSection(eventsState, onNavigateToEvents, onNavigateToEventDetails) }
            item { SectionSpacer() }
            item {
                ExploreTheCitySection(
                    venues = exploreVenues,
                    onVenueClick = { selectedPreviewVenue = it },
                    onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) },
                    onSeeAllClick = { isExploreTheCityOpen = true }
                )
            }
            item { SectionSpacer() }
            item { ChannelsSection(onOpenClick = { isChannelsOpen = true }) }
            item { SectionSpacer() }
            item { PrepRoomsSection(onOpenClick = { isPrepRoomsOpen = true }) }
            item { SectionSpacer() }
            item { TonightSection(onNavigateToNightGuard = onNavigateToNightGuard, onNavigateToPlansWorkspace = onNavigateToPlansWorkspace) }
        }
    }

    if (selectedGlobalPlanTarget != null) {
        GlobalPlanContextSheet(
            targetName = selectedGlobalPlanTarget!!,
            onDismiss = { selectedGlobalPlanTarget = null }
        )
    }

    if (selectedPreviewVenue != null) {
        val currentVenue = exploreVenues.find { it.id == selectedPreviewVenue?.id } ?: selectedPreviewVenue!!
        VenuePreviewOverlay(
            venue = currentVenue,
            onDismiss = { selectedPreviewVenue = null },
            onNavigateToLobby = onNavigateToLobby,
            onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) }
        )
    }

    if (selectedStoryIndex != null) {
        ImmersiveStoryViewer(
            stories = storiesState,
            initialIndex = selectedStoryIndex!!,
            onDismiss = { selectedStoryIndex = null },
            onNavigateToLobby = onNavigateToLobby
        )
    }

    if (isMyCircleHubOpen) {
        MyCircleHubOverlay(
            onDismiss = { isMyCircleHubOpen = false },
            onStoryClick = { index -> selectedStoryIndex = index },
            onNavigateToLobby = onNavigateToLobby,
            onNavigateToEventDetails = onNavigateToEventDetails
        )
    }

    if (selectedFlashDropForClaim != null) {
        FlashDropClaimDialog(
            drop = selectedFlashDropForClaim!!,
            onDismiss = { selectedFlashDropForClaim = null },
            onConfirmClaim = { id -> com.example.core.data.VenueRepository.claimGlobalFlashDrop(id) }
        )
    }

    if (isPrepRoomsOpen) {
        PrepRoomsOverlay(
            onDismiss = { isPrepRoomsOpen = false },
            onNavigateToEventDetails = onNavigateToEventDetails
        )
    }

    if (isChannelsOpen) {
        ChannelsOverlay(
            onDismiss = { isChannelsOpen = false },
            onNavigateToEventDetails = onNavigateToEventDetails
        )
    }

    if (isExploreTheCityOpen) {
        ExploreTheCityOverlay(
            venues = exploreVenues,
            onDismiss = { isExploreTheCityOpen = false },
            onSelectVenue = { selectedPreviewVenue = it },
            onNavigateToLobby = onNavigateToLobby,
            onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) }
        )
    }

    if (isFlashDropsHubOpen) {
        FlashDropsHubOverlay(
            flashDrops = globalFlashDrops,
            onDismiss = { isFlashDropsHubOpen = false },
            onSelectDrop = { drop -> selectedFlashDropForDetail = drop },
            onOpenRoute = { drop -> selectedFlashDropForRoute = drop },
            onClaimDrop = { id -> com.example.core.data.VenueRepository.claimGlobalFlashDrop(id) }
        )
    }

    if (isSmartPlacesHubOpen) {
        SmartPlacesHubOverlay(
            venues = exploreVenues,
            onDismiss = { isSmartPlacesHubOpen = false },
            onSelectVenue = { selectedPreviewVenue = it },
            onNavigateToLobby = onNavigateToLobby,
            onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) }
        )
    }

    if (selectedFlashDropForDetail != null) {
        FlashDropDetailOverlay(
            drop = selectedFlashDropForDetail!!,
            onDismiss = { selectedFlashDropForDetail = null },
            onNavigateToLobby = onNavigateToLobby,
            onNavigateToEventDetails = onNavigateToEventDetails,
            onOpenRoute = { drop -> selectedFlashDropForRoute = drop },
            onClaimConfirm = { id -> com.example.core.data.VenueRepository.claimGlobalFlashDrop(id) }
        )
    }

    if (selectedFlashDropForRoute != null) {
        FlashDropRouteDialog(
            drop = selectedFlashDropForRoute!!,
            onDismiss = { selectedFlashDropForRoute = null }
        )
    }
}
