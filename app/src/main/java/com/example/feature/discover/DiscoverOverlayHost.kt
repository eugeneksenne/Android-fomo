package com.example.feature.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.example.core.data.CircleStory
import com.example.core.data.ExploreVenue
import com.example.core.data.FlashDrop
import com.example.core.data.VenueRepository

/**
 * Renders every overlay/dialog Discover can show, driven entirely by
 * [overlayState].
 *
 * Extracting this from `DiscoverScreen` keeps the route shell's body a pure
 * declarative section list, with overlay presentation delegated to a single
 * call (`DiscoverOverlayHost(...)`). Every branch below reads state from
 * [DiscoverOverlayState] and calls back into it (`overlayState::dismissX`,
 * `overlayState::openY`) rather than mutating local composable state, so
 * there is exactly one source of truth for "what overlay is showing".
 */
@Composable
fun DiscoverOverlayHost(
    overlayState: DiscoverOverlayState,
    exploreVenues: List<ExploreVenue>,
    storiesState: List<CircleStory>,
    globalFlashDrops: List<FlashDrop>,
    onNavigateToLobby: (String) -> Unit,
    onNavigateToEventDetails: (String) -> Unit
) {
    val globalPlanTarget = overlayState.globalPlanTarget
    if (globalPlanTarget != null) {
        // ModalBottomSheet provides its own dismiss animation and back handling.
        GlobalPlanContextSheet(
            targetName = globalPlanTarget,
            onDismiss = overlayState::dismissGlobalPlanSheet
        )
    }

    val previewVenue = overlayState.previewVenue
    if (previewVenue != null) {
        val currentVenue = exploreVenues.find { it.id == previewVenue.id } ?: previewVenue
        DiscoverOverlay("venue_preview") {
            VenuePreviewOverlay(
                venue = currentVenue,
                onDismiss = overlayState::dismissVenuePreview,
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> VenueRepository.toggleLikeVenue(id) }
            )
        }
    }

    val storyViewerIndex = overlayState.storyViewerIndex
    if (storyViewerIndex != null) {
        DiscoverOverlay("story_viewer") {
            ImmersiveStoryViewer(
                stories = storiesState,
                initialIndex = storyViewerIndex,
                onDismiss = overlayState::dismissStoryViewer,
                onNavigateToLobby = onNavigateToLobby
            )
        }
    }

    if (overlayState.isMyCircleHubOpen) {
        DiscoverOverlay("my_circle_hub") {
            MyCircleHubOverlay(
                onDismiss = overlayState::dismissMyCircleHub,
                onStoryClick = overlayState::openStoryViewer,
                onNavigateToLobby = onNavigateToLobby,
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }
    }

    val flashDropForClaim = overlayState.flashDropForClaim
    if (flashDropForClaim != null) {
        // AlertDialog provides its own back handling.
        FlashDropClaimDialog(
            drop = flashDropForClaim,
            onDismiss = overlayState::dismissFlashDropClaim,
            onConfirmClaim = { id -> VenueRepository.claimGlobalFlashDrop(id) }
        )
    }

    if (overlayState.isPrepRoomsOpen) {
        DiscoverOverlay("prep_rooms") {
            PrepRoomsOverlay(
                onDismiss = overlayState::dismissPrepRooms,
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }
    }

    if (overlayState.isChannelsOpen) {
        DiscoverOverlay("channels") {
            ChannelsOverlay(
                onDismiss = overlayState::dismissChannels,
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }
    }

    if (overlayState.isExploreTheCityOpen) {
        DiscoverOverlay("explore_the_city") {
            ExploreTheCityOverlay(
                venues = exploreVenues,
                onDismiss = overlayState::dismissExploreTheCity,
                onSelectVenue = overlayState::openVenuePreview,
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> VenueRepository.toggleLikeVenue(id) }
            )
        }
    }

    if (overlayState.isFlashDropsHubOpen) {
        DiscoverOverlay("flash_drops_hub") {
            FlashDropsHubOverlay(
                flashDrops = globalFlashDrops,
                onDismiss = overlayState::dismissFlashDropsHub,
                onSelectDrop = overlayState::openFlashDropDetail,
                onOpenRoute = overlayState::openFlashDropRoute,
                onClaimDrop = { id -> VenueRepository.claimGlobalFlashDrop(id) }
            )
        }
    }

    if (overlayState.isSmartPlacesHubOpen) {
        DiscoverOverlay("smart_places_hub") {
            SmartPlacesHubOverlay(
                venues = exploreVenues,
                onDismiss = overlayState::dismissSmartPlacesHub,
                onSelectVenue = overlayState::openVenuePreview,
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> VenueRepository.toggleLikeVenue(id) }
            )
        }
    }

    val flashDropForDetail = overlayState.flashDropForDetail
    if (flashDropForDetail != null) {
        DiscoverOverlay("flash_drop_detail") {
            FlashDropDetailOverlay(
                drop = flashDropForDetail,
                onDismiss = overlayState::dismissFlashDropDetail,
                onNavigateToLobby = onNavigateToLobby,
                onNavigateToEventDetails = onNavigateToEventDetails,
                onOpenRoute = overlayState::openFlashDropRoute,
                onClaimConfirm = { id -> VenueRepository.claimGlobalFlashDrop(id) }
            )
        }
    }

    val flashDropForRoute = overlayState.flashDropForRoute
    if (flashDropForRoute != null) {
        // AlertDialog provides its own back handling.
        FlashDropRouteDialog(
            drop = flashDropForRoute,
            onDismiss = overlayState::dismissFlashDropRoute
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
