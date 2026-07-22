package com.example.feature.clublobby

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.data.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubLobbyScreen(
    onBackClick: () -> Unit,
    onNavigateToRoute: () -> Unit
) {
    val context = LocalContext.current
    val state by VenueRepository.venueState.collectAsState()

    // Screen interaction states
    var showPlanSheet by remember { mutableStateOf(false) }
    var activePlan by remember { mutableStateOf<String?>(null) }
    var showClaimedDrop by remember { mutableStateOf<FlashDrop?>(null) }
    var activeBroadcastChat by remember { mutableStateOf(listOf("John: Vibe inside is crazy right now! 🔥", "Lerato: Kabza is going on in 20 min!", "Sbu: Rooftop has the best view.", "Amelia: Queue is moving fast!")) }

    // Floating Live Broadcaster message simulator
    LaunchedEffect(state.liveBroadcast.isLive) {
        if (state.liveBroadcast.isLive) {
            val names = listOf("Dave", "Nthabi", "Kagiso", "Zama", "Priscilla", "Brandon")
            val texts = listOf("Is Uncle Waffles performing yet?", "Best music in SA! 🇿🇦", "Drinks are priced well.", "Rooftop deck is packed!", "On my way right now!", "Amapiano to the world!")
            while (true) {
                delay(4000)
                val newMsg = "${names.random()}: ${texts.random()}"
                activeBroadcastChat = (activeBroadcastChat + newMsg).takeLast(4)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14)) // Immersive Midnight OLED Dark Black
            .testTag("club_lobby_screen")
    ) {
        // Main Scrollable Content containing all sections
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp) // Leave space for sticky bottom bar
        ) {
            // 1. HERO EXPERIENCE
            HeroExperienceSection(
                state = state,
                onBackClick = onBackClick,
                onViewLiveStreamClick = {
                    Toast.makeText(context, "Streaming DJ Booth live!", Toast.LENGTH_SHORT).show()
                }
            )

            // 2. TONIGHT AT A GLANCE
            TonightAtAGlanceSection(state = state)

            // 3. LIVE UPDATES
            LiveUpdatesSection(state = state)

            // 4. TONIGHT'S EVENTS
            TonightEventsSection(state = state, context = context)

            // 5. TONIGHT'S LINEUP
            TonightLineupSection(state = state)

            // 6. FLASH DROPS
            FlashDropsSection(
                state = state,
                onClaim = { drop ->
                    VenueRepository.claimFlashDrop(drop.id)
                    showClaimedDrop = drop.copy(claimed = true, currentStock = drop.currentStock - 1)
                }
            )

            // 7. LIVE BROADCAST PREVIEW
            LiveBroadcastSection(
                state = state,
                comments = activeBroadcastChat,
                onWatchStream = {
                    Toast.makeText(context, "Expanding Live HD DJ Stream...", Toast.LENGTH_SHORT).show()
                }
            )

            // 8. TONIGHT AT THE VENUE & 9. MOMENTS
            TonightAtVenueAndMomentsSection(state = state)

            // 10. TONIGHT'S VIBE SENTIMENT POLL
            TonightVibePollSection(state = state)

            // 11. PLANNING WITH FRIENDS
            PlanningWithFriendsSection(
                state = state,
                onPlanNightClick = { showPlanSheet = true },
                activePlan = activePlan
            )

            // 12. VENUE EXPERIENCE & 13. AMENITIES
            ExperienceAndAmenitiesSection(state = state)

            // 14. VENUE HIGHLIGHTS
            VenueHighlightsSection(state = state)

            // 15. VENUE NOTICES
            VenueNoticesSection(state = state)

            // 16. VENUE INFORMATION
            VenueInformationSection(state = state)

            // 17. LOCATION & ARRIVAL
            LocationAndArrivalSection(state = state, onNavigateToRoute = onNavigateToRoute)
        }

        // 18. STICKY ACTION DOCK
        StickyActionDock(
            modifier = Modifier.align(Alignment.BottomCenter),
            state = state,
            activePlan = activePlan,
            onPlanClick = { showPlanSheet = true },
            onRouteClick = onNavigateToRoute
        )

        // DIALOGS & SHEET OVERLAYS

        // Planning Bottom Sheet Simulator
        if (showPlanSheet) {
            PlanNightDialog(
                onDismiss = { showPlanSheet = false },
                onPlanSelected = { plan ->
                    activePlan = plan
                    showPlanSheet = false
                    Toast.makeText(context, "Plan registered: $plan!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // QR Code Claimed Reward voucher overlay
        if (showClaimedDrop != null) {
            FlashDropClaimedDialog(
                drop = showClaimedDrop!!,
                onDismiss = { showClaimedDrop = null }
            )
        }
    }
}

// 1. HERO EXPERIENCE COMPOSABLE
@Composable
fun HeroExperienceSection(
    state: VenueProfileState,
    onBackClick: () -> Unit,
    onViewLiveStreamClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        // Carousel Backdrop image (represented with premium Club image)
        AsyncImage(
            model = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=800&auto=format&fit=crop",
            contentDescription = "Venue Cover Video Highlight",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Gradient for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color(0xFF0F0F14)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Top bar action buttons (Back & Save Favorite)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            IconButton(
                onClick = { VenueRepository.toggleSave() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("save_button")
            ) {
                Icon(
                    imageVector = if (state.isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Save Venue",
                    tint = if (state.isSaved) Color(0xFFFF2D55) else Color.White
                )
            }
        }

        // Live streaming floating badge
        if (state.liveBroadcast.isLive) {
            Surface(
                color = Color(0xFFFF2D55),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 16.dp)
                    .clickable { onViewLiveStreamClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("VIEW LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Venue details aligned at the bottom of hero
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.name,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Verified Venue",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFD60A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${state.rating} (${state.reviewCount})", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Text("•", color = Color.White.copy(alpha = 0.4f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = "Location", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(state.neighborhood.substringBefore(","), color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                }

                Text("•", color = Color.White.copy(alpha = 0.4f))

                Text(state.distance, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    state.openStatus,
                    color = Color(0xFF34C759),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    state.closingTime,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

// 2. TONIGHT AT A GLANCE COMPOSABLE
@Composable
fun TonightAtAGlanceSection(state: VenueProfileState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Tonight at a Glance",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlanceBadge(Icons.Default.Event, "${state.events.size} Events", "Featured lineup")
                GlanceBadge(Icons.Default.Headphones, "${state.lineup.size} Artists", "Music timetable")
                GlanceBadge(Icons.Default.CardGiftcard, "${state.flashDrops.size} Flash Drops", "VIP deals active")
                if (state.liveBroadcast.isLive) {
                    GlanceBadge(Icons.Default.LiveTv, "LIVE Stream", "Watch DJ Booth", Color(0xFFFF2D55))
                }
            }
        }
    }
}

@Composable
fun GlanceBadge(icon: ImageVector, title: String, subtitle: String, textColor: Color = Color.White) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (textColor != Color.White) textColor else Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

// 3. LIVE UPDATES COMPOSABLE (24H EPHEMERAL VIBE LOG)
@Composable
fun LiveUpdatesSection(state: VenueProfileState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Live Updates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text("Updates hourly", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.liveUpdates.forEachIndexed { idx, update ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = update.authorImage,
                                contentDescription = update.author,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(update.author, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(update.authorRole.uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Text(update.timeAgo, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = update.content,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 46.dp, top = 4.dp)
                        )

                        if (idx < state.liveUpdates.size - 1) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
    }
}

// 4. TONIGHT'S EVENTS COMPOSABLE
@Composable
fun TonightEventsSection(state: VenueProfileState, context: android.content.Context) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            "Tonight's Featured Events",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.events) { event ->
                TonightEventCard(event = event) {
                    Toast.makeText(context, "${event.title}: Ticket booked successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun TonightEventCard(event: TonightEvent, onTicketClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(280.dp)
            .clickable { }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                AsyncImage(
                    model = event.posterUrl,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Ticket status badge overlay
                Surface(
                    color = when (event.ticketStatus) {
                        "Selling Fast" -> Color(0xFFFF9500)
                        "Sold Out" -> Color(0xFFFF3B30)
                        else -> Color(0xFF34C759)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        event.ticketStatus.uppercase(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    event.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Time: ${event.time}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "Headliner: ${event.headliner}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🔥 ${event.interestedCount} interested",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = onTicketClick,
                        enabled = event.ticketStatus != "Sold Out",
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Get Tickets", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 5. TONIGHT'S LINEUP TIMETABLE COMPOSABLE
@Composable
fun TonightLineupSection(state: VenueProfileState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tonight's Performance Lineup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Timetable", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                state.lineup.forEachIndexed { idx, slot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Timetable block
                        Text(
                            slot.time,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.width(100.dp)
                        )

                        // Vertical Timeline separator bar
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(36.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        AsyncImage(
                            model = slot.imageUrl,
                            contentDescription = slot.artistName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(slot.artistName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(slot.role, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    if (idx < state.lineup.size - 1) {
                        Divider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

// 6. FLASH DROPS REWARDS COMPOSABLE
@Composable
fun FlashDropsSection(state: VenueProfileState, onClaim: (FlashDrop) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Flash Drops Available", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text("Claim in-person", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.flashDrops.forEach { drop ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
                    border = if (drop.claimed) BorderStroke(1.dp, Color(0xFF34C759)) else null,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (drop.claimed) Color(0xFF34C759).copy(alpha = 0.1f)
                                    else Color(0xFFFF9500).copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (drop.claimed) Icons.Default.CheckCircle else Icons.Default.LocalDrink,
                                contentDescription = null,
                                tint = if (drop.claimed) Color(0xFF34C759) else Color(0xFFFF9500),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(drop.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(drop.subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Expires in ${drop.expiresMinutes}m", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Remaining: ${drop.currentStock}/${drop.initialStock}", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { onClaim(drop) },
                            enabled = !drop.claimed && drop.currentStock > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (drop.claimed) Color(0xFF34C759) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                if (drop.claimed) "Claimed" else "Claim",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// 7. LIVE BROADCAST PREVIEW COMPOSABLE
@Composable
fun LiveBroadcastSection(
    state: VenueProfileState,
    comments: List<String>,
    onWatchStream: () -> Unit
) {
    if (!state.liveBroadcast.isLive) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF2D55))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Live DJ Booth Broadcast", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Surface(
                color = Color(0xFFFF2D55),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "LIVE",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(Color.Black)
                ) {
                    // Simulated visual video frequency waves (animated)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF8E2DE2).copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(80.dp)
                        )

                        // Scrolling comments preview on top of stream
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            comments.forEach { comment ->
                                Text(comment, color = Color.White, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }

                    // Floating watchers badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${state.liveBroadcast.watcherCount} watching", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(state.liveBroadcast.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Muted Preview • Tap to Listen", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }

                    Button(
                        onClick = onWatchStream,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Watch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 8. TONIGHT AT THE VENUE & 9. MOMENTS COM_POSABLES
@Composable
fun TonightAtVenueAndMomentsSection(state: VenueProfileState) {
    var activeTab by remember { mutableStateOf("Live Vibe") }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            listOf("Live Vibe", "Golden Moments").forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tab,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeTab == "Live Vibe") {
            // Tonight at Venue Live media feed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TonightMediaItem(
                    url = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=300&auto=format&fit=crop",
                    badge = "Crowd Live",
                    modifier = Modifier.weight(1f)
                )
                TonightMediaItem(
                    url = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=300&auto=format&fit=crop",
                    badge = "DJ Booth",
                    modifier = Modifier.weight(1f)
                )
                TonightMediaItem(
                    url = "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=300&auto=format&fit=crop",
                    badge = "Vibe Inside",
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // 9. Permanent Golden Moments
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.moments.forEach { moment ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = moment.imageUrl,
                                contentDescription = moment.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(moment.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(moment.date, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("📸 ${moment.photoCount} Photos", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    Text("🎥 ${moment.videoCount} Videos", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD60A), modifier = Modifier.size(16.dp))
                                Text("${moment.rating}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TonightMediaItem(url: String, badge: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = url,
            contentDescription = badge,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        ) {
            Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}

// 10. TONIGHT'S VIBE SENTIMENT POLL
@Composable
fun TonightVibePollSection(state: VenueProfileState) {
    val poll = state.vibePoll
    val totalVotes = poll.litCount + poll.goodCount + poll.quietCount
    val litPercentage = if (totalVotes > 0) (poll.litCount * 100 / totalVotes) else 0
    val goodPercentage = if (totalVotes > 0) (poll.goodCount * 100 / totalVotes) else 0
    val quietPercentage = if (totalVotes > 0) (poll.quietCount * 100 / totalVotes) else 0

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tonight's Vibe Poll", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Verified voters only", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Attending tonight? Help others decide by voting on the current vibe inside.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VibeVoteRow("🔥 Lit", litPercentage, poll.userVote == "Lit") {
                    VenueRepository.submitVibeVote("Lit")
                }
                VibeVoteRow("🙂 Good", goodPercentage, poll.userVote == "Good") {
                    VenueRepository.submitVibeVote("Good")
                }
                VibeVoteRow("😴 Quiet", quietPercentage, poll.userVote == "Quiet") {
                    VenueRepository.submitVibeVote("Quiet")
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Vibe sentiment computed live from $totalVotes verified visitor votes.", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun VibeVoteRow(label: String, percentage: Int, isSelected: Boolean, onVote: () -> Unit) {
    val animatedProgress by animateFloatAsState(targetValue = percentage / 100f, animationSpec = tween(800))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onVote() }
    ) {
        // Dynamic Progress bar filling
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.05f)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
            Text("$percentage%", color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// 11. PLANNING WITH FRIENDS COMPOSABLE
@Composable
fun PlanningWithFriendsSection(
    state: VenueProfileState,
    onPlanNightClick: () -> Unit,
    activePlan: String?
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Planning Tonight", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("${state.friendsGoing.size} friends going", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Horizontal list of friends going
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.friendsGoing) { friend ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                            Box(modifier = Modifier.size(48.dp)) {
                                AsyncImage(
                                    model = friend.imgUrl,
                                    contentDescription = friend.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34C759))
                                        .align(Alignment.BottomEnd)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(friend.name.substringBefore(" "), color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(friend.status, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (activePlan != null) "Active Plan: $activePlan" else "Plan your night at ${state.name}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (activePlan != null) "Status shared with close friends only" else "Coordinate with friends securely",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onPlanNightClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp).testTag("plan_night_button")
                    ) {
                        Text(if (activePlan != null) "Change Plan" else "Plan Night", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 12. VENUE EXPERIENCE & 13. AMENITIES COMPOSABLES
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExperienceAndAmenitiesSection(state: VenueProfileState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Venue Vibe & Amenities", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Experience tags FlowRow
                Text("Music Genres", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.musicTags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(tag, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Atmosphere", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.atmosphereTags.forEach { tag ->
                        Surface(
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(tag, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                // Amenities list grid
                Text("Facilities & Amenities", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.amenities.forEach { facility ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(facility, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// 14. VENUE HIGHLIGHTS COMPOSABLE
@Composable
fun VenueHighlightsSection(state: VenueProfileState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Venue Highlights", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.highlights.forEach { highlight ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = highlight.imageUrl,
                            contentDescription = highlight.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(highlight.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(highlight.description, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// 15. VENUE NOTICES COMPOSABLE
@Composable
fun VenueNoticesSection(state: VenueProfileState) {
    if (state.notices.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Operational Notices", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.notices.forEach { notice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (notice.type == "Alert") Color(0xFFFF3B30).copy(alpha = 0.1f)
                            else Color(0xFF007AFF).copy(alpha = 0.1f)
                        )
                        .border(
                            1.dp,
                            if (notice.type == "Alert") Color(0xFFFF3B30).copy(alpha = 0.2f)
                            else Color(0xFF007AFF).copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (notice.type == "Alert") Icons.Default.Security else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (notice.type == "Alert") Color(0xFFFF3B30) else Color(0xFF007AFF),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(notice.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(notice.timeAgo, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(notice.content, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

// 16. VENUE INFORMATION COMPOSABLE
@Composable
fun VenueInformationSection(state: VenueProfileState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Venue Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = state.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Divider(color = Color.White.copy(alpha = 0.05f))

                InfoLabelRow("Opening Hours", "Wed - Sun (18:00 - 04:00)")
                InfoLabelRow("Dress Code", "Smart Casual / Premium Fashion")
                InfoLabelRow("Age Requirement", "Over 21s Only (ID Required)")
                InfoLabelRow("Capacity Limit", "1,200 Persons maximum")
                InfoLabelRow("Official Website", "www.fomoclub.co.za", isLink = true)
            }
        }
    }
}

@Composable
fun InfoLabelRow(label: String, value: String, isLink: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        Text(
            value,
            color = if (isLink) MaterialTheme.colorScheme.primary else Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.clickable(enabled = isLink) { }
        )
    }
}

// 17. LOCATION & ARRIVAL COMPOSABLE (WITH CLUB LOBBY ENTRANCE ROUTE MAP DETAILS)
@Composable
fun LocationAndArrivalSection(
    state: VenueProfileState,
    onNavigateToRoute: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Location & Arrival Entrance Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Location Address Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("144 Jan Smuts Ave", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(state.neighborhood, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Distance & Travel timings row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚗 ${state.driveTime}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚶 ${state.walkTime}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                // Custom Entrance & Floorplan Layout Info
                Text("Club Lobby Layout Routes", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntranceLayoutRow(Icons.Default.LocalParking, "Safe Parking Deck", "Basement Level B1 & B2, fully monitored")
                    EntranceLayoutRow(Icons.Default.Directions, "Main Entrance Queue", "Standard ticketing entry queue (Fast)")
                    EntranceLayoutRow(Icons.Default.Star, "VIP Lounge Fast-track", "Rooftop elevator queue (Exclusive)")
                    EntranceLayoutRow(Icons.Default.People, "Smoking Lounge / Courtyard", "Located next to main bar area")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToRoute,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("route_button")
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Route & Floor Map", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EntranceLayoutRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
    }
}

// 18. STICKY BOTTOM ACTION DOCK
@Composable
fun StickyActionDock(
    modifier: Modifier = Modifier,
    state: VenueProfileState,
    activePlan: String?,
    onPlanClick: () -> Unit,
    onRouteClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1E26),
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Follow
            IconButton(
                onClick = { VenueRepository.toggleFollow() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (state.isFollowing) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.06f)
                    )
            ) {
                Icon(
                    imageVector = if (state.isFollowing) Icons.Default.CheckCircle else Icons.Default.GroupAdd,
                    contentDescription = "Follow Venue",
                    tint = if (state.isFollowing) MaterialTheme.colorScheme.primary else Color.White
                )
            }

            // Route Button (Immediate navigation)
            Button(
                onClick = onRouteClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Route", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Interactive Plan Night CTA
            Button(
                onClick = onPlanClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (activePlan != null) "Plan: $activePlan" else "Plan Night",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// SIMULATOR DIALOG FOR PLANNING NIGHT WITH FRIENDS
@Composable
fun PlanNightDialog(onDismiss: () -> Unit, onPlanSelected: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Coordinate Your Night", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select your attendance plans for FOMO Club tonight. This updates your friends securely so you can meet up easily.", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                PlanOptionRow("Going Solo", "Cruising alone for a chill night inside.", onPlanSelected)
                PlanOptionRow("With Close Friends", "Meeting up with specific buddies in a small crew.", onPlanSelected)
                PlanOptionRow("Large Group Takeover", "Part of a massive squad booking tables.", onPlanSelected)
                PlanOptionRow("Maybe Later", "Interested but still keeping moves flexible.", onPlanSelected)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PlanOptionRow(label: String, subtitle: String, onSelect: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(label) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

// SIMULATOR DIALOG FOR FLASH DROP REDEMPTION
@Composable
fun FlashDropClaimedDialog(drop: FlashDrop, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Reward Voucher Claimed!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(drop.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, textAlign = TextAlign.Center)
                Text(drop.subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code simulation box
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Custom drawn QR Grid Simulator
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val size = 4
                                val cellW = this.size.width / size
                                val cellH = this.size.height / size
                                for (x in 0 until size) {
                                    for (y in 0 until size) {
                                        if ((x + y) % 2 == 0) {
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = Offset(x * cellW, y * cellH),
                                                size = androidx.compose.ui.geometry.Size(cellW, cellH)
                                            )
                                        }
                                    }
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Voucher ID: FOMO-${drop.id.uppercase()}-9842", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Show this screen to the venue door or bar staff to redeem. Do not claim until you are ready to be served.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done", color = Color.White)
            }
        }
    )
}
