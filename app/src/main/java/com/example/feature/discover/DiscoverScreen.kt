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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onProfileClick: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToEventDetails: (String) -> Unit = {},
    onNavigateToLobby: (String) -> Unit = {},
    onNavigateToNightGuard: () -> Unit = {},
    onNavigateToCountryPackHub: () -> Unit = {},
    onNavigateToPlansWorkspace: () -> Unit = {}
) {
    val eventsState by com.example.core.data.EventRepository.eventsState.collectAsState()
    val exploreVenues by com.example.core.data.VenueRepository.exploreVenuesState.collectAsState()
    val storiesState by com.example.core.data.MyCircleRepository.storiesState.collectAsState()
    val globalFlashDrops by com.example.core.data.VenueRepository.globalFlashDropsState.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationRepository = remember { com.example.core.data.notification.NotificationRepository.getInstance(context) }
    val unreadCount by notificationRepository.unreadCount.collectAsState(initial = 0)
    
    var selectedPreviewVenue by remember { mutableStateOf<com.example.core.data.ExploreVenue?>(null) }
    var isMyCircleHubOpen by remember { mutableStateOf(false) }
    var selectedStoryIndex by remember { mutableStateOf<Int?>(null) }
    var selectedFlashDropForClaim by remember { mutableStateOf<com.example.core.data.FlashDrop?>(null) }
    var selectedGlobalPlanTarget by remember { mutableStateOf<String?>(null) }
    var isPrepRoomsOpen by remember { mutableStateOf(false) }
    var isChannelsOpen by remember { mutableStateOf(false) }
    var isExploreTheCityOpen by remember { mutableStateOf(false) }
    var isFlashDropsHubOpen by remember { mutableStateOf(false) }
    var isSmartPlacesHubOpen by remember { mutableStateOf(false) }
    var selectedFlashDropForDetail by remember { mutableStateOf<com.example.core.data.FlashDrop?>(null) }
    var selectedFlashDropForRoute by remember { mutableStateOf<com.example.core.data.FlashDrop?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { DiscoverTopBar(unreadCount = unreadCount, onProfileClick = onProfileClick) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item { HeroSection() }
                item { SectionSpacer() }
                item { NightGuardQuickBanner(onNavigateToNightGuard) }
                item { SectionSpacer() }
                item { CountryPackQuickBanner(onNavigateToCountryPackHub) }
                item { SectionSpacer() }
                item { ClosingSoonSection() }
                item { SectionSpacer() }
                item { 
                    FlashDropsSection(
                        flashDrops = globalFlashDrops,
                        onSeeAllClick = { isFlashDropsHubOpen = true },
                        onClaimClick = { selectedFlashDropForDetail = it }
                    ) 
                }
                item { SectionSpacer() }
                item { 
                    MyCircleSection(
                        stories = storiesState,
                        onSeeAllClick = { isMyCircleHubOpen = true },
                        onStoryClick = { selectedStoryIndex = it }
                    ) 
                }
                item { SectionSpacer() }
                item { LiveMomentsSection() }
                item { SectionSpacer() }
                item { 
                    SmartPlacesSection(
                        venues = exploreVenues,
                        onSeeAllClick = { isSmartPlacesHubOpen = true },
                        onVenueClick = { selectedPreviewVenue = it }
                    ) 
                }
                item { SectionSpacer() }
                item { TrendingNowSection() }
                item { SectionSpacer() }
                item { EventsSection(eventsState, onNavigateToEvents, onNavigateToEventDetails) }
                item { SectionSpacer() }
                item { 
                    ExploreTheCitySection(
                        venues = exploreVenues,
                        onVenueClick = { selectedPreviewVenue = it },
                        onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) },
                        onSeeAllClick = { isExploreTheCityOpen = true }
                    ) 
                }
                item { SectionSpacer() }
                item { ChannelsSection(onOpenClick = { isChannelsOpen = true }) }
                item { SectionSpacer() }
                item { PrepRoomsSection(onOpenClick = { isPrepRoomsOpen = true }) }
                item { SectionSpacer() }
                item { TonightSection(onNavigateToNightGuard = onNavigateToNightGuard, onNavigateToPlansWorkspace = onNavigateToPlansWorkspace) }
            }
        }

        // Global Contextual Plan Sheet Overlay
        if (selectedGlobalPlanTarget != null) {
            GlobalPlanContextSheet(
                targetName = selectedGlobalPlanTarget!!,
                onDismiss = { selectedGlobalPlanTarget = null }
            )
        }

        // Stage 2 Venue Preview System Overlay
        if (selectedPreviewVenue != null) {
            val currentVenue = exploreVenues.find { it.id == selectedPreviewVenue?.id } ?: selectedPreviewVenue!!
            VenuePreviewOverlay(
                venue = currentVenue,
                onDismiss = { selectedPreviewVenue = null },
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) }
            )
        }

        // Immersive Story Viewer
        if (selectedStoryIndex != null) {
            ImmersiveStoryViewer(
                stories = storiesState,
                initialIndex = selectedStoryIndex!!,
                onDismiss = { selectedStoryIndex = null },
                onNavigateToLobby = onNavigateToLobby
            )
        }

        // My Circle Social Hub (Immersive Full-Screen Overlay)
        if (isMyCircleHubOpen) {
            MyCircleHubOverlay(
                onDismiss = { isMyCircleHubOpen = false },
                onStoryClick = { index ->
                    selectedStoryIndex = index
                },
                onNavigateToLobby = onNavigateToLobby,
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }

        // Flash Drop Claim Dialog Overlay
        if (selectedFlashDropForClaim != null) {
            FlashDropClaimDialog(
                drop = selectedFlashDropForClaim!!,
                onDismiss = { selectedFlashDropForClaim = null },
                onConfirmClaim = { id ->
                    com.example.core.data.VenueRepository.claimGlobalFlashDrop(id)
                }
            )
        }

        // Prep Rooms Dedicated Experience Overlay
        if (isPrepRoomsOpen) {
            PrepRoomsOverlay(
                onDismiss = { isPrepRoomsOpen = false },
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }

        // Channels OS Full-Stack Experience Overlay
        if (isChannelsOpen) {
            ChannelsOverlay(
                onDismiss = { isChannelsOpen = false },
                onNavigateToEventDetails = onNavigateToEventDetails
            )
        }

        // Explore The City Full-Stack Experience Overlay
        if (isExploreTheCityOpen) {
            ExploreTheCityOverlay(
                venues = exploreVenues,
                onDismiss = { isExploreTheCityOpen = false },
                onSelectVenue = { selectedPreviewVenue = it },
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) }
            )
        }

        // Flash Drops Hub ("See All") Immersive Overlay
        if (isFlashDropsHubOpen) {
            FlashDropsHubOverlay(
                flashDrops = globalFlashDrops,
                onDismiss = { isFlashDropsHubOpen = false },
                onSelectDrop = { drop -> selectedFlashDropForDetail = drop },
                onOpenRoute = { drop -> selectedFlashDropForRoute = drop },
                onClaimDrop = { id -> com.example.core.data.VenueRepository.claimGlobalFlashDrop(id) }
            )
        }

        // Smart Places Hub ("See All") Concierge Overlay
        if (isSmartPlacesHubOpen) {
            SmartPlacesHubOverlay(
                venues = exploreVenues,
                onDismiss = { isSmartPlacesHubOpen = false },
                onSelectVenue = { selectedPreviewVenue = it },
                onNavigateToLobby = onNavigateToLobby,
                onLikeToggle = { id -> com.example.core.data.VenueRepository.toggleLikeVenue(id) }
            )
        }

        // Flash Drop Full Cinematic Detail Overlay
        if (selectedFlashDropForDetail != null) {
            FlashDropDetailOverlay(
                drop = selectedFlashDropForDetail!!,
                onDismiss = { selectedFlashDropForDetail = null },
                onNavigateToLobby = onNavigateToLobby,
                onNavigateToEventDetails = onNavigateToEventDetails,
                onOpenRoute = { drop -> selectedFlashDropForRoute = drop },
                onClaimConfirm = { id -> com.example.core.data.VenueRepository.claimGlobalFlashDrop(id) }
            )
        }

        // Flash Drop Navigation Route Dialog
        if (selectedFlashDropForRoute != null) {
            FlashDropRouteDialog(
                drop = selectedFlashDropForRoute!!,
                onDismiss = { selectedFlashDropForRoute = null }
            )
        }
    }
}

