package com.example.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import java.util.Calendar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage


@Composable
fun SmartPlacesSection(
    venues: List<com.example.core.data.ExploreVenue> = emptyList(),
    isOnline: Boolean = true,
    onSeeAllClick: () -> Unit = {},
    onVenueClick: (com.example.core.data.ExploreVenue) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val displayVenues = if (venues.isNotEmpty()) venues.take(5) else emptyList()
    Column {
        SectionHeader("Smart Places", "Curated for your vibe tonight", "See all", onActionClick = onSeeAllClick)
        if (displayVenues.isEmpty() && !isOnline) {
            DiscoverOfflineState(
                message = "Reconnect to refresh curated places for tonight.",
                onRetryClick = onRetry
            )
            return@Column
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (displayVenues.isEmpty()) {
                items(3) { index ->
                    SmartPlaceCard(
                        venueName = if (index == 0) "D48 Midrand" else if (index == 1) "Konka Soweto" else "Taboo Lounge",
                        categoryText = if (index == 0) "Nightclub • Midrand" else if (index == 1) "Nightclub • Soweto" else "Lounge • Sandton",
                        matchScore = if (index == 0) "98% Match" else if (index == 1) "96% Match" else "94% Match",
                        imageUrl = if (index == 0) "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop" else if (index == 1) "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=600&auto=format&fit=crop" else "https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=600&auto=format&fit=crop",
                        friendsCount = "${index + 2} Friends",
                        onClick = onSeeAllClick
                    )
                }
            } else {
                items(items = displayVenues, key = { it.id }) { venue ->
                    val matchScore = remember(venue.id) {
                        when (venue.id) {
                            "d48_midrand" -> "98% Match"
                            "konka_soweto" -> "96% Match"
                            "taboo_sandton" -> "95% Match"
                            "marble_rosebank" -> "93% Match"
                            else -> "${88 + (venue.rating * 2).toInt()}% Match"
                        }
                    }
                    val friendsText = remember(venue.id) {
                        when (venue.id) {
                            "d48_midrand" -> "5 Friends"
                            "konka_soweto" -> "4 Friends"
                            "taboo_sandton" -> "3 Friends"
                            else -> "2 Friends"
                        }
                    }
                    SmartPlaceCard(
                        venueName = venue.name,
                        categoryText = "${venue.subcategory} • ${venue.area}",
                        matchScore = matchScore,
                        imageUrl = venue.imageUrl,
                        friendsCount = friendsText,
                        onClick = { onVenueClick(venue) }
                    )
                }
            }
        }
    }
}

@Composable
fun SmartPlaceCard(
    venueName: String = "The Artistry",
    categoryText: String = "Lounge • Cocktails",
    matchScore: String = "98% Match",
    imageUrl: String = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop",
    friendsCount: String = "2 Friends",
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(340.dp)
            .clip(RoundedCornerShape(20.dp))
            .semantics {
                contentDescription = "$venueName, $matchScore. Smart place card."
                role = Role.Button
            }
            .clickable {
                DiscoverAnalytics.cardOpened("smart_places", venueName.lowercase().replace(" ", "_"), "venue")
                onClick()
            }
            .testTag("smart_place_card_${venueName.lowercase().replace(" ", "_")}")
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = venueName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 300f
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(friendsCount, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(matchScore, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(venueName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text(categoryText, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}
