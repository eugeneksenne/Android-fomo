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
import com.example.core.data.NightGuardRepository
import com.example.core.data.NightGuardState

/**
 * Screen 2 & 5: Walk Me Home Journey Setup Modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkMeHomeInviteDialog(
    onDismiss: () -> Unit,
    onStartJourney: (destination: String, mode: String, companions: List<String>) -> Unit
) {
    var selectedDestinationLabel by remember { mutableStateOf("🏠 Home (124 Rosebank)") }
    var customDestinationText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("🚶 Walking") }
    var selectedCompanions by remember { mutableStateOf(setOf("Sarah", "James")) }

    val destinationPresets = listOf(
        "🏠 Home (124 Rosebank)",
        "🏨 Michelangelo Hotel",
        "🅿 Sandton Parking B",
        "📍 Custom Address"
    )

    val modeOptions = listOf(
        "🚶 Walking" to 18,
        "🚕 Ride-Share" to 8,
        "🚗 Driving" to 6,
        "🚴 Cycling" to 10,
        "🚌 Public Transport" to 15
    )

    val companionList = listOf(
        "Sarah" to "https://i.pravatar.cc/150?img=32",
        "James" to "https://i.pravatar.cc/150?img=12",
        "Peter" to "https://i.pravatar.cc/150?img=11",
        "Kgomotso" to "https://i.pravatar.cc/150?img=49"
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
            color = Color(0xFF141420),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E42))
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
                        Text("🚶", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Start Walk Me Home",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Never walk or travel alone after dark",
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

                // Select Destination
                Text(
                    text = "Select Destination:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    destinationPresets.forEach { preset ->
                        val isSel = selectedDestinationLabel == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF1F1F2C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF2C2C3C)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDestinationLabel = preset }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(preset, color = Color.White, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                if (selectedDestinationLabel == "📍 Custom Address") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customDestinationText,
                        onValueChange = { customDestinationText = it },
                        label = { Text("Enter Destination Address", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF333342)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Travel Mode
                Text(
                    text = "Mode of Travel:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(modeOptions) { (mode, eta) ->
                        val isSel = selectedMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF1F1F2C),
                            modifier = Modifier.clickable { selectedMode = mode }
                        ) {
                            Text(
                                text = "$mode ($eta min)",
                                color = if (isSel) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select Watcher Companions
                Text(
                    text = "Live Companion Watchers:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(companionList) { (name, img) ->
                        val isSel = selectedCompanions.contains(name)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF1F1F2C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF2C2C3C)
                            ),
                            modifier = Modifier.clickable {
                                selectedCompanions = if (isSel) selectedCompanions - name else selectedCompanions + name
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = img,
                                    contentDescription = name,
                                    modifier = Modifier.size(20.dp).clip(CircleShape),
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

                Spacer(modifier = Modifier.height(20.dp))

                // Start Journey Button
                Button(
                    onClick = {
                        val dest = if (selectedDestinationLabel == "📍 Custom Address") customDestinationText.ifBlank { "Custom Destination" } else selectedDestinationLabel
                        val cleanMode = selectedMode.split(" ").lastOrNull() ?: "Walking"
                        onStartJourney(dest, cleanMode, selectedCompanions.toList())
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Walk Me Home Journey",
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
 * Rich Card Snippet rendered inside Chat Threads
 */