@Composable
fun DiscoverTopBar(unreadCount: Int, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(
                    model = "https://i.pravatar.cc/150?img=12",
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clickable { onProfileClick() }
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Johannesburg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "City Energy",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "High Energy",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = { /*TODO*/ },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(320.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1577546684742-df290b201464?q=80&w=1000&auto=format&fit=crop",
            contentDescription = "City background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 100f
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF4500), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Trending Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Johannesburg is alive tonight",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 36.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroStat("83", "Active Venues")
                HeroStat("14", "Live Events")
                HeroStat("4", "Friends Out")
            }
        }
    }
}

@Composable
fun HeroStat(value: String, label: String) {
    Column {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
fun SectionSpacer() {
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, actionText: String? = null, onActionClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionText != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
fun NightGuardQuickBanner(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .testTag("discover_night_guard_banner")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🛡 Night Guard Secured",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF32D74B))
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tap to schedule check-ins, pair with buddies, or trigger emergency SOS.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CountryPackQuickBanner(onClick: () -> Unit) {
    val packState by com.example.core.data.CountryPackRepository.state.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131026)),
        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF9D4EDD), Color(0xFF00E5FF)))),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .testTag("discover_country_pack_banner")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9D4EDD).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🇿🇦", fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Global Venue Pack Intelligence",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        color = Color(0xFF00E676).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "v18 ONLINE",
                            color = Color(0xFF00E676),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${packState.currentCountry} Pack Active • ${packState.totalVenuesInCountry} Permanent Venues in SQLite Engine",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ClosingSoonSection() {
    Column {
        SectionHeader("Closing Soon", "Don't miss out tonight", "See all")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(2) {
                ClosingSoonCard()
            }
        }
    }
}

