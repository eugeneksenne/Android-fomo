package com.example.feature.chats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.data.NightGuardRepository
import com.example.core.data.NightGuardState

/**
 * Schedule Safety Check Setup Modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCheckInviteDialog(
    onDismiss: () -> Unit,
    onStartCheck: (durationMinutes: Int, durationText: String, contacts: List<String>) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(60) }
    var selectedDurationText by remember { mutableStateOf("1 Hour") }
    var selectedContacts by remember { mutableStateOf(setOf("Sarah", "Kgomotso")) }
    var customMinutesInput by remember { mutableStateOf("") }

    val presetDurations = listOf(
        Triple("15m", 15, "15 Minutes"),
        Triple("30m", 30, "30 Minutes"),
        Triple("1 Hour", 60, "1 Hour"),
        Triple("2 Hours", 120, "2 Hours"),
        Triple("Event End", 90, "After Event Ends")
    )

    val companionList = listOf(
        "Sarah" to "https://i.pravatar.cc/150?img=32",
        "Kgomotso" to "https://i.pravatar.cc/150?img=49",
        "James" to "https://i.pravatar.cc/150?img=12",
        "Peter" to "https://i.pravatar.cc/150?img=11"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF161426),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2A4A))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Schedule Safety Check",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Verification without continuous location sharing",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select Interval
                Text(
                    text = "Check-in Interval:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetDurations) { (label, mins, desc) ->
                        val isSel = selectedMinutes == mins
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) Color(0xFF5856D6) else Color(0xFF222038),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) Color(0xFF7D7AFF) else Color(0xFF2C2A48)
                            ),
                            modifier = Modifier.clickable {
                                selectedMinutes = mins
                                selectedDurationText = desc
                            }
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select Watchers
                Text(
                    text = "Notify Contacts if Missed:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(companionList) { (name, img) ->
                        val isSel = selectedContacts.contains(name)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSel) Color(0xFF5856D6).copy(alpha = 0.3f) else Color(0xFF222038),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) Color(0xFF7D7AFF) else Color(0xFF2C2A48)
                            ),
                            modifier = Modifier.clickable {
                                selectedContacts = if (isSel) selectedContacts - name else selectedContacts + name
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = img,
                                    contentDescription = name,
                                    modifier = Modifier.size(22.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy Guarantees Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF100E20),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262242)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🔒 Zero Tracking Guarantee", color = Color(0xFF7D7AFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your location is NOT shared while waiting. Location is only transmitted if you fail to respond after reminders escalate.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        onStartCheck(selectedMinutes, selectedDurationText, selectedContacts.toList())
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6))
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Schedule Safety Check ($selectedDurationText)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Rich Card Snippet rendered inside Chat Threads
 */
