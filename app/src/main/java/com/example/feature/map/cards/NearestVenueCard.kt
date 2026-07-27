package com.example.feature.map.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.core.data.ExploreVenue
import com.example.feature.map.util.VenueActionLabels
import com.example.feature.map.util.VenueFilter

/**
 * The category-adaptive "Nearest Hotspot" card shown pinned under the Map
 * screen's category chips.
 *
 * Assembled from [NearestVenueHeader] (location + trending badge),
 * [NearestVenueHeroImage] (photo + tags), [NearestVenueDetailsColumn]
 * (title/address/hours/rating) and [NearestVenueActionRow] (primary action +
 * Route). This composition matches `MapScreen.kt`'s original inline
 * `NearestVenueCard` pixel-for-pixel; only the >250-line single function was
 * split into these sibling files per the Map architecture split.
 */
@Composable
fun NearestVenueCard(
    venue: ExploreVenue,
    categoryLabel: String,
    onNavigateToLobby: (String) -> Unit,
    onRouteClick: () -> Unit
) {
    val context = LocalContext.current
    val cleanCategory = VenueFilter.normalizeCategory(categoryLabel).uppercase()
    val textTag = if (cleanCategory == "ALL") "NEAREST HOTSPOT" else "NEAREST $cleanCategory"
    val normalizedCategory = if (cleanCategory == "ALL") VenueActionLabels.normalize(venue.category) else cleanCategory

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = Color(0xFF0C0F19).copy(alpha = 0.95f),
        border = BorderStroke(1.5.dp, Color(0xFFC026D3).copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            NearestVenueHeader(venue = venue, categoryLabel = textTag)

            Spacer(modifier = Modifier.height(8.dp))

            // Two-Column Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NearestVenueHeroImage(
                    venue = venue,
                    normalizedCategory = normalizedCategory,
                    modifier = Modifier.weight(0.38f)
                )

                NearestVenueDetailsColumn(
                    venue = venue,
                    onViewReviewsClick = {
                        Toast.makeText(context, "Opening reviews for ${venue.name}...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(0.62f)
                ) {
                    NearestVenueActionRow(
                        normalizedCategory = normalizedCategory,
                        onPrimaryActionClick = {
                            if (VenueActionLabels.opensInAppLobby(normalizedCategory)) {
                                onNavigateToLobby(venue.id)
                            } else {
                                com.example.feature.website.openFomoWebsite(context, venue.websiteUrl)
                            }
                        },
                        onRouteClick = onRouteClick
                    )
                }
            }
        }
    }
}
