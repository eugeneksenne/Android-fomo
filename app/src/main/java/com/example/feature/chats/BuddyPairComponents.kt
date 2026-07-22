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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.data.BuddyMember
import com.example.core.data.NightGuardRepository
import com.example.core.data.NightGuardState

/**
 * Screen / Modal 2: Pair Invitation Engine
 * Allows starting a temporary consent-driven event Buddy Pair.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddyPairInviteDialog(
    onDismiss: () -> Unit,
    onSendInvite: (sessionName: String, durationText: String, durationMinutes: Int, message: String) -> Unit
) {
    var sessionName by remember { mutableStateOf("Festival Night Crew") }
    var selectedDurationText by remember { mutableStateOf("Until Event Ends") }
    var durationMinutes by remember { mutableStateOf(240) }
    var customMessage by remember { mutableStateOf("Let's stay together tonight.") }
    var searchQuery by remember { mutableStateOf("") }

    val durationOptions = listOf(
        "30 min" to 30,
        "1 hour" to 60,
        "2 hours" to 120,
        "Until Event Ends" to 240,
        "Until I Leave Venue" to 180
    )

    val trustedFriends = listOf(
        "Sarah Jenkins" to "https://i.pravatar.cc/150?img=32",
        "James Walker" to "https://i.pravatar.cc/150?img=12",
        "Peter Vance" to "https://i.pravatar.cc/150?img=11",
        "Kgomotso Dlamini" to "https://i.pravatar.cc/150?img=49",
        "Aria Chen" to "https://i.pravatar.cc/150?img=25"
    )

    var selectedFriends by remember { mutableStateOf(setOf("Sarah Jenkins", "James Walker")) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF16161E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E3A))
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
                        Text("🤝", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Start Buddy Pair",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Temporary & event-based location safety",
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

                // Session Name
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Session Name", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF333342)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Select Friends
                Text(
                    text = "Select Trusted Circle Friends:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(trustedFriends) { (name, img) ->
                        val isSelected = selectedFriends.contains(name)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF22222E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF333342)
                            ),
                            modifier = Modifier.clickable {
                                selectedFriends = if (isSelected) selectedFriends - name else selectedFriends + name
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
                                    text = name.split(" ").first(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Duration Selector
                Text(
                    text = "Session Duration (Auto-Expires):",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(durationOptions) { (label, mins) ->
                        val isSel = selectedDurationText == label
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF22222E),
                            modifier = Modifier.clickable {
                                selectedDurationText = label
                                durationMinutes = mins
                            }
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Note
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("Note / Message", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF333342)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = {
                        onSendInvite(sessionName, selectedDurationText, durationMinutes, customMessage)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Buddy Pair Invitation",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * Interactive Rich Message Card in Chat Thread
 */