@Composable
fun WalkMeHomeCardSnippet(
    metadata: Map<String, String>,
    onOpenDashboard: () -> Unit,
    onSendSupportReaction: (String) -> Unit
) {
    val state by NightGuardRepository.state.collectAsState()
    val destination = metadata["destination"] ?: state.journeyDestination
    val mode = metadata["mode"] ?: state.journeyMode

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF14241C),
                        Color(0xFF0F1813)
                    )
                )
            )
            .border(1.dp, Color(0xFF32D74B).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
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
                    color = Color(0xFF32D74B).copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🚶", fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Walk Me Home Journey",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Heading to $destination • $mode",
                        color = Color(0xFF32D74B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF32D74B).copy(alpha = 0.2f)
            ) {
                Text(
                    text = "${state.journeyEtaMinutes}m left",
                    color = Color(0xFF32D74B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar & Distance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Progress", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text("${state.journeyRemainingDistanceKm} km left", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (1.0f - (state.journeyRemainingDistanceKm / 2.0).toFloat()).coerceIn(0.1f, 1.0f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF32D74B),
            trackColor = Color(0xFF22382B)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Companion Messages / Quick support reactions
        if (state.companionMessages.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💬 Companion: \"${state.companionMessages.last()}\"",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Primary Button
        Button(
            onClick = onOpenDashboard,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32D74B))
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Watch Live Progress Dashboard", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Companion React Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { onSendSupportReaction("👍 Still with you") },
                modifier = Modifier.weight(1f).height(30.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A4232)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("👍 With You", fontSize = 10.sp)
            }

            OutlinedButton(
                onClick = { onSendSupportReaction("❤️ Almost there") },
                modifier = Modifier.weight(1f).height(30.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A4232)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("❤️ Almost There", fontSize = 10.sp)
            }
        }
    }
}

/**
 * Screen 6: Live Control Center Dashboard Modal for Walk Me Home
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkMeHomeDashboardModal(
    onDismiss: () -> Unit
) {
    val state by NightGuardRepository.state.collectAsState()
    var activeTab by remember { mutableStateOf(0) } // 0: Live Map & Path, 1: Watchers Room, 2: Timeline, 3: Safety Settings
    var showArrivalSummary by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A120E)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = Color(0xFF101C16),
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
                                            text = "Journey to ${state.journeyDestination}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF32D74B).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (state.routeDeviationDetected) "⚠️ Deviation" else "🟢 Protected",
                                                color = if (state.routeDeviationDetected) Color(0xFFFF9500) else Color(0xFF32D74B),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${state.journeyEtaMinutes} mins left • ${state.journeyRemainingDistanceKm} km • ${state.journeyMode}",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    NightGuardRepository.endJourney()
                                    showArrivalSummary = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32D74B)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Arrived Safely", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Navigation Tabs
                        ScrollableTabRow(
                            selectedTabIndex = activeTab,
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF32D74B),
                            edgePadding = 0.dp
                        ) {
                            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                                Text("🗺️ Live Path", color = if (activeTab == 0) Color(0xFF32D74B) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                                Text("👁️ Watchers (${state.companionsWatching.size})", color = if (activeTab == 1) Color(0xFF32D74B) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                                Text("📜 Timeline", color = if (activeTab == 2) Color(0xFF32D74B) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                                Text("🚨 Emergency & Risk", color = if (activeTab == 3) Color(0xFF32D74B) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        0 -> LivePathMapTab(state = state)
                        1 -> WatchersRoomTab(state = state)
                        2 -> JourneyTimelineTab(state = state)
                        3 -> EmergencyRiskTab(state = state)
                    }
                }

                // Interactive Bottom Simulation & SOS Quick Control
                Surface(
                    color = Color(0xFF122018),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223E2E))
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
                                onClick = { NightGuardRepository.simulateJourneyProgress() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF32D74B))
                            ) {
                                Text("🚶 Walk Step", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { NightGuardRepository.triggerRouteDeviation() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9500)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9500))
                            ) {
                                Text("⚠️ Route Deviation", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { /* Emergency SOS */ },
                                modifier = Modifier.height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("🚨 SOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        if (showArrivalSummary) {
            WalkMeHomeArrivalSummaryDialog(
                destination = state.journeyDestination,
                onDismiss = {
                    showArrivalSummary = false
                    onDismiss()
                }
            )
        }
    }
}

/**
 * Tab 0: Live Glowing Path Map Viewport
 */
