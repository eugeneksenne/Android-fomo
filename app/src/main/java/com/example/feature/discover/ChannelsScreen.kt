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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import kotlinx.coroutines.delay

// -------------------------------------------------------------------------
// DATA MODELS FOR CHANNELS NETWORK
// -------------------------------------------------------------------------
data class NightlifeChannel(
    val id: String,
    val name: String,
    val category: String, // "City", "Province", "Club", "Pinned"
    val activeCountText: String,
    val heroImage: String,
    val subtitleText: String,
    val isPinned: Boolean = false
)

data class TrendingVenueChannelItem(
    val id: String,
    val name: String,
    val image: String,
    val energyPercent: Int,
    val queueText: String,
    val distanceText: String,
    val friendsHereCount: Int,
    val isLive: Boolean
)

data class LiveStreamChannelItem(
    val id: String,
    val title: String,
    val venueName: String,
    val thumbnail: String,
    val viewersCount: Int,
    val energyPercent: Int,
    val distanceText: String
)

data class ChannelPost(
    val id: String,
    val authorName: String,
    val authorAvatar: String,
    val venueName: String,
    val textContent: String,
    val mediaImage: String?,
    val timestamp: String,
    val distanceText: String,
    var keepVotes: Int,
    var garbageVotes: Int,
    var userVoted: String? = null // "KEEP", "GARBAGE", or null
)

data class NightPlanItem(
    val id: String,
    val title: String,
    val memberCount: Int,
    val routeText: String, // e.g. "Hatfield → Konka"
    val statusState: String, // "Planning", "Meeting", "Travelling", "Inside Venue", "Moving Again", "Ended"
    val organizerAvatar: String,
    val memberAvatars: List<String>
)

data class VisualBattlePoll(
    val id: String,
    val title: String,
    val categoryTag: String,
    val optionAImage: String,
    val optionAName: String,
    var votesA: Int,
    val optionBImage: String,
    val optionBName: String,
    var votesB: Int,
    var userVoted: String? = null // "A", "B", or null
)

data class FriendNightStatus(
    val name: String,
    val avatar: String,
    val statusText: String,
    val locationName: String,
    val actionType: String // "inside", "heading", "watching"
)

data class SmartRecommendationItem(
    val id: String,
    val venueName: String,
    val matchRate: Int,
    val reasonText: String,
    val image: String
)

data class FlashDropChannelItem(
    val id: String,
    val title: String,
    val venueName: String,
    var remainingMinutes: Int,
    val image: String,
    var isClaimed: Boolean = false
)

