package com.example.feature.map.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.ExploreVenue
import com.example.feature.map.util.VenueRanking

/**
 * The right-hand metadata column of [NearestVenueCard]: title + verified
 * badge + FOMO score box, address/distance row, open-hours status, ratings
 * row with a "View Reviews" link, and (via [content]) the caller-supplied
 * action button row.
 *
 * Extracted verbatim from `MapScreen.kt`'s inline `NearestVenueCard` right
 * column `Column`. `onViewReviewsClick` replaces the inline `Toast.makeText`
 * side-effect so this file has no Android `Context` dependency.
 */
@Composable
internal fun NearestVenueDetailsColumn(
    venue: ExploreVenue,
    onViewReviewsClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        // Title and FOMO box Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = venue.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = venue.subcategory,
                        color = Color(0xFFD946EF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (venue.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFFC026D3),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // FOMO score vertical metric box
            val vibeScore = VenueRanking.vibeScore(venue)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color(0xFFC026D3).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFC026D3).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFD946EF),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "$vibeScore%",
                    color = Color(0xFFD946EF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "FOMO SCORE",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 5.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Address and Distance Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFFC026D3),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = venue.address,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.DirectionsWalk,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "${venue.distanceText} (2 min walk)",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Open / hours status row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(Color(0xFF32D74B), CircleShape)
            )
            Text(
                text = "Open Now",
                color = Color(0xFF32D74B),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "•",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 9.sp
            )
            Text(
                text = "Closes 04:00 AM",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Hours Details Pill
        Surface(
            color = Color.White.copy(alpha = 0.06f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "Thu – Sun 18:00 – 04:00",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Rating Row + View Reviews Action Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = " ${venue.rating}",
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " (${venue.reviewCount} reviews)",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 8.5.sp
                )
            }

            Text(
                text = "View Reviews",
                color = Color(0xFFD946EF),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onViewReviewsClick() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        actions()
    }
}