@Composable
fun SafetyCheckCardSnippet(
    metadata: Map<String, String>,
    onOpenDashboard: () -> Unit,
    onConfirmSafe: () -> Unit,
    onSnooze: () -> Unit
) {
    val state by NightGuardRepository.state.collectAsState()
    val durationText = metadata["duration"] ?: "1 Hour"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1838),
                        Color(0xFF141028)
                    )
                )
            )
            .border(1.dp, Color(0xFF5856D6).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF5856D6).copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🛡️", fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Safety Check Active",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (state.showSafetyCheckPrompt) "⚠️ Check Due NOW!" else "Interval: $durationText",
                        color = if (state.showSafetyCheckPrompt) Color(0xFFFF9500) else Color(0xFF9E9BFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (state.showSafetyCheckPrompt) Color(0xFFFF9500).copy(alpha = 0.2f) else Color(0xFF5856D6).copy(alpha = 0.2f)
            ) {
                Text(
                    text = "${state.safetyCheckRemainingMinutes}m left",
                    color = if (state.showSafetyCheckPrompt) Color(0xFFFF9500) else Color(0xFF9E9BFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onConfirmSafe,
                modifier = Modifier.weight(1.2f).height(36.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32D74B))
            ) {
                Text("✅ I'm Safe", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5856D6)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("🟡 +15m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = onOpenDashboard,
                modifier = Modifier.size(36.dp).background(Color(0xFF282442), RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Manage", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Control Center Dashboard Modal for Safety Check
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCheckDashboardModal(
    onDismiss: () -> Unit
) {
    val state by NightGuardRepository.state.collectAsState()
    var activeTab by remember { mutableStateOf(0) } // 0: Status & 1-Tap, 1: Watchers, 2: Smart Escalation

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F0D1C)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Surface(
                    color = Color(0xFF171428),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = "Safety Check Control",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "Zero-location background verification",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    NightGuardRepository.cancelSafetyCheck()
                                    onDismiss()
                                }
                            ) {
                                Text("Cancel Check", color = Color(0xFFFF3B30), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Navigation Tabs
                        TabRow(
                            selectedTabIndex = activeTab,
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF7D7AFF)
                        ) {
                            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                                Text("⏳ Verification", color = if (activeTab == 0) Color(0xFF7D7AFF) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
                            }
                            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                                Text("👥 Watchers (${state.safetyCheckContacts.size})", color = if (activeTab == 1) Color(0xFF7D7AFF) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
                            }
                            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                                Text("🚨 Escalation Rules", color = if (activeTab == 2) Color(0xFF7D7AFF) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
                            }
                        }
                    }
                }

                // Tab View Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        0 -> VerificationStatusTab(state = state)
                        1 -> WatchersContactsTab(state = state)
                        2 -> EscalationRulesTab(state = state)
                    }
                }

                // Bottom Simulation Controls
                Surface(
                    color = Color(0xFF18152B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2A4A))
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { NightGuardRepository.triggerSafetyCheckPrompt() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9500)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9500))
                            ) {
                                Text("⏰ Trigger Due Prompt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { NightGuardRepository.simulateMissedCheckEscalation() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3B30)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF3B30))
                            ) {
                                Text("🚨 Missed Escalation", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 0: Status & 1-Tap Affirmation
 */
@Composable
fun VerificationStatusTab(state: NightGuardState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Countdown Ring Visual
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF2A2648),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx())
                )
                drawArc(
                    color = if (state.isEscalationActive) Color(0xFFFF3B30) else if (state.showSafetyCheckPrompt) Color(0xFFFF9500) else Color(0xFF5856D6),
                    startAngle = -90f,
                    sweepAngle = if (state.isSafetyCheckActive) (state.safetyCheckRemainingMinutes / 60f) * 360f else 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx())
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛡️", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.safetyCheckRemainingMinutes} MINS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = if (state.isEscalationActive) "ESCALATED!" else if (state.showSafetyCheckPrompt) "CHECK DUE NOW" else "NEXT CHECK-IN",
                    color = if (state.isEscalationActive) Color(0xFFFF3B30) else if (state.showSafetyCheckPrompt) Color(0xFFFF9500) else Color(0xFF9E9BFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Big Primary Action
        Button(
            onClick = { NightGuardRepository.confirmImsafe() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32D74B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("✅ I'm Safe & Fine", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { NightGuardRepository.snoozeSafetyCheck(15) },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5856D6)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("🟡 Snooze +15m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { NightGuardRepository.snoozeSafetyCheck(30) },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5856D6)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("🟡 Snooze +30m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Tab 1: Watchers List
 */
@Composable
fun WatchersContactsTab(state: NightGuardState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Trusted Circle Watchers (${state.safetyCheckContacts.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Contacts receive alert SMS & push notifications ONLY if check-in is missed.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }

        items(state.safetyCheckContacts) { contact ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1B1832),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C284A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, modifier = Modifier.size(36.dp), color = Color(0xFF2E2954)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👤", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(contact, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("🛡️ Standing by for escalations", color = Color(0xFF9E9BFF), fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = { /* Call */ }) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Escalation Rules
 */
@Composable
fun EscalationRulesTab(state: NightGuardState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Smart Escalation Pipeline", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        val steps = listOf(
            "1. Check Timer Expires" to "Sound loud notification prompt on device",
            "2. Grace Period (+3 Mins)" to "Repeat vibration & push notification",
            "3. Missed Check (+5 Mins)" to "SMS alert sent to trusted contacts",
            "4. Critical Timeout (+10 Mins)" to "Live GPS location broadcast triggered"
        )

        steps.forEachIndexed { idx, (title, desc) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1B1832),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C284A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFF5856D6).copy(alpha = 0.3f), modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${idx + 1}", color = Color(0xFF9E9BFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(desc, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Banner Card on Chats Screen
 */
@Composable
fun SafetyCheckHeaderCard(
    onStartClick: () -> Unit,
    onViewClick: () -> Unit
) {
    val state by NightGuardRepository.state.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (state.isSafetyCheckActive) onViewClick() else onStartClick()
            },
        color = if (state.isSafetyCheckActive) Color(0xFF1E1838) else Color(0xFF181628),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (state.isSafetyCheckActive) Color(0xFF5856D6).copy(alpha = 0.6f) else Color(0xFF2E2A48)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF5856D6).copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🛡️", fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    if (state.isSafetyCheckActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Safety Check Active",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF5856D6).copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = if (state.showSafetyCheckPrompt) "⚠️ DUE NOW" else "🟢 Scheduled",
                                    color = if (state.showSafetyCheckPrompt) Color(0xFFFF9500) else Color(0xFF9E9BFF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${state.safetyCheckRemainingMinutes}m until verification • ${state.safetyCheckContacts.size} watchers",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "Safety Check",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Zero-location periodic verification check-in",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (state.isSafetyCheckActive) Color(0xFF5856D6) else Color(0xFF282442)
            ) {
                Text(
                    text = if (state.isSafetyCheckActive) "Manage Check →" else "🛡️ Schedule",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