// -------------------------------------------------------------------------
// MAIN CHANNELS OVERLAY COMPOSABLE
// -------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsOverlay(
    onDismiss: () -> Unit,
    onNavigateToEventDetails: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // Colors
    val themeBg = Color(0xFF090D16)
    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val liveRed = Color(0xFFFF2D55)
    val goldAccent = Color(0xFFFFD700)

    // Active Channel State
    val channelsList = remember {
        listOf(
            NightlifeChannel("c_1", "Johannesburg Nights", "City", "24,200 people exploring tonight", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=800&auto=format&fit=crop", "Peak nightlife has just begun in Sandton & Rosebank", true),
            NightlifeChannel("c_2", "Cape Town After Dark", "City", "18,900 people exploring tonight", "https://images.unsplash.com/photo-1576485375217-d6a95e34d043?q=80&w=800&auto=format&fit=crop", "Kloof Street & Camps Bay VIP energy surging", true),
            NightlifeChannel("c_3", "Durban VIP Nights", "City", "12,100 people exploring tonight", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=800&auto=format&fit=crop", "Florida Road & Umhlanga Beachfront live", false),
            NightlifeChannel("c_4", "Konka Club Channel", "Club", "3,400 inside & queued", "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=800&auto=format&fit=crop", "Soweto's flagship luxury deck with live DJ set", true),
            NightlifeChannel("c_5", "And Club Channel", "Club", "1,800 techno raving", "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=800&auto=format&fit=crop", "Underground Newtown vault live stream active", false)
        )
    }

    var selectedChannel by remember { mutableStateOf(channelsList[0]) }
    var showChannelSelectorModal by remember { mutableStateOf(false) }

    // Navigation Tabs State: Overview, Feed, Live, Plans, Polls
    var selectedTab by remember { mutableStateOf("Overview") }

    // Search query
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // AI Night Brief Data
    val aiBriefItems = remember {
        listOf(
            "🔥 DBN Gogo main set starts at Konka in 35 minutes.",
            "⚡ Konka is filling rapidly (92% capacity).",
            "🚶 Madison Avenue has shorter queues nearby (5 min wait).",
            "📍 Rosebank nightlife hub is gaining peak momentum.",
            "👥 4 of your friends are currently heading to Booth Sandton."
        )
    }

    // Trending Venues Database
    val trendingVenues = remember {
        listOf(
            TrendingVenueChannelItem("tv_1", "Konka Soweto", "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=400&auto=format&fit=crop", 92, "Fast Line", "4.2 km", 5, true),
            TrendingVenueChannelItem("tv_2", "And Club Newtown", "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=400&auto=format&fit=crop", 88, "10 min queue", "2.1 km", 2, true),
            TrendingVenueChannelItem("tv_3", "The Artistry Rosebank", "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?q=80&w=400&auto=format&fit=crop", 84, "No queue", "1.5 km", 4, false),
            TrendingVenueChannelItem("tv_4", "Booth Sandton", "https://images.unsplash.com/photo-1578922746465-3a80a228f223?q=80&w=400&auto=format&fit=crop", 79, "15 min queue", "3.8 km", 3, true)
        )
    }

    // Live Streams Database
    val liveStreams = remember {
        listOf(
            LiveStreamChannelItem("ls_1", "Main Stage Deep Afro House", "Konka", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=400&auto=format&fit=crop", 1420, 95, "4.2 km"),
            LiveStreamChannelItem("ls_2", "Underground Industrial Set", "And Club", "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=400&auto=format&fit=crop", 890, 88, "2.1 km"),
            LiveStreamChannelItem("ls_3", "Rooftop Sunset Deck Vibes", "AfroHaus", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=400&auto=format&fit=crop", 640, 81, "1.2 km")
        )
    }

    // Active Live Stream Viewer State
    var activeLiveStream by remember { mutableStateOf<LiveStreamChannelItem?>(null) }

    // Posts Database for Feed Tab
    val feedPosts = remember {
        mutableStateListOf(
            ChannelPost("p_1", "Thabo M.", "https://i.pravatar.cc/150?img=12", "Konka Soweto", "DBN Gogo just took the stage! Energy is unmatched tonight! Get here before VIP gates close.", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop", "3 min ago", "4.2 km", 84, 2),
            ChannelPost("p_2", "Zoe Kravitz", "https://i.pravatar.cc/150?img=25", "And Club", "Sound system inside the vault is bass heavy. No lines at the bar currently!", "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=600&auto=format&fit=crop", "12 min ago", "2.1 km", 45, 1),
            ChannelPost("p_3", "Kabelo D.", "https://i.pravatar.cc/150?img=33", "The Artistry", "Cocktail bar is firing on all cylinders. Who's around Rosebank for a round?", null, "25 min ago", "1.5 km", 29, 0)
        )
    }

    // Night Plans Database
    val nightPlansList = remember {
        mutableStateListOf(
            NightPlanItem("np_1", "Saturday Night Groove Run", 7, "Rosebank → Konka", "Travelling", "https://i.pravatar.cc/150?img=32", listOf("https://i.pravatar.cc/150?img=32", "https://i.pravatar.cc/150?img=49", "https://i.pravatar.cc/150?img=11")),
            NightPlanItem("np_2", "Techno Vault Crawl", 4, "And Club → Espresso", "Inside Venue", "https://i.pravatar.cc/150?img=68", listOf("https://i.pravatar.cc/150?img=68", "https://i.pravatar.cc/150?img=12")),
            NightPlanItem("np_3", "VIP Sunset & Cocktail Crew", 5, "AfroHaus Rooftop", "Meeting", "https://i.pravatar.cc/150?img=41", listOf("https://i.pravatar.cc/150?img=41", "https://i.pravatar.cc/150?img=59"))
        )
    }

    // Visual Polls Database
    val visualPollsList = remember {
        mutableStateListOf(
            VisualBattlePoll("vp_1", "Tonight's Venue Battle", "Venue Battle", "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=400&auto=format&fit=crop", "Konka Soweto", 142, "https://images.unsplash.com/photo-1578922746465-3a80a228f223?q=80&w=400&auto=format&fit=crop", "Booth Sandton", 118),
            VisualBattlePoll("vp_2", "Dress Code Vibe", "Outfit Battle", "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?q=80&w=400&auto=format&fit=crop", "Golden Glam", 89, "https://images.unsplash.com/photo-1509631179647-0177331693ae?q=80&w=400&auto=format&fit=crop", "Cyber Minimal", 104)
        )
    }

    // Friends Status Layer
    val friendsStatusList = remember {
        listOf(
            FriendNightStatus("Sipho D.", "https://i.pravatar.cc/150?img=12", "Inside VIP lounge", "Konka", "inside"),
            FriendNightStatus("Neo M.", "https://i.pravatar.cc/150?img=49", "Heading to venue", "Madison Ave", "heading"),
            FriendNightStatus("Lerato K.", "https://i.pravatar.cc/150?img=32", "Watching Live Stream", "And Club Channel", "watching"),
            FriendNightStatus("Marcus V.", "https://i.pravatar.cc/150?img=11", "In Uber with squad", "Rosebank Loop", "heading")
        )
    }

    // Smart Recommendations
    val smartRecommendations = remember {
        listOf(
            SmartRecommendationItem("sr_1", "Madison Avenue", 89, "Because 3 of your friends are inside right now", "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?q=80&w=400&auto=format&fit=crop"),
            SmartRecommendationItem("sr_2", "Booth Sandton", 84, "Shortest queues in Sandton nightlife corridor", "https://images.unsplash.com/photo-1578922746465-3a80a228f223?q=80&w=400&auto=format&fit=crop"),
            SmartRecommendationItem("sr_3", "Ayepyep Lounge", 82, "Matches your preferred Amapiano & Afro House music taste", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=400&auto=format&fit=crop")
        )
    }

    // Flash Drop Item
    var flashDropState by remember {
        mutableStateOf(
            FlashDropChannelItem("fd_1", "FREE VIP ENTRY & COCKTAIL", "Konka Soweto", 12, "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=400&auto=format&fit=crop")
        )
    }

    // Flash Drop countdown timer
    LaunchedEffect(flashDropState.remainingMinutes) {
        while (flashDropState.remainingMinutes > 0 && !flashDropState.isClaimed) {
            delay(60000)
            flashDropState = flashDropState.copy(remainingMinutes = flashDropState.remainingMinutes - 1)
        }
    }

    // Modal creation states
    var showCreatePlanModal by remember { mutableStateOf(false) }
    var showCreatePostModal by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(themeBg),
        color = themeBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. TOP NAVIGATION
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("channels_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Channels", tint = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RssFeed, contentDescription = null, tint = accentPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Channels OS", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White, modifier = Modifier.testTag("channels_header_title"))
                    }

                    Row {
                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                        IconButton(onClick = { Toast.makeText(context, "Notifications: 3 live channel alerts active", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                        }
                    }
                }

                // EXPANDABLE SEARCH BAR
                AnimatedVisibility(visible = isSearchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search channels, streams, plans...", color = Color.White.copy(alpha = 0.4f)) },
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

                // 2. CHANNEL SWITCHER BAR
                Surface(
                    onClick = { showChannelSelectorModal = true },
                    color = cardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("channel_switcher_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedChannel.category == "Club") liveRed else neonCyan)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = selectedChannel.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = accentPurple.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    selectedChannel.category,
                                    color = accentPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Channel", tint = Color.White)
                    }
                }

                // 3. MAIN TAB NAVIGATION ROW
                ScrollableTabRow(
                    selectedTabIndex = when (selectedTab) {
                        "Overview" -> 0
                        "Feed" -> 1
                        "Live" -> 2
                        "Plans" -> 3
                        "Polls" -> 4
                        else -> 0
                    },
                    containerColor = themeBg,
                    contentColor = Color.White,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[
                                when (selectedTab) {
                                    "Overview" -> 0
                                    "Feed" -> 1
                                    "Live" -> 2
                                    "Plans" -> 3
                                    "Polls" -> 4
                                    else -> 0
                                }
                            ]),
                            color = accentPurple
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == "Overview",
                        onClick = { selectedTab = "Overview" },
                        text = { Text("✨ Overview", fontSize = 13.sp) },
                        modifier = Modifier.testTag("channel_tab_overview")
                    )
                    Tab(
                        selected = selectedTab == "Feed",
                        onClick = { selectedTab = "Feed" },
                        text = { Text("📡 Feed", fontSize = 13.sp) },
                        modifier = Modifier.testTag("channel_tab_feed")
                    )
                    Tab(
                        selected = selectedTab == "Live",
                        onClick = { selectedTab = "Live" },
                        text = { Text("🔴 Live", fontSize = 13.sp) },
                        modifier = Modifier.testTag("channel_tab_live")
                    )
                    Tab(
                        selected = selectedTab == "Plans",
                        onClick = { selectedTab = "Plans" },
                        text = { Text("🗺 Plans", fontSize = 13.sp) },
                        modifier = Modifier.testTag("channel_tab_plans")
                    )
                    Tab(
                        selected = selectedTab == "Polls",
                        onClick = { selectedTab = "Polls" },
                        text = { Text("📊 Polls", fontSize = 13.sp) },
                        modifier = Modifier.testTag("channel_tab_polls")
                    )
                }

                // SCROLLABLE CONTENT BODY
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // HERO BANNER (Shown in Overview and Feed)
                    if (selectedTab == "Overview" || selectedTab == "Feed") {
                        item {
                            ChannelHeroCard(
                                channel = selectedChannel,
                                onExploreClick = { selectedTab = "Live" }
                            )
                        }
                    }

                    // OVERVIEW TAB CONTENT
                    if (selectedTab == "Overview") {
                        // AI Night Brief
                        item {
                            AINightBriefCard(
                                items = aiBriefItems,
                                channelName = selectedChannel.name
                            )
                        }

                        // Quick Actions
                        item {
                            QuickActionsGrid(
                                onActionClick = { action ->
                                    when (action) {
                                        "Live Now" -> selectedTab = "Live"
                                        "Events Tonight" -> onNavigateToEventDetails("ev_1")
                                        "Flash Drops" -> Toast.makeText(context, "Navigating to Flash Drops", Toast.LENGTH_SHORT).show()
                                        "Trending Venues" -> selectedTab = "Overview"
                                    }
                                }
                            )
                        }

                        // Flash Drop Integration
                        if (!flashDropState.isClaimed) {
                            item {
                                FlashDropBannerItem(
                                    drop = flashDropState,
                                    onClaim = {
                                        flashDropState = flashDropState.copy(isClaimed = true)
                                        Toast.makeText(context, "🎉 VIP Pass Claimed! Ticket saved in wallet.", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }

                        // Trending Tonight Horizontal Carousel
                        item {
                            TrendingTonightSection(
                                items = trendingVenues,
                                onVenueClick = { name ->
                                    Toast.makeText(context, "Opening $name channel details", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // Live Streams Section
                        item {
                            LiveStreamsSection(
                                streams = liveStreams,
                                onStreamClick = { stream -> activeLiveStream = stream }
                            )
                        }

                        // Friend Layer
                        item {
                            FriendsTonightSection(friends = friendsStatusList)
                        }

                        // Smart Recommendations
                        item {
                            SmartRecommendationsSection(recommendations = smartRecommendations)
                        }
                    }

                    // FEED TAB CONTENT
                    if (selectedTab == "Feed") {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Realtime Nightlife Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Prioritized by distance & momentum", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { showCreatePostModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("post_to_channel_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Post", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        items(feedPosts, key = { it.id }) { post ->
                            ChannelPostCard(
                                post = post,
                                onKeepVote = {
                                    val idx = feedPosts.indexOfFirst { it.id == post.id }
                                    if (idx != -1) {
                                        val p = feedPosts[idx]
                                        if (p.userVoted != "KEEP") {
                                            val newGarbage = if (p.userVoted == "GARBAGE") p.garbageVotes - 1 else p.garbageVotes
                                            feedPosts[idx] = p.copy(keepVotes = p.keepVotes + 1, garbageVotes = newGarbage, userVoted = "KEEP")
                                            Toast.makeText(context, "🛡 Upvoted quality post", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onGarbageVote = {
                                    val idx = feedPosts.indexOfFirst { it.id == post.id }
                                    if (idx != -1) {
                                        val p = feedPosts[idx]
                                        if (p.userVoted != "GARBAGE") {
                                            val newKeep = if (p.userVoted == "KEEP") p.keepVotes - 1 else p.keepVotes
                                            feedPosts[idx] = p.copy(garbageVotes = p.garbageVotes + 1, keepVotes = newKeep, userVoted = "GARBAGE")
                                            Toast.makeText(context, "🚮 Marked for moderation review", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // LIVE TAB CONTENT
                    if (selectedTab == "Live") {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text("Live Nightlife Broadcasts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Real-time cameras & DJ set livestreams nearby", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }

                        items(liveStreams) { stream ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { activeLiveStream = stream },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                ) {
                                    AsyncImage(
                                        model = stream.thumbnail,
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
                                            color = liveRed,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                        }

                                        Surface(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${stream.viewersCount} watching", color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(16.dp)
                                    ) {
                                        Text(stream.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${stream.venueName} • Energy ${stream.energyPercent}% • ${stream.distanceText}", color = neonCyan, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // PLANS TAB CONTENT
                    if (selectedTab == "Plans") {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Nightlife Squad Runs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("Temporary groups coordinating tonight", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { showCreatePlanModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("create_plan_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Create Plan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        items(nightPlansList) { plan ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(plan.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Surface(
                                            color = when (plan.statusState) {
                                                "Inside Venue" -> liveRed.copy(alpha = 0.2f)
                                                "Travelling" -> neonCyan.copy(alpha = 0.2f)
                                                else -> accentPurple.copy(alpha = 0.2f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                plan.statusState,
                                                color = when (plan.statusState) {
                                                    "Inside Venue" -> liveRed
                                                    "Travelling" -> neonCyan
                                                    else -> accentPurple
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Route: ${plan.routeText}", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                                            plan.memberAvatars.forEach { avatar ->
                                                AsyncImage(
                                                    model = avatar,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .border(1.5.dp, cardBg, CircleShape)
                                                )
                                            }
                                        }
                                        Text("${plan.memberCount} Squad Members", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // POLLS TAB CONTENT
                    if (selectedTab == "Polls") {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text("Visual Nightlife Battles", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Vote using imagery to steer the city momentum", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }

                        items(visualPollsList, key = { it.id }) { poll ->
                            VisualPollCard(
                                poll = poll,
                                onVote = { option ->
                                    val idx = visualPollsList.indexOfFirst { it.id == poll.id }
                                    if (idx != -1 && poll.userVoted == null) {
                                        val p = visualPollsList[idx]
                                        if (option == "A") {
                                            visualPollsList[idx] = p.copy(votesA = p.votesA + 1, userVoted = "A")
                                        } else {
                                            visualPollsList[idx] = p.copy(votesB = p.votesB + 1, userVoted = "B")
                                        }
                                        Toast.makeText(context, "🎉 Vote registered!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // MODAL 1: CHANNEL SELECTOR BOTTOM SHEET
            if (showChannelSelectorModal) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Select Channel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    IconButton(onClick = { showChannelSelectorModal = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                channelsList.forEach { channel ->
                                    Surface(
                                        onClick = {
                                            selectedChannel = channel
                                            showChannelSelectorModal = false
                                            Toast.makeText(context, "Switched to ${channel.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        color = if (selectedChannel.id == channel.id) accentPurple.copy(alpha = 0.2f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = channel.heroImage,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(channel.activeCountText, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                            }
                                            if (selectedChannel.id == channel.id) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentPurple)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // MODAL 2: LIVE STREAM PLAYER OVERLAY
            if (activeLiveStream != null) {
                val stream = activeLiveStream!!
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = stream.thumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

                        // Top bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = liveRed, shape = RoundedCornerShape(8.dp)) {
                                Text("LIVE BROADCAST", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            IconButton(onClick = { activeLiveStream = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close stream", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }

                        // Bottom info
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Text(stream.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Text("${stream.venueName} • ${stream.viewersCount} active viewers", color = neonCyan, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { Toast.makeText(context, "Shared stream with squad!", Toast.LENGTH_SHORT).show() },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentPurple)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Stream")
                                }
                                OutlinedButton(
                                    onClick = { Toast.makeText(context, "Navigating to ${stream.venueName}", Toast.LENGTH_SHORT).show() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Navigate to Venue")
                                }
                            }
                        }
                    }
                }
            }

            // MODAL 3: CREATE NIGHT PLAN DIALOG
            if (showCreatePlanModal) {
                var planTitleInput by remember { mutableStateOf("") }
                var planRouteInput by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showCreatePlanModal = false },
                    title = { Text("Create Squad Nightlife Plan", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = planTitleInput,
                                onValueChange = { planTitleInput = it },
                                label = { Text("Plan Title (e.g. VIP Sunset Run)", color = Color.White.copy(alpha = 0.6f)) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = planRouteInput,
                                onValueChange = { planRouteInput = it },
                                label = { Text("Route (e.g. Rosebank → Konka)", color = Color.White.copy(alpha = 0.6f)) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (planTitleInput.isNotBlank()) {
                                    nightPlansList.add(
                                        0,
                                        NightPlanItem(
                                            "np_${System.currentTimeMillis()}",
                                            planTitleInput,
                                            1,
                                            if (planRouteInput.isBlank()) "Central Loop" else planRouteInput,
                                            "Planning",
                                            "https://i.pravatar.cc/150?img=33",
                                            listOf("https://i.pravatar.cc/150?img=33")
                                        )
                                    )
                                    showCreatePlanModal = false
                                    Toast.makeText(context, "🎉 Squad Plan Created!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentPurple)
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePlanModal = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = cardBg
                )
            }

            // MODAL 4: CREATE POST DIALOG
            if (showCreatePostModal) {
                var postTextInput by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showCreatePostModal = false },
                    title = { Text("Post to ${selectedChannel.name}", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = postTextInput,
                                onValueChange = { postTextInput = it },
                                placeholder = { Text("Share queue status, DJ vibe, or venue update...", color = Color.White.copy(alpha = 0.4f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (postTextInput.isNotBlank()) {
                                    feedPosts.add(
                                        0,
                                        ChannelPost(
                                            "p_${System.currentTimeMillis()}",
                                            "You",
                                            "https://i.pravatar.cc/150?img=33",
                                            selectedChannel.name,
                                            postTextInput,
                                            null,
                                            "Just now",
                                            "0.1 km",
                                            1,
                                            0
                                        )
                                    )
                                    showCreatePostModal = false
                                    Toast.makeText(context, "Broadcast posted to channel!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentPurple)
                        ) {
                            Text("Broadcast")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePostModal = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = cardBg
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT WIDGETS FOR CHANNELS
// -------------------------------------------------------------------------

@Composable
fun ChannelHeroCard(
    channel: NightlifeChannel,
    onExploreClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        AsyncImage(
            model = channel.heroImage,
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
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Verified, contentDescription = "Verified Channel", tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(channel.subtitleText, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Text(channel.activeCountText, color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Explore Tonight →", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AINightBriefCard(
    items: List<String>,
    channelName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1626)),
        border = BorderStroke(1.dp, Color(0xFF9D4EDD).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("✨ AI Night Brief — $channelName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            items.forEach { brief ->
                Text(
                    text = brief,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    onActionClick: (String) -> Unit
) {
    val actions = listOf(
        Pair("🔥 Live Now", Color(0xFFFF2D55)),
        Pair("🎉 Events Tonight", Color(0xFF9D4EDD)),
        Pair("⚡ Flash Drops", Color(0xFFFFD700)),
        Pair("🏛 Trending Venues", Color(0xFF00E5FF))
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.take(2).forEach { (label, tint) ->
                Surface(
                    onClick = { onActionClick(label.drop(2).trim()) },
                    color = Color(0xFF0F1626),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, tint.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.drop(2).forEach { (label, tint) ->
                Surface(
                    onClick = { onActionClick(label.drop(2).trim()) },
                    color = Color(0xFF0F1626),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, tint.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FlashDropBannerItem(
    drop: FlashDropChannelItem,
    onClaim: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1626)),
        border = BorderStroke(1.5.dp, Color(0xFFFF2D55))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FLASH DROP • ${drop.remainingMinutes}M REMAINING", color = Color(0xFFFF2D55), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(drop.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(drop.venueName, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Button(
                onClick = onClaim,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Claim Pass", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TrendingTonightSection(
    items: List<TrendingVenueChannelItem>,
    onVenueClick: (String) -> Unit
) {
    Column {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Trending Venues Tonight", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Real-time momentum & queue status", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { venue ->
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .clickable { onVenueClick(venue.name) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1626)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            AsyncImage(
                                model = venue.image,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (venue.isLive) {
                                Surface(
                                    color = Color(0xFFFF2D55),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(venue.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Energy ${venue.energyPercent}%", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(venue.queueText, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${venue.friendsHereCount} friends here • ${venue.distanceText}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStreamsSection(
    streams: List<LiveStreamChannelItem>,
    onStreamClick: (LiveStreamChannelItem) -> Unit
) {
    Column {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Live Stage Streams", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Peek inside the atmosphere right now", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(streams) { stream ->
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .clickable { onStreamClick(stream) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1626)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Box(modifier = Modifier.height(130.dp)) {
                        AsyncImage(
                            model = stream.thumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                        Surface(
                            color = Color(0xFFFF2D55),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        ) {
                            Text(stream.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${stream.venueName} • ${stream.viewersCount} watching", color = Color(0xFF00E5FF), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendsTonightSection(
    friends: List<FriendNightStatus>
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Friends Tonight", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))

        friends.forEach { friend ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = friend.avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(friend.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${friend.statusText} at ${friend.locationName}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Surface(
                    color = Color(0xFF0F1626),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text(friend.actionType.uppercase(), color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun SmartRecommendationsSection(
    recommendations: List<SmartRecommendationItem>
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Recommended for You", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Powered by social graph & music taste", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

        recommendations.forEach { rec ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1626), RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = rec.image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(rec.venueName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Match ${rec.matchRate}%", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(rec.reasonText, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ChannelPostCard(
    post: ChannelPost,
    onKeepVote: () -> Unit,
    onGarbageVote: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1626)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = post.authorAvatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(post.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${post.venueName} • ${post.timestamp}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                }
                Text(post.distanceText, color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(post.textContent, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)

            if (post.mediaImage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = post.mediaImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Community Moderation buttons: Keep vs Garbage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onKeepVote,
                        color = if (post.userVoted == "KEEP") Color(0xFF32D74B).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (post.userVoted == "KEEP") Color(0xFF32D74B) else Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡 Keep (${post.keepVotes})", color = if (post.userVoted == "KEEP") Color(0xFF32D74B) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        onClick = onGarbageVote,
                        color = if (post.userVoted == "GARBAGE") Color(0xFFFF2D55).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (post.userVoted == "GARBAGE") Color(0xFFFF2D55) else Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🚮 Garbage (${post.garbageVotes})", color = if (post.userVoted == "GARBAGE") Color(0xFFFF2D55) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }

                IconButton(onClick = { }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun VisualPollCard(
    poll: VisualBattlePoll,
    onVote: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1626)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(poll.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Surface(color = Color(0xFF9D4EDD).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                    Text(poll.categoryTag, color = Color(0xFF9D4EDD), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val total = (poll.votesA + poll.votesB).coerceAtLeast(1)
            val pctA = (poll.votesA * 100) / total
            val pctB = (poll.votesB * 100) / total

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Option A
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onVote("A") }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(BorderStroke(if (poll.userVoted == "A") 2.dp else 0.dp, Color(0xFF00E5FF)), RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = poll.optionAImage,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                        Text(poll.optionAName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${poll.votesA} Votes ($pctA%)", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Option B
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onVote("B") }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(BorderStroke(if (poll.userVoted == "B") 2.dp else 0.dp, Color(0xFFFF2D55)), RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = poll.optionBImage,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                        Text(poll.optionBName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${poll.votesB} Votes ($pctB%)", color = Color(0xFFFF2D55), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
