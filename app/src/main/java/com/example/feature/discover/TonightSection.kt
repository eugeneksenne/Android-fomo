package com.example.feature.discover

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.core.data.*

// -------------------------------------------------------------------------
// TONIGHT / MY MOVES INLINE SECTION FOR DISCOVER SCREEN
// -------------------------------------------------------------------------
@Composable
fun TonightSection(
    onNavigateToNightGuard: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToPlansWorkspace: () -> Unit = {}
) {
    val context = LocalContext.current
    val tonightState by TonightRepository.state.collectAsState()
    val nightGuardState by NightGuardRepository.state.collectAsState()

    var isHubOpen by remember { mutableStateOf(false) }
    var isCreatePlanModalOpen by remember { mutableStateOf(false) }
    var isSplitFareModalOpen by remember { mutableStateOf(false) }

    val themeBg = Color(0xFF0D121F)
    val cardBg = Color(0xFF141C2E)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val warmAmber = Color(0xFFFFB703)
    val activeGreen = Color(0xFF00E676)
    val alertRed = Color(0xFFFF1744)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("tonight_my_moves_section")
    ) {
        // Glassmorphism Section Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = cardBg,
            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(accentPurple.copy(alpha = 0.4f), neonCyan.copy(alpha = 0.4f))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 1. SECTION HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "My Moves",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = accentPurple.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, accentPurple.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    "TONIGHT",
                                    color = neonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (nightGuardState.isBuddyActive || nightGuardState.isJourneyActive)
                                "🛡️ Protection active • ${tonightState.plans.size} plans lined up"
                            else
                                "Everything ready for tonight • ${tonightState.plans.size} moves active",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // Live Status Capsule / Plans OS Button
                    Surface(
                        color = activeGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, activeGreen),
                        modifier = Modifier.clickable { onNavigateToPlansWorkspace() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(activeGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "PLANS ENGINE ➔",
                                color = activeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. PRIMARY CARDS GRID / ROW
                // Card 1: NightGuard Safety System Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToNightGuard() }
                        .testTag("tonight_nightguard_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1426)),
                    border = BorderStroke(1.dp, Color(0xFF1E2D4A))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F1B33),
                                        Color(0xFF091021)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(accentPurple.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "NightGuard",
                                    tint = neonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "NightGuard Protection",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🛡️", fontSize = 12.sp)
                                }
                                Text(
                                    text = if (nightGuardState.isBuddyActive)
                                        "Monitoring session '${nightGuardState.buddySessionName}' (${nightGuardState.buddies.size} buddies)"
                                    else
                                        "Monitoring active until 4:00 AM • Safe route aware",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 2: My Plans Card
                val primaryPlan = tonightState.plans.firstOrNull()
                if (primaryPlan != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHubOpen = true }
                            .testTag("tonight_my_plans_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16102B)),
                        border = BorderStroke(1.dp, accentPurple.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF221540),
                                            Color(0xFF120B24)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = warmAmber, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        primaryPlan.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Surface(
                                    color = accentPurple,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "${primaryPlan.type} MOVE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Timeline stops preview
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                primaryPlan.stops.forEachIndexed { index, stop ->
                                    val isCurrent = index == primaryPlan.currentStopIndex
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (isCurrent) neonCyan else Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${stop.time.take(5)} ${stop.venueName.take(8)}",
                                                color = if (isCurrent) Color.Black else Color.White,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (index < primaryPlan.stops.size - 1) {
                                            Text(" ➔ ", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Group Presence Avatars & Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box {
                                        primaryPlan.members.take(4).forEachIndexed { index, member ->
                                            AsyncImage(
                                                model = member.avatarUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .padding(start = (index * 18).dp)
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, cardBg, CircleShape)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width((primaryPlan.members.take(4).size * 18 + 12).dp))
                                    Text(
                                        "${primaryPlan.members.count { it.status == "Arrived" }}/${primaryPlan.members.size} Friends Arrived",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    "Manage Move ➔",
                                    color = neonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 3: Smart Nightlife Intelligence Card
                val topSuggestion = tonightState.suggestions.firstOrNull()
                if (topSuggestion != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHubOpen = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1628),
                        border = BorderStroke(1.dp, warmAmber.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(topSuggestion.iconEmoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(topSuggestion.title, color = warmAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    topSuggestion.description,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(topSuggestion.actionText, color = neonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. MINI QUICK ACTIONS ROW
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Surface(
                            color = accentPurple,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable { isCreatePlanModalOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Plan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { isSplitFareModalOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💸", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Split Fare", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { onNavigateToMap() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🗺️", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Live Map", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        Surface(
                            color = alertRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, alertRed),
                            modifier = Modifier.clickable {
                                NightGuardRepository.setSosActive(true)
                                Toast.makeText(context, "🚨 NightGuard SOS Initiated!", Toast.LENGTH_SHORT).show()
                                onNavigateToNightGuard()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🚨", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Emergency SOS", color = alertRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CTA BAR: OPEN FULL MY MOVES HUB
                Button(
                    onClick = { isHubOpen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tonight_open_hub_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Explore My Moves Hub", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = neonCyan)
                    }
                }
            }
        }
    }

    // FULL-SCREEN MY MOVES HUB OVERLAY
    if (isHubOpen) {
        MyMovesHubOverlay(
            onDismiss = { isHubOpen = false },
            onNavigateToNightGuard = {
                isHubOpen = false
                onNavigateToNightGuard()
            },
            onNavigateToMap = {
                isHubOpen = false
                onNavigateToMap()
            },
            onCreatePlanRequested = { isCreatePlanModalOpen = true }
        )
    }

    // CREATE PLAN MODAL DIALOG
    if (isCreatePlanModalOpen) {
        CreatePlanModalDialog(
            onDismiss = { isCreatePlanModalOpen = false },
            onCreatePlan = { title, type, venues, invited ->
                TonightRepository.createPlan(title, type, venues, invited)
                Toast.makeText(context, "🎉 Night Plan '$title' Created!", Toast.LENGTH_SHORT).show()
                isCreatePlanModalOpen = false
                isHubOpen = true
            }
        )
    }

    // SPLIT FARE DIALOG
    if (isSplitFareModalOpen) {
        SplitFareDialog(
            onDismiss = { isSplitFareModalOpen = false },
            onConfirmSplit = { amount ->
                TonightRepository.splitFare(amount)
                Toast.makeText(context, "💸 R$amount split request sent to group!", Toast.LENGTH_SHORT).show()
                isSplitFareModalOpen = false
            }
        )
    }
}

// -------------------------------------------------------------------------
// FULL-SCREEN MY MOVES HUB OVERLAY
// -------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMovesHubOverlay(
    onDismiss: () -> Unit,
    onNavigateToNightGuard: () -> Unit,
    onNavigateToMap: () -> Unit,
    onCreatePlanRequested: () -> Unit
) {
    val context = LocalContext.current
    val tonightState by TonightRepository.state.collectAsState()
    val nightGuardState by NightGuardRepository.state.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("📋 Active Plans", "🛡️ NightGuard", "⚡ Intelligence", "💬 Moves Chat")

    val themeBg = Color(0xFF090D16)
    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val warmAmber = Color(0xFFFFB703)
    val activeGreen = Color(0xFF00E676)

    val currentPlanId = tonightState.currentSelectedPlanId
    val activePlan = tonightState.plans.find { it.id == currentPlanId } ?: tonightState.plans.firstOrNull()

    var newChatMessage by remember { mutableStateOf("") }
    var newVenueNameInput by remember { mutableStateOf("") }
    var newVenueAreaInput by remember { mutableStateOf("") }
    var isAddingStop by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(themeBg),
        color = themeBg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. TOP BAR
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
                        modifier = Modifier.testTag("my_moves_hub_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("My Moves Hub", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("Nightlife Operating System", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentPurple,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { onCreatePlanRequested() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Move", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. TAB ROW
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = cardBg,
                contentColor = Color.White,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = neonCyan
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTabIndex == index) neonCyan else Color.White.copy(alpha = 0.6f),
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // 3. TAB CONTENT
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // TAB 1: ACTIVE PLANS MANAGEMENT
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // PLAN SELECTION CHIPS
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(tonightState.plans) { p ->
                                        val isSel = p.id == activePlan?.id
                                        Surface(
                                            color = if (isSel) accentPurple else cardBg,
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, if (isSel) neonCyan else Color.White.copy(alpha = 0.1f)),
                                            modifier = Modifier.clickable { TonightRepository.selectPlan(p.id) }
                                        ) {
                                            Text(
                                                "${p.title} (${p.type})",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (activePlan != null) {
                                // ACTIVE PLAN OVERVIEW CARD
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(activePlan.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                                    Text("Invite Code: ${activePlan.inviteCode}", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                IconButton(onClick = {
                                                    Toast.makeText(context, "🔗 Invite link copied! Code: ${activePlan.inviteCode}", Toast.LENGTH_SHORT).show()
                                                }) {
                                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                            Spacer(modifier = Modifier.height(14.dp))

                                            // TIMELINE STOPS LIST
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("TIMELINE STOPS", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                TextButton(onClick = { isAddingStop = !isAddingStop }) {
                                                    Text("+ Add Stop", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            if (isAddingStop) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = newVenueNameInput,
                                                        onValueChange = { newVenueNameInput = it },
                                                        placeholder = { Text("Venue", fontSize = 11.sp, color = Color.White.copy(0.4f)) },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                                    )
                                                    OutlinedTextField(
                                                        value = newVenueAreaInput,
                                                        onValueChange = { newVenueAreaInput = it },
                                                        placeholder = { Text("Area", fontSize = 11.sp, color = Color.White.copy(0.4f)) },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                                    )
                                                    Button(
                                                        onClick = {
                                                            if (newVenueNameInput.isNotBlank()) {
                                                                TonightRepository.addStopToPlan(activePlan.id, newVenueNameInput, newVenueAreaInput.ifBlank { "Johannesburg" }, "12:00 AM")
                                                                newVenueNameInput = ""
                                                                newVenueAreaInput = ""
                                                                isAddingStop = false
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple)
                                                    ) {
                                                        Text("Add")
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                            }

                                            activePlan.stops.forEachIndexed { idx, stop ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        color = if (stop.status == "Active") neonCyan else if (stop.status == "Completed") Color.White.copy(alpha = 0.2f) else accentPurple.copy(alpha = 0.3f),
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(10.dp)
                                                    ) {}
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(stop.venueName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text("${stop.time} • ${stop.area}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                                    }
                                                    Surface(
                                                        color = if (stop.status == "Active") activeGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(stop.status, color = if (stop.status == "Active") activeGreen else Color.White.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // GROUP PRESENCE CARD
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text("GROUP PRESENCE (${activePlan.members.size} Members)", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(10.dp))

                                            activePlan.members.forEach { member ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        AsyncImage(
                                                            model = member.avatarUrl,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(34.dp)
                                                                .clip(CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(member.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text(member.etaText, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                                        }
                                                    }

                                                    Surface(
                                                        color = if (member.status == "Arrived") activeGreen.copy(alpha = 0.2f) else accentPurple.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(member.status, color = if (member.status == "Arrived") activeGreen else neonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // GROUP VOTING WIDGET (IF ACTIVE)
                                if (activePlan.activeVote != null) {
                                    val vote = activePlan.activeVote
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B132E)),
                                            border = BorderStroke(1.dp, warmAmber.copy(alpha = 0.4f))
                                        ) {
                                            Column(modifier = Modifier.padding(18.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("🗳️ GROUP DECISION POLL", color = warmAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(vote.question, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                                Spacer(modifier = Modifier.height(12.dp))

                                                vote.options.forEach { opt ->
                                                    Surface(
                                                        color = if (opt.votedByUser) accentPurple else cardBg,
                                                        shape = RoundedCornerShape(12.dp),
                                                        border = BorderStroke(1.dp, if (opt.votedByUser) neonCyan else Color.White.copy(alpha = 0.1f)),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .clickable { TonightRepository.castVote(activePlan.id, opt.id) }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(opt.venueName, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                            Text("${opt.votes} Votes", color = if (opt.votedByUser) neonCyan else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

                    1 -> {
                        // TAB 2: NIGHTGUARD STATUS & CONTROL
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    border = BorderStroke(1.dp, accentPurple.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Security, contentDescription = null, tint = neonCyan, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("NightGuard Safety Module", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                                                Text("Active monitoring and emergency protection", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = onNavigateToNightGuard,
                                                colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Launch NightGuard")
                                            }

                                            Button(
                                                onClick = onNavigateToMap,
                                                colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("View Radar Map", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text("TRUSTED CONTACTS WATCHING", color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                nightGuardState.buddies.forEach { buddy ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(model = buddy.imageUrl, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(buddy.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Battery: ${buddy.batteryPercent}% • ${buddy.distanceText}", color = Color.White.copy(0.5f), fontSize = 11.sp)
                                            }
                                            Surface(color = activeGreen.copy(0.2f), shape = RoundedCornerShape(6.dp)) {
                                                Text(buddy.status, color = activeGreen, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 3: SMART NIGHTLIFE INTELLIGENCE
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text("REALTIME NIGHTLIFE SUGGESTIONS", color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            items(tonightState.suggestions) { sug ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    border = BorderStroke(1.dp, Color.White.copy(0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(sug.iconEmoji, fontSize = 22.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(sug.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(sug.description, color = Color.White.copy(0.7f), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "⚡ Action '${sug.actionText}' triggered!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentPurple.copy(alpha = 0.5f))
                                        ) {
                                            Text(sug.actionText, color = neonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // TAB 4: PLANS CHAT
                        if (activePlan != null) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(activePlan.chatMessages) { msg ->
                                        if (msg.isSystem) {
                                            Surface(
                                                color = Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    "${msg.senderName}: ${msg.text}",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        } else {
                                            val isMe = msg.senderName == "You"
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                            ) {
                                                Surface(
                                                    color = if (isMe) accentPurple else cardBg,
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        if (!isMe) {
                                                            Text(msg.senderName, color = neonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        }
                                                        Text(msg.text, color = Color.White, fontSize = 13.sp)
                                                    }
                                                }
                                                Text(msg.timeText, color = Color.White.copy(0.4f), fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newChatMessage,
                                        onValueChange = { newChatMessage = it },
                                        placeholder = { Text("Message move chat...", color = Color.White.copy(0.4f), fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (newChatMessage.isNotBlank()) {
                                                TonightRepository.addPlanMessage(activePlan.id, newChatMessage)
                                                newChatMessage = ""
                                            }
                                        },
                                        modifier = Modifier.background(accentPurple, CircleShape)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
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
// CREATE PLAN MODAL DIALOG
// -------------------------------------------------------------------------
@Composable
fun CreatePlanModalDialog(
    onDismiss: () -> Unit,
    onCreatePlan: (title: String, type: PlanType, venues: List<Pair<String, String>>, invited: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PlanType.GROUP) }
    var venue1 by remember { mutableStateOf("Marble Restaurant") }
    var area1 by remember { mutableStateOf("Rosebank") }
    var venue2 by remember { mutableStateOf("LIV Sandton") }
    var area2 by remember { mutableStateOf("Sandton") }
    var invitedNames by remember { mutableStateOf("Amanda, Thabo, Sarah") }

    val cardBg = Color(0xFF141C2E)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Night Move", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Move Title (e.g. Sandton Party)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanType.values().forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.name, fontSize = 11.sp, color = if (selectedType == t) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = neonCyan)
                        )
                    }
                }

                OutlinedTextField(
                    value = venue1,
                    onValueChange = { venue1 = it },
                    label = { Text("First Venue Stop") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = venue2,
                    onValueChange = { venue2 = it },
                    label = { Text("Second Venue Stop") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = invitedNames,
                    onValueChange = { invitedNames = it },
                    label = { Text("Invite Friends (Comma separated)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val venuesList = listOf(Pair(venue1, area1), Pair(venue2, area2)).filter { it.first.isNotBlank() }
                    val friendsList = invitedNames.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onCreatePlan(title, selectedType, venuesList, friendsList)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentPurple)
            ) {
                Text("Launch Move", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(0.6f)) }
        },
        containerColor = cardBg
    )
}

// -------------------------------------------------------------------------
// SPLIT FARE DIALOG
// -------------------------------------------------------------------------
@Composable
fun SplitFareDialog(
    onDismiss: () -> Unit,
    onConfirmSplit: (Double) -> Unit
) {
    var amountInput by remember { mutableStateOf("450") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Rideshare / Table Bill", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter total bill amount (ZAR) to split equally among group members:", color = Color.White.copy(0.7f), fontSize = 12.sp)
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount (R)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 450.0
                    onConfirmSplit(amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Request Split", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(0.6f)) }
        },
        containerColor = Color(0xFF141C2E)
    )
}
