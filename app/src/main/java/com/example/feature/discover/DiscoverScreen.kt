package com.example.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import java.util.Calendar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
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

    var selectedPreviewVenueId by rememberSaveable { mutableStateOf<String?>(null) }
    var isMyCircleHubOpen by rememberSaveable { mutableStateOf(false) }
    var selectedStoryIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedFlashDropForClaimId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedGlobalPlanTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var isPrepRoomsOpen by rememberSaveable { mutableStateOf(false) }
    var isChannelsOpen by rememberSaveable { mutableStateOf(false) }
    var isExploreTheCityOpen by rememberSaveable { mutableStateOf(false) }
    var isFlashDropsHubOpen by rememberSaveable { mutableStateOf(false) }
    var isSmartPlacesHubOpen by rememberSaveable { mutableStateOf(false) }
    var selectedFlashDropForDetailId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFlashDropForRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPreviewVenue = remember(selectedPreviewVenueId, exploreVenues) {
        selectedPreviewVenueId?.let { id -> exploreVenues.find { it.id == id } }
    }
    val selectedFlashDropForClaim = remember(selectedFlashDropForClaimId, globalFlashDrops) {
        selectedFlashDropForClaimId?.let { id -> globalFlashDrops.find { it.id == id } }
    }
    val selectedFlashDropForDetail = remember(selectedFlashDropForDetailId, globalFlashDrops) {
        selectedFlashDropForDetailId?.let { id -> globalFlashDrops.find { it.id == id } }
    }
    val selectedFlashDropForRoute = remember(selectedFlashDropForRouteId, globalFlashDrops) {
        selectedFlashDropForRouteId?.let { id -> globalFlashDrops.find { it.id == id } }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { DiscoverTopBar(unreadCount = unreadCount, onProfileClick = { DiscoverAnalytics.actionClicked("profile_opened"); onProfileClick() }) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "discover_hero", contentType = "hero") { HeroSection() }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "closing_soon", contentType = "section") { ClosingSoonSection(onSeeAllClick = { DiscoverAnalytics.overlayOpened("smart_places", "closing_soon"); isSmartPlacesHubOpen = true }) }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "flash_drops", contentType = "section") {
                    FlashDropsSection(
                        flashDrops = globalFlashDrops,
                        onSeeAllClick = { DiscoverAnalytics.overlayOpened("flash_drops", "flash_drops"); isFlashDropsHubOpen = true },
                        onClaimClick = { DiscoverAnalytics.cardOpened("Flash Drops", it.id, "flash_drop"); selectedFlashDropForDetailId = it.id }
                    )
                }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "my_circle", contentType = "section") {
                    MyCircleSection(
                        stories = storiesState,
                        onSeeAllClick = { DiscoverAnalytics.overlayOpened("my_circle", "my_circle"); isMyCircleHubOpen = true },
                        onStoryClick = { DiscoverAnalytics.cardOpened("My Circle", it.toString(), "story"); selectedStoryIndex = it }
                    )
                }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "live_moments", contentType = "section") { LiveMomentsSection(onSeeAllClick = { DiscoverAnalytics.overlayOpened("my_circle", "live_moments"); isMyCircleHubOpen = true }) }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "smart_places", contentType = "section") {
                    SmartPlacesSection(
                        venues = exploreVenues,
                        onSeeAllClick = { DiscoverAnalytics.overlayOpened("smart_places", "smart_places"); isSmartPlacesHubOpen = true },
                        onVenueClick = { DiscoverAnalytics.cardOpened("Smart Places", it.id, "venue"); selectedPreviewVenueId = it.id }
                    )
                }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "trending_now", contentType = "section") { TrendingNowSection(onSeeAllClick = { DiscoverAnalytics.overlayOpened("explore_city", "trending_now"); isExploreTheCityOpen = true }) }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "events", contentType = "section") { EventsSection(eventsState, { DiscoverAnalytics.seeAllClicked("Events"); onNavigateToEvents() }, { id -> DiscoverAnalytics.cardOpened("Events", id, "event"); onNavigateToEventDetails(id) }) }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "explore_city", contentType = "section") {
                    ExploreTheCitySection(
                        venues = exploreVenues,
                        onVenueClick = { DiscoverAnalytics.cardOpened("Explore The City", it.id, "venue"); selectedPreviewVenueId = it.id },
                        onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) },
                        onSeeAllClick = { DiscoverAnalytics.overlayOpened("explore_city", "explore_city"); isExploreTheCityOpen = true }
                    )
                }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "channels", contentType = "section") { ChannelsSection(onOpenClick = { DiscoverAnalytics.overlayOpened("channels", "channels"); isChannelsOpen = true }) }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "prep_rooms", contentType = "section") { PrepRoomsSection(onOpenClick = { DiscoverAnalytics.overlayOpened("prep_rooms", "prep_rooms"); isPrepRoomsOpen = true }) }
                item(contentType = "spacer") { SectionSpacer() }
                item(key = "tonight", contentType = "section") { TonightSection(onNavigateToNightGuard = onNavigateToNightGuard, onNavigateToPlansWorkspace = onNavigateToPlansWorkspace) }
            }
        }

        // Global Contextual Plan Sheet Overlay
        if (selectedGlobalPlanTarget != null) {
            GlobalPlanContextSheet(
                targetName = selectedGlobalPlanTarget!!,
                onDismiss = { DiscoverAnalytics.overlayDismissed("global_plan_context"); selectedGlobalPlanTarget = null }
            )
        }

        // Stage 2 Venue Preview System Overlay
        if (selectedPreviewVenue != null) {
            val currentVenue = exploreVenues.find { it.id == selectedPreviewVenueId } ?: selectedPreviewVenue
            VenuePreviewOverlay(
                venue = currentVenue,
                onDismiss = { DiscoverAnalytics.overlayDismissed("venue_preview"); selectedPreviewVenueId = null },
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) }
            )
        }

        // Immersive Story Viewer
        if (selectedStoryIndex != null) {
            ImmersiveStoryViewer(
                stories = storiesState,
                initialIndex = selectedStoryIndex!!,
                onDismiss = { DiscoverAnalytics.overlayDismissed("story_viewer"); selectedStoryIndex = null },
                onNavigateToLobby = onNavigateToLobby
            )
        }

        // My Circle Social Hub (Immersive Full-Screen Overlay)
        if (isMyCircleHubOpen) {
            MyCircleHubOverlay(
                onDismiss = { DiscoverAnalytics.overlayDismissed("my_circle"); isMyCircleHubOpen = false },
                onStoryClick = { index ->
                    selectedStoryIndex = index
                },
                onNavigateToLobby = onNavigateToLobby,
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }

        // Flash Drop Claim Dialog Overlay
        if (selectedFlashDropForClaim != null) {
            FlashDropClaimDialog(
                drop = selectedFlashDropForClaim,
                onDismiss = { DiscoverAnalytics.overlayDismissed("flash_drop_claim"); selectedFlashDropForClaimId = null },
                onConfirmClaim = { id ->
                    com.example.core.data.VenueRepository.claimGlobalFlashDrop(id)
                }
            )
        }

        // Prep Rooms Dedicated Experience Overlay
        if (isPrepRoomsOpen) {
            PrepRoomsOverlay(
                onDismiss = { DiscoverAnalytics.overlayDismissed("prep_rooms"); isPrepRoomsOpen = false },
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }

        // Channels OS Full-Stack Experience Overlay
        if (isChannelsOpen) {
            ChannelsOverlay(
                onDismiss = { DiscoverAnalytics.overlayDismissed("channels"); isChannelsOpen = false },
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }

        // Explore The City Full-Stack Experience Overlay
        if (isExploreTheCityOpen) {
            ExploreTheCityOverlay(
                venues = exploreVenues,
                onDismiss = { DiscoverAnalytics.overlayDismissed("explore_city"); isExploreTheCityOpen = false },
                onSelectVenue = { selectedPreviewVenueId = it.id },
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) }
            )
        }

        // Flash Drops Hub ("See All") Immersive Overlay
        if (isFlashDropsHubOpen) {
            FlashDropsHubOverlay(
                flashDrops = globalFlashDrops,
                onDismiss = { DiscoverAnalytics.overlayDismissed("flash_drops"); isFlashDropsHubOpen = false },
                onSelectDrop = { drop -> selectedFlashDropForDetailId = drop.id },
                onOpenRoute = { drop -> selectedFlashDropForRouteId = drop.id },
                onClaimDrop = { id -> com.example.core.data.VenueRepository.claimGlobalFlashDrop(id) }
            )
        }

        // Smart Places Hub ("See All") Concierge Overlay
        if (isSmartPlacesHubOpen) {
            SmartPlacesHubOverlay(
                venues = exploreVenues,
                onDismiss = { DiscoverAnalytics.overlayDismissed("smart_places"); isSmartPlacesHubOpen = false },
                onSelectVenue = { selectedPreviewVenueId = it.id },
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) }
            )
        }

        // Flash Drop Full Cinematic Detail Overlay
        if (selectedFlashDropForDetail != null) {
            FlashDropDetailOverlay(
                drop = selectedFlashDropForDetail,
                onDismiss = { DiscoverAnalytics.overlayDismissed("flash_drop_detail"); selectedFlashDropForDetailId = null },
                onNavigateToLobby = onNavigateToLobby,
                onNavigateToEventDetails = onNavigateToEventDetails,
                onOpenRoute = { drop -> selectedFlashDropForRouteId = drop.id },
                onClaimConfirm = { id -> com.example.core.data.VenueRepository.claimGlobalFlashDrop(id) }
            )
        }

        // Flash Drop Navigation Route Dialog
        if (selectedFlashDropForRoute != null) {
            FlashDropRouteDialog(
                drop = selectedFlashDropForRoute,
                onDismiss = { DiscoverAnalytics.overlayDismissed("flash_drop_route"); selectedFlashDropForRouteId = null }
            )
        }
    }
}
