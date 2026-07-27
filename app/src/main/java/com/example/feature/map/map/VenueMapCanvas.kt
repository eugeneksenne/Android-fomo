package com.example.feature.map.map

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import com.example.core.data.CircleFriend
import com.example.core.data.ExploreVenue
import com.example.feature.map.util.MarkerGenerator
import com.example.feature.map.util.defaultFomoClubVenue

/**
 * The Map screen's WebView-backed venue/friend canvas.
 *
 * Owns:
 * - building the Leaflet HTML page from the current venue/friend lists
 *   (via [MarkerGenerator]);
 * - hosting the [WebViewMap] Android view;
 * - resolving the raw venue/friend ids the Javascript bridge reports back
 *   into real [ExploreVenue] / [CircleFriend] objects (the same
 *   `allVenues.find { it.id == venueId }` / `friends.find { it.id == friendId }`
 *   lookup, including the `fomo_club` -> [defaultFomoClubVenue] fallback,
 *   that used to live inline inside `MapScreen`'s `AndroidBridge` interface).
 *
 * `MapScreen` still owns the resulting `WebView` reference (needed by the
 * top bar, chips, cards and floating buttons to issue further JS commands
 * such as `centerOn`/`drawRoute`/`toggleHeatmap`), so this composable reports
 * it back via [onWebViewReady] exactly as the pre-refactor `webViewRef =
 * this` assignment did.
 */
@Composable
fun VenueMapCanvas(
    venues: List<ExploreVenue>,
    friends: List<CircleFriend>,
    onVenueSelected: (ExploreVenue) -> Unit,
    onFriendSelected: (CircleFriend) -> Unit,
    onOverpassResult: (count: Int, category: String) -> Unit,
    onWebViewReady: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val html = remember(venues, friends) {
        buildLeafletHtml(
            venueMarkersScript = MarkerGenerator.buildVenueMarkersScript(venues),
            friendMarkersScript = MarkerGenerator.buildFriendMarkersScript(friends)
        )
    }

    WebViewMap(
        initialHtml = html,
        onVenueMarkerClick = { venueId ->
            val matched = venues.find { it.id == venueId }
                ?: if (venueId == "fomo_club") defaultFomoClubVenue else null
            if (matched != null) {
                onVenueSelected(matched)
            }
        },
        onFriendMarkerClick = { friendId ->
            friends.find { it.id == friendId }?.let(onFriendSelected)
        },
        onOverpassResult = onOverpassResult,
        onWebViewReady = onWebViewReady,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
    )
}
