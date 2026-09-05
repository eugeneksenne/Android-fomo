package com.example.feature.map.cards

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.data.ExploreVenue
import com.example.feature.map.util.VenueActionLabels
import com.example.feature.map.util.VenueFilter

@Composable
fun NearestVenueCard(
    venue: ExploreVenue,
    categoryLabel: String,
    onNavigateToLobby: (String) -> Unit,
    onRouteClick: () -> Unit
) {
    val context = LocalContext.current
    val cleanCategory = VenueFilter.normalizeCategory(categoryLabel).uppercase()
    val textTag = if (cleanCategory == "ALL" || cleanCategory == "NIGHTLIFE") "NEAREST VENUE" else "NEAREST $cleanCategory"
    val normalizedCategory = if (cleanCategory == "ALL") VenueActionLabels.normalize(venue.category) else cleanCategory

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = Color(0xFF0D111D).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            NearestVenueHeader(venue = venue, categoryLabel = textTag)

            Spacer(modifier = Modifier.height(8.dp))

            // Two-Column Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
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
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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