@Composable
fun LivePathMapTab(state: NightGuardState) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08100C))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val start = Offset(size.width * 0.2f, size.height * 0.75f)
            val end = Offset(size.width * 0.8f, size.height * 0.25f)
            val control1 = Offset(size.width * 0.35f, size.height * 0.5f)
            val control2 = Offset(size.width * 0.65f, size.height * 0.4f)

            // Draw glowing path
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(start.x, start.y)
                cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
            }

            drawPath(
                path = path,
                color = Color(0xFF32D74B).copy(alpha = 0.3f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16.dp.toPx())
            )
            drawPath(
                path = path,
                color = Color(0xFF32D74B),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )

            // Draw start & destination nodes
            drawCircle(color = Color.White, radius = 8.dp.toPx(), center = start)
            drawCircle(color = Color(0xFFFF3B30), radius = 10.dp.toPx(), center = end)
        }

        // Animated user position marker along path
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-20).dp, y = 30.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF32D74B),
                shadowElevation = 8.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🚶", fontSize = 18.sp)
                }
            }
        }

        // Overlay Destination Card at Top
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF101F18).copy(alpha = 0.9f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223E30)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Destination: ${state.journeyDestination}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Safe Path Confidence: 99% • Standard Lighting", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF32D74B).copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🛡️", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Watchers Room & Companions
 */
@Composable
fun WatchersRoomTab(state: NightGuardState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Companions Watching Live (${state.companionsWatching.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("These trusted friends receive instant alerts if you stop moving or deviate.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }

        items(state.companionsWatching) { companionName ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF14221A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223A2C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, modifier = Modifier.size(36.dp), color = Color(0xFF22382B)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👤", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(companionName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("🟢 Watching live progress", color = Color(0xFF32D74B), fontSize = 11.sp)
                        }
                    }

                    Row {
                        IconButton(onClick = { /* Call */ }) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Companion Reaction Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(state.companionMessages) { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1A1A26),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

/**
 * Tab 2: Private Journey Timeline
 */
@Composable
fun JourneyTimelineTab(state: NightGuardState) {
    val sampleEvents = listOf(
        "22:03" to "Journey Started towards ${state.journeyDestination}",
        "22:06" to "Exited venue gate successfully",
        "22:09" to "Crossed 5th Avenue (Safe Lighting)",
        "22:12" to "Battery level check: 84%",
        "22:16" to "Nearing final block"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Private Journey Audit Log", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Self-clears automatically when journey completes.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }

        items(sampleEvents) { (time, title) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF32D74B).copy(alpha = 0.2f)
                ) {
                    Text(time, color = Color(0xFF32D74B), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Tab 3: Emergency & Safety Settings
 */
@Composable
fun EmergencyRiskTab(state: NightGuardState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Safety Intelligence & Protection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1C1416),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5E2228)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("🚨 Instant SOS Escalation", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Broadcasts your live coordinates, 10-second audio stream, and alerts emergency services.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { /* SOS */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Trigger Emergency SOS", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF142018),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("🛡️ Smart Features Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("✓ Automatic Route Deviation Alarm", color = Color(0xFF32D74B), fontSize = 12.sp)
                Text("✓ Low Battery Alerting to Watchers", color = Color(0xFF32D74B), fontSize = 12.sp)
                Text("✓ Inactivity Detection (> 3 mins)", color = Color(0xFF32D74B), fontSize = 12.sp)
            }
        }
    }
}

/**
 * Arrival Celebration Summary Modal
 */
@Composable
fun WalkMeHomeArrivalSummaryDialog(
    destination: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF122018),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF32D74B)),
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Arrived Safely!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Destination: $destination", color = Color(0xFF32D74B), fontSize = 13.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅ All trusted companions notified of safe arrival.", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🔒 Live location tracking auto-terminated and session cleared.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32D74B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close & Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Header Card on Chats Screen
 */
@Composable
fun WalkMeHomeHeaderCard(
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
                if (state.isJourneyActive) onViewClick() else onStartClick()
            },
        color = if (state.isJourneyActive) Color(0xFF14241C) else Color(0xFF161C18),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (state.isJourneyActive) Color(0xFF32D74B).copy(alpha = 0.6f) else Color(0xFF243028)
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
                    color = Color(0xFF32D74B).copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🚶", fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    if (state.isJourneyActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Journey: ${state.journeyDestination}",
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
                            text = "${state.journeyEtaMinutes}m left • ${state.journeyRemainingDistanceKm} km • ${state.companionsWatching.size} watchers",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "Walk Me Home",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Live companion monitoring on night journeys",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (state.isJourneyActive) Color(0xFF32D74B) else Color(0xFF223629)
            ) {
                Text(
                    text = if (state.isJourneyActive) "View Journey →" else "🚶 Start",
                    color = if (state.isJourneyActive) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
