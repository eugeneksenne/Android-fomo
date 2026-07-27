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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage


private data class SmartGuideItem(
    val title: String,
    val count: String,
    val subtitle: String,
    val imageUrl: String,
    val badge: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartPlacesHubOverlay(
    venues: List<com.example.core.data.ExploreVenue>,
    onDismiss: () -> Unit,
    onSelectVenue: (com.example.core.data.ExploreVenue) -> Unit,
    onNavigateToLobby: (String) -> Unit,
    onLikeToggle: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("🔥 Tonight") }
    var selectedGuide by remember { mutableStateOf<String?>(null) }
    var isFilterSheetOpen by remember { mutableStateOf(false) }
    var venueToRoute by remember { mutableStateOf<com.example.core.data.ExploreVenue?>(null) }
    var sortBy by remember { mutableStateOf("Recommended") }
    var filterOnlyLobby by remember { mutableStateOf(false) }
    var savedItineraryState by remember { mutableStateOf(false) }

    val moodFilters = listOf(
        "🔥 Tonight",
        "❤️ Date Night",
        "🎉 Party & Club",
        "🍸 Cocktails",
        "🍽 Dinner",
        "🎵 Live Music",
        "🌇 Rooftops",
        "💎 Luxury",
        "☕ Chill",
        "🌃 Late Night",
        "✨ Hidden Gems"
    )

    val guides = listOf(
        SmartGuideItem("Tonight's Best Picks", "12 places", "Hand-selected by FOMO editors for tonight", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop", "⭐ Editor's Pick"),
        SmartGuideItem("Dinner Before Dancing", "8 places", "Seamless transition from fine dining to lounge", "https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=600&auto=format&fit=crop", "🍽 Fine Dining"),
        SmartGuideItem("Perfect First Date", "10 places", "Intimate lighting, craft cocktails, low noise", "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=600&auto=format&fit=crop", "❤️ Date Night"),
        SmartGuideItem("Luxury Evenings", "6 places", "VIP tables, bottle service, champagne bars", "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=600&auto=format&fit=crop", "💎 VIP Luxury"),
        SmartGuideItem("Live Music Tonight", "9 places", "Amapiano, Afro-house & jazz live sets", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=600&auto=format&fit=crop", "🎵 Live Shows"),
        SmartGuideItem("After Midnight", "7 places", "Venues open past 2 AM with active crowds", "https://images.unsplash.com/photo-1571266028243-e4733b0f0bb1?q=80&w=600&auto=format&fit=crop", "🌃 Late Night"),
        SmartGuideItem("Hidden Rooftops", "5 places", "Panoramas & sunset vibes", "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?q=80&w=600&auto=format&fit=crop", "🌇 Rooftop")
    )

    val heroVenue = remember(venues) {
        venues.find { it.id == "d48_midrand" }
            ?: venues.firstOrNull()
            ?: com.example.core.data.ExploreVenue(
                id = "d48_midrand",
                name = "D48 Midrand",
                category = "Nightlife",
                subcategory = "VIP Lounge",
                imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop",
                isVerified = true,
                rating = 4.9f,
                reviewCount = 943,
                address = "563 Old Pretoria Road",
                area = "Midrand",
                distanceText = "18 km away",
                attributes = listOf("VIP Seating", "Hubbly", "Live DJs", "Bottle Service"),
                openDays = "Thu–Sun",
                startHour = 18,
                endHour = 4,
                hasClubLobby = true
            )
    }

    val filteredVenues = remember(venues, searchQuery, selectedMood, selectedGuide, filterOnlyLobby, sortBy) {
        var list = if (venues.isNotEmpty()) venues else listOf(heroVenue)

        if (filterOnlyLobby) {
            list = list.filter { it.hasClubLobby }
        }

        if (selectedGuide != null) {
            list = when (selectedGuide) {
                "Dinner Before Dancing" -> list.filter { it.category == "Food" || it.subcategory.contains("Dining", ignoreCase = true) || it.subcategory.contains("Lounge", ignoreCase = true) }
                "Perfect First Date" -> list.filter { it.rating >= 4.5f }
                "Luxury Evenings" -> list.filter { it.attributes.any { a -> a.contains("VIP", ignoreCase = true) || a.contains("Premium", ignoreCase = true) } }
                "Live Music Tonight" -> list.filter { it.attributes.any { a -> a.contains("DJ", ignoreCase = true) || a.contains("Music", ignoreCase = true) } }
                "After Midnight" -> list.filter { it.endHour >= 2 || it.is24Hours }
                "Hidden Rooftops" -> list.filter { it.attributes.any { a -> a.contains("Rooftop", ignoreCase = true) } }
                else -> list
            }
        } else if (selectedMood != "🔥 Tonight") {
            list = when (selectedMood) {
                "❤️ Date Night" -> list.filter { it.rating >= 4.5f || it.category == "Food" }
                "🎉 Party & Club" -> list.filter { it.category == "Nightlife" || it.subcategory.contains("Nightclub", ignoreCase = true) }
                "🍸 Cocktails" -> list.filter { it.attributes.any { a -> a.contains("Cocktail", ignoreCase = true) || a.contains("Bar", ignoreCase = true) } }
                "🍽 Dinner" -> list.filter { it.category == "Food" }
                "🎵 Live Music" -> list.filter { it.attributes.any { a -> a.contains("DJ", ignoreCase = true) || a.contains("Music", ignoreCase = true) } }
                "🌇 Rooftops" -> list.filter { it.attributes.any { a -> a.contains("Rooftop", ignoreCase = true) } }
                "💎 Luxury" -> list.filter { it.attributes.any { a -> a.contains("VIP", ignoreCase = true) || a.contains("Premium", ignoreCase = true) } }
                "☕ Chill" -> list.filter { it.category == "Prep" || it.category == "Recover" || it.subcategory.contains("Casual", ignoreCase = true) }
                "🌃 Late Night" -> list.filter { it.endHour >= 2 || it.is24Hours }
                else -> list
            }
        }

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.area.contains(searchQuery, ignoreCase = true) ||
                it.subcategory.contains(searchQuery, ignoreCase = true) ||
                it.attributes.any { attr -> attr.contains(searchQuery, ignoreCase = true) }
            }
        }

        when (sortBy) {
            "Highest Rated" -> list.sortedByDescending { it.rating }
            "Nearest" -> list.sortedBy { it.distanceText }
            else -> list
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_places_hub_overlay"),
        color = Color(0xFF090A0F)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .size(38.dp)
                        .testTag("smart_places_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✨ Smart Places",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CONCIERGE",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Curated for you in Johannesburg • Updated moments ago",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                IconButton(
                    onClick = { isFilterSheetOpen = true },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .size(38.dp)
                        .testTag("smart_places_filter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = Color.White
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Tonight Recommendation (Hero Card)
                item {
                    Text(
                        text = "TONIGHT'S TOP RECOMMENDATION",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onSelectVenue(heroVenue) }
                            .testTag("smart_places_hero_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141520)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                AsyncImage(
                                    model = heroVenue.imageUrl,
                                    contentDescription = heroVenue.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color(0xFF141520)),
                                                startY = 100f
                                            )
                                        )
                                )
                                Surface(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .align(Alignment.TopStart),
                                    color = Color.Black.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔥 98% Vibe Match", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("• Peak Crowd 10 PM", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = heroVenue.name,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (heroVenue.isVerified) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Verified",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        color = Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${heroVenue.rating}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Text(
                                    text = "${heroVenue.subcategory} • ${heroVenue.area} • ${heroVenue.distanceText} • Open until ${if (heroVenue.endHour == 24) "24 Hours" else "${heroVenue.endHour} AM"}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                // Reason Chips
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    listOf("⭐ Top Editor Pick", "❤️ Perfect for Date Night", "🎵 Live DJ at 9 PM", "🌇 VIP Lounge").forEach { chip ->
                                        Surface(
                                            color = Color.White.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                                        ) {
                                            Text(
                                                text = chip,
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onSelectVenue(heroVenue) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hero_view_venue"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("View Venue", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { venueToRoute = heroVenue },
                                        modifier = Modifier.testTag("hero_route_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                                    ) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Route", color = Color.White, fontSize = 13.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            Toast.makeText(context, "Saved ${heroVenue.name} to your night plans!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Save", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Tonight Context Intelligence Strip
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Tonight in Johannesburg • 22°C • 63 venues open • 18 live shows • Light traffic",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 3. Mood & Occasion Filter Chips
                item {
                    Column {
                        Text(
                            text = "CHOOSE YOUR MOOD",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items = moodFilters, key = { it }) { mood ->
                                val isSelected = selectedMood == mood && selectedGuide == null
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedGuide = null
                                        selectedMood = mood
                                        Toast.makeText(context, "Filtering by: $mood", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text(mood, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color.White.copy(alpha = 0.06f),
                                        labelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = Color.White.copy(alpha = 0.15f),
                                        selectedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("mood_chip_${mood.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }
                }

                // 4. Editor's Guides / Curated Collections
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "EDITOR'S GUIDES",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Curated scenarios for every moment",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (selectedGuide != null) {
                                TextButton(onClick = { selectedGuide = null }) {
                                    Text("Clear Guide", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = guides, key = { it.title }) { guide ->
                                val isGuideSelected = selectedGuide == guide.title
                                Box(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .border(
                                            width = if (isGuideSelected) 2.dp else 1.dp,
                                            color = if (isGuideSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable {
                                            selectedGuide = if (isGuideSelected) null else guide.title
                                            Toast.makeText(context, "Selected guide: ${guide.title}", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("guide_card_${guide.title.lowercase().replace(" ", "_")}")
                                ) {
                                    AsyncImage(
                                        model = guide.imageUrl,
                                        contentDescription = guide.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                                                    startY = 40f
                                                )
                                            )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = guide.badge,
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = guide.title,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${guide.count} • ${guide.subtitle}",
                                                color = Color.White.copy(alpha = 0.75f),
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Tonight Timeline (Suggested Evening Flow)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF11121C)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SUGGESTED NIGHT TIMELINE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("4 STOPS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            val timelineSteps = listOf(
                                Triple("8:00 PM", "Dinner & Sunset Cocktails", "Marble Restaurant • Fine Dining"),
                                Triple("9:30 PM", "Warmup Lounge & Drinks", "Proud Mary • Rosebank"),
                                Triple("11:00 PM", "Main VIP Party & DJ Set", "D48 Midrand • High Energy"),
                                Triple("1:00 AM", "Late Night Afterhours", "Konka Soweto • Open till late")
                            )

                            timelineSteps.forEachIndexed { idx, (time, title, sub) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape,
                                            modifier = Modifier.size(10.dp)
                                        ) {}
                                        if (idx < timelineSteps.size - 1) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(30.dp)
                                                    .background(Color.White.copy(alpha = 0.15f))
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "$time • $title", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(text = sub, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    savedItineraryState = !savedItineraryState
                                    Toast.makeText(context, if (savedItineraryState) "Night Itinerary saved to My Plans!" else "Itinerary removed", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_itinerary_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (savedItineraryState) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (savedItineraryState) Icons.Default.Check else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (savedItineraryState) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (savedItineraryState) "Itinerary Saved" else "Save Full Night Itinerary",
                                    color = if (savedItineraryState) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // 6. Smart City Pulse Dashboard
                item {
                    Column {
                        Text(
                            text = "SMART CITY PULSE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val pulseItems = listOf(
                                "145" to "Events Tonight",
                                "38" to "Rooftops Open",
                                "21" to "Live Shows",
                                "10 PM" to "Peak Crowd"
                            )
                            pulseItems.forEach { (count, label) ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(count, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. Recommended Venues Feed Header & Search
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedGuide != null) "GUIDE: ${selectedGuide?.uppercase()}" else if (selectedMood != "🔥 Tonight") "MOOD: ${selectedMood.uppercase()}" else "RECOMMENDED SMART PLACES",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${filteredVenues.size} places", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search places, areas, music, vibes...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("smart_places_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                // 8. Recommended Venues List
                items(items = filteredVenues, key = { it.id }) { venue ->
                    val reasonChip = remember(venue.id) {
                        when (venue.id) {
                            "d48_midrand" -> "⭐ Top Recommendation • VIP Lounge"
                            "konka_soweto" -> "🔥 Peak Crowd & Live Amapiano"
                            "taboo_sandton" -> "🍸 Craft Cocktails & Celebrity Vibe"
                            "marble_rosebank" -> "❤️ Romantic Fine Dining & Sunset Views"
                            "proud_mary" -> "☕ Casual Chic & Pre-party Drinks"
                            else -> "✨ Recommended for your vibe tonight"
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSelectVenue(venue) }
                            .testTag("smart_venue_card_${venue.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF12131D)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                AsyncImage(
                                    model = venue.imageUrl,
                                    contentDescription = venue.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color(0xFF12131D)),
                                                startY = 60f
                                            )
                                        )
                                )
                                Surface(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .align(Alignment.TopStart),
                                    color = Color.Black.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = reasonChip,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = venue.name,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (venue.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${venue.rating}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = "${venue.subcategory} • ${venue.area} • ${venue.distanceText}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onSelectVenue(venue) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text("View Details", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    if (venue.hasClubLobby) {
                                        OutlinedButton(
                                            onClick = { onNavigateToLobby(venue.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text("Lobby 🔴", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { venueToRoute = venue },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }

                                    IconButton(
                                        onClick = { onLikeToggle(venue.id) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (venue.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Like",
                                            tint = if (venue.isLiked) Color(0xFFFF2D55) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Route Dialog
        if (venueToRoute != null) {
            SmartPlaceRouteDialog(
                venue = venueToRoute!!,
                onDismiss = { venueToRoute = null }
            )
        }

        // Filter & Sort Bottom Sheet
        if (isFilterSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isFilterSheetOpen = false },
                containerColor = Color(0xFF141522)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Filter & Sort Smart Places", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Text("Sort By", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Recommended", "Nearest", "Highest Rated").forEach { opt ->
                            FilterChip(
                                selected = sortBy == opt,
                                onClick = { sortBy = opt },
                                label = { Text(opt) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Has Club Lobby (Live Stream)", color = Color.White, fontSize = 14.sp)
                        Switch(
                            checked = filterOnlyLobby,
                            onCheckedChange = { filterOnlyLobby = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Button(
                        onClick = { isFilterSheetOpen = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Apply Filters", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
