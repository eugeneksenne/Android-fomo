package com.example.feature.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.core.data.ExploreVenue
import com.example.core.data.FlashDrop

/**
 * Centralised overlay/dialog selection state for the Discover route shell.
 *
 * Before this type existed, `DiscoverScreen` juggled a dozen independent
 * `mutableStateOf` flags (one per overlay/dialog) directly in the composable
 * body. That made the shell hard to scan, easy to leave inconsistent (e.g.
 * two overlays open at once) and awkward to unit test in isolation.
 *
 * `DiscoverOverlayState` collects all of that selection state behind a small
 * intent-style API. Sections and overlays call `open*()` functions (or pass
 * them as callback references, e.g. `onVenueClick = overlayState::openVenuePreview`)
 * instead of mutating shell-local state directly, and `DiscoverScreen` simply
 * renders whatever the state currently holds.
 */
class DiscoverOverlayState {
    var previewVenue by mutableStateOf<ExploreVenue?>(null)
        private set

    var isMyCircleHubOpen by mutableStateOf(false)
        private set

    var storyViewerIndex by mutableStateOf<Int?>(null)
        private set

    var flashDropForClaim by mutableStateOf<FlashDrop?>(null)
        private set

    var globalPlanTarget by mutableStateOf<String?>(null)
        private set

    var isPrepRoomsOpen by mutableStateOf(false)
        private set

    var isChannelsOpen by mutableStateOf(false)
        private set

    var isExploreTheCityOpen by mutableStateOf(false)
        private set

    var isFlashDropsHubOpen by mutableStateOf(false)
        private set

    var isSmartPlacesHubOpen by mutableStateOf(false)
        private set

    var flashDropForDetail by mutableStateOf<FlashDrop?>(null)
        private set

    var flashDropForRoute by mutableStateOf<FlashDrop?>(null)
        private set

    fun openVenuePreview(venue: ExploreVenue) { previewVenue = venue }
    fun dismissVenuePreview() { previewVenue = null }

    fun openMyCircleHub() { isMyCircleHubOpen = true }
    fun dismissMyCircleHub() { isMyCircleHubOpen = false }

    fun openStoryViewer(index: Int) { storyViewerIndex = index }
    fun dismissStoryViewer() { storyViewerIndex = null }

    fun openFlashDropClaim(drop: FlashDrop) { flashDropForClaim = drop }
    fun dismissFlashDropClaim() { flashDropForClaim = null }

    fun openGlobalPlanSheet(targetName: String) { globalPlanTarget = targetName }
    fun dismissGlobalPlanSheet() { globalPlanTarget = null }

    fun openPrepRooms() { isPrepRoomsOpen = true }
    fun dismissPrepRooms() { isPrepRoomsOpen = false }

    fun openChannels() { isChannelsOpen = true }
    fun dismissChannels() { isChannelsOpen = false }

    fun openExploreTheCity() { isExploreTheCityOpen = true }
    fun dismissExploreTheCity() { isExploreTheCityOpen = false }

    fun openFlashDropsHub() { isFlashDropsHubOpen = true }
    fun dismissFlashDropsHub() { isFlashDropsHubOpen = false }

    fun openSmartPlacesHub() { isSmartPlacesHubOpen = true }
    fun dismissSmartPlacesHub() { isSmartPlacesHubOpen = false }

    fun openFlashDropDetail(drop: FlashDrop) { flashDropForDetail = drop }
    fun dismissFlashDropDetail() { flashDropForDetail = null }

    fun openFlashDropRoute(drop: FlashDrop) { flashDropForRoute = drop }
    fun dismissFlashDropRoute() { flashDropForRoute = null }

    /** Dismiss every overlay/dialog at once, e.g. before navigating away from Discover. */
    fun dismissAll() {
        previewVenue = null
        isMyCircleHubOpen = false
        storyViewerIndex = null
        flashDropForClaim = null
        globalPlanTarget = null
        isPrepRoomsOpen = false
        isChannelsOpen = false
        isExploreTheCityOpen = false
        isFlashDropsHubOpen = false
        isSmartPlacesHubOpen = false
        flashDropForDetail = null
        flashDropForRoute = null
    }
}

@Composable
fun rememberDiscoverOverlayState(): DiscoverOverlayState = remember { DiscoverOverlayState() }
