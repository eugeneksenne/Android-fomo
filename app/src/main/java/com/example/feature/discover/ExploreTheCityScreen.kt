package com.example.feature.discover

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.data.ExploreVenue
import com.example.core.data.VenueRepository
import java.util.Calendar

// -------------------------------------------------------------------------
// EXPLORE THE CITY - BILLION-DOLLAR DISCOVER EXPERIENCE OVERLAY
// -------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreTheCityOverlay(
    venues: List<ExploreVenue>,
    onDismiss: () -> Unit,
    onSelectVenue: (ExploreVenue) -> Unit = {},
    onNavigateToLobby: (String) -> Unit = {},
    onLikeToggle: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // Modern Dark Theme Palette
    val themeBg = Color(0xFF090D16)
    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val warmAmber = Color(0xFFFFB703)
    val openGreen = Color(0xFF00E676)
    val closedRed = Color(0xFFFF1744)

    // Current Time Calculation & Rhythm
    val currentCalendar = remember { Calendar.getInstance() }
    var currentHour by remember { mutableIntStateOf(currentCalendar.get(Calendar.HOUR_OF_DAY)) }

    val timeCategory = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..21 -> "Evening"
            else -> "Late Night"
        }
    }

    // Dynamic Hero Banner Metadata
    val (heroTitle, heroSubtitle, heroImg) = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> Triple(
                "🌅 Good Morning Johannesburg",
                "Coffee, brunch and places to start your day.",
                "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?q=80&w=800&auto=format&fit=crop"
            )
            in 12..16 -> Triple(
                "☀️ Explore Johannesburg",
                "Great food, shopping and places to discover nearby.",
                "https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=800&auto=format&fit=crop"
            )
            in 17..21 -> Triple(
                "🌆 The City Is Coming Alive",
                "Nightlife, rooftops and unforgettable experiences await.",
                "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=800&auto=format&fit=crop"
            )
            else -> Triple(
                "🌌 Johannesburg Never Sleeps",
                "Find places still open around you.",
                "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=800&auto=format&fit=crop"
            )
        }
    }

    // Dynamic Discovery Rhythm - World Sorting
    val worldsOrder = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> listOf("Prep", "Recover", "Food", "Travel", "24/7", "Nightlife")
            in 12..16 -> listOf("Food", "Prep", "Travel", "Recover", "24/7", "Nightlife")
            in 17..21 -> listOf("Nightlife", "Food", "Prep", "24/7", "Recover", "Travel")
            else -> listOf("24/7", "Nightlife", "Food", "Recover", "Travel", "Prep")
        }
    }

    var selectedWorld by remember(worldsOrder) { mutableStateOf(worldsOrder.first()) }
    var selectedSubcategory by remember(selectedWorld) { mutableStateOf("All") }
    var onlyOpenNow by remember { mutableStateOf(false) }

    // Search and Sort states
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var sortByOption by remember { mutableStateOf("Distance") } // "Distance", "Rating", "Open First"
    var showSortMenu by remember { mutableStateOf(false) }

    // Selected Venue Detail Bottom Sheet State
    var selectedVenueDetail by remember { mutableStateOf<ExploreVenue?>(null) }

    // Subcategories for current World
    val subcategories = remember(selectedWorld) {
        when (selectedWorld) {
            "Nightlife" -> listOf("All", "Nightclubs", "Lounges", "Cocktail Bars", "Shisanyama", "Rooftops", "Pubs")
            "Food" -> listOf("All", "Fine Dining", "Casual Dining", "Cafés", "Coffee Shops", "Brunch", "Food Markets")
            "Prep" -> listOf("All", "Barbers", "Salons", "Boutiques", "Gyms", "Pilates")
            "Recover" -> listOf("All", "Spas", "Recovery Clubs", "Yoga", "Wellness")
            "Travel" -> listOf("All", "Hotels", "Resorts", "Viewpoints", "Wine Estates")
            else -> listOf("All", "24-Hour Dining", "Casinos", "24-Hour Gyms", "Fuel Stations")
        }
    }

    // Filter & Sort Logic
    val filteredVenues = remember(venues, selectedWorld, selectedSubcategory, onlyOpenNow, searchQuery, sortByOption, currentHour) {
        var result = venues.filter { venue ->
            (selectedWorld.isEmpty() || venue.category == selectedWorld) &&
            (selectedSubcategory == "All" || venue.subcategory.contains(selectedSubcategory, ignoreCase = true) || venue.attributes.any { it.contains(selectedSubcategory, ignoreCase = true) })
        }

        if (onlyOpenNow) {
            result = result.filter { venue ->
                if (venue.is24Hours) true
                else {
                    val start = venue.startHour
                    val end = venue.endHour
                    if (start < end) currentHour in start until end
                    else currentHour >= start || currentHour < end
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            result = result.filter { venue ->
                venue.name.lowercase().contains(q) ||
                venue.area.lowercase().contains(q) ||
                venue.subcategory.lowercase().contains(q) ||
                venue.attributes.any { it.lowercase().contains(q) }
            }
        }

        when (sortByOption) {
            "Rating" -> result.sortedByDescending { it.rating }
            "Open First" -> result.sortedByDescending { if (it.is24Hours) 24 else it.endHour }
            else -> result.sortedBy { it.distanceText }
        }
    }

    // Heading Title & Subtitle based on World
    val (headingTitle, headingSubtitle) = remember(selectedWorld) {
        when (selectedWorld) {
            "Nightlife" -> Pair("Tonight's Hotspots", "Discover the city's best nightlife venues & lounges.")
            "Food" -> Pair("Places to Eat", "Restaurants, cafés and fine dining experiences nearby.")
            "Prep" -> Pair("Get Ready", "Barbers, salons, boutiques and prep spots before heading out.")
            "Recover" -> Pair("Time to Recharge", "Spas, wellness retreats and recovery clubs nearby.")
            "Travel" -> Pair("Explore Local Gems", "Hotels, scenic viewpoints and iconic attractions.")
            else -> Pair("Always Open 24/7", "Places you can visit any time of the day or night.")
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(themeBg),
        color = themeBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. TOP NAVIGATION BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("explore_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Explore The City", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Text("Johannesburg • Realtime Discovery", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                    }

                    Row {
                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Tune, contentDescription = "Sort", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(cardBg)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort by Distance", color = Color.White) },
                                    onClick = { sortByOption = "Distance"; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Rating", color = Color.White) },
                                    onClick = { sortByOption = "Rating"; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Operating Hours", color = Color.White) },
                                    onClick = { sortByOption = "Open First"; showSortMenu = false }
                                )
                            }
                        }
                    }
                }

                // EXPANDABLE SEARCH BAR
                AnimatedVisibility(visible = isSearchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search venues, suburbs, tags...", color = Color.White.copy(alpha = 0.4f)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentPurple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // MAIN CONTENT LIST
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // 2. DYNAMIC TIME-AWARE HERO CARD
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            ) {
                                AsyncImage(
                                    model = heroImg,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.3f),
                                                    Color.Black.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                )

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(20.dp)
                                ) {
                                    Surface(
                                        color = accentPurple.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, accentPurple.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            "RHYTHM OF THE CITY • $timeCategory".uppercase(),
                                            color = neonCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        heroTitle,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        heroSubtitle,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // 3. DISCOVERY WORLDS CAROUSEL
                    item {
                        Column {
                            Text(
                                text = "Discover Places Around You",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(worldsOrder) { world ->
                                    val isSelected = world == selectedWorld
                                    val (worldIcon, worldLabel) = when (world) {
                                        "Nightlife" -> Pair("🌙", "Nightlife")
                                        "Food" -> Pair("🍽️", "Food")
                                        "Prep" -> Pair("✨", "Prep")
                                        "Recover" -> Pair("🌿", "Recover")
                                        "Travel" -> Pair("✈️", "Travel")
                                        "24/7" -> Pair("🕒", "24/7")
                                        else -> Pair("📍", world)
                                    }

                                    Surface(
                                        color = if (isSelected) accentPurple else cardBg,
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, if (isSelected) neonCyan else Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier
                                            .clickable { selectedWorld = world }
                                            .testTag("explore_world_$world")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(worldIcon, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                worldLabel,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. SUB-CATEGORY & TOGGLE FILTER CHIPS
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // "Open Now" Filter Chip
                            item {
                                FilterChip(
                                    selected = onlyOpenNow,
                                    onClick = { onlyOpenNow = !onlyOpenNow },
                                    label = { Text("🟢 Open Now Only", fontSize = 11.sp, color = if (onlyOpenNow) Color.Black else Color.White) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = openGreen,
                                        containerColor = cardBg
                                    )
                                )
                            }

                            items(subcategories) { sub ->
                                val isSubSelected = sub == selectedSubcategory
                                FilterChip(
                                    selected = isSubSelected,
                                    onClick = { selectedSubcategory = sub },
                                    label = { Text(sub, fontSize = 11.sp, color = if (isSubSelected) Color.White else Color.White.copy(alpha = 0.7f)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accentPurple.copy(alpha = 0.6f),
                                        containerColor = cardBg
                                    )
                                )
                            }
                        }
                    }

                    // 5. DYNAMIC SECTION HEADING & RESULTS STATS
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(headingTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(headingSubtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                            Surface(
                                color = cardBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    "${filteredVenues.size} Places",
                                    color = neonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // 6. VENUE CARDS LIST
                    if (filteredVenues.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No venues match your current filters.", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                    TextButton(onClick = {
                                        selectedSubcategory = "All"
                                        onlyOpenNow = false
                                        searchQuery = ""
                                    }) {
                                        Text("Reset Filters", color = accentPurple, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredVenues, key = { it.id }) { venue ->
                            ExploreVenueCardDetailed(
                                venue = venue,
                                currentHour = currentHour,
                                onCardClick = {
                                    selectedVenueDetail = venue
                                    onSelectVenue(venue)
                                },
                                onLikeToggle = { onLikeToggle(venue.id) },
                                onDirectionsClick = {
                                    Toast.makeText(context, "🗺️ Opening Route to ${venue.name} (${venue.distanceText})", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // MODAL / BOTTOM SHEET: VENUE DETAIL OVERLAY
            if (selectedVenueDetail != null) {
                val v = selectedVenueDetail!!
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.85f),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Image & Header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                ) {
                                    AsyncImage(
                                        model = v.imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, cardBg)
                                                )
                                            )
                                    )
                                    IconButton(
                                        onClick = { selectedVenueDetail = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                    }
                                }

                                // Info Details Body
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 20.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(v.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                if (v.isVerified) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = neonCyan, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Text("${v.subcategory} • ${v.area}", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                                        }

                                        IconButton(onClick = { onLikeToggle(v.id) }) {
                                            Icon(
                                                if (v.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Favorite",
                                                tint = if (v.isLiked) Color.Red else Color.White
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = warmAmber, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${v.rating} (${v.reviewCount} reviews)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Icon(Icons.Default.Place, contentDescription = null, tint = accentPurple, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(v.distanceText, color = Color.White, fontSize = 13.sp)
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                                    Text("Address & Operating Hours", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("📍 ${v.address}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text("🕒 ${v.openDays} • ${if (v.is24Hours) "Open 24 Hours" else "${v.startHour}:00 - ${v.endHour}:00"}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Experience Tags", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        v.attributes.forEach { attr ->
                                            Surface(
                                                color = cardBg,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                            ) {
                                                Text(attr, color = neonCyan, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (v.hasClubLobby) {
                                            Button(
                                                onClick = {
                                                    selectedVenueDetail = null
                                                    onNavigateToLobby(v.id)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.MeetingRoom, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Club Lobby")
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "🗺️ Directions started to ${v.address}", Toast.LENGTH_LONG).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Directions, contentDescription = null, tint = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Directions", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// DETAILED EXPLORE VENUE CARD COMPOSABLE
// -------------------------------------------------------------------------
@Composable
fun ExploreVenueCardDetailed(
    venue: ExploreVenue,
    currentHour: Int,
    onCardClick: () -> Unit,
    onLikeToggle: () -> Unit,
    onDirectionsClick: () -> Unit
) {
    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val warmAmber = Color(0xFFFFB703)
    val openGreen = Color(0xFF00E676)
    val closedRed = Color(0xFFFF1744)

    val isOpen = remember(venue, currentHour) {
        if (venue.is24Hours) true
        else {
            val start = venue.startHour
            val end = venue.endHour
            if (start < end) currentHour in start until end
            else currentHour >= start || currentHour < end
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                AsyncImage(
                    model = venue.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isOpen) openGreen.copy(alpha = 0.2f) else closedRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isOpen) openGreen else closedRed)
                    ) {
                        Text(
                            text = if (isOpen) "🟢 Open • Closes ${venue.endHour}:00" else "🔴 Closed • Opens ${venue.startHour}:00",
                            color = if (isOpen) openGreen else closedRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(
                        onClick = onLikeToggle,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            if (venue.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save",
                            tint = if (venue.isLiked) Color.Red else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = warmAmber, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${venue.rating}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${venue.subcategory} • ${venue.area}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(venue.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(venue.distanceText, color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        venue.attributes.take(3).forEach { attr ->
                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(attr, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = onDirectionsClick, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Directions, contentDescription = "Directions", tint = neonCyan, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onCardClick, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Details", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
