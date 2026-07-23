package com.example.feature.nightguard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.data.BuddyMember
import com.example.core.data.NightGuardRepository
import com.example.core.data.NightGuardState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class NightGuardTab {
    DASHBOARD,
    BUDDY_PAIR,
    WALK_ME_HOME,
    SAFETY_CHECK
}

@Composable
fun NightGuardScreen(onBackClick: () -> Unit) {
    val state by NightGuardRepository.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var activeTab by remember { mutableStateOf(NightGuardTab.DASHBOARD) }
    var showCreateBuddyDialog by remember { mutableStateOf(false) }
    var showCreateJourneyDialog by remember { mutableStateOf(false) }
    var showCreateCheckDialog by remember { mutableStateOf(false) }
    
    // Hold-to-SOS gestures variable
    var isHoldingSos by remember { mutableStateOf(false) }
    var sosHoldProgress by remember { mutableStateOf(0f) }

    // Run periodic real-time location updates (battery, position drift)
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000)
            NightGuardRepository.updateBuddyLocation()
            if (state.isJourneyActive) {
                NightGuardRepository.updateJourneyProgress()
            }
        }
    }

    // Monitor SOS hold countdown
    LaunchedEffect(isHoldingSos) {
        if (isHoldingSos) {
            NightGuardRepository.setSosActive(true)
            var count = 5
            while (count > 0 && isHoldingSos) {
                delay(1000)
                NightGuardRepository.decrementSosCountdown()
                sosHoldProgress = (5 - count + 1) / 5f
                count--
            }
            if (isHoldingSos) {
                // Fully triggered
                Toast.makeText(context, "🚨 EMERGENCY SOS ACTIVATED!", Toast.LENGTH_LONG).show()
                NightGuardRepository.connectEmergencyCall(true)
            }
        } else {
            if (state.isSosActive && !state.isEmergencyCallConnected) {
                NightGuardRepository.setSosActive(false)
            }
            sosHoldProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B13)) // Custom safety-midnight dark
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            NightGuardHeader(
                state = state,
                onBackClick = onBackClick,
                activeTab = activeTab,
                onTabSelect = { activeTab = it }
            )

            // Live Content Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    NightGuardTab.DASHBOARD -> {
                        DashboardPanel(
                            state = state,
                            onTabSelect = { activeTab = it },
                            onTriggerSosDirectly = {
                                NightGuardRepository.setSosActive(true)
                                NightGuardRepository.connectEmergencyCall(true)
                            }
                        )
                    }
                    NightGuardTab.BUDDY_PAIR -> {
                        BuddyPairPanel(
                            state = state,
                            onCreateSessionClick = { showCreateBuddyDialog = true },
                            onEndSessionClick = { NightGuardRepository.endBuddyPair() }
                        )
                    }
                    NightGuardTab.WALK_ME_HOME -> {
                        WalkMeHomePanel(
                            state = state,
                            onCreateJourneyClick = { showCreateJourneyDialog = true },
                            onEndJourneyClick = { NightGuardRepository.endJourney() }
                        )
                    }
                    NightGuardTab.SAFETY_CHECK -> {
                        SafetyCheckPanel(
                            state = state,
                            onCreateCheckClick = { showCreateCheckDialog = true },
                            onCancelCheckClick = { NightGuardRepository.cancelSafetyCheck() }
                        )
                    }
                }
            }
        }

        // --- Overlays and Dialogs ---

        // Safety Check In-App Prompt overlay (Intelligent Scheduled Reminder)
        if (state.showSafetyCheckPrompt) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
                    border = BorderStroke(2.dp, Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(36.dp))
                        }
                        
                        Text(
                            text = "🛡 Safety Check-In",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Text(
                            text = "How is your night out going? Confirming your safety notifies your trusted circle that you are okay.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { NightGuardRepository.snoozeSafetyCheck(10) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26334D)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Snooze (10m)", color = Color.White)
                            }

                            Button(
                                onClick = { 
                                    NightGuardRepository.confirmImsafe()
                                    Toast.makeText(context, "Confirmed safe! Contacts updated.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32D74B)),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("✅ I'm Safe", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        TextButton(
                            onClick = {
                                NightGuardRepository.confirmImsafe()
                                NightGuardRepository.setSosActive(true)
                                NightGuardRepository.connectEmergencyCall(true)
                            }
                        ) {
                            Text("🚨 SOS / Need Help", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // SOS Active Emergency Panel Overlay
        if (state.isSosActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0003)) // Dark red tint
                    .testTag("sos_emergency_overlay")
            ) {
                // Pulse waves behind SOS
                EmergencyPulseBackground()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    if (!state.isEmergencyCallConnected) {
                        // Countdown Mode
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TRIGGERING SOS IN",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "${state.sosCountdown}",
                                color = Color(0xFFFF453A),
                                fontSize = 120.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Release button to cancel emergency trigger",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }

                        // Drag/Cancel indicator
                        Button(
                            onClick = { NightGuardRepository.setSosActive(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E0F14)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF453A))
                        ) {
                            Text("Cancel SOS Panic Alert", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Fully Connected Emergency Center
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f).padding(top = 24.dp)
                        ) {
                            Text(
                                text = "🚨 SOS PLATFORM ACTIVE",
                                color = Color(0xFFFF453A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )

                            // Status card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E0F14).copy(alpha = 0.8f)),
                                border = BorderStroke(1.dp, Color(0xFFFF453A)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF453A).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFFF453A))
                                    }
                                    Column {
                                        Text("Trusted Circle Notified", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Live location, battery level, and ETA shared.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                }
                            }

                            // Dynamic Live Audio Record Waves (Evidence collector)
                            Spacer(modifier = Modifier.height(16.dp))
                            VoiceWaveformCard(
                                isRecording = state.isEvidenceRecorded,
                                onToggleRecording = { 
                                    NightGuardRepository.toggleEvidenceRecording() 
                                    Toast.makeText(context, if (!state.isEvidenceRecorded) "Evidence Recording Started!" else "Evidence Recording Paused", Toast.LENGTH_SHORT).show()
                                }
                            )

                            // Active Call Status
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = Color(0xFF1F3A24),
                                border = BorderStroke(1.dp, Color(0xFF32D74B)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color(0xFF32D74B))
                                    Text("Emergency Center Connected (Simulated)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Complete / Safe verification button to dismiss emergency session
                        Button(
                            onClick = { 
                                NightGuardRepository.setSosActive(false)
                                NightGuardRepository.connectEmergencyCall(false)
                                Toast.makeText(context, "Safety confirmed. Emergency session deactivated.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32D74B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Confirm I'm Safe (Disable SOS)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // --- Creation Dialogs ---

        // 1. Create Buddy Pair Dialog
        if (showCreateBuddyDialog) {
            CreateBuddyPairDialog(
                onDismiss = { showCreateBuddyDialog = false },
                onStart = { name, duration ->
                    NightGuardRepository.startBuddyPair(name, duration)
                    showCreateBuddyDialog = false
                    activeTab = NightGuardTab.BUDDY_PAIR
                    Toast.makeText(context, "Buddy session started: $name", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 2. Create Walk Me Home Dialog
        if (showCreateJourneyDialog) {
            CreateWalkMeHomeDialog(
                onDismiss = { showCreateJourneyDialog = false },
                onStart = { destination, mode, contacts ->
                    NightGuardRepository.startJourney(destination, mode, contacts)
                    showCreateJourneyDialog = false
                    activeTab = NightGuardTab.WALK_ME_HOME
                    Toast.makeText(context, "Journey started to $destination", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 3. Create Safety Check Dialog
        if (showCreateCheckDialog) {
            CreateSafetyCheckDialog(
                onDismiss = { showCreateCheckDialog = false },
                onSchedule = { mins, contacts ->
                    NightGuardRepository.scheduleSafetyCheck(mins, contacts)
                    showCreateCheckDialog = false
                    activeTab = NightGuardTab.SAFETY_CHECK
                    Toast.makeText(context, "Safety check set for $mins mins!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun NightGuardHeader(
    state: NightGuardState,
    onBackClick: () -> Unit,
    activeTab: NightGuardTab,
    onTabSelect: (NightGuardTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1524))
            .padding(top = 48.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                Text(
                    text = "FOMO NIGHT GUARD",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp
                )
            }

            Surface(
                color = if (state.isSosActive) Color(0xFFFF453A).copy(alpha = 0.2f) else Color(0xFF32D74B).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, if (state.isSosActive) Color(0xFFFF453A) else Color(0xFF32D74B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (state.isSosActive) Color(0xFFFF453A) else Color(0xFF32D74B))
                    )
                    Text(
                        text = if (state.isSosActive) "SOS" else "SECURE",
                        color = if (state.isSosActive) Color(0xFFFF453A) else Color(0xFF32D74B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf(
                Triple(NightGuardTab.DASHBOARD, "Dashboard", Icons.Default.GridOn),
                Triple(NightGuardTab.BUDDY_PAIR, "Buddy Pair", Icons.Default.Group),
                Triple(NightGuardTab.WALK_ME_HOME, "Walk Home", Icons.Default.DirectionsWalk),
                Triple(NightGuardTab.SAFETY_CHECK, "Safety Check", Icons.Default.Timer)
            )

            tabs.forEach { (tab, label, icon) ->
                val isSelected = activeTab == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelect(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(2.dp)
                            .background(if (isSelected) Color(0xFF00E5FF) else Color.Transparent)
                    )
                }
            }
        }
    }
}

// --- Tab Panels ---

@Composable
fun DashboardPanel(
    state: NightGuardState,
    onTabSelect: (NightGuardTab) -> Unit,
    onTriggerSosDirectly: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Platform Status Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF32D74B).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF32D74B),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Safety Intelligence Active",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Continuous context monitoring is protecting your night out.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatusIndicatorItem("Presence", "🟢 Secured")
                        StatusIndicatorItem("Confidence", "99% GPS")
                        StatusIndicatorItem("Network", "🟢 Strong")
                    }
                }
            }
        }

        // Big Tactile Panic Alert Trigger
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21080B)),
                border = BorderStroke(1.dp, Color(0xFF7A1C25)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onTriggerSosDirectly() }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF453A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🚨 ACTIVATE EMERGENCY SOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Instant alert dispatched to your contacts & nearby security guards.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
                }
            }
        }

        // Active Protection Overview Headers
        item {
            Text(
                text = "My Protection Modules",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Buddy Pair Dashboard Shortcut Card
        item {
            DashboardModuleCard(
                title = "🤝 Buddy Pair Status",
                subtitle = if (state.isBuddyActive) "Active with ${state.buddySessionName}" else "Keep track of friends temporarily during nightlife.",
                statusText = if (state.isBuddyActive) "🟢 active" else "inactive",
                statusColor = if (state.isBuddyActive) Color(0xFF32D74B) else Color.White.copy(alpha = 0.5f),
                onClick = { onTabSelect(NightGuardTab.BUDDY_PAIR) }
            )
        }

        // Walk Me Home Shortcut Card
        item {
            DashboardModuleCard(
                title = "🚶 Walk Me Home Journey",
                subtitle = if (state.isJourneyActive) "Journeying to ${state.journeyDestination} (${state.journeyEtaMinutes}m left)" else "Navigate via glowing paths, watched by live companions.",
                statusText = if (state.isJourneyActive) "🟢 traveling" else "inactive",
                statusColor = if (state.isJourneyActive) Color(0xFF32D74B) else Color.White.copy(alpha = 0.5f),
                onClick = { onTabSelect(NightGuardTab.WALK_ME_HOME) }
            )
        }

        // Safety Check Shortcut Card
        item {
            DashboardModuleCard(
                title = "🛡 Scheduled Safety Checks",
                subtitle = if (state.isSafetyCheckActive) "Next check-in scheduled in ${state.safetyCheckRemainingMinutes} minutes" else "Verify wellness on a timer without sharing live GPS.",
                statusText = if (state.isSafetyCheckActive) "🟢 active" else "inactive",
                statusColor = if (state.isSafetyCheckActive) Color(0xFF32D74B) else Color.White.copy(alpha = 0.5f),
                onClick = { onTabSelect(NightGuardTab.SAFETY_CHECK) }
            )
        }
    }
}

@Composable
fun StatusIndicatorItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun DashboardModuleCard(
    title: String,
    subtitle: String,
    statusText: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = statusText.uppercase(),
                            color = statusColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun BuddyPairPanel(
    state: NightGuardState,
    onCreateSessionClick: () -> Unit,
    onEndSessionClick: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!state.isBuddyActive) {
            // Unactive State Layout
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(36.dp))
                        }
                        
                        Text("Start a Buddy Pair", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Coordinate easily at festivals or club outings. Share temporary locations, monitor battery levels, set rendezvous points, and locate lost friends instantly. Expires automatically.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Button(
                            onClick = onCreateSessionClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("＋ Pair with Friends", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Active Mode Layout
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(state.buddySessionName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Remaining session: ${state.buddyRemainingMinutes} mins", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    TextButton(onClick = onEndSessionClick) {
                        Text("End Pair", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Interactive Social Safety Map representation
            item {
                Text("Buddy Map Viewport", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                SocialBuddyMap(state = state)
            }

            // Quick Rendezvous/Meetup actions
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("📍 Meet-Up Rendezvous Point", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (state.meetupPoint != null) {
                                    Text(state.meetupPoint.title, color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("No meet-up point created yet.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                            }

                            if (state.meetupPoint != null) {
                                TextButton(onClick = { NightGuardRepository.removeMeetupPoint() }) {
                                    Text("Remove", color = Color(0xFFFF453A), fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { NightGuardRepository.setMeetupPoint("FOMO Main Entrance Lobby") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Set Rendezvous Lobby", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Buddy List with interactive buttons for Lost Friend locator
            item {
                Text("Active Buddies Details", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            items(state.buddies) { buddy ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (buddy.isSelectedForLostFriend) Color(0xFF1F122B) else Color(0xFF0F1524)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (buddy.isSelectedForLostFriend) Color(0xFFBB86FC) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = buddy.imageUrl,
                            contentDescription = buddy.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(buddy.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(
                                    color = Color(0xFF32D74B).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = buddy.status,
                                        color = Color(0xFF32D74B),
                                        fontSize = 8.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 2.dp)) {
                                Text("⚡ ${buddy.batteryPercent}% Batt", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Text("📍 ${buddy.distanceText}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { 
                                NightGuardRepository.highlightFriendForLostFriend(buddy.id)
                                if (!buddy.isSelectedForLostFriend) {
                                    Toast.makeText(context, "Lost Friend Engine: Compass locked on ${buddy.name}!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (buddy.isSelectedForLostFriend) Color(0xFFBB86FC) else Color(0xFF26334D)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                Text(if (buddy.isSelectedForLostFriend) "Tracking" else "Locate", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalkMeHomePanel(
    state: NightGuardState,
    onCreateJourneyClick: () -> Unit,
    onEndJourneyClick: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!state.isJourneyActive) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF2D55).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(36.dp))
                        }
                        
                        Text("Start Walk Me Home Journey", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Walk or drive under continuous intelligence monitoring. Trusted companions can view your progress on a glowing map path in real-time. Automatic arrival alerts end sharing.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Button(
                            onClick = onCreateJourneyClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("＋ Set Destination", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Active Journey state
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Journey to ${state.journeyDestination}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Mode: ${state.journeyMode} • Companion secure", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    TextButton(onClick = onEndJourneyClick) {
                        Text("End Trip", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Interactive Glowing Route Map Visualization
            item {
                JourneyMapView(state = state)
            }

            // Simulated Journey Controls / Progress trigger
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
                    border = BorderStroke(1.dp, Color(0xFF26334D)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Safety Companion Console", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "🟢 SECURED",
                                color = Color(0xFF32D74B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text("Monitor real-time route progress, safety checkpoints, and companion tracking updates.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { 
                                    NightGuardRepository.updateJourneyProgress()
                                    Toast.makeText(context, "Location updated. Position moved closer.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3D59)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Advance Trip", fontSize = 10.sp, color = Color.White)
                            }

                            Button(
                                onClick = { 
                                    NightGuardRepository.triggerRouteDeviation() 
                                    Toast.makeText(context, "⚠️ Rerouting alert triggered!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1E22)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.3f),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFF453A))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Trigger Deviation", fontSize = 10.sp, color = Color(0xFFFF453A))
                            }
                        }
                    }
                }
            }

            // Companion Watchers dashboard
            item {
                Text("Companions Chat Feed", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            items(state.companionMessages) { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF2D55).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = msg,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyCheckPanel(
    state: NightGuardState,
    onCreateCheckClick: () -> Unit,
    onCancelCheckClick: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!state.isSafetyCheckActive) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF32D74B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF32D74B), modifier = Modifier.size(36.dp))
                        }
                        
                        Text("Configure Safety Check-In", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "The ultimate silent safety tool. Schedule a check-in timer (e.g. 30 mins). If you do not tap 'I'm Safe' when it fires, contacts are notified. Quiet, privacy-safe, with zero constant GPS tracking.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Button(
                            onClick = onCreateCheckClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32D74B)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("＋ Set Safety Timer", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Active countdown mode
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🛡 ACTIVE SAFETY CHECK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF32D74B),
                            letterSpacing = 1.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Text(
                                text = "${state.safetyCheckRemainingMinutes} Mins Left",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "We will prompt you with a safety check-in when the countdown ends.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Simulation / Action options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onCancelCheckClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E0F14)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", color = Color(0xFFFF453A))
                            }

                            Button(
                                onClick = { 
                                    NightGuardRepository.triggerSafetyCheckPrompt() 
                                    Toast.makeText(context, "Simulated check-in prompt!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32D74B)),
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Trigger Prompt Now", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Escalation info / Contacts details
            item {
                Text("Escalation Recipients", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            items(state.safetyCheckContacts) { contactName ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF32D74B).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF32D74B), modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(contactName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Alerts if response missed", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF32D74B), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// --- Custom Graphics Widgets ---

@Composable
fun SocialBuddyMap(state: NightGuardState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=600&auto=format&fit=crop",
            contentDescription = "Buddy Coordination Map",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.35f
        )

        // Custom canvas overlay representing friendship paths/radar sweeps
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Radar sweep ring
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                center = center,
                radius = 180f,
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            // Connection line to buddies
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                start = center,
                end = Offset(center.x + 120f, center.y - 140f),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 5f)
            )

            drawLine(
                color = Color(0xFFBB86FC).copy(alpha = 0.4f),
                start = center,
                end = Offset(center.x - 140f, center.y + 110f),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 5f)
            )
        }

        // --- Simulated Floating Avatars on Map ---
        
        // Main User Pin
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, Color(0xFF00E5FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://i.pravatar.cc/150?img=12",
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }

        // Buddy Pin 1 (Sarah)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 60.dp, y = (-70).dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, Color(0xFF00E5FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://i.pravatar.cc/150?img=32",
                contentDescription = "Sarah Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }

        // Buddy Pin 2 (James)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-70).dp, y = 55.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, Color(0xFFBB86FC), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://i.pravatar.cc/150?img=12",
                contentDescription = "James Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }

        // Meetup Point pin
        if (state.meetupPoint != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 10.dp, y = (-40).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun JourneyMapView(state: NightGuardState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=600&auto=format&fit=crop",
            contentDescription = "Walk Me Home Live Route Map",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.3f
        )

        // Draw neon progress line and destination glowing effects
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startPoint = Offset(140f, size.height - 100f)
            val detourPoint = Offset(size.width / 2 + 80f, size.height / 2 + 60f)
            val endPoint = Offset(size.width - 120f, 100f)

            // Neon glowing baseline route
            drawLine(
                color = if (state.routeDeviationDetected) Color(0xFFFF453A) else Color(0xFFFF2D55),
                start = startPoint,
                end = detourPoint,
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = if (state.routeDeviationDetected) Color(0xFFFF453A).copy(alpha = 0.3f) else Color(0xFFFF2D55),
                start = detourPoint,
                end = endPoint,
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )

            // If deviation, draw dotted/dashed detour track
            if (state.routeDeviationDetected) {
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = detourPoint,
                    end = Offset(size.width / 2 - 40f, size.height / 2 - 20f),
                    strokeWidth = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f),
                    cap = StrokeCap.Round
                )
            }
        }

        // Destination Pin Card overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color(0xFF131A26).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFFF2D55).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ETA", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                Text("${state.journeyEtaMinutes}m", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF2D55))
                Text("${String.format("%.1f", state.journeyRemainingDistanceKm)} km", fontSize = 10.sp, color = Color.White)
            }
        }

        // Live User pin
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 32.dp, y = (-24).dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, Color(0xFFFF2D55), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://i.pravatar.cc/150?img=12",
                contentDescription = "My Position",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }

        // Destination marker
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-100).dp, y = 16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF2D55)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Home, contentDescription = "Destination Home", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun EmergencyPulseBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFFF453A).copy(alpha = alpha),
                radius = size.minDimension / 4 * scale,
                center = center
            )
        }
    }
}

@Composable
fun VoiceWaveformCard(
    isRecording: Boolean,
    onToggleRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
        border = BorderStroke(1.dp, Color(0xFF26334D)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color(0xFFFF453A) else Color.White.copy(alpha = 0.3f))
                    )
                    Text(
                        text = if (isRecording) "LIVE AUDIOMODULATOR EVIDENCE ACTIVE" else "Silent Mode Active",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onToggleRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color(0xFFFF453A) else Color(0xFF26334D)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRecording) "Stop" else "Record Audio", fontSize = 10.sp, color = Color.White)
                }
            }

            // Waveform visualizer bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heights = listOf(0.3f, 0.6f, 0.4f, 0.8f, 0.5f, 0.9f, 0.4f, 0.7f, 0.3f, 0.5f)
                heights.forEachIndexed { index, baseHeight ->
                    val waveHeight = if (isRecording) {
                        baseHeight * (0.4f + animationProgress * 0.6f)
                    } else {
                        0.1f
                    }
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight(waveHeight)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isRecording) Color(0xFFFF453A) else Color.White.copy(alpha = 0.2f))
                    )
                }
            }
        }
    }
}

// --- Specific Selection Dialogs ---

@Composable
fun CreateBuddyPairDialog(
    onDismiss: () -> Unit,
    onStart: (String, Int) -> Unit
) {
    var sessionName by remember { mutableStateOf("Club Session") }
    var duration by remember { mutableStateOf(120) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onStart(sessionName, duration) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Start Buddy Pair", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        title = {
            Text("🤝 Pair with Buddies", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Session Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF00E5FF),
                        focusedBorderColor = Color(0xFF00E5FF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Choose Duration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val durations = listOf(60, 120, 240, 480)
                    durations.forEach { mins ->
                        val selected = duration == mins
                        Surface(
                            color = if (selected) Color(0xFF00E5FF) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { duration = mins }
                        ) {
                            Text(
                                text = "${mins / 60}h",
                                color = if (selected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0F1524),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun CreateWalkMeHomeDialog(
    onDismiss: () -> Unit,
    onStart: (String, String, List<String>) -> Unit
) {
    var destination by remember { mutableStateOf("Home") }
    var transportMode by remember { mutableStateOf("Walking") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onStart(destination, transportMode, listOf("Sarah", "James")) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55))
            ) {
                Text("Start Walk Me Home", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        title = {
            Text("🚶 Secure Journey Setup", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Select Destination") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFFF2D55),
                        focusedBorderColor = Color(0xFFFF2D55)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Choose Travel Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf("Walking", "Driving", "Ride-share")
                    modes.forEach { m ->
                        val selected = transportMode == m
                        Surface(
                            color = if (selected) Color(0xFFFF2D55) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { transportMode = m }
                        ) {
                            Text(
                                text = m,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0F1524),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun CreateSafetyCheckDialog(
    onDismiss: () -> Unit,
    onSchedule: (Int, List<String>) -> Unit
) {
    var durationMinutes by remember { mutableStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSchedule(durationMinutes, listOf("Sarah", "Kgomotso")) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32D74B))
            ) {
                Text("Set Timer", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        title = {
            Text("🛡 Schedule Safety Check", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select Check-in Frequency:", color = Color.White, fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(15, 30, 60, 120)
                    presets.forEach { mins ->
                        val selected = durationMinutes == mins
                        Surface(
                            color = if (selected) Color(0xFF32D74B) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { durationMinutes = mins }
                        ) {
                            Text(
                                text = "${mins}m",
                                color = if (selected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0F1524),
        shape = RoundedCornerShape(24.dp)
    )
}
