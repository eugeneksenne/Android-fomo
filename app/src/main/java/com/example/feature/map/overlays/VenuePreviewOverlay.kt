package com.example.feature.map.overlays

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.feature.map.cards.FriendDetailsPanel
import com.example.feature.map.cards.VenueDetailsPanel
import com.example.feature.map.state.SelectedMapItem

/**
 * Renders whichever pin is currently selected on the map: a
 * [VenueDetailsPanel] for [SelectedMapItem.Venue] or a [FriendDetailsPanel]
 * for [SelectedMapItem.Friend]. Renders nothing when [selectedItem] is null.
 *
 * Extracted from the inline `when (val item = selectedMapItem) { ... }`
 * block that used to live directly in `MapScreen`'s body, between the top
 * HUD and the bottom carousel sheet.
 */
@Composable
fun VenuePreviewOverlay(
    selectedItem: SelectedMapItem?,
    onNavigateToLobby: (String) -> Unit,
    onDismiss: () -> Unit,
    onRouteToVenue: (SelectedMapItem.Venue) -> Unit
) {
    if (selectedItem == null) return

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        when (selectedItem) {
            is SelectedMapItem.Venue -> {
                VenueDetailsPanel(
                    venue = selectedItem.venue,
                    onNavigateToLobby = onNavigateToLobby,
                    onClose = onDismiss,
                    onRoute = { onRouteToVenue(selectedItem) }
                )
            }
            is SelectedMapItem.Friend -> {
                FriendDetailsPanel(
                    friend = selectedItem.friend,
                    onClose = onDismiss
                )
            }
        }
    }
}