@Composable
fun BuddyPairCardSnippet(
    metadata: Map<String, String>,
    onOpenDashboard: () -> Unit,
    onMeetupClick: () -> Unit,
    onFindFriendClick: () -> Unit
) {
    val state by NightGuardRepository.state.collectAsState()
    val sessionName = metadata["session"] ?: state.buddySessionName
    val durationText = metadata["duration"] ?: state.buddyDurationText
    val note = metadata["note"] ?: state.buddyInviteMessage

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1A38),
                        Color(0xFF12121C)
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Top Header Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🤝", fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = sessionName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "FOMO Buddy Pair • $durationText",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (state.isBuddyActive) Color(0xFF32D74B).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (state.isBuddyActive) Color(0xFF32D74B) else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.isBuddyActive) "Active" else "Ended",
                        color = if (state.isBuddyActive) Color(0xFF32D74B) else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (note.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\"$note\"",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Buddies Avatars Row with Distance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.buddies.take(3).forEach { buddy ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box {
                            AsyncImage(
                                model = buddy.imageUrl,
                                contentDescription = buddy.name,
                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF32D74B))
                                    .border(1.dp, Color.Black, CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = buddy.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buddy.distanceText,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Primary Action
        Button(
            onClick = onOpenDashboard,
            modifier = Modifier.fillMaxWidth().height(38.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Map, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Live Buddy Dashboard", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Sub Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onMeetupClick,
                modifier = Modifier.weight(1f).height(32.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A4A))
            ) {
                Text("🎯 Meet-Up", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onFindFriendClick,
                modifier = Modifier.weight(1f).height(32.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A4A))
            ) {
                Text("🔍 Find Friend", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Screen 5: Comprehensive Session Dashboard Modal Sheet
 * Full Control Center for FOMO Buddy Pair
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddyPairDashboardModal(
    onDismiss: () -> Unit
) {
    val state by NightGuardRepository.state.collectAsState()
    var activeTab by remember { mutableStateOf(0) } // 0: Live Map, 1: Meet-Up & Lost Friend, 2: Timeline & Alerts, 3: Controls & Privacy
    var showMeetupDialog by remember { mutableStateOf(false) }
    var selectedLostFriendId by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0D0D14)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar Control Header
                Surface(
                    color = Color(0xFF161622),
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = state.buddySessionName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF32D74B).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "🟢 Active",
                                                color = Color(0xFF32D74B),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Time Remaining: ${state.buddyRemainingMinutes / 60}h ${state.buddyRemainingMinutes % 60}m • ${state.buddies.size} Buddies",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { NightGuardRepository.extendSession(30) }) {
                                    Icon(Icons.Default.MoreTime, contentDescription = "Extend", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { NightGuardRepository.endBuddyPair() }) {
                                    Icon(Icons.Default.PowerSettingsNew, contentDescription = "End", tint = Color(0xFFFF453A))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Navigation Tabs
                        ScrollableTabRow(
                            selectedTabIndex = activeTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            edgePadding = 0.dp
                        ) {
                            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                                Text("🗺️ Live Map", color = if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                                Text("🎯 Meet-Up & Lost", color = if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                                Text("📜 Timeline", color = if (activeTab == 2) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                                Text("🔒 Privacy & Controls", color = if (activeTab == 3) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }

                // Tab Content Body
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        0 -> LiveMapTabSection(
                            state = state,
                            onSelectBuddyForLost = { selectedLostFriendId = it; activeTab = 1 },
                            onMeetupClick = { showMeetupDialog = true }
                        )
                        1 -> MeetupAndLostFriendTabSection(
                            state = state,
                            selectedLostFriendId = selectedLostFriendId,
                            onSelectLostFriend = { selectedLostFriendId = it },
                            onCreateMeetup = { showMeetupDialog = true }
                        )
                        2 -> TimelineTabSection(state = state)
                        3 -> PrivacyAndControlsTabSection(state = state)
                    }
                }

                // Bottom Emergency SOS Quick Action Bar
                Surface(
                    color = Color(0xFF1B0B0E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5E171E))
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFFF3B30))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Night Guard Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("1-Tap Emergency SOS & Walk Me Home", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                        }

                        Button(
                            onClick = { /* Trigger SOS */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🚨 SOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (showMeetupDialog) {
            MeetupPointCreationDialog(
                onDismiss = { showMeetupDialog = false },
                onCreate = { title ->
                    NightGuardRepository.setMeetupPoint(title)
                    showMeetupDialog = false
                }
            )
        }
    }
}

/**
 * Tab 0: Live Map & Presence List
 */
@Composable
fun LiveMapTabSection(
    state: NightGuardState,
    onSelectBuddyForLost: (String) -> Unit,
    onMeetupClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Live Radar Map Viewport Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .background(Color(0xFF101018))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Concentric distance radar rings
                drawCircle(color = Color(0xFF2A2A3A), radius = 80.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
                drawCircle(color = Color(0xFF2A2A3A), radius = 150.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
                drawCircle(color = Color(0xFF2A2A3A), radius = 220.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))

                // Center User Pin
                drawCircle(color = Color(0xFF007AFF).copy(alpha = 0.3f), radius = 18.dp.toPx(), center = center)
                drawCircle(color = Color(0xFF007AFF), radius = 8.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
            }

            // Overlay Buddy Pins
            state.buddies.forEachIndexed { index, buddy ->
                val offsetX = when (index) {
                    0 -> (-50).dp
                    1 -> 70.dp
                    2 -> (-110).dp
                    else -> 120.dp
                }
                val offsetY = when (index) {
                    0 -> (-40).dp
                    1 -> (-80).dp
                    2 -> 60.dp
                    else -> 100.dp
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = offsetX, y = offsetY)
                        .clickable { onSelectBuddyForLost(buddy.id) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(38.dp)
                        ) {
                            AsyncImage(
                                model = buddy.imageUrl,
                                contentDescription = buddy.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "${buddy.name} • ${buddy.distanceText}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Meetup Point Pin Overlay
            state.meetupPoint?.let { mp ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 10.dp, y = (-20).dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFF9500),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎯", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(mp.title, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Quick Map Action Bar Overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = onMeetupClick,
                    containerColor = Color(0xFFFF9500),
                    contentColor = Color.Black,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = "Set Meetup", modifier = Modifier.size(20.dp))
                }
            }
        }

        // Live Presence Buddies List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF12121A))
                .padding(12.dp)
        ) {
            Text(
                text = "Live Members Presence (${state.buddies.size})",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.buddies) { buddy ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1B1B26),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    AsyncImage(
                                        model = buddy.imageUrl,
                                        contentDescription = buddy.name,
                                        modifier = Modifier.size(40.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF32D74B))
                                            .border(1.dp, Color.Black, CircleShape)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(buddy.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = buddy.status,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${buddy.distanceText} • ${buddy.etaText}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🔋 ${buddy.batteryPercent}%", color = if (buddy.batteryPercent < 20) Color(0xFFFF453A) else Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { onSelectBuddyForLost(buddy.id) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Text("Navigate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Meet-Up Engine & Lost Friend Engine
 */
@Composable
fun MeetupAndLostFriendTabSection(
    state: NightGuardState,
    selectedLostFriendId: String?,
    onSelectLostFriend: (String?) -> Unit,
    onCreateMeetup: () -> Unit
) {
    var selectedFriend = remember(selectedLostFriendId, state.buddies) {
        state.buddies.firstOrNull { it.id == selectedLostFriendId } ?: state.buddies.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Meet-Up Engine Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF181824),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Meet-Up Rendezvous Point", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Single tap to alert everyone where to gather", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.meetupPoint != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF9500).copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("📍 ${state.meetupPoint.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Distance: ${state.meetupPoint.distanceText} • ETA: ${state.meetupPoint.etaMinutes} min", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    Text("Rendezvous Timer: ${state.meetupPoint.countdownMinutes} mins left", color = Color(0xFFFF9500), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { NightGuardRepository.removeMeetupPoint() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Clear", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = onCreateMeetup,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500))
                        ) {
                            Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set 'Meet Here' Point", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Lost Friend Engine Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF181824),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔍", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Lost Friend Finder Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Instant compass, walking directions & live distance", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Friend to Locate:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.buddies) { buddy ->
                            val isSel = buddy.id == selectedFriend?.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF282836),
                                modifier = Modifier.clickable { onSelectLostFriend(buddy.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = buddy.imageUrl,
                                        contentDescription = buddy.name,
                                        modifier = Modifier.size(24.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = buddy.name,
                                        color = if (isSel) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    selectedFriend?.let { friend ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Locating ${friend.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Status: ${friend.status} • Battery: ${friend.batteryPercent}%", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                        }
                                    }
                                    Text("🎯 ${friend.distanceText}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { /* Open full navigation overlay */ },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Walking Directions (${friend.etaText})", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Session Timeline & History
 */
@Composable
fun TimelineTabSection(state: NightGuardState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Session Activity Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Encrypted chronological session history (deleted upon session expiry)", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(state.timeline) { item ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF161622),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.iconEmoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(item.timestamp, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Privacy & Granular Session Controls
 */
@Composable
fun PrivacyAndControlsTabSection(state: NightGuardState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Privacy & Live Sharing Controls", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Customize what your buddies can see during this session", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF181824),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Pause Live Location", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Temporarily hide exact GPS until resumed", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = state.isSharingPaused,
                            onCheckedChange = { NightGuardRepository.togglePauseSharing() }
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF181824),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Share Exact Location", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = state.shareExactLocation,
                            onCheckedChange = { NightGuardRepository.togglePrivacy(exactLoc = it) }
                        )
                    }
                    HorizontalDivider(color = Color(0xFF282836), modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Share Battery Level", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = state.shareBattery,
                            onCheckedChange = { NightGuardRepository.togglePrivacy(battery = it) }
                        )
                    }
                    HorizontalDivider(color = Color(0xFF282836), modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Share Estimated Arrival (ETA)", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = state.shareEta,
                            onCheckedChange = { NightGuardRepository.togglePrivacy(eta = it) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { NightGuardRepository.endBuddyPair() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("End Buddy Pair Session", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Dialog to set a Rendezvous Meet-Up Point
 */
@Composable
fun MeetupPointCreationDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String) -> Unit
) {
    var meetupTitle by remember { mutableStateOf("Main Stage Bar") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1C1C28),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Set Meet-Up Point", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Notify all buddies where to meet up right now.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = meetupTitle,
                    onValueChange = { meetupTitle = it },
                    label = { Text("Meet-Up Location Name", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9500),
                        unfocusedBorderColor = Color(0xFF333342)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onCreate(meetupTitle) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500))
                    ) {
                        Text("Set Rendezvous Point", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Top Header Card for Chats Screen
 */
@Composable
fun BuddyPairHeaderCard(
    onPairClick: () -> Unit,
    onViewSessionClick: () -> Unit
) {
    val state by NightGuardRepository.state.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (state.isBuddyActive) onViewSessionClick() else onPairClick()
            },
        color = if (state.isBuddyActive) Color(0xFF1E1A38) else Color(0xFF1A1A24),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (state.isBuddyActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color(0xFF2E2E3E)
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
                    color = if (state.isBuddyActive) Color(0xFF32D74B).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🤝", fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    if (state.isBuddyActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.buddySessionName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF32D74B).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "🟢 Active",
                                    color = Color(0xFF32D74B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${state.buddyRemainingMinutes / 60}h ${state.buddyRemainingMinutes % 60}m remaining • ${state.buddies.size} buddies",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "FOMO Buddy Pair",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Temporary event safety with trusted circle",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (state.isBuddyActive) MaterialTheme.colorScheme.primary else Color(0xFF282838)
            ) {
                Text(
                    text = if (state.isBuddyActive) "View Session →" else "＋ Start Pair",
                    color = if (state.isBuddyActive) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

