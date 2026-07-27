package com.example.feature.map.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import com.example.feature.map.components.SectionHeader
import com.example.feature.map.state.MapBottomTab
import com.example.feature.map.state.SelectedMapItem
import com.example.feature.map.util.VenueActionLabels
import com.example.feature.map.util.VenueRanking

/**
 * The Map screen's bottom-sheet carousel cards: [HorizontalVenueCard] (for
 * the "🔥 Live Spots" tab) and [HorizontalFriendCard] (for the "👥 Friends
 * Map" tab). Extracted verbatim from `MapScreen.kt`'s inline composables of
 * the same names.
 */

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
    val primaryTitle = VenueActionLabels.primaryActionTitle(normalizedCategory)

    Surface(
        modifier = Modifier
            .width(220.dp)
            .clickable { onSelect() },
        color = Color(0xFF141A29),
        border = BorderStroke(1.dp, if (isExpanded) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Photo with Vibe Score overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                AsyncImage(
                    model = venue.imageUrl,
                    contentDescription = venue.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
                // Vibe Badge
                Surface(
                    color = Color(0xFF0F1524).copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, Color(0xFFFF4500).copy(alpha = 0.5f)),
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
                            tint = Color(0xFFFF4500),
                            modifier = Modifier.size(11.dp)
                        )
                        val score = VenueRanking.vibeScore(venue)
                        Text(
                            text = " $score% Vibe",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                // Name Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = venue.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (venue.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier
                                .size(13.dp)
                                .padding(start = 2.dp)
                        )
                    }
                }

                // Category/Subcategory
                Text(
                    text = venue.subcategory,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Rating, Distance, and Open Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("⭐ ${venue.rating}", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                    Text("•", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                    val isOpen = venue.openDays.lowercase().contains("now") || venue.openDays.lowercase().contains("mon") || venue.openDays.lowercase().contains("active")
                    Text(
                        text = "${venue.distanceText} • ${if (isOpen) "Open" else "Closed"}",
                        color = if (isOpen) Color(0xFF32D74B) else Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Expanded State Actions
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = primaryTitle,
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onRouteClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Route",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalFriendCard(friend: CircleFriend, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .clickable { onSelect() },
        color = Color(0xFF141A29),
        border = BorderStroke(1.dp, if (friend.status == "Online") Color(0xFF32D74B).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = friend.avatarUrl,
                    contentDescription = friend.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (friend.status == "Online") Color(0xFF32D74B) else Color.Gray)
                        .border(1.dp, Color(0xFF141A29), CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(friend.currentActivity, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("📍 ${friend.distanceText}", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * The Map screen's full bottom carousel sheet: the "🔥 Live Spots" /
 * "👥 Friends Map" [com.example.feature.map.components.SectionHeader] tab
 * switch plus the matching horizontal card list ([HorizontalVenueCard] or
 * [HorizontalFriendCard]).
 *
 * Extracted from `MapScreen.kt`'s inline bottom `Surface` so the route shell
 * only has to call `NearbyVenueCarousel(...)` instead of assembling the
 * header + `LazyRow` branching itself.
 */
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
    Surface(
        color = Color(0xFF0C1221).copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.15f)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
            SectionHeader(
                selectedTab = bottomTab,
                onTabSelected = onTabSelected,
                onlineFriendsCount = friends.count { it.status == "Online" }
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (bottomTab == MapBottomTab.VENUES) {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredVenues) { venue ->
                        val isCardExpanded = (selectedMapItem as? SelectedMapItem.Venue)?.venue?.id == venue.id
                        HorizontalVenueCard(
                            venue = venue,
                            isExpanded = isCardExpanded,
                            onSelect = { onSelectVenue(venue) },
                            onNavigateToLobby = onNavigateToLobby,
                            onRouteClick = { onRouteToVenue(venue) }
                        )
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(friends) { friend ->
                        HorizontalFriendCard(friend = friend, onSelect = { onSelectFriend(friend) })
                    }
                }
            }
        }
    }
}
