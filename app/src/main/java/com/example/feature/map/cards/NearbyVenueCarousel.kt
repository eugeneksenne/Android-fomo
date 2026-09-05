package com.example.feature.map.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.data.CircleFriend
import com.example.core.data.ExploreVenue
import com.example.feature.map.state.MapBottomTab
import com.example.feature.map.state.SelectedMapItem
import com.example.feature.map.util.VenueActionLabels
import com.example.feature.map.util.VenueRanking

@Composable
fun HorizontalVenueCard(
    venue: ExploreVenue,
    isExpanded: Boolean,
    onSelect: () -> Unit,
    onNavigateToLobby: (String) -> Unit,
    onRouteClick: () -> Unit
) {
    val context = LocalContext.current
    val normalizedCategory = VenueActionLabels.normalize(venue.category)

    Surface(
        modifier = Modifier
            .width(220.dp)
            .clickable { onSelect() },
        color = Color(0xFF0D111D),
        border = BorderStroke(
            1.5.dp,
            if (isExpanded) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            // Photo with Vibe Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                AsyncImage(
                    model = venue.imageUrl,
                    contentDescription = venue.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                )
                // Score Badge top right
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF2D55),
                            modifier = Modifier.size(12.dp)
                        )
                        val score = VenueRanking.vibeScore(venue)
                        Text(
                            text = " $score%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                // Name Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = venue.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (venue.isVerified || venue.id == "fomo_club") {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(start = 2.dp)
                        )
                    }
                }

                // Subcategory
                Text(
                    text = venue.subcategory,
                    color = Color(0xFFC084FC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Rating & Open status
                val isClosingSoon = venue.id == "afterglow_house" || venue.openDays.contains("Closing Soon", ignoreCase = true)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("⭐ ${venue.rating}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("•", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
                    Text(
                        text = venue.distanceText,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Text("•", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isClosingSoon) Color(0xFFF97316) else Color(0xFF22C55E), CircleShape)
                    )
                    Text(
                        text = if (isClosingSoon) "Closing Soon" else "Open Now",
                        color = if (isClosingSoon) Color(0xFFF97316) else Color(0xFF22C55E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Action buttons if card expanded/selected
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (VenueActionLabels.opensInAppLobby(normalizedCategory)) {
                                    onNavigateToLobby(venue.id)
                                } else {
                                    com.example.feature.website.openFomoWebsite(context, venue.websiteUrl)
                                }
                            },
                            color = Color(0xFF201638),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Lobby", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            onClick = onRouteClick,
                            color = Color(0xFF8B5CF6),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.NorthEast, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Route", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NearbyVenueCarousel(
    bottomTab: MapBottomTab,
    onTabSelected: (MapBottomTab) -> Unit,
    friends: List<CircleFriend>,
    filteredVenues: List<ExploreVenue>,
    selectedMapItem: SelectedMapItem?,
    onSelectVenue: (ExploreVenue) -> Unit,
    onSelectFriend: (CircleFriend) -> Unit,
    onNavigateToLobby: (String) -> Unit,
    onRouteToVenue: (ExploreVenue) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // Section Header: "Nightlife Nearby" ... "See All >"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nightlife Nearby",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "See All",
                    color = Color(0xFF8B5CF6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredVenues) { venue ->
                val isCardExpanded = (selectedMapItem as? SelectedMapItem.Venue)?.venue?.id == venue.id || venue.id == "fomo_club"
                HorizontalVenueCard(
                    venue = venue,
                    isExpanded = isCardExpanded,
                    onSelect = { onSelectVenue(venue) },
                    onNavigateToLobby = onNavigateToLobby,
                    onRouteClick = { onRouteToVenue(venue) }
                )
            }
        }
    }
}