@Composable
fun ClosingSoonCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.width(280.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guest List closes in 28m", color = Color(0xFFFF2D55), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("1.2 km", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=200&auto=format&fit=crop",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("And Club", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Techno • Minimal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Claim Entry", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun FlashDropsSection(
    flashDrops: List<com.example.core.data.FlashDrop>,
    onSeeAllClick: () -> Unit = {},
    onClaimClick: (com.example.core.data.FlashDrop) -> Unit
) {
    Column {
        SectionHeader(
            title = "Flash Drops",
            subtitle = "Exclusive rewards for arriving now",
            actionText = "See all",
            onActionClick = onSeeAllClick
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(flashDrops) { drop ->
                FlashDropCard(drop = drop, onClaimClick = { onClaimClick(drop) })
            }
        }
    }
}

@Composable
fun FlashDropCard(
    drop: com.example.core.data.FlashDrop,
    onClaimClick: () -> Unit
) {
    val brush = remember(drop.id) {
        if (drop.id == "fd_d48_vip_special") {
            Brush.linearGradient(
                colors = listOf(Color(0xFFD4AF37), Color(0xFF8B6508)) // Luxurious Gold/Bronze for VIP
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFF4F378B), Color(0xFF2D0A40)) // Violet theme
            )
        }
    }

    Box(
        modifier = Modifier
            .width(240.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clickable { onClaimClick() }
            .testTag("flash_drop_card_${drop.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drop.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (drop.price != null) {
                        Text(
                            text = drop.price,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (drop.claimed) "Claimed" else "${drop.currentStock} Left",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column {
                Text(
                    text = drop.subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = drop.venueName,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${drop.expiresMinutes}m",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlashDropClaimDialog(
    drop: com.example.core.data.FlashDrop,
    onDismiss: () -> Unit,
    onConfirmClaim: (String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (drop.id == "fd_d48_vip_special") {
                Button(
                    onClick = {
                        onConfirmClaim(drop.id)
                        onDismiss()
                        try {
                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=27${drop.tableReservations?.substring(1)}&text=Hello%20D48%20Midrand,%20I%20would%20like%20to%20book%20the%20Sunday%20VIP%20Special%20(R950)%20via%20the%20FOMO%20App!")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open WhatsApp. Booking info: WhatsApp ${drop.tableReservations}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)) // Gold button
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Book on WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = {
                        onConfirmClaim(drop.id)
                        onDismiss()
                        Toast.makeText(context, "Flash Drop Claimed! Show the voucher in your profile at the venue.", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text("Claim Voucher")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        title = {
            Text(
                text = drop.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (drop.id == "fd_d48_vip_special") Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = drop.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Venue:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(drop.venueName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (drop.price != null) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Price:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(drop.price, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF32C759))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Available Stock:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${drop.currentStock} / ${drop.initialStock}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Time Remaining:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${drop.expiresMinutes} mins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                    }
                }
                
                if (drop.id == "fd_d48_vip_special") {
                    Text(
                        text = "⚠️ WhatsApp Reservations Only. Table includes VIP booth seating, 1x Hennessy, 4x RedBull, and 2x Hubbly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Once claimed, show this voucher to the bartender or receptionist at ${drop.venueName} to redeem your reward.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun MyCircleSection(
    stories: List<com.example.core.data.CircleStory>,
    onSeeAllClick: () -> Unit,
    onStoryClick: (Int) -> Unit
) {
    Column {
        SectionHeader(
            title = "My Circle",
            subtitle = "See where your friends are heading",
            actionText = "See all",
            onActionClick = onSeeAllClick
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            // Your Story
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSeeAllClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
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
                                .padding(4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Your Story", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Real Friend Stories
            items(stories.size) { index ->
                val story = stories[index]
                StoryCircle(
                    story = story,
                    onClick = { onStoryClick(index) }
                )
            }
        }
    }
}

@Composable
fun StoryCircle(
    story: com.example.core.data.CircleStory,
    onClick: () -> Unit
) {
    val ringColor = remember(story.ringColor) {
        try {
            Color(android.graphics.Color.parseColor(story.ringColor))
        } catch (e: Exception) {
            Color(0xFF8A2BE2)
        }
    }

    val badgeText = when (story.badgeText) {
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
            .clickable { onClick() }
            .testTag("story_circle_${story.userName.lowercase()}")
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft glowing ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.5.dp, ringColor, CircleShape)
                    .padding(4.dp)
            ) {
                AsyncImage(
                    model = story.userAvatar,
                    contentDescription = "${story.userName}'s Story",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            // Small badge on bottom-right
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(badgeText, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = story.userName,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SmartPlacesSection(
    venues: List<com.example.core.data.ExploreVenue> = emptyList(),
    onSeeAllClick: () -> Unit = {},
    onVenueClick: (com.example.core.data.ExploreVenue) -> Unit = {}
) {
    val displayVenues = if (venues.isNotEmpty()) venues.take(5) else emptyList()
    Column {
        SectionHeader("Smart Places", "Curated for your vibe tonight", "See all", onActionClick = onSeeAllClick)
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
                items(displayVenues) { venue ->
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
            .clickable { onClick() }
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

@Composable
fun LiveMomentsSection() {
    Column {
        SectionHeader("Live Moments", "Happening right now", "See all")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(3) {
                LiveMomentCard()
            }
        }
    }
}

@Composable
fun LiveMomentCard() {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=400&auto=format&fit=crop",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 100f
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
                color = Color.Red,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Text("The Artistry", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun TrendingNowSection() {
    Column {
        SectionHeader("Trending Now", "The city's fastest-growing experiences", "See all")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(3) {
                TrendingCard()
            }
        }
    }
}

@Composable
fun TrendingCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.width(280.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=200&auto=format&fit=crop",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF4500), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Trending", color = Color(0xFFFF4500), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Konka", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Nightclub • Soweto", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EventsSection(
    events: List<com.example.core.data.Event>,
    onNavigateToEvents: () -> Unit,
    onNavigateToEventDetails: (String) -> Unit
) {
    Column {
        SectionHeader(
            title = "Events",
            subtitle = "Tonight and upcoming",
            actionText = "See all",
            onActionClick = onNavigateToEvents
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(events) { event ->
                EventCard(event = event, onCardClick = { onNavigateToEventDetails(event.id) })
            }
        }
    }
}

@Composable
fun EventCard(event: com.example.core.data.Event, onCardClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onCardClick() }
            .testTag("discover_event_card_${event.id}")
    ) {
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
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 100f
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(event.dateText.take(3).uppercase(), color = Color(0xFFFF2D55), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(if (event.dateText == "Tonight") "NOW" else "SUN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column {
                Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(event.venueName, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreTheCitySection(
    venues: List<com.example.core.data.ExploreVenue>,
    onVenueClick: (com.example.core.data.ExploreVenue) -> Unit,
    onLikeToggle: (String) -> Unit,
    onSeeAllClick: () -> Unit = {}
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Explore The City",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

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
            items(worldsOrder) { world ->
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
                        onSeeAllClick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Venue Cards List (Stage 1 Browse Cards)
            if (filteredVenues.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No venues available in this category.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredVenues) { venue ->
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
            .clickable { onCardClick() }
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
                    contentDescription = venue.name,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VenuePreviewOverlay(
    venue: com.example.core.data.ExploreVenue,
    onDismiss: () -> Unit,
    onNavigateToLobby: (String) -> Unit,
    onLikeToggle: (String) -> Unit
) {
    val context = LocalContext.current
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val (statusText, statusColor, extraStatus) = getVenueStatus(venue, currentHour)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            modifier = Modifier
                .width(320.dp)
                .padding(16.dp)
                .clickable(enabled = true, onClick = {})
                .testTag("venue_preview_card")
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AsyncImage(
                        model = venue.imageUrl,
                        contentDescription = venue.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                            .testTag("preview_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Preview",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onLikeToggle(venue.id) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                            .testTag("preview_like_button")
                    ) {
                        Icon(
                            imageVector = if (venue.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Love Reaction",
                            tint = if (venue.isLiked) Color.Red else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = venue.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (venue.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = venue.distanceText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFD60A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${venue.rating} (${venue.reviewCount} Reviews)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = venue.address,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Operating Hours",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${venue.openDays} • ${formatHours(venue.startHour, venue.endHour)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (venue.is24Hours) {
                        Text(
                            text = "🕒 Open 24 Hours",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = extraStatus,
                            fontSize = 12.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tags & Offerings",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = getPrefixedAttribute(venue.subcategory),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        venue.attributes.forEach { attr ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = getPrefixedAttribute(attr),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (venue.category == "Nightlife") {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onNavigateToLobby(venue.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("preview_club_lobby_button")
                            ) {
                                Text("Club Lobby", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    openWebsite(venue.websiteUrl, context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("preview_website_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Website", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                openRoute(venue.address, context)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("preview_route_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Route", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper functions
fun getVenueStatus(venue: com.example.core.data.ExploreVenue, currentHour: Int): Triple<String, Color, String> {
    if (venue.is24Hours) {
        return Triple("🟢 Open Now", Color(0xFF34C759), "Open 24 Hours")
    }

    val start = venue.startHour
    val end = venue.endHour

    val isOpen = if (start < end) {
        currentHour in start until end
    } else {
        currentHour >= start || currentHour < end
    }

    if (isOpen) {
        val closingSoon = currentHour == (end - 1 + 24) % 24
        if (closingSoon) {
            val minutesLeft = 60 - Calendar.getInstance().get(Calendar.MINUTE)
            return Triple("🟠 Closing Soon", Color(0xFFFF9500), "Closes in $minutesLeft min")
        } else {
            val closeTimeFormatted = String.format("%02d:00", end)
            return Triple("🟢 Open Now", Color(0xFF34C759), "Closes $closeTimeFormatted")
        }
    } else {
        val openTimeFormatted = String.format("%02d:00", start)
        return Triple("⚫ Closed", Color(0xFF8E8E93), "Opens Today at $openTimeFormatted")
    }
}

fun formatHours(start: Int, end: Int): String {
    return String.format("%02d:00–%02d:00", start, end)
}

fun getPrefixedAttribute(attr: String): String {
    return when (attr.lowercase().trim()) {
        "nightclub" -> "🍸 Nightclub"
        "dj" -> "🎧 DJ"
        "dance floor" -> "💃 Dance Floor"
        "vip" -> "🍾 VIP"
        "vip friendly" -> "🍾 VIP Friendly"
        "live music" -> "🎵 Live Music"
        "cocktails" -> "🍹 Cocktails"
        "restaurant" -> "🍽️ Restaurant"
        "steakhouse" -> "🥩 Steakhouse"
        "fine dining" -> "🍷 Fine Dining"
        "rooftop" -> "🌇 Rooftop"
        "reservations" -> "🥂 Reservations"
        "barber" -> "💈 Barber"
        "walk-ins" -> "✂️ Walk-ins"
        "premium" -> "✨ Premium"
        "card payments" -> "💳 Card Payments"
        "spa" -> "🧖 Spa"
        "massage" -> "💆 Massage"
        "ice bath" -> "🧊 Ice Bath"
        "wellness" -> "🌿 Wellness"
        "hotel" -> "🏨 Hotel"
        "resort" -> "🌊 Resort"
        "pool" -> "🏊 Pool"
        "filling station" -> "⛽ Filling Station"
        "coffee" -> "☕ Coffee"
        "convenience store" -> "🛒 Convenience"
        "fast food" -> "🍔 Fast Food"
        "open 24 hours" -> "🕒 Open 24 Hours"
        else -> attr
    }
}

fun openWebsite(url: String, context: android.content.Context) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open website", Toast.LENGTH_SHORT).show()
    }
}

fun openRoute(address: String, context: android.content.Context) {
    try {
        val uri = "geo:0,0?q=" + android.net.Uri.encode(address)
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(address)))
            context.startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(context, "Could not open map", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun ChannelsSection(onOpenClick: () -> Unit) {
    Column {
        SectionHeader(
            title = "Channels",
            subtitle = "Live nightlife operating system",
            actionText = "See all",
            onActionClick = onOpenClick
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(4) { index ->
                val (title, members, emoji, bgImg) = when (index) {
                    0 -> Quadruple("Johannesburg Nights", "24.2K Active", "🌆", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=400&auto=format&fit=crop")
                    1 -> Quadruple("Cape Town After Dark", "18.9K Active", "🌊", "https://images.unsplash.com/photo-1576485375217-d6a95e34d043?q=80&w=400&auto=format&fit=crop")
                    2 -> Quadruple("Konka Club Channel", "3.4K Inside", "🔥", "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=400&auto=format&fit=crop")
                    else -> Quadruple("And Club Channel", "1.8K Raving", "⚡", "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=400&auto=format&fit=crop")
                }
                ChannelCard(
                    title = title,
                    membersText = members,
                    emoji = emoji,
                    bgImage = bgImg,
                    onClick = onOpenClick
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ChannelCard(
    title: String,
    membersText: String,
    emoji: String,
    bgImage: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .width(170.dp)
            .height(130.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = bgImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(emoji, fontSize = 22.sp)
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(membersText, color = Color(0xFF00E5FF), fontWeight = FontWeight.Medium, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PrepRoomsSection(onOpenClick: () -> Unit) {
    Column {
        SectionHeader(
            title = "Prep Rooms",
            subtitle = "Get ready for tonight",
            actionText = "See all",
            onActionClick = onOpenClick
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(4) { index ->
                val (category, status, img) = when (index) {
                    0 -> Triple("Outfit Polls", "42 Active Votes", "https://images.unsplash.com/photo-1483985988355-763728e1935b?q=80&w=400&auto=format&fit=crop")
                    1 -> Triple("Makeup Glow", "5-Min Masterclass", "https://images.unsplash.com/photo-1522337660859-02fbefca4702?q=80&w=400&auto=format&fit=crop")
                    2 -> Triple("Grooming & Hair", "Clean Fade Prep", "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?q=80&w=400&auto=format&fit=crop")
                    else -> Triple("Venue Dress Guides", "AfroHaus Sunset", "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?q=80&w=400&auto=format&fit=crop")
                }
                PrepRoomCard(
                    categoryName = category,
                    statusText = status,
                    imageModel = img,
                    onClick = onOpenClick
                )
            }
        }
    }
}

@Composable
fun PrepRoomCard(
    categoryName: String,
    statusText: String,
    imageModel: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(190.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = categoryName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomStart) {
            Column {
                Text(categoryName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(statusText, color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }
    }
}



@Composable
fun ImmersiveStoryViewer(
    stories: List<com.example.core.data.CircleStory>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onNavigateToLobby: (String) -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val currentStory = stories.getOrNull(currentIndex) ?: return
    val context = LocalContext.current

    remember(currentIndex) {
        com.example.core.data.MyCircleRepository.markStoryViewed(currentStory.id)
        Unit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {}
            .testTag("story_viewer_container")
    ) {
        AsyncImage(
            model = currentStory.mediaUrl,
            contentDescription = "Story Media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (currentIndex > 0) currentIndex-- else onDismiss()
                    }
                    .testTag("story_nav_prev")
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (currentIndex < stories.size - 1) currentIndex++ else onDismiss()
                    }
                    .testTag("story_nav_next")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { idx, _ ->
                    val progressColor = if (idx < currentIndex) {
                        Color.White
                    } else if (idx == currentIndex) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.3f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(progressColor, RoundedCornerShape(1.5.dp))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = currentStory.userAvatar,
                    contentDescription = currentStory.userName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentStory.userName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = currentStory.timestamp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val emoji = when (currentStory.badgeText) {
                            "Story" -> "📸"
                            "Live" -> "🔴"
                            "Event" -> "🎉"
                            "Venue" -> "📍"
                            "Close Friends" -> "⭐"
                            else -> "📸"
                        }
                        Text("$emoji ${currentStory.badgeText}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("story_viewer_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        if (currentStory.musicPlaying != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Music",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentStory.musicPlaying,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            if (currentStory.venueName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentStory.venueName,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            } else if (currentStory.eventName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentStory.eventName,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStory.venueId != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToLobby(currentStory.venueId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Enter Lobby", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            openRoute(currentStory.venueName ?: "Johannesburg", context)
                        },
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Get Route")
                    }
                } else if (currentStory.eventName != null) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Opening Event: ${currentStory.eventName}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Event", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Ticket Purchase Initiated!", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Buy Ticket")
                    }
                } else if (currentStory.storyType == "Live") {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Entering Live Broadcaster Channel...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Watch Live Broadcaster", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Message sent to ${currentStory.userName}!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Quick Reply", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Added to Close Friends!", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Star ⭐")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCircleHubOverlay(
    onDismiss: () -> Unit,
    onStoryClick: (Int) -> Unit,
    onNavigateToLobby: (String) -> Unit,
    onNavigateToEventDetails: (String) -> Unit
) {
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

                            items(stories.size) { index ->
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

                val mockPins = listOf(
                    Triple("Amanda", Alignment.TopCenter, "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=200&auto=format&fit=crop"),
                    Triple("Jason", Alignment.BottomStart, "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=200&auto=format&fit=crop"),
                    Triple("Sarah", Alignment.CenterEnd, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop"),
                    Triple("Jessica", Alignment.TopStart, "https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=200&auto=format&fit=crop")
                )

                mockPins.forEach { (name, align, avatar) ->
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

@Composable
fun FlashDropsHubOverlay(
    flashDrops: List<com.example.core.data.FlashDrop>,
    onDismiss: () -> Unit,
    onSelectDrop: (com.example.core.data.FlashDrop) -> Unit,
    onOpenRoute: (com.example.core.data.FlashDrop) -> Unit,
    onClaimDrop: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") } // ALL, VENUE, EVENT, CREATOR, BRAND, MYSTERY
    var selectedStatus by remember { mutableStateOf("ALL") } // ALL, LIVE, ENDING_SOON, TRENDING, CLAIMED

    val categories = listOf(
        "ALL" to "⚡ All Drops",
        "VENUE" to "🍾 Venue",
        "EVENT" to "🎵 Event",
        "CREATOR" to "🎤 Creator",
        "BRAND" to "🛍 Brand",
        "MYSTERY" to "👀 Mystery"
    )

    val filteredDrops = remember(flashDrops, searchQuery, selectedCategory, selectedStatus) {
        flashDrops.filter { drop ->
            val matchesCategory = selectedCategory == "ALL" || drop.category.equals(selectedCategory, ignoreCase = true)
            val matchesStatus = when (selectedStatus) {
                "LIVE" -> drop.status == "LIVE"
                "ENDING_SOON" -> drop.expiresMinutes <= 30 || drop.status == "ENDING_SOON"
                "TRENDING" -> drop.status == "TRENDING"
                "CLAIMED" -> drop.claimed
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() || 
                drop.title.contains(searchQuery, ignoreCase = true) ||
                drop.subtitle.contains(searchQuery, ignoreCase = true) ||
                drop.venueName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesStatus && matchesSearch
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("flash_drops_hub_overlay"),
        color = Color(0xFF0D0D12)
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
                            text = "⚡ Flash Drops Hub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF2D55).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE NEAR YOU",
                                color = Color(0xFFFF2D55),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Real-world limited offers • Discover & get there before expiry",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Live City Radar Metrics Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${flashDrops.size}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF00E5FF))
                        Text("Active Near You", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${flashDrops.count { it.expiresMinutes <= 30 }}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFFF2D55))
                        Text("Ending <30m", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("250m", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFFFD700))
                        Text("Nearest Venue", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search by title, venue, or cocktail...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Category Filter Tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { (code, label) ->
                    val isSelected = selectedCategory == code
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = code },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.White.copy(alpha = 0.08f),
                            labelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
                    )
                }
            }

            // Status Quick Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "All Status",
                    "LIVE" to "⚡ Live",
                    "ENDING_SOON" to "⏳ Ending Soon",
                    "TRENDING" to "🔥 Trending"
                ).forEach { (code, label) ->
                    val isSelected = selectedStatus == code
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.1f), CircleShape)
                            .clickable { selectedStatus = code }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(label, fontSize = 11.sp, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Drops List
            if (filteredDrops.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Flash Drops found in this category",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredDrops) { drop ->
                        FlashDropHubCard(
                            drop = drop,
                            onSelectDrop = { onSelectDrop(drop) },
                            onOpenRoute = { onOpenRoute(drop) },
                            onClaimDrop = { onClaimDrop(drop.id) }
                        )
                    }

                    item {
                        // FOMO Platform Disclaimer
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            color = Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "FOMO connects you with real-world Flash Drops. We do not distribute vouchers or handle redemption. Arrive at the venue to claim directly.",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlashDropHubCard(
    drop: com.example.core.data.FlashDrop,
    onSelectDrop: () -> Unit,
    onOpenRoute: () -> Unit,
    onClaimDrop: () -> Unit
) {
    val categoryGradient = remember(drop.category) {
        when (drop.category.uppercase()) {
            "VENUE" -> listOf(Color(0xFF2C0B4D), Color(0xFF140524))
            "EVENT" -> listOf(Color(0xFF3B2404), Color(0xFF1B1002))
            "CREATOR" -> listOf(Color(0xFF032B30), Color(0xFF011417))
            "BRAND" -> listOf(Color(0xFF0A331A), Color(0xFF04170B))
            "MYSTERY" -> listOf(Color(0xFF3B082C), Color(0xFF1B0314))
            else -> listOf(Color(0xFF1E1E28), Color(0xFF12121A))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelectDrop() },
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(categoryGradient))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Top Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⚡ ${drop.category.uppercase()}",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = drop.urgencyBadge,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = drop.distanceText,
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = drop.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        if (drop.price != null) {
                            Text(
                                text = drop.price,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFFFFD700)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = drop.subtitle,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (drop.heroImageUrl != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        AsyncImage(
                            model = drop.heroImageUrl,
                            contentDescription = drop.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stock & Expiry Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = drop.venueName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFFFF2D55),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ends in ${drop.expiresMinutes}m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF2D55)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar for Stock
                LinearProgressIndicator(
                    progress = { (drop.currentStock.toFloat() / drop.initialStock.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSelectDrop,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("View Drop", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenRoute,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Route", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// FLASH DROP DETAIL FULL-SCREEN OVERLAY
// =========================================================================

@Composable
fun FlashDropDetailOverlay(
    drop: com.example.core.data.FlashDrop,
    onDismiss: () -> Unit,
    onNavigateToLobby: (String) -> Unit,
    onNavigateToEventDetails: (String) -> Unit,
    onOpenRoute: (com.example.core.data.FlashDrop) -> Unit,
    onClaimConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    var isHintRevealed by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("flash_drop_detail_overlay"),
        color = Color(0xFF09090E)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Banner Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    AsyncImage(
                        model = drop.heroImageUrl ?: "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=800&auto=format&fit=crop",
                        contentDescription = drop.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        Color(0xFF09090E)
                                    )
                                )
                            )
                    )

                    // Top Dismiss Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Expires in ${drop.expiresMinutes} mins", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Main Details Body
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "⚡ ${drop.category.uppercase()} DROP",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = drop.urgencyBadge,
                            color = Color(0xFFFF2D55),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = drop.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    if (drop.price != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Offer Price: ${drop.price}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Host & Venue Info Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = drop.venueName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    if (drop.isVerifiedVenue) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                    }
                                }
                                Text(
                                    text = "${drop.distanceText} • Real-World Live Location",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Offer Description",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = drop.subtitle,
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 20.sp
                    )

                    if (drop.category == "MYSTERY" && drop.hintText != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF2B0A22),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF007A).copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("👀 Mystery Drop Hint", color = Color(0xFFFF007A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (isHintRevealed) {
                                    Text(drop.hintText, color = Color.White, fontSize = 13.sp)
                                } else {
                                    Button(
                                        onClick = { isHintRevealed = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007A)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Tap to Reveal Hint", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stock & Availability
                    Text(
                        text = "Availability: ${drop.currentStock} of ${drop.initialStock} remaining",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (drop.currentStock.toFloat() / drop.initialStock.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Platform Disclosure Callout
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "FOMO does not distribute vouchers or manage redemption. Arrive at the destination before expiry to claim directly at the venue.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Primary Category Context Action Button
                    when (drop.category.uppercase()) {
                        "VENUE" -> {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onNavigateToLobby(drop.venueId)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Preview Club Lobby", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        "EVENT" -> {
                            Button(
                                onClick = {
                                    onDismiss()
                                    if (drop.eventId != null) {
                                        onNavigateToEventDetails(drop.eventId)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Event Details", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        "CREATOR" -> {
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Opening ${drop.creatorName ?: "Creator"}'s Profile...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creator Profile (${drop.creatorName ?: "Host"})", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Navigation Action Button
                    Button(
                        onClick = { onOpenRoute(drop) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get Navigation Route", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = {
                            onClaimConfirm(drop.id)
                            Toast.makeText(context, "Saved ${drop.title} to your Flash Drop alerts!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Drop to Alerts", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// =========================================================================
// ROUTE PREVIEW DIALOG
// =========================================================================

@Composable
fun FlashDropRouteDialog(
    drop: com.example.core.data.FlashDrop,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141E),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Route to ${drop.venueName}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Destination distance: ${drop.distanceText}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )

                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("3 mins drive", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("11 mins walk", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "Choose navigation provider:",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    try {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(drop.venueName)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Opening maps route to ${drop.venueName}...", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Open Google Maps", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

// =========================================================================
// FOMO SMART PLACES HUB ("SEE ALL") CONCIERGE OVERLAY
// =========================================================================

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
                            items(moodFilters) { mood ->
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
                            items(guides) { guide ->
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
                items(filteredVenues) { venue ->
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

@Composable
fun SmartPlaceRouteDialog(
    venue: com.example.core.data.ExploreVenue,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141E),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Route to ${venue.name}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Location: ${venue.address}, ${venue.area} (${venue.distanceText})",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )

                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Fastest Drive", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Safe Walking Route", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    try {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode("${venue.name} ${venue.address}")}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Opening directions to ${venue.name}...", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Open Google Maps", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

