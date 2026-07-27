package com.example.feature.discover

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
    BackHandler { onDismiss() }
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
                                    items(items = tonightState.plans, key = { it.id }) { p ->
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

                            items(items = tonightState.suggestions, key = { it.id }) { sug ->
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
                                    items(items = activePlan.chatMessages, key = { it.id }) { msg ->
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
