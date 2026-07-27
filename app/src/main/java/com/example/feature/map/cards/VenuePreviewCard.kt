package com.example.feature.map.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.ExploreVenue
import com.example.feature.map.util.VenueActionLabels

/**
 * The compact venue preview card shown above the bottom carousel sheet when
 * a venue pin is selected on the map.
 *
 * Extracted verbatim from `MapScreen.kt`'s inline `VenueDetailsPanel`
 * composable (kept under its original name for call-site parity across the
 * codebase). Hosted by [com.example.feature.map.overlays.VenuePreviewOverlay].
 */
@Composable
fun VenueDetailsPanel(
    venue: ExploreVenue,
    onNavigateToLobby: (String) -> Unit,
    onClose: () -> Unit,
    onRoute: () -> Unit
) {
    val context = LocalContext.current
    val normalizedCategory = VenueActionLabels.normalize(venue.category)
    val primaryTitle = VenueActionLabels.primaryActionTitle(normalizedCategory)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = Color(0xFF0F1524).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(venue.subcategory.uppercase(), color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close pin detail", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            Text(venue.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⭐ ${venue.rating}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("📍 ${venue.distanceText}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("🕒 ${venue.openDays}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(venue.attributes) { attr ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00E5FF).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(attr, color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (VenueActionLabels.opensInAppLobby(normalizedCategory)) {
                            onNavigateToLobby(venue.id)
                        } else {
                            com.example.feature.website.openFomoWebsite(context, venue.websiteUrl)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151D30)),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text(primaryTitle, color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRoute,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text("Draw Directions", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
