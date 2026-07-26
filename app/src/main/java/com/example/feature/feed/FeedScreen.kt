package com.example.feature.feed

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.core.data.feed.*

// -------------------------------------------------------------------------
// COMPOSABLE SCREEN
// -------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedScreen(onNavigateToLobby: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // -------------------------------------------------------------------------
    // FEED REPOSITORY & LOCAL INTERACTIVE STATES
    // -------------------------------------------------------------------------
    val feedState by com.example.core.data.feed.FeedRepository.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var feedFilterTag by remember { mutableStateOf<String?>(null) }
    var commentSheetMoment by remember { mutableStateOf<com.example.core.data.feed.Moment?>(null) }
    var analyticsSheetMoment by remember { mutableStateOf<com.example.core.data.feed.Moment?>(null) }
    var showPresenceDialog by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var presenceSelectedPrivacy by remember { mutableStateOf("PUBLIC") }
    var showCreateMomentSheet by remember { mutableStateOf(false) }

    // -------------------------------------------------------------------------
    // LUXURY NIGHTLIFE PALETTE
    // -------------------------------------------------------------------------
    val themeBg = Color(0xFF090D16)
    val cardBg = Color(0xFF111726)
    val neonCyan = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF2D55)
    val successGreen = Color(0xFF32D74B)
    val luxGold = Color(0xFFD4AF37)

    // -------------------------------------------------------------------------
    // IN-MEMORY COMPREHENSIVE FEED DATASET
    // -------------------------------------------------------------------------
    val momentsList = feedState.moments
    val filteredMoments by remember {
        derivedStateOf {
            momentsList.filter { moment ->
                // Apply Search query & tag filter
                val matchesSearch = if (searchQuery.isNotEmpty()) {
                    moment.username.contains(searchQuery, ignoreCase = true) ||
                    moment.captionOriginal.contains(searchQuery, ignoreCase = true) ||
                    moment.locationName.contains(searchQuery, ignoreCase = true)
                } else true

                val matchesTag = if (feedFilterTag != null) {
                    moment.captionOriginal.contains(feedFilterTag!!, ignoreCase = true) ||
                    moment.locationName.contains(feedFilterTag!!, ignoreCase = true) ||
                    moment.username.contains(feedFilterTag!!, ignoreCase = true)
                } else true

                // Apply Top Tabs filter
                val matchesTab = when (feedState.activeTab) {
                    "Live" -> moment.momentType == "LIVE"
                    "Nearby" -> moment.distanceText.contains("m") || moment.distanceText.contains("km")
                    "Following" -> moment.isFollowing
                    else -> true // For You
                }

                matchesSearch && matchesTag && matchesTab
            }
        }
    }

    // Pager state synced to filtered moments size
    val pagerState = rememberPagerState(pageCount = { filteredMoments.size })

    // Simulate global countdown clocks for all invitations
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
        }
    }

    // Base Scaffold
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // -------------------------------------------------------------------------
            // VERTICAL MOMENTS FEED PAGER
            // -------------------------------------------------------------------------
            if (filteredMoments.isNotEmpty()) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val moment = filteredMoments[page]
                    MomentPlayerItem(
                        moment = moment,
                        onNavigateToLobby = onNavigateToLobby,
                        onCommentClick = { commentSheetMoment = moment },
                        onAnalyticsClick = { analyticsSheetMoment = moment },
                        onPresenceTrigger = { showPresenceDialog = true },
                        onFollowToggle = {
                            com.example.core.data.feed.FeedRepository.toggleFollow(moment.id)
                        },
                        onLikeToggle = {
                            com.example.core.data.feed.FeedRepository.toggleLike(moment.id)
                        },
                        onRippleClick = {
                            com.example.core.data.feed.FeedRepository.rippleMoment(moment.id)
                        },
                        onSaveToggle = {
                            com.example.core.data.feed.FeedRepository.toggleSave(moment.id)
                            val willSave = !moment.isSaved
                            val msg = if (willSave) "Saved to collection!" else "Removed from saved!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                // Empty state for filters
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = "No results",
                        tint = neonPink,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Moments Match Filter",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try switching tabs or resetting your active hashtag filter.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            feedFilterTag = null
                            searchQuery = ""
                            com.example.core.data.feed.FeedRepository.setActiveTab("For You")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, neonCyan)
                    ) {
                        Text("Reset Feed Filter", color = Color.White)
                    }
                }
            }

            // -------------------------------------------------------------------------
            // TOP FLOATING HUD NAVIGATION SYSTEM
            // -------------------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Horizontal Glass Pill tab bar
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf("For You", "Following", "Nearby", "Live")
                    tabs.forEach { tab ->
                        val isSelected = feedState.activeTab == tab
                        val backgroundPillColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                            label = "TabPill"
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(backgroundPillColor)
                                .clickable { com.example.core.data.feed.FeedRepository.setActiveTab(tab) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tab == "Live") {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Glass Search and Filter Badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (feedFilterTag != null) {
                        Surface(
                            color = neonPink.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, neonPink.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { feedFilterTag = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(feedFilterTag!!, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                        }
                    }

                    IconButton(
                        onClick = { isSearchOpen = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // -------------------------------------------------------------------------
            // OVERLAY: SEARCH ENGINE SYSTEM (FADE OVERLAY)
            // -------------------------------------------------------------------------
            AnimatedVisibility(
                visible = isSearchOpen,
                enter = fadeIn() + slideInVertically { -100 },
                exit = fadeOut() + slideOutVertically { -100 }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.96f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Discovery Search", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            IconButton(onClick = { isSearchOpen = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Styled Input Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search venues, hashtags, creators...", color = Color.White.copy(alpha = 0.4f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = neonCyan) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = neonCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = cardBg,
                                unfocusedContainerColor = cardBg
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Trending tags and sounds
                        Text("TRENDING IN THE CITY TONIGHT", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        val trendingHashtags = listOf("#Amapiano", "#Cocoon", "#Sunset", "#Techno", "#AndClub", "#Rosebank", "#Sandton")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            trendingHashtags.forEach { tag ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.clickable {
                                        feedFilterTag = tag
                                        isSearchOpen = false
                                        Toast.makeText(context, "Filtered feed by $tag", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = tag,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text("FEATURED VENUE CHANNELS", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        val venuesChannels = listOf("Cocoon Nightclub", "AfroHaus Rooftop", "And Club", "The Artistry", "Marble Club")
                        venuesChannels.forEach { venue ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        feedFilterTag = venue
                                        isSearchOpen = false
                                        Toast.makeText(context, "Filtered feed to $venue", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = neonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(venue, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f))
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------
            // BOTTOM SHEET: CHATS & FEED COMMENTS INTERACTIVE LOGIC
            // -------------------------------------------------------------------------
            if (commentSheetMoment != null) {
                val currentMoment = commentSheetMoment!!
                var myCommentText by remember { mutableStateOf("") }

                ModalBottomSheet(
                    onDismissRequest = { commentSheetMoment = null },
                    containerColor = cardBg,
                    contentColor = Color.White,
                    scrimColor = Color.Black.copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Comments (${currentMoment.comments.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // List of comments
                        Box(modifier = Modifier.height(260.dp)) {
                            if (currentMoment.comments.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Be the first to leave a comment!", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(currentMoment.comments) { comment ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            AsyncImage(
                                                model = comment.avatar,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(comment.author, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text(comment.time, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(comment.text, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 17.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Text Field Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = myCommentText,
                                onValueChange = { myCommentText = it },
                                placeholder = { Text("Add comment for the room...", color = Color.White.copy(alpha = 0.3f)) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = neonCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (myCommentText.trim().isNotEmpty()) {
                                        com.example.core.data.feed.FeedRepository.addComment(currentMoment.id, myCommentText.trim())
                                        // Update local reference to force UI refresh
                                        val updatedMoment = momentsList.find { it.id == currentMoment.id }
                                        if (updatedMoment != null) {
                                            commentSheetMoment = updatedMoment
                                        }
                                        myCommentText = ""
                                    }
                                },
                                modifier = Modifier
                                    .background(neonCyan, CircleShape)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post Comment", tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------
            // BOTTOM SHEET: CREATOR INTENSE PERFORMANCE ANALYTICS
            // -------------------------------------------------------------------------
            if (analyticsSheetMoment != null) {
                val analyticMoment = analyticsSheetMoment!!
                ModalBottomSheet(
                    onDismissRequest = { analyticsSheetMoment = null },
                    containerColor = cardBg,
                    contentColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Moment Performance Insights", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Real-time telemetry and momentum indicators", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                            Surface(
                                color = neonCyan.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = analyticMoment.momentumState,
                                    color = neonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Grid statistics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnalyticsCardItem(title = "Rippling Velocity", value = "${analyticMoment.currentVelocity} r/min", badge = "🔥 High", modifier = Modifier.weight(1f))
                            AnalyticsCardItem(title = "Total Views", value = "${analyticMoment.likesCount * 3 + 240}", badge = "+12%", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnalyticsCardItem(title = "Completion Rate", value = "94.2%", badge = "🎯 Target", modifier = Modifier.weight(1f))
                            AnalyticsCardItem(title = "Route Navigation", value = "84 clicks", badge = "📍 Map", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Custom Graphics Momentum Chart
                        Text("MOMENTUM VELOCITY CURVE (24H)", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height

                                // Draw live trend velocity curve
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, height * 0.8f)
                                    quadraticTo(width * 0.3f, height * 0.7f, width * 0.5f, height * 0.4f)
                                    cubicTo(width * 0.7f, height * 0.1f, width * 0.85f, height * 0.3f, width, height * 0.05f)
                                }

                                drawPath(
                                    path = path,
                                    color = neonCyan,
                                    style = Stroke(width = 3.dp.toPx())
                                )

                                // Draw gradient fill below path
                                val fillPath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, height)
                                    lineTo(0f, height * 0.8f)
                                    quadraticTo(width * 0.3f, height * 0.7f, width * 0.5f, height * 0.4f)
                                    cubicTo(width * 0.7f, height * 0.1f, width * 0.85f, height * 0.3f, width, height * 0.05f)
                                    lineTo(width, height)
                                    close()
                                }

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(neonCyan.copy(alpha = 0.25f), Color.Transparent)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { analyticsSheetMoment = null },
                            colors = ButtonDefaults.buttonColors(containerColor = neonPink),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Acknowledge Performance Matrix", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------
            // DIALOG: GEOFENCE GUEST PRESENCE PROMPT ("WHO'S HERE")
            // -------------------------------------------------------------------------
            if (showPresenceDialog) {
                AlertDialog(
                    onDismissRequest = { showPresenceDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = neonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Geofence Presence", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            Text(
                                "You've been inside Cocoon Nightclub geofence for 5 minutes. Would you like to share your presence in the live \"Who's Here\" section?",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("CHOOSE PRIVACY MODE (DEFAULT IS PRIVATE)", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            val modes = listOf("Public", "Followers", "Friends Only", "Private (Default)")
                            modes.forEach { mode ->
                                val selected = presenceSelectedPrivacy == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                        .clickable { presenceSelectedPrivacy = mode }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { presenceSelectedPrivacy = mode },
                                        colors = RadioButtonDefaults.colors(selectedColor = neonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(mode, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showPresenceDialog = false
                                Toast.makeText(context, "Presence shared successfully inside Who's Here as $presenceSelectedPrivacy!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
                        ) {
                            Text("Share Presence", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPresenceDialog = false }) {
                            Text("Not Now", color = Color.White.copy(alpha = 0.5f))
                        }
                    },
                    containerColor = cardBg
                )
            }

            // -------------------------------------------------------------------------
            // FLOATING ACTION BUTTON: CAPTURE & POST NEW MOMENT
            // -------------------------------------------------------------------------
            FloatingActionButton(
                onClick = { showCreateMomentSheet = true },
                containerColor = neonPink,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 48.dp)
                    .size(54.dp)
                    .testTag("create_moment_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Moment",
                    modifier = Modifier.size(28.dp)
                )
            }

            // -------------------------------------------------------------------------
            // OVERLAY: CREATE MOMENT BOTTOM SHEET
            // -------------------------------------------------------------------------
            if (showCreateMomentSheet) {
                CreateMomentSheet(
                    onDismiss = { showCreateMomentSheet = false },
                    onPublish = { caption, venue, mediaUrl, momentType ->
                        com.example.core.data.feed.FeedRepository.addMoment(
                            username = "You",
                            avatarUrl = "https://i.pravatar.cc/150?img=12",
                            momentType = momentType,
                            mediaUrl = mediaUrl,
                            captionOriginal = caption,
                            locationName = venue
                        )
                        showCreateMomentSheet = false
                        Toast.makeText(context, "🎉 Moment published to Feed & synced to Firestore!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT: HIGH-FIDELITY MOMENT LIST COMPOSABLE ITEM
// -------------------------------------------------------------------------
@Composable
fun MomentPlayerItem(
    moment: Moment,
    onNavigateToLobby: (String) -> Unit,
    onCommentClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onPresenceTrigger: () -> Unit,
    onFollowToggle: () -> Unit,
    onLikeToggle: () -> Unit,
    onRippleClick: () -> Unit,
    onSaveToggle: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTranslated by remember { mutableStateOf(false) }

    // Invitation State Selector Cycle (Active, Ended, Closed)
    var currentInviteState by remember { mutableStateOf(1) } // 1: Active, 2: Ended, 3: Closed

    // Ripple click scale animation
    var isRippleWaveActive by remember { mutableStateOf(false) }
    val rippleScale by animateFloatAsState(
        targetValue = if (isRippleWaveActive) 3.5f else 0.5f,
        animationSpec = tween(500, easing = LinearOutSlowInEasing),
        finishedListener = { isRippleWaveActive = false },
        label = "RippleScale"
    )
    val rippleAlpha by animateFloatAsState(
        targetValue = if (isRippleWaveActive) 0f else 0.7f,
        animationSpec = tween(500),
        label = "RippleAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // FULL-SCREEN VISUAL BACKGROUND
        AsyncImage(
            model = moment.mediaUrl,
            contentDescription = "Moment Video/Photo Frame",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // AMBIENT GRADIENT SHADOWS
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f,
                        endY = 2200f
                    )
                )
        )

        // RIPPLE WAVE SIMULATOR OVERLAY EFFECT
        if (isRippleWaveActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = rippleAlpha),
                                radius = size.minDimension / 2 * rippleScale,
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                )
                Text(
                    text = "RIPPLE WAVE DISPATCHED",
                    color = Color(0xFF00E5FF).copy(alpha = rippleAlpha + 0.1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // RIGHT ACTION BAR RAIL
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile & Follow Action
            Box(contentAlignment = Alignment.BottomCenter) {
                AsyncImage(
                    model = moment.avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onAnalyticsClick() }
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = !moment.isFollowing,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = 8.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF2D55))
                            .clickable {
                                onFollowToggle()
                                Toast.makeText(context, "Following ${moment.username}!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Follow", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Interactive Telemetry Stats indicator
            IconButton(
                onClick = onAnalyticsClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = "Analytics", tint = Color(0xFFD4AF37))
            }

            // Like Action
            ActionRailItem(
                icon = if (moment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (moment.isLiked) Color(0xFFFF2D55) else Color.White,
                label = moment.likesCount.toString(),
                onClick = onLikeToggle
            )

            // Ripple Action
            ActionRailItem(
                icon = Icons.Default.Waves,
                tint = Color(0xFF00E5FF),
                label = "${moment.ripplesCount} Ripples",
                onClick = {
                    isRippleWaveActive = true
                    onRippleClick()
                }
            )

            // Comment Action
            ActionRailItem(
                icon = Icons.AutoMirrored.Outlined.Chat,
                tint = Color.White,
                label = moment.comments.size.toString(),
                onClick = onCommentClick
            )

            // Save Action
            ActionRailItem(
                icon = if (moment.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                tint = if (moment.isSaved) Color(0xFF00E5FF) else Color.White,
                label = if (moment.isSaved) "Saved" else "Save",
                onClick = onSaveToggle
            )

            // Share Action
            ActionRailItem(
                icon = Icons.Default.Share,
                tint = Color.White,
                label = "Share",
                onClick = {
                    Toast.makeText(context, "Copied Moment link to clipboard!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // BOTTOM TEXT STACK (METADATA + ACTIVE INVITATION)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 92.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Simulated presence trigger tool
            Button(
                onClick = onPresenceTrigger,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(26.dp),
                border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF32D74B)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Presence Prompt", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Friend Intelligence row
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = moment.friendActivityText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // User Info header with custom validation status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = moment.username,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                if (moment.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified Partner",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = when (moment.momentumState) {
                        "Viral" -> Color(0xFFFF2D55).copy(alpha = 0.15f)
                        "Hot" -> Color(0xFFE57373).copy(alpha = 0.15f)
                        "Heating" -> Color(0xFFD4AF37).copy(alpha = 0.15f)
                        "Active" -> Color(0xFF32D74B).copy(alpha = 0.15f)
                        else -> Color.White.copy(alpha = 0.05f)
                    },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(
                        0.5.dp,
                        when (moment.momentumState) {
                            "Viral" -> Color(0xFFFF2D55)
                            "Hot" -> Color(0xFFE57373)
                            "Heating" -> Color(0xFFD4AF37)
                            "Active" -> Color(0xFF32D74B)
                            else -> Color.White.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Text(
                        text = moment.momentumState,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = moment.timeAgo,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            // Caption supporting Translation toggle
            Column {
                Text(
                    text = if (isTranslated) moment.captionTranslation else moment.captionOriginal,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTranslated) "See Original" else "Translate Slang / Zulu",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { isTranslated = !isTranslated }
                            .padding(vertical = 2.dp)
                    )

                    // Audio Track Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                Toast.makeText(context, "🎵 Audio: ${moment.audioTrackName}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Audio Track", tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = moment.audioTrackName,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // MOMENT INVITATION CARD WITH ALL 3 INTERACTIVE STATES SELECTOR
            moment.invitation?.let { inv ->
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(inv.venueName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (inv.isVenueVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, contentDescription = "Verified Venue", tint = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                                }
                            }

                            // Interactive State switcher button inside HUD (For evaluation)
                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable {
                                    currentInviteState = if (currentInviteState == 3) 1 else currentInviteState + 1
                                    Toast.makeText(context, "Evaluated State changed to State $currentInviteState", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    "Invite State: $currentInviteState",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // State Renderings
                        when (currentInviteState) {
                            1 -> {
                                // STATE 1: ACTIVE INVITATION
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF32D74B)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${inv.creatorName} is here now", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Available for 02:44:18", // Countdown state
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                            2 -> {
                                // STATE 2: INVITATION ENDED
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${inv.creatorName} has left", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                }
                                Text(
                                    text = "Invitation expired. Venue is still open.",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                            3 -> {
                                // STATE 3: VENUE CLOSED
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF2D55)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Venue Closed", color = Color(0xFFFF2D55), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = inv.venueClosedText,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onNavigateToLobby("lobby_${inv.venueName.lowercase().replace(" ", "_")}")
                                    Toast.makeText(context, "Opening ${inv.venueName} Lobby Highlights!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Club Lobby", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "📍 Launching Maps route instructions for ${inv.venueName}!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Route", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT: TELEMETRY RAIL ICON BUTTON
// -------------------------------------------------------------------------
@Composable
fun ActionRailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = null // Custom visual feedback done via wave scale
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------------------
// COMPONENT: INDEPENDENT METRIC CARD COMPOSABLE ITEM FOR ANALYTICS
// -------------------------------------------------------------------------
@Composable
fun AnalyticsCardItem(
    title: String,
    value: String,
    badge: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Surface(
                    color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color(0xFF00E5FF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT: CREATE MOMENT BOTTOM SHEET
// -------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMomentSheet(
    onDismiss: () -> Unit,
    onPublish: (caption: String, venue: String, mediaUrl: String, momentType: String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var selectedVenue by remember { mutableStateOf("Cocoon Nightclub") }
    var selectedType by remember { mutableStateOf("PHOTO") }
    var selectedMediaIndex by remember { mutableIntStateOf(0) }

    val presetImages = listOf(
        "https://images.unsplash.com/photo-1545128485-c400e7702796",
        "https://images.unsplash.com/photo-1574169208507-84376144848b",
        "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7",
        "https://images.unsplash.com/photo-1566737236500-c8ac43014a67"
    )

    val venues = listOf("Cocoon Nightclub", "Sandton Stage", "AfroHaus Rooftop", "Marble Lounge", "Truth Nightclub")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111726),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Share Nightlife Moment", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Thumbnail visual picker
            Text("SELECT MEDIA THUMBNAIL", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presetImages.forEachIndexed { idx, url ->
                    val isSelected = selectedMediaIndex == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedMediaIndex = idx }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Caption Field
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption & Hashtags") },
                placeholder = { Text("VIP Booths lit tonight! 🔥 #JHB", color = Color.White.copy(alpha = 0.3f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Venue Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TAG VENUE LOCATION", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(venues) { venue ->
                        val isSelected = selectedVenue == venue
                        Surface(
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { selectedVenue = venue }
                        ) {
                            Text(
                                text = venue,
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Type Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MOMENT TYPE", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("PHOTO", "VIDEO", "LIVE").forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            color = if (isSelected) Color(0xFFFF2D55).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { selectedType = type }
                        ) {
                            Text(
                                text = type,
                                color = if (isSelected) Color(0xFFFF2D55) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Publish Button
            Button(
                onClick = {
                    val finalCaption = if (caption.isBlank()) "Vibing at $selectedVenue 🔥" else caption
                    onPublish(finalCaption, selectedVenue, presetImages[selectedMediaIndex], selectedType)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Publish Moment to Feed", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
