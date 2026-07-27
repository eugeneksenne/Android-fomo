package com.example.feature.discover

import androidx.activity.compose.BackHandler
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCircleHubOverlay(
    onDismiss: () -> Unit,
    onStoryClick: (Int) -> Unit,
    onNavigateToLobby: (String) -> Unit,
    onNavigateToEventDetails: (String) -> Unit
) {
    BackHandler { onDismiss() }
    val context = LocalContext.current
    val stories by com.example.core.data.MyCircleRepository.storiesState.collectAsState()
    val activityItems by com.example.core.data.MyCircleRepository.activityItemsState.collectAsState()
    val friends by com.example.core.data.MyCircleRepository.friendsState.collectAsState()
    val discoverPeople by com.example.core.data.MyCircleRepository.discoverPeopleState.collectAsState()
    val requests by com.example.core.data.MyCircleRepository.friendRequestsState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var activeFilter by remember { mutableStateOf<String?>(null) }
    var isQuickActionsMenuOpen by remember { mutableStateOf(false) }
    var isFabMenuOpen by remember { mutableStateOf(false) }

    var isMapView by remember { mutableStateOf(false) }

    var isQrDialogVisible by remember { mutableStateOf(false) }
    var isQrScannerVisible by remember { mutableStateOf(false) }
    var isNewStoryDialogVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("my_circle_hub_container"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("hub_back_button")) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "My Circle",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Your people. Your vibe.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedTab = 2 },
                                    modifier = Modifier.testTag("hub_top_add_friend")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                Box {
                                    IconButton(
                                        onClick = { isQuickActionsMenuOpen = !isQuickActionsMenuOpen },
                                        modifier = Modifier.testTag("hub_quick_actions_button")
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Quick Actions", tint = MaterialTheme.colorScheme.onSurface)
                                    }

                                    DropdownMenu(
                                        expanded = isQuickActionsMenuOpen,
                                        onDismissRequest = { isQuickActionsMenuOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Add Friend") },
                                            onClick = {
                                                isQuickActionsMenuOpen = false
                                                selectedTab = 2
                                                Toast.makeText(context, "Search nearby or add suggested", Toast.LENGTH_SHORT).show()
                                            },
                                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Scan QR Code") },
                                            onClick = {
                                                isQuickActionsMenuOpen = false
                                                isQrScannerVisible = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("My QR Code") },
                                            onClick = {
                                                isQuickActionsMenuOpen = false
                                                isQrDialogVisible = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Invite Contacts") },
                                            onClick = {
                                                isQuickActionsMenuOpen = false
                                                Toast.makeText(context, "Accessing Phone Contacts...", Toast.LENGTH_SHORT).show()
                                            },
                                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Find Nearby People") },
                                            onClick = {
                                                isQuickActionsMenuOpen = false
                                                selectedTab = 3
                                            },
                                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                                        )
                                    }
                                }
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { isNewStoryDialogVisible = true }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200&auto=format&fit=crop",
                                            contentDescription = "Your Story",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .padding(3.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                                .align(Alignment.BottomEnd),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Add Story", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            items(count = stories.size, key = { index -> stories[index].id }) { index ->
                                val story = stories[index]
                                val rColor = remember(story.ringColor) {
                                    try { Color(android.graphics.Color.parseColor(story.ringColor)) } catch(e: Exception) { Color(0xFF8A2BE2) }
                                }
                                val badgeIcon = when (story.badgeText) {
                                    "Story" -> "📸"
                                    "Live" -> "🔴"
                                    "Event" -> "🎉"
                                    "Venue" -> "📍"
                                    "Close Friends" -> "⭐"
                                    else -> "📸"
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { onStoryClick(index) }
                                        .testTag("hub_story_avatar_${story.userName.lowercase()}")
                                ) {
                                    Box(
                                        modifier = Modifier.size(68.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .border(2.5.dp, rColor, CircleShape)
                                                .padding(3.dp)
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
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(Color.Black.copy(alpha = 0.85f), CircleShape)
                                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                                .align(Alignment.BottomEnd),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(badgeIcon, fontSize = 10.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = story.userName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search friends, venues or discover people...", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("hub_search_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val metrics = listOf(
                                Triple("Online", "🟢 18 Online", 1),
                                Triple("Stories", "📸 9 Stories", 0),
                                Triple("Live", "🔴 4 Live", 0),
                                Triple("Events", "🎉 12 Events", 0),
                                Triple("Nearby", "📍 7 Nearby", 3)
                            )

                            metrics.forEach { (key, label, targetTab) ->
                                val isSelected = activeFilter == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            activeFilter = null
                                        } else {
                                            activeFilter = key
                                            selectedTab = targetTab
                                            Toast.makeText(context, "Filtered by $key", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        ) {
                            val tabTitles = listOf("Activity", "Friends", "Discover", "Nearby", "Requests")
                            tabTitles.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    modifier = Modifier.testTag("hub_tab_$title")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (isFabMenuOpen) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 60.dp)
                            ) {
                                val fabActions = listOf(
                                    Triple("Add Friend", Icons.Default.PersonAdd, {
                                        isFabMenuOpen = false
                                        selectedTab = 2
                                    }),
                                    Triple("Create Story", Icons.Default.CameraAlt, {
                                        isFabMenuOpen = false
                                        isNewStoryDialogVisible = true
                                    }),
                                    Triple("Scan QR Code", Icons.Default.Search, {
                                        isFabMenuOpen = false
                                        isQrScannerVisible = true
                                    }),
                                    Triple("My QR Code", Icons.Default.QrCode, {
                                        isFabMenuOpen = false
                                        isQrDialogVisible = true
                                    })
                                )

                                fabActions.forEach { (label, icon, action) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        SmallFloatingActionButton(
                                            onClick = action,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = { isFabMenuOpen = !isFabMenuOpen },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("hub_fab")
                        ) {
                            Icon(
                                imageVector = if (isFabMenuOpen) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Quick Floating Menu"
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (selectedTab) {
                        0 -> ActivityFeedTab(
                            items = activityItems.filter {
                                searchQuery.isEmpty() || it.userName.contains(searchQuery, ignoreCase = true) || (it.venueName?.contains(searchQuery, ignoreCase = true) ?: false)
                            },
                            onNavigateToLobby = onNavigateToLobby,
                            onNavigateToEventDetails = onNavigateToEventDetails
                        )
                        1 -> FriendsTab(
                            friends = friends.filter {
                                searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true)
                            },
                            onCloseFriendToggle = { id -> com.example.core.data.MyCircleRepository.toggleCloseFriend(id) },
                            onRemoveFriend = { id -> com.example.core.data.MyCircleRepository.handleRemoveFriend(id) }
                        )
                        2 -> DiscoverTab(
                            people = discoverPeople.filter {
                                searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.reason.contains(searchQuery, ignoreCase = true)
                            },
                            onAddFriend = { id -> com.example.core.data.MyCircleRepository.handleAddFriendDiscover(id) },
                            onFollow = { id -> com.example.core.data.MyCircleRepository.handleFollowDiscover(id) }
                        )
                        3 -> NearbyTab(
                            friends = friends,
                            isMapView = isMapView,
                            onToggleView = { isMapView = !isMapView }
                        )
                        4 -> RequestsTab(
                            incoming = requests.filter { it.type == "Incoming" },
                            outgoing = requests.filter { it.type == "Outgoing" },
                            onAccept = { id -> com.example.core.data.MyCircleRepository.handleAcceptRequest(id) },
                            onDecline = { id -> com.example.core.data.MyCircleRepository.handleDeclineRequest(id) }
                        )
                    }
                }
            }

            if (isQrDialogVisible) {
                AlertDialog(
                    onDismissRequest = { isQrDialogVisible = false },
                    title = { Text("My QR Code") },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Scan this code to add me as a friend", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .background(Color.White)
                                    .border(4.dp, Color.Black)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("@fomo_user_2026", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { isQrDialogVisible = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            if (isQrScannerVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Scan QR Code", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Hold camera up to QR Code to find your circle", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(40.dp))

                        Box(
                            modifier = Modifier
                                .size(250.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(150.dp).background(Color.White.copy(alpha = 0.1f)))
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, "Scanning completed. Neon_Vibe added successfully!", Toast.LENGTH_LONG).show()
                                isQrScannerVisible = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Simulate Successful Scan")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { isQrScannerVisible = false },
                            border = BorderStroke(1.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }

            if (isNewStoryDialogVisible) {
                var storyText by remember { mutableStateOf("") }
                var selectedStoryType by remember { mutableStateOf("Story") }
                val types = listOf("Story", "Live", "Event", "Venue", "Close Friends")

                AlertDialog(
                    onDismissRequest = { isNewStoryDialogVisible = false },
                    title = { Text("Post a Story") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Add a quick vibe check or live moment to your story timeline", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                types.forEach { t ->
                                    val active = selectedStoryType == t
                                    ElevatedFilterChip(
                                        selected = active,
                                        onClick = { selectedStoryType = t },
                                        label = { Text(t, fontSize = 11.sp) }
                                    )
                                }
                            }

                            TextField(
                                value = storyText,
                                onValueChange = { storyText = it },
                                label = { Text("Caption or Music playing...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                com.example.core.data.MyCircleRepository.addStory(
                                    userName = "You",
                                    mediaUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=800&auto=format&fit=crop",
                                    text = storyText,
                                    type = selectedStoryType
                                )
                                Toast.makeText(context, "Story posted to My Circle!", Toast.LENGTH_SHORT).show()
                                isNewStoryDialogVisible = false
                            }
                        ) {
                            Text("Post Vibe")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isNewStoryDialogVisible = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ActivityFeedTab(
    items: List<com.example.core.data.ActivityItem>,
    onNavigateToLobby: (String) -> Unit,
    onNavigateToEventDetails: (String) -> Unit
) {
    val context = LocalContext.current
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No social updates in your vibe feed yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items.size) { index ->
                val act = items[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_card_${act.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = act.userAvatar,
                                contentDescription = act.userName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(act.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (act.isLive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color.Red,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "LIVE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(act.timestamp, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(act.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                        if (act.mediaUrl != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            AsyncImage(
                                model = act.mediaUrl,
                                contentDescription = "Moment Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        if (act.isLive && act.watchersCount != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("🔴 ${act.watchersCount} people watching right now", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (act.venueId != null) {
                                Button(
                                    onClick = { onNavigateToLobby(act.venueId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (act.isLive) "Watch Broadcast" else "View Venue", fontSize = 11.sp, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = { openRoute(act.venueName ?: "Johannesburg", context) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Route", fontSize = 11.sp)
                                }
                            } else if (act.eventId != null) {
                                Button(
                                    onClick = { onNavigateToEventDetails(act.eventId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("View Event", fontSize = 11.sp, color = Color.Black)
                                }
                            } else if (act.distanceText != null) {
                                Button(
                                    onClick = { Toast.makeText(context, "Invited ${act.userName} out!", Toast.LENGTH_SHORT).show() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Invite Out", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendsTab(
    friends: List<com.example.core.data.CircleFriend>,
    onCloseFriendToggle: (String) -> Unit,
    onRemoveFriend: (String) -> Unit
) {
    val context = LocalContext.current
    if (friends.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No friends match search query.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(friends.size) { index ->
                val friend = friends[index]
                var showContextMenu by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { Toast.makeText(context, "Tap and hold for more options", Toast.LENGTH_SHORT).show() },
                            onLongClick = { showContextMenu = true }
                        )
                        .testTag("friend_card_${friend.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = friend.avatarUrl,
                                    contentDescription = friend.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                                val statusColor = if (friend.status == "Online") Color.Green else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(statusColor, CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (friend.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    }
                                    if (friend.isCloseFriend) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Star, contentDescription = "Close Friend", tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(friend.username, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            IconButton(onClick = { onCloseFriendToggle(friend.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Toggle Close Friend",
                                    tint = if (friend.isCloseFriend) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("📌 Current activity: ${friend.currentActivity}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("📍 Distance: ${friend.distanceText}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { Toast.makeText(context, "Opening direct chat...", Toast.LENGTH_SHORT).show() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Message", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { Toast.makeText(context, "Ringing friend...", Toast.LENGTH_SHORT).show() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Call", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { Toast.makeText(context, "Invite out request sent to ${friend.name}!", Toast.LENGTH_SHORT).show() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Invite Out", fontSize = 11.sp)
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share Profile") },
                        onClick = {
                            showContextMenu = false
                            Toast.makeText(context, "Profile shared to other groups", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (friend.isCloseFriend) "Remove from Close Friends" else "Mark Close Friend") },
                        onClick = {
                            showContextMenu = false
                            onCloseFriendToggle(friend.id)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mute activity notifications", color = Color.Red) },
                        onClick = {
                            showContextMenu = false
                            Toast.makeText(context, "Notifications muted", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove Friend", color = Color.Red) },
                        onClick = {
                            showContextMenu = false
                            onRemoveFriend(friend.id)
                            Toast.makeText(context, "${friend.name} removed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverTab(
    people: List<com.example.core.data.DiscoverPerson>,
    onAddFriend: (String) -> Unit,
    onFollow: (String) -> Unit
) {
    if (people.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No recommendations found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val categories = people.map { it.category }.distinct()

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            categories.forEach { cat ->
                val catPeople = people.filter { it.category == cat }
                if (catPeople.isNotEmpty()) {
                    item {
                        Text(
                            text = cat,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(catPeople.size) { index ->
                        val p = catPeople[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("discover_card_${p.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = p.avatarUrl,
                                    contentDescription = p.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (p.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(p.reason, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (p.isFriendRequested) {
                                        OutlinedButton(
                                            onClick = {},
                                            enabled = false,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Sent", fontSize = 10.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = { onAddFriend(p.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("add_friend_btn_${p.id}")
                                        ) {
                                            Text("Add", fontSize = 10.sp)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onFollow(p.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (p.isFollowing) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("follow_btn_${p.id}")
                                    ) {
                                        Text(if (p.isFollowing) "Following" else "Follow", fontSize = 10.sp)
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

@Composable
fun NearbyTab(
    friends: List<com.example.core.data.CircleFriend>,
    isMapView: Boolean,
    onToggleView: () -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isMapView) "Map View (Friends Map)" else "List View (Nearby Friends)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = onToggleView,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isMapView) Icons.Default.List else Icons.Default.Map,
                        contentDescription = "Switch View",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isMapView) "List" else "Map", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }

        if (isMapView) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    for (i in 0..10) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(300.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(180.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("You", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val activeMemberPins = listOf(
                    Triple("Amanda", Alignment.TopCenter, "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=200&auto=format&fit=crop"),
                    Triple("Jason", Alignment.BottomStart, "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=200&auto=format&fit=crop"),
                    Triple("Sarah", Alignment.CenterEnd, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop"),
                    Triple("Jessica", Alignment.TopStart, "https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=200&auto=format&fit=crop")
                )

                activeMemberPins.forEach { (name, align, avatar) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(align)
                            .padding(40.dp)
                            .clickable { Toast.makeText(context, "$name is nearby on the map", Toast.LENGTH_SHORT).show() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .border(2.dp, Color.Green, CircleShape)
                                .padding(2.dp)
                        ) {
                            AsyncImage(
                                model = avatar,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(name, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Friends Active (Live)", color = Color.White, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Your Location", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(friends.size) { index ->
                    val friend = friends[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = friend.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (friend.venueName != null) {
                                    Text("At ${friend.venueName}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("Roaming • ${friend.distanceText}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { Toast.makeText(context, "Inviting ${friend.name}...", Toast.LENGTH_SHORT).show() },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Invite", fontSize = 11.sp)
                                }
                                IconButton(onClick = { openRoute("Rockets Sandton", context) }) {
                                    Icon(Icons.Default.Navigation, contentDescription = "Route", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestsTab(
    incoming: List<com.example.core.data.FriendRequest>,
    outgoing: List<com.example.core.data.FriendRequest>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    var subTab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Incoming (${incoming.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Outgoing (${outgoing.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
        }

        val listToRender = if (subTab == 0) incoming else outgoing

        if (listToRender.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (subTab == 0) "No pending incoming requests." else "No pending outgoing requests.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(listToRender.size) { index ->
                    val req = listToRender[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_card_${req.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                Text(req.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${req.mutualFriendsCount} Mutual • ${req.reason}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            if (subTab == 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { onDecline(req.id) },
                                        modifier = Modifier
                                            .background(Color.Red.copy(alpha = 0.15f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Decline", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onAccept(req.id) },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                            .size(36.dp),
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                    ) {
                                        Icon(Icons.Default.Done, contentDescription = "Accept", modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onDecline(req.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("Cancel", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// FLASH DROPS HUB SYSTEM ("SEE ALL" FULL STACK EXPERIENCE)
// =========================================================================
