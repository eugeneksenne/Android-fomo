package com.example.feature.discover

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.core.data.Event
import com.example.core.data.EventRepository
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsHomeScreen(
    onBackClick: () -> Unit,
    onNavigateToEventDetails: (String) -> Unit,
    onNavigateToLobby: (String) -> Unit
) {
    val context = LocalContext.current
    val events by EventRepository.eventsState.collectAsState()
    val plannedEvents = remember(events) { events.filter { it.isPlanned } }

    // Screen States
    var searchQuery by remember { mutableStateOf("") }
    
    // Determine default category based on current time
    // Specification: "Daytime -> All selected by default. From 18:00 onward -> Tonight selected automatically."
    val defaultCategory = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= 18) "Tonight" else "All"
    }
    var selectedCategory by remember { mutableStateOf(defaultCategory) }
    
    var showCalendarDialog by remember { mutableStateOf(false) }
    var selectedPreviewEvent by remember { mutableStateOf<Event?>(null) }
    
    // Auto-scroll Highlights state
    var highlightIndex by remember { mutableStateOf(0) }
    val highlights = remember(events) { events.filter { it.isSponsored } }

    LaunchedEffect(highlights) {
        if (highlights.isNotEmpty()) {
            while (true) {
                delay(6000)
                highlightIndex = (highlightIndex + 1) % highlights.size
            }
        }
    }

    // Filter events
    val filteredEvents = remember(events, searchQuery, selectedCategory) {
        events.filter { event ->
            // Search Query
            val matchesSearch = searchQuery.isBlank() || 
                event.title.contains(searchQuery, ignoreCase = true) ||
                event.venueName.contains(searchQuery, ignoreCase = true) ||
                event.artists.any { it.contains(searchQuery, ignoreCase = true) } ||
                event.genres.any { it.contains(searchQuery, ignoreCase = true) }

            // Category Filter
            val matchesCategory = when (selectedCategory) {
                "Tonight" -> event.dateText.equals("Tonight", ignoreCase = true)
                "This Week" -> event.dateText.equals("Tonight", ignoreCase = true) || event.dateText.contains("Tomorrow", ignoreCase = true)
                else -> true // "All"
            }

            matchesSearch && matchesCategory
        }.sortedBy { 
            // Intelligent Sorting
            // 1. Sponsored first, then countdown/starts soon
            if (it.isSponsored) 0 else 1 
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            topBar = {
                EventsHeader(
                    onBackClick = onBackClick,
                    onCalendarClick = { showCalendarDialog = true },
                    onFilterClick = {
                        Toast.makeText(context, "Sorting prioritized by: Distance, Live Now & Ripple Heat", Toast.LENGTH_LONG).show()
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search events, artists, venues, genres...", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1C1C1E),
                            unfocusedContainerColor = Color(0xFF1C1C1E),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("event_search_input")
                    )
                }

                // Tonight's Highlights Hero Section
                if (searchQuery.isBlank() && highlights.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "Tonight's Highlights",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            )
                            HeroCarousel(
                                event = highlights[highlightIndex % highlights.size],
                                onPreviewClick = { selectedPreviewEvent = it }
                            )
                        }
                    }
                }

                // My Plans Section
                if (searchQuery.isBlank() && plannedEvents.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 24.dp)) {
                            Text(
                                text = "My Plans",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(plannedEvents) { event ->
                                    MyPlanCard(
                                        event = event,
                                        onCardClick = { selectedPreviewEvent = event },
                                        onRouteClick = {
                                            Toast.makeText(context, "Routing with FOMO Navigation to ${event.venueName}. ETA: ${event.driveTime} drive.", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Categories Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryChip(
                            label = "All 🔥",
                            selected = selectedCategory == "All",
                            onClick = { selectedCategory = "All" }
                        )
                        CategoryChip(
                            label = "Tonight 🌙",
                            selected = selectedCategory == "Tonight",
                            onClick = { selectedCategory = "Tonight" }
                        )
                        CategoryChip(
                            label = "This Week 📅",
                            selected = selectedCategory == "This Week",
                            onClick = { selectedCategory = "This Week" }
                        )
                    }
                }

                // Event Feed List
                if (filteredEvents.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                        ) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No events found in this category.", color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                } else {
                    items(filteredEvents) { event ->
                        PremiumEventCard(
                            event = event,
                            onCardClick = { selectedPreviewEvent = event }
                        )
                    }
                }
            }
        }

        // Calendar Dialog Overlay
        if (showCalendarDialog) {
            CalendarViewDialog(
                plannedEvents = plannedEvents,
                onDismiss = { showCalendarDialog = false },
                onEventClick = { event ->
                    selectedPreviewEvent = event
                    showCalendarDialog = false
                }
            )
        }

        // Spring-Animated Centered Floating Event Preview
        AnimatedVisibility(
            visible = selectedPreviewEvent != null,
            enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = 0.75f)),
            exit = fadeOut() + scaleOut()
        ) {
            selectedPreviewEvent?.let { event ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable { selectedPreviewEvent = null }, // Dismiss on background tap
                    contentAlignment = Alignment.Center
                ) {
                    FloatingEventPreviewCard(
                        event = event,
                        onClose = { selectedPreviewEvent = null },
                        onNavigateToDetails = {
                            selectedPreviewEvent = null
                            onNavigateToEventDetails(event.id)
                        },
                        onNavigateToLobby = {
                            selectedPreviewEvent = null
                            onNavigateToLobby(event.venueId)
                        },
                        onToggleInterested = { EventRepository.toggleInterested(event.id) },
                        onTogglePlan = {
                            EventRepository.togglePlanned(event.id)
                            val planMsg = if (!event.isPlanned) "Added to plans!" else "Removed from plans"
                            Toast.makeText(context, planMsg, Toast.LENGTH_SHORT).show()
                        },
                        onSetReminder = { min ->
                            EventRepository.setReminder(event.id, min)
                            val remText = if (min != null) "Reminder set for $min mins before event" else "Reminder cleared"
                            Toast.makeText(context, remText, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EventsHeader(
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Johannesburg",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCalendarClick,
                    modifier = Modifier.testTag("calendar_button")
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = Color.White)
                }
                IconButton(onClick = onFilterClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Filters & Sort", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun HeroCarousel(
    event: Event,
    onPreviewClick: (Event) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(horizontal = 16.dp)
            .clickable { onPreviewClick(event) }
            .testTag("hero_carousel_card")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = event.posterUrl,
                contentDescription = event.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)),
                            startY = 150f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tonight's Highlight", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.venueName, color = Color.LightGray, fontSize = 13.sp)
                    if (event.isVenueVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("TICKETS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(event.ticketPrice, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("COUNTDOWN", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(event.countdownText, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { onPreviewClick(event) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MyPlanCard(
    event: Event,
    onCardClick: () -> Unit,
    onRouteClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(260.dp)
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = event.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        event.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        event.venueName,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (event.reminderMinutes != null) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        contentDescription = "Reminder Status",
                        tint = if (event.reminderMinutes != null) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (event.reminderMinutes != null) "Active" else "No reminder",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = onRouteClick,
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = "Route", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF1C1C1E),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("category_chip_$label")
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.LightGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun PremiumEventCard(
    event: Event,
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F10)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable { onCardClick() }
            .testTag("premium_event_card_${event.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                AsyncImage(
                    model = event.posterUrl,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Left overlay info
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(event.distance, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Heat Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = event.rippleHeat,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Shadow Gradient Bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                                startY = 120f
                            )
                        )
                )
            }
            
            // Metadata bottom details
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(event.venueName, color = Color.LightGray, fontSize = 13.sp)
                            if (event.isVenueVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                    
                    Surface(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = event.dateText,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingEventPreviewCard(
    event: Event,
    onClose: () -> Unit,
    onNavigateToDetails: () -> Unit,
    onNavigateToLobby: () -> Unit,
    onToggleInterested: () -> Unit,
    onTogglePlan: () -> Unit,
    onSetReminder: (Int?) -> Unit
) {
    val context = LocalContext.current
    var isInterested by remember { mutableStateOf(event.isInterested) }
    var isPlanned by remember { mutableStateOf(event.isPlanned) }
    var showReminderMenu by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF121214),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clickable(enabled = false) {} // block click propagation
            .testTag("floating_preview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Visual Banner Poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                AsyncImage(
                    model = event.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF121214)),
                                startY = 80f
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = event.rippleHeat,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Description and Core content
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Venue sub card
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(event.venueName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (event.isVenueVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(event.venueStatus, color = Color.LightGray, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(event.venueRating.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Logistics Grid Rows
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(event.dateText, color = Color.White, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(event.timeText, color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PinDrop, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${event.distance} (${event.driveTime} drive)", color = Color.White, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.LocalActivity, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${event.ticketPrice} (${event.ticketAvailability})", color = Color.White, fontSize = 13.sp)
                    }
                }

                // Active Flash Drop Notice
                if (event.hasFlashDrop && event.flashDropText != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D0A40), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF9C27B0), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = event.flashDropText,
                            color = Color(0xFFE040FB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = event.description,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Social Action Row (❤️ Interested, 🗓 Plan, ⏰ Reminder, 📤 Share)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SocialButton(
                        icon = if (isInterested) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = "Interested",
                        selected = isInterested,
                        onClick = {
                            isInterested = !isInterested
                            onToggleInterested()
                        }
                    )
                    SocialButton(
                        icon = if (isPlanned) Icons.Default.EventAvailable else Icons.Default.Event,
                        label = "Plan",
                        selected = isPlanned,
                        onClick = {
                            isPlanned = !isPlanned
                            onTogglePlan()
                        }
                    )
                    Box {
                        SocialButton(
                            icon = Icons.Default.Notifications,
                            label = "Reminder",
                            selected = event.reminderMinutes != null,
                            onClick = { showReminderMenu = true }
                        )
                        DropdownMenu(
                            expanded = showReminderMenu,
                            onDismissRequest = { showReminderMenu = false },
                            modifier = Modifier.background(Color(0xFF1E1E22))
                        ) {
                            DropdownMenuItem(
                                text = { Text("15 mins before", color = Color.White) },
                                onClick = {
                                    onSetReminder(15)
                                    showReminderMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("30 mins before", color = Color.White) },
                                onClick = {
                                    onSetReminder(30)
                                    showReminderMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("1 hour before", color = Color.White) },
                                onClick = {
                                    onSetReminder(60)
                                    showReminderMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("No Reminder", color = Color.Gray) },
                                onClick = {
                                    onSetReminder(null)
                                    showReminderMenu = false
                                }
                            )
                        }
                    }
                    SocialButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        selected = false,
                        onClick = {
                            Toast.makeText(context, "Link copied! Share to Instagram/WhatsApp.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Transactional Actions Row (Tickets, Route, Club Lobby)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "VIP Booking / Tickets page loading...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = event.ticketAvailability != "Sold Out",
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                    ) {
                        val ticketBtnText = if (event.ticketAvailability == "Sold Out") "Sold Out" else "Tickets"
                        Text(ticketBtnText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Launching Map Navigation to ${event.venueName}...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Route", fontSize = 13.sp)
                        }
                    }

                    if (event.venueId == "fomo_club") {
                        Button(
                            onClick = onNavigateToLobby,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F378B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                        ) {
                            Text("Lobby", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expand Detail link
                Text(
                    text = "View Full Details",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDetails() }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun SocialButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.05f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun CalendarViewDialog(
    plannedEvents: List<Event>,
    onDismiss: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF1E1E22),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FOMO Calendar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Simulated Month Matrix View (July 2026)
                Text("July 2026", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                // Days headers
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(day, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Calendar Rows
                val weeks = listOf(
                    listOf("", "", "1", "2", "3", "4", "5"),
                    listOf("6", "7", "8", "9", "10", "11", "12"),
                    listOf("13", "14", "15", "16", "17", "18", "19"),
                    listOf("20", "21", "22", "23", "24", "25", "26"),
                    listOf("27", "28", "29", "30", "31", "", "")
                )

                weeks.forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { dateStr ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (dateStr == "20") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dateStr,
                                        color = if (dateStr == "20") MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (dateStr == "20") FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (dateStr == "20" || dateStr == "24" || dateStr == "26") {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(Color.Red, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Your Scheduled Plans", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                if (plannedEvents.isEmpty()) {
                    Text("No planned events. Tap 'Plan' on any event to build your schedule.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(plannedEvents) { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEventClick(event) }
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    AsyncImage(model = event.posterUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(event.venueName, color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
