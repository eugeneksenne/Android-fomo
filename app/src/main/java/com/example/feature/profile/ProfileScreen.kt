package com.example.feature.profile

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.data.MyCircleRepository
import com.example.core.data.CircleStory
import com.example.core.data.CircleFriend
import com.example.core.data.DiscoverPerson
import com.example.core.data.FriendRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBackClick: () -> Unit, onSettingsClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationRepository = remember { com.example.core.data.notification.NotificationRepository.getInstance(context) }
    
    // Notifications State
    val notifications by notificationRepository.allNotifications.collectAsState(initial = emptyList())
    val unreadCount by notificationRepository.unreadCount.collectAsState(initial = 0)
    
    // Circle Repository States
    val stories by MyCircleRepository.storiesState.collectAsState()
    val friends by MyCircleRepository.friendsState.collectAsState()
    val discoverPeople by MyCircleRepository.discoverPeopleState.collectAsState()
    val friendRequests by MyCircleRepository.friendRequestsState.collectAsState()
    
    // Core Profile Tab state: 0: Vibe & Stats, 1: Social Circle, 2: Activity Feed
    var selectedTab by remember { mutableStateOf(0) }
    var selectedNotificationFilter by remember { mutableStateOf("ALL") }

    // -------------------------------------------------------------------------
    // CORE INTERACTIVE USER STATE OVERRIDES
    // -------------------------------------------------------------------------
    var currentPresenceVenue by remember { mutableStateOf("The Artistry") }
    var currentPresenceStatus by remember { mutableStateOf("Amapiano • With 5 Friends") }
    var showCheckInSelectionDialog by remember { mutableStateOf(false) }

    // Interactive Vibe DNA states
    var userVibeDnaList by remember { mutableStateOf(listOf("Amapiano", "Rooftops", "Foodie", "Creator")) }
    var showVibeDnaCustomizerDialog by remember { mutableStateOf(false) }

    // Interactive Social Energy booster states
    var weekendEnergyValue by remember { mutableStateOf(96) }
    var outsideStreakCount by remember { mutableStateOf(8) }
    var socialEnergyPulseActive by remember { mutableStateOf(false) }

    // Story overlay viewer states
    var activeStoryViewList by remember { mutableStateOf<List<CircleStory>?>(null) }
    var activeStoryIndex by remember { mutableStateOf(0) }
    var storyProgressPercent by remember { mutableStateOf(0f) }
    var storyViewerTimerActive by remember { mutableStateOf(false) }

    // Posting story wizard states
    var showPostStoryDialog by remember { mutableStateOf(false) }
    var postStorySelectedVibeIdx by remember { mutableStateOf(0) }
    var postStoryMusicOverlayText by remember { mutableStateOf("Mnike - Tyler ICU") }

    // Story preset backgrounds for the user posting a story
    val postStoryBackgrounds = listOf(
        "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop" to "Altitude Rooftop",
        "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=600&auto=format&fit=crop" to "Taboo Dancefloor",
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=600&auto=format&fit=crop" to "Amapiano Summit Stage",
        "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=600&auto=format&fit=crop" to "Konka Main Floor",
        "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=600&auto=format&fit=crop" to "VIP Lounge View"
    )

    // Running effect for full screen Instagram-like stories viewer progression
    LaunchedEffect(activeStoryViewList, activeStoryIndex, storyViewerTimerActive) {
        if (activeStoryViewList != null && storyViewerTimerActive) {
            storyProgressPercent = 0f
            val storyDurationMs = 5000f
            val updateIntervalMs = 50
            val increments = storyDurationMs / updateIntervalMs
            for (i in 1..increments.toInt()) {
                delay(updateIntervalMs.toLong())
                if (activeStoryViewList == null || !storyViewerTimerActive) break
                storyProgressPercent = i / increments
            }
            if (activeStoryViewList != null && storyViewerTimerActive) {
                // Advance to next story or auto-close
                val nextIdx = activeStoryIndex + 1
                if (nextIdx < activeStoryViewList!!.size) {
                    activeStoryIndex = nextIdx
                } else {
                    activeStoryViewList = null
                    storyViewerTimerActive = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp)
            ) {
                // Visual Background Hero image
                item {
                    HeroSection()
                }
                
                // Tab Selection Row (Expanded to 3 Premium Navigation Panels)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TabButton(
                            text = "Vibe & Stats",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "Social Circle",
                            isSelected = selectedTab == 1,
                            badgeCount = friendRequests.count { it.type == "Incoming" },
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "Activity Feed",
                            isSelected = selectedTab == 2,
                            badgeCount = unreadCount,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // RENDER TAB VIEW
                when (selectedTab) {
                    0 -> {
                        // TAB 0: VIBE & STATS (MAIN IDENTIFICATION AND DISCOVERY PROGRESSION)
                        item {
                            LivePresenceCard(
                                venue = currentPresenceVenue,
                                status = currentPresenceStatus,
                                onChangeClick = { showCheckInSelectionDialog = true }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            StoriesSection(
                                stories = stories,
                                onStoryClick = { clickedStory ->
                                    // Mark story viewed in repository
                                    MyCircleRepository.markStoryViewed(clickedStory.id)
                                    // Load list of stories starting at this clicked story index
                                    val startIdx = stories.indexOfFirst { it.id == clickedStory.id }.coerceAtLeast(0)
                                    activeStoryViewList = stories
                                    activeStoryIndex = startIdx
                                    storyViewerTimerActive = true
                                },
                                onPostStoryClick = { showPostStoryDialog = true }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            IdentitySection(
                                vibeTags = userVibeDnaList,
                                weekendEnergy = weekendEnergyValue,
                                outsideStreak = outsideStreakCount,
                                socialEnergyPulseActive = socialEnergyPulseActive,
                                onCustomizeVibeClick = { showVibeDnaCustomizerDialog = true },
                                onEnergyBoostClick = {
                                    scope.launch {
                                        socialEnergyPulseActive = true
                                        weekendEnergyValue = (weekendEnergyValue + 2).coerceAtMost(100)
                                        outsideStreakCount += 1
                                        Toast.makeText(context, "🔥 Weekend Vibe Energy Boosted +2%!", Toast.LENGTH_SHORT).show()
                                        delay(1000)
                                        socialEnergyPulseActive = false
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            PlacesSection(onSeeAllClick = {
                                Toast.makeText(context, "Loading weekly Heatmap trail for Rosebank", Toast.LENGTH_SHORT).show()
                            })
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }

                    1 -> {
                        // TAB 1: SOCIAL CIRCLE (TRANSACTIONS WITH FRIENDS, MUTUAL REQUESTS, DISCOVERY SELECTION)
                        
                        // Section: Friend Requests
                        if (friendRequests.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Pending Friend Requests (${friendRequests.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                            items(friendRequests) { req ->
                                FriendRequestRow(
                                    req = req,
                                    onAccept = {
                                        MyCircleRepository.handleAcceptRequest(req.id)
                                        Toast.makeText(context, "Accepted friend request from ${req.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    onDecline = {
                                        MyCircleRepository.handleDeclineRequest(req.id)
                                        Toast.makeText(context, "Declined friend request", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp), color = MaterialTheme.colorScheme.surfaceVariant) }
                        }

                        // Section: Friends List Sentry
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "My Circle Friends (${friends.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = friends.count { it.status == "Online" }.toString() + " Online",
                                    fontSize = 12.sp,
                                    color = Color(0xFF34C759),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (friends.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No friends connected. Use Vibe Discovery below!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(friends) { friend ->
                                FriendItemRow(
                                    friend = friend,
                                    onCloseFriendToggle = {
                                        MyCircleRepository.toggleCloseFriend(friend.id)
                                        val isCFNow = !friend.isCloseFriend
                                        Toast.makeText(context, if (isCFNow) "Starred as Close Friend!" else "Removed from Close Friends", Toast.LENGTH_SHORT).show()
                                    },
                                    onRemoveFriend = {
                                        MyCircleRepository.handleRemoveFriend(friend.id)
                                        Toast.makeText(context, "Disconnected connection", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp), color = MaterialTheme.colorScheme.surfaceVariant) }

                        // Section: People Discovery Vibe Match
                        item {
                            Text(
                                text = "Vibe Match Suggestions (AI Generated)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }

                        items(discoverPeople) { person ->
                            DiscoverPersonRow(
                                person = person,
                                onAddFriend = {
                                    MyCircleRepository.handleAddFriendDiscover(person.id)
                                    Toast.makeText(context, "Sent friend request to ${person.name}", Toast.LENGTH_SHORT).show()
                                },
                                onFollow = {
                                    MyCircleRepository.handleFollowDiscover(person.id)
                                    val followText = if (!person.isFollowing) "Following" else "Unfollowed"
                                    Toast.makeText(context, "$followText ${person.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }

                    2 -> {
                        // TAB 2: ACTIVITY NOTIFICATION FEED
                        item {
                            NotificationHeaderSection(
                                selectedFilter = selectedNotificationFilter,
                                onFilterSelected = { selectedNotificationFilter = it },
                                onMarkAllRead = {
                                    scope.launch { notificationRepository.markAllAsRead() }
                                },
                                onClearAll = {
                                    scope.launch { notificationRepository.clearAll() }
                                }
                            )
                        }
                        
                        // Activity generator console
                        item {
                            ActivityConsoleCard(
                                onGenerate = { type ->
                                    scope.launch {
                                        val newNotif = createSampleNotification(type)
                                        notificationRepository.insert(newNotif)
                                    }
                                }
                            )
                        }
                        
                        // Notification list
                        val filteredList = notifications.filter { 
                            selectedNotificationFilter == "ALL" || it.type == selectedNotificationFilter 
                        }
                        
                        if (filteredList.isEmpty()) {
                            item {
                                EmptyNotificationsView()
                            }
                        } else {
                            items(
                                items = filteredList,
                                key = { it.id }
                            ) { notification ->
                                NotificationItemRow(
                                    notification = notification,
                                    onMarkRead = {
                                        scope.launch { notificationRepository.markAsRead(notification.id) }
                                    },
                                    onDelete = {
                                        scope.launch { notificationRepository.delete(notification.id) }
                                    }
                                )
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------
            // OVERLAY SYSTEM: INSTAGRAM-LIKE STORY VIEWER CONSOLE
            // -------------------------------------------------------------------------
            if (activeStoryViewList != null && activeStoryIndex < activeStoryViewList!!.size) {
                val currentStory = activeStoryViewList!![activeStoryIndex]
                
                // Full Screen Story Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            // Tap right to skip, left to go back
                            storyViewerTimerActive = false
                            val nextIdx = activeStoryIndex + 1
                            if (nextIdx < activeStoryViewList!!.size) {
                                activeStoryIndex = nextIdx
                                storyViewerTimerActive = true
                            } else {
                                activeStoryViewList = null
                            }
                        }
                ) {
                    // Main high-res background media
                    AsyncImage(
                        model = currentStory.mediaUrl,
                        contentDescription = "Story Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Cyber shadow gradient at top and bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )

                    // Top progression bar and user headers
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                            .align(Alignment.TopCenter),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Horizontal multi-segmented progress indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            activeStoryViewList!!.forEachIndexed { idx, _ ->
                                val progress = when {
                                    idx < activeStoryIndex -> 1.0f
                                    idx == activeStoryIndex -> storyProgressPercent
                                    else -> 0.0f
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }

                        // User profile meta row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = currentStory.userAvatar,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        currentStory.userName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        currentStory.timestamp + " • " + currentStory.badgeText,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(onClick = { activeStoryViewList = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }

                    // Bottom info (Music overlay + active check-in presence)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStory.venueName != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Place, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        currentStory.venueName!!,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        if (currentStory.eventName != null) {
                            Surface(
                                color = Color(0xFFFF2D55).copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FlashOn, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        currentStory.eventName!!,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        if (currentStory.musicPlaying != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = "Music", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    currentStory.musicPlaying!!,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------
            // DIALOGS: PRESENCE CHECK-IN SELECTOR
            // -------------------------------------------------------------------------
            if (showCheckInSelectionDialog) {
                AlertDialog(
                    onDismissRequest = { showCheckInSelectionDialog = false },
                    title = { Text("Set My Presence Check-In", color = Color.White, fontWeight = FontWeight.Bold) },
                    containerColor = Color(0xFF0F1524),
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Select an active nightlife location to broadcast to your circle friends:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            val venues = listOf(
                                "The Artistry" to "Amapiano • With 5 Friends",
                                "Konka Soweto" to "VIP Stage • Table 12",
                                "Rockets Lounge" to "Deep House Sunset Session",
                                "Altitude Club" to "DJ Kent Live Stream",
                                "Sky Lounge" to "Unwind Chill Mode"
                            )
                            
                            venues.forEach { (venueName, statusLine) ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentPresenceVenue = venueName
                                            currentPresenceStatus = statusLine
                                            showCheckInSelectionDialog = false
                                            Toast.makeText(context, "Check-In updated! Presence set to $venueName", Toast.LENGTH_SHORT).show()
                                        },
                                    color = if (currentPresenceVenue == venueName) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(venueName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(statusLine, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCheckInSelectionDialog = false }) {
                            Text("Close", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }

            // -------------------------------------------------------------------------
            // DIALOGS: VIBE DNA CUSTOMIZER
            // -------------------------------------------------------------------------
            if (showVibeDnaCustomizerDialog) {
                AlertDialog(
                    onDismissRequest = { showVibeDnaCustomizerDialog = false },
                    title = { Text("Customize Vibe DNA", color = Color.White, fontWeight = FontWeight.Bold) },
                    containerColor = Color(0xFF0F1524),
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Select tags that represent your nightlife identity. This fuels the AI Vibe Match recommendations.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            val allVibeTags = listOf("Amapiano", "Rooftops", "Foodie", "Creator", "Techno", "Deep House", "VIP Tables", "R&B", "Jazz", "Afro House", "Mixology")
                            
                            // Flow row or simple grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val chunks = allVibeTags.chunked(3)
                                chunks.forEach { rowTags ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        rowTags.forEach { tag ->
                                            val isSelected = userVibeDnaList.contains(tag)
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        if (isSelected) {
                                                            userVibeDnaList = userVibeDnaList.filter { it != tag }
                                                        } else {
                                                            userVibeDnaList = userVibeDnaList + tag
                                                        }
                                                    },
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(
                                                    text = tag,
                                                    color = if (isSelected) Color.Black else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                        if (rowTags.size < 3) {
                                            Spacer(modifier = Modifier.weight((3 - rowTags.size).toFloat()))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showVibeDnaCustomizerDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Tags", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // -------------------------------------------------------------------------
            // DIALOGS: POST STORY PRESSET FORM
            // -------------------------------------------------------------------------
            if (showPostStoryDialog) {
                AlertDialog(
                    onDismissRequest = { showPostStoryDialog = false },
                    title = { Text("Publish Live Moment", color = Color.White, fontWeight = FontWeight.Bold) },
                    containerColor = Color(0xFF0F1524),
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Text("Select an atmosphere template and attach a music overlay to post directly to friends:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            // Preset backgrounds selector
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(items = postStoryBackgrounds) { idx, item ->
                                    val url = item.first
                                    val name = item.second
                                    val isSelected = postStorySelectedVibeIdx == idx
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                2.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { postStorySelectedVibeIdx = idx }
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Text(name, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp), textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = postStoryMusicOverlayText,
                                onValueChange = { postStoryMusicOverlayText = it },
                                label = { Text("Music / Caption Overlay Text") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val selectedBgUrl = postStoryBackgrounds[postStorySelectedVibeIdx].first
                                MyCircleRepository.addStory(
                                    userName = "Jordan Reed",
                                    mediaUrl = selectedBgUrl,
                                    text = postStoryMusicOverlayText,
                                    type = "Story"
                                )
                                showPostStoryDialog = false
                                Toast.makeText(context, "🎉 Live Moment posted successfully to My Circle!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Post Now", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPostStoryDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT MODULES
// -------------------------------------------------------------------------

@Composable
fun HeroSection() {
    val currentUser = remember {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }
    }
    val displayName = currentUser?.displayName?.ifEmpty { null }
        ?: currentUser?.email?.substringBefore("@")
        ?: if (currentUser?.isAnonymous == true) "Guest Explorer" else "Jordan Reed"
    val handle = currentUser?.email?.substringBefore("@") ?: "jordan_r"
    val avatarUrl = currentUser?.photoUrl?.toString() ?: "https://i.pravatar.cc/150?img=11"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=1000&auto=format&fit=crop",
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                        startY = 180f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp),
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Text("@$handle", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Weekend explorer chasing unforgettable experiences in the city.",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun LivePresenceCard(
    venue: String,
    status: String,
    onChangeClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF34C759)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Online Presence Broadcast", color = Color(0xFF34C759), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                
                Button(
                    onClick = onChangeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Change", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(venue, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
fun StoriesSection(
    stories: List<CircleStory>,
    onStoryClick: (CircleStory) -> Unit,
    onPostStoryClick: () -> Unit
) {
    Column {
        Text("Stories & Moments", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Post story circular CTA trigger
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onPostStoryClick() }) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Story", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Post Moment", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Circle Story item circles
            items(stories) { story ->
                val borderColor = try {
                    if (story.isViewed) Color.Gray else Color(android.graphics.Color.parseColor(story.ringColor))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onStoryClick(story) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, borderColor, CircleShape)
                            .padding(4.dp)
                    ) {
                        AsyncImage(
                            model = story.userAvatar,
                            contentDescription = story.userName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(story.userName, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun IdentitySection(
    vibeTags: List<String>,
    weekendEnergy: Int,
    outsideStreak: Int,
    socialEnergyPulseActive: Boolean,
    onCustomizeVibeClick: () -> Unit,
    onEnergyBoostClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Vibe DNA Identity", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Customize Vibe",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onCustomizeVibeClick() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            vibeTags.forEach { tag ->
                IdentityChip(tag)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Social Energy & Progression", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Boost Vibe",
                color = if (socialEnergyPulseActive) Color.Green else MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onEnergyBoostClick() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EnergyCard(title = "Weekend Energy", value = "$weekendEnergy%", modifier = Modifier.weight(1f))
            EnergyCard(title = "Outside Streak", value = "$outsideStreak Weeks", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun IdentityChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
fun EnergyCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun PlacesSection(onSeeAllClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Weekly Trails", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("See Trail Map", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSeeAllClick() })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=600&auto=format&fit=crop",
                    contentDescription = "Heatmap",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.5f
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text("Most Active Region", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Rosebank Hub", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("42 Venues Explored", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 1: SOCIAL CIRCLE COMPONENT ROWS
// -------------------------------------------------------------------------

@Composable
fun FriendRequestRow(
    req: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = req.avatarUrl,
                contentDescription = req.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(req.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Text(req.reason, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${req.mutualFriendsCount} mutual friends", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Decline", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
                
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun FriendItemRow(
    friend: CircleFriend,
    onCloseFriendToggle: () -> Unit,
    onRemoveFriend: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = friend.avatarUrl,
                    contentDescription = friend.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            if (friend.isCloseFriend) Color(0xFF34C759) else Color.Transparent,
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (friend.status == "Online") Color(0xFF34C759) else Color.Gray)
                        .border(1.dp, Color.Black, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    if (friend.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    }
                }
                Text(friend.username, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(friend.currentActivity, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                Text(friend.distanceText, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCloseFriendToggle) {
                    Icon(
                        imageVector = if (friend.isCloseFriend) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Close Friend",
                        tint = if (friend.isCloseFriend) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f)
                    )
                }
                IconButton(onClick = onRemoveFriend) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Connection", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun DiscoverPersonRow(
    person: DiscoverPerson,
    onAddFriend: () -> Unit,
    onFollow: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = person.avatarUrl,
                contentDescription = person.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(person.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    if (person.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    }
                }
                Text(person.reason, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(person.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!person.isFriendRequested) {
                    Button(
                        onClick = onAddFriend,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Connect", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Pending", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    }
                }
                
                IconButton(onClick = onFollow) {
                    Icon(
                        imageVector = if (person.isFollowing) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Follow",
                        tint = if (person.isFollowing) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

fun createSampleNotification(type: String): com.example.core.data.notification.NotificationEntity {
    return com.example.core.data.notification.NotificationEntity(
        type = type,
        senderName = "VIP Member",
        senderAvatar = "https://i.pravatar.cc/150?img=5",
        content = when (type) {
            "LIKE" -> "liked your moment"
            "COMMENT" -> "commented: '🔥 Great vibe!'"
            "MENTION" -> "mentioned you in a post"
            "FOLLOW" -> "started following you"
            else -> "interacted with your profile"
        },
        timestamp = System.currentTimeMillis(),
        isRead = false
    )
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
            contentColor = if (isSelected) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier.height(38.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.Black else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = if (isSelected) Color.White else Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationHeaderSection(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Activity Feed", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onMarkAllRead) { Text("Mark Read", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary) }
                TextButton(onClick = onClearAll) { Text("Clear", fontSize = 11.sp, color = Color.Red) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL", "LIKE", "COMMENT", "MENTION", "FOLLOW").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter, fontSize = 10.sp) }
                )
            }
        }
    }
}

@Composable
fun ActivityConsoleCard(onGenerate: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("⚡ Activity Test Actions", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("LIKE", "COMMENT", "MENTION", "FOLLOW").forEach { type ->
                    Button(
                        onClick = { onGenerate(type) },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("+ $type", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationsView() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text("No notifications match filter", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
    }
}

@Composable
fun NotificationItemRow(
    notification: com.example.core.data.notification.NotificationEntity,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        color = if (notification.isRead) Color.White.copy(alpha = 0.03f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = notification.senderAvatar,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(notification.senderName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Text(notification.content, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
            IconButton(onClick = onMarkRead) {
                Icon(Icons.Default.Check, contentDescription = "Read", tint = if (notification.isRead) Color.Gray else MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}
