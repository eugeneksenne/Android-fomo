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


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreTheCitySection(
    venues: List<com.example.core.data.ExploreVenue>,
    isOnline: Boolean = true,
    onVenueClick: (com.example.core.data.ExploreVenue) -> Unit,
    onLikeToggle: (String) -> Unit,
    onSeeAllClick: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val timeCategory = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..21 -> "Evening"
            else -> "Late Night"
        }
    }

    // Dynamic Greeting and Hero Info
    val (heroTitle, heroSubtitle, heroImg) = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> Triple(
                "☀️ Good Morning",
                "Coffee, brunch and places to start your day.",
                "https://images.unsplash.com/photo-1507133750040-4a8f57021571?q=80&w=600&auto=format&fit=crop"
            )
            in 12..16 -> Triple(
                "🏙️ Explore Johannesburg",
                "Great food, shopping and places to discover nearby.",
                "https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=600&auto=format&fit=crop"
            )
            in 17..21 -> Triple(
                "🌆 The City Is Coming Alive",
                "Nightlife, rooftops and unforgettable experiences await.",
                "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=600&auto=format&fit=crop"
            )
            else -> Triple(
                "🌌 Johannesburg Never Sleeps",
                "Find places still open around you.",
                "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop"
            )
        }
    }

    // Dynamic Discovery Rhythm
    val worldsOrder = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> listOf("Prep", "Recover", "Food", "Travel", "24/7", "Nightlife")
            in 12..16 -> listOf("Food", "Prep", "Travel", "Recover", "24/7", "Nightlife")
            in 17..21 -> listOf("Nightlife", "Food", "Prep", "24/7", "Recover", "Travel")
            else -> listOf("24/7", "Nightlife", "Food", "Recover", "Travel", "Prep")
        }
    }

    var selectedWorld by remember(worldsOrder) { mutableStateOf(worldsOrder.first()) }

    // Dynamic Discovery Heading Section Text
    val (headingTitle, headingSubtitle) = remember(selectedWorld) {
        when (selectedWorld) {
            "Nightlife" -> Pair("Tonight's Hotspots", "Discover the city's best nightlife venues.")
            "Food" -> Pair("Places to Eat", "Restaurants, cafés and dining experiences nearby.")
            "Prep" -> Pair("Get Ready", "Everything you need before heading out.")
            "Recover" -> Pair("Time to Recharge", "Wellness and recovery experiences nearby.")
            "Travel" -> Pair("Explore Local Gems", "Discover iconic places and memorable experiences.")
            else -> Pair("Always Open", "Places you can visit any time of the day or night.")
        }
    }

    // Filtered Venues
    val filteredVenues = remember(selectedWorld, venues) {
        venues.filter { it.category == selectedWorld }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        SectionHeader(
            title = "Explore The City",
            subtitle = "Curated collections & hidden gems across the city",
            actionText = "See all",
            onActionClick = onSeeAllClick
        )

        // 1. Dynamic Hero Card
        Box(modifier = Modifier.padding(horizontal = 16.dp).clickable { onSeeAllClick() }) {
            ExploreHeroCard(
                timeCategory = timeCategory,
                title = heroTitle,
                subtitle = heroSubtitle,
                imageUrl = heroImg
            )
        }

        // 2. Discover Places Around You (Discovery Worlds)
        Text(
            text = "Discover Places Around You",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = worldsOrder, key = { it }) { world ->
                val isSelected = world == selectedWorld
                val worldDisplay = when (world) {
                    "Nightlife" -> "🌙 Nightlife"
                    "Food" -> "🍽️ Food"
                    "Prep" -> "✨ Prep"
                    "Recover" -> "🌿 Recover"
                    "Travel" -> "✈️ Travel"
                    "24/7" -> "🕒 24/7"
                    else -> world
                }
                val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    color = backgroundColor,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clickable { selectedWorld = world }
                        .testTag("world_chip_$world")
                ) {
                    Text(
                        text = worldDisplay,
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 3. Dynamic Discovery Section Title and Horizontal Cards List
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headingTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = headingSubtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "See all →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        DiscoverAnalytics.seeAllClicked("Explore The City Places")
                        onSeeAllClick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Venue Cards List (Stage 1 Browse Cards)
            if (filteredVenues.isEmpty() && !isOnline) {
                DiscoverOfflineState(
                    message = "Reconnect to browse venues in this world.",
                    onRetryClick = onRetry
                )
            } else if (filteredVenues.isEmpty()) {
                DiscoverEmptyState(
                    title = "No venues in this world yet",
                    message = "Try another category or refresh when more venues come online."
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(items = filteredVenues, key = { it.id }) { venue ->
                        ExploreVenueCard(
                            venue = venue,
                            onCardClick = { onVenueClick(venue) },
                            onLikeToggle = { onLikeToggle(venue.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExploreHeroCard(
    timeCategory: String,
    title: String,
    subtitle: String,
    imageUrl: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .testTag("explore_hero_card")
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Explore Hero Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.85f)),
                        startY = 50f
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "📍 Johannesburg",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "VIBE STATUS: LIVE",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ExploreVenueCard(
    venue: com.example.core.data.ExploreVenue,
    onCardClick: () -> Unit,
    onLikeToggle: () -> Unit
) {
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val (statusText, statusColor, extraStatus) = getVenueStatus(venue, currentHour)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .width(280.dp)
            .semantics {
                contentDescription = "${venue.name}, ${venue.subcategory} in ${venue.area}. Venue card."
                role = Role.Button
            }
            .clickable {
                DiscoverAnalytics.cardOpened("explore_the_city", venue.id, "venue")
                onCardClick()
            }
            .testTag("venue_card_${venue.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = venue.imageUrl,
                    contentDescription = null, // parent card node carries the description
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { onLikeToggle() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(36.dp)
                        .testTag("like_button_${venue.id}")
                ) {
                    Icon(
                        imageVector = if (venue.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Love Reaction",
                        tint = if (venue.isLiked) Color.Red else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = venue.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (venue.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = venue.distanceText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFD60A),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${venue.rating} (${venue.reviewCount} reviews)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = venue.address,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Opening Hours",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${venue.openDays} • ${formatHours(venue.startHour, venue.endHour)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = getPrefixedAttribute(venue.subcategory),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    venue.attributes.take(2).forEach { attr ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = getPrefixedAttribute(attr),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
