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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import com.example.core.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansWorkspaceScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToNightGuard: () -> Unit = {},
    onNavigateToMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val tonightState by TonightRepository.state.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("⚡ Active Night OS", "📩 Requests & Invites", "🤖 AI & Templates", "💰 Shared Wallet", "📜 Public Plans & Memories")

    var isAddExpenseDialogOpen by remember { mutableStateOf(false) }
    var isAddStopDialogOpen by remember { mutableStateOf(false) }
    var aiQueryText by remember { mutableStateOf("") }

    val activePlan = tonightState.plans.find { it.id == tonightState.currentSelectedPlanId } ?: tonightState.plans.firstOrNull()

    val themeBg = Color(0xFF090D16)
    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val warmAmber = Color(0xFFFFB703)
    val activeGreen = Color(0xFF00E676)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = themeBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Plans Engine", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                        Text("Night Operating System • Collaborative Command Center", fontSize = 11.sp, color = neonCyan)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("plans_workspace_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Surface(
                        color = activeGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, activeGreen)
                    ) {
                        Text(
                            "${tonightState.plans.size} MOVES LIVE",
                            color = activeGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeBg)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TAB ROW
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = cardBg,
                contentColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = neonCyan
                        )
                    }
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

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTabIndex) {
                    0 -> ActiveNightOsTab(
                        activePlan = activePlan,
                        tonightState = tonightState,
                        onSelectPlan = { TonightRepository.selectPlan(it) },
                        onAddStopClick = { isAddStopDialogOpen = true },
                        onNavigateToNightGuard = onNavigateToNightGuard,
                        onNavigateToMap = onNavigateToMap
                    )
                    1 -> RequestsAndInvitesTab(tonightState)
                    2 -> AiAndTemplatesTab(
                        tonightState = tonightState,
                        aiQueryText = aiQueryText,
                        onAiQueryChange = { aiQueryText = it },
                        onGenerateAi = {
                            TonightRepository.generateAiPlan(aiQueryText)
                            Toast.makeText(context, "🤖 AI Generated Itinerary added to your Plans!", Toast.LENGTH_SHORT).show()
                            selectedTabIndex = 0
                        }
                    )
                    3 -> SharedWalletTab(activePlan, tonightState) { isAddExpenseDialogOpen = true }
                    4 -> PublicPlansAndMemoriesTab(tonightState)
                }
            }
        }
    }

    if (isAddExpenseDialogOpen && activePlan != null) {
        AddExpenseDialog(
            planId = activePlan.id,
            onDismiss = { isAddExpenseDialogOpen = false },
            onAddExpense = { title, amount, paidBy ->
                TonightRepository.addExpense(activePlan.id, title, amount, paidBy)
                Toast.makeText(context, "💸 Expense '$title' added & split with crew!", Toast.LENGTH_SHORT).show()
                isAddExpenseDialogOpen = false
            }
        )
    }

    if (isAddStopDialogOpen && activePlan != null) {
        AddStopDialog(
            onDismiss = { isAddStopDialogOpen = false },
            onAddStop = { name, area, time ->
                TonightRepository.addStopToPlan(activePlan.id, name, area, time)
                Toast.makeText(context, "📍 '$name' added to ${activePlan.title}!", Toast.LENGTH_SHORT).show()
                isAddStopDialogOpen = false
            }
        )
    }
}

// -------------------------------------------------------------------------
// TAB 1: ACTIVE NIGHT OPERATING SYSTEM DASHBOARD
// -------------------------------------------------------------------------
@Composable
fun ActiveNightOsTab(
    activePlan: NightPlan?,
    tonightState: TonightState,
    onSelectPlan: (String) -> Unit,
    onAddStopClick: () -> Unit,
    onNavigateToNightGuard: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    val context = LocalContext.current
    var chatInput by remember { mutableStateOf("") }

    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val warmAmber = Color(0xFFFFB703)
    val activeGreen = Color(0xFF00E676)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PLAN SWITCHER CAROUSEL
        item {
            Text("SWITCH ACTIVE PLAN WORKSPACE", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tonightState.plans) { plan ->
                    val isSel = plan.id == activePlan?.id
                    Surface(
                        color = if (isSel) Color(0xFF1E1438) else cardBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isSel) neonCyan else Color.White.copy(0.1f)),
                        modifier = Modifier.clickable { onSelectPlan(plan.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (plan.type == PlanType.GROUP) "👥" else if (plan.type == PlanType.DUO) "🕯️" else "👤", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(plan.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${plan.stops.size} Stops • ${plan.members.size} Members", color = Color.White.copy(0.6f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        if (activePlan != null) {
            // ACTIVE PLAN HEADER CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(accentPurple, neonCyan)))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(activePlan.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Text("Code: ${activePlan.inviteCode} • Budget: R${activePlan.budgetRands.toInt()} pp", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Surface(
                                color = activeGreen.copy(0.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("OPERATING", color = activeGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // METRICS QUICK ROW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QuickPill("📅 Calendar", if (activePlan.isCalendarSynced) "Synced" else "Sync Off", activeGreen) { TonightRepository.toggleCalendarSync(activePlan.id) }
                            QuickPill("🎟️ Table", activePlan.reservationStatus.split(" ").first(), warmAmber) {}
                            QuickPill("🚘 Ride", "Bolt XL", neonCyan) { Toast.makeText(context, "🚘 Rideshare tracked in real-time!", Toast.LENGTH_SHORT).show() }
                            QuickPill("🛡️ Safety", "NightGuard", accentPurple) { onNavigateToNightGuard() }
                        }
                    }
                }
            }

            // SHARED TIMELINE / STOPS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SHARED ITINERARY TIMELINE", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onAddStopClick,
                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("+ Add Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            itemsIndexed(activePlan.stops) { index, stop ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (stop.status == "Active") Color(0xFF191330) else cardBg),
                    border = BorderStroke(1.dp, if (stop.status == "Active") neonCyan else Color.White.copy(0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (stop.status == "Active") neonCyan else Color.White.copy(0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", color = if (stop.status == "Active") Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(stop.venueName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${stop.area} • ${stop.time}", color = Color.White.copy(0.6f), fontSize = 11.sp)
                        }

                        Surface(
                            color = when (stop.status) {
                                "Completed" -> activeGreen.copy(0.2f)
                                "Active" -> neonCyan.copy(0.2f)
                                else -> Color.White.copy(0.08f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stop.status.uppercase(), color = if (stop.status == "Active") neonCyan else Color.White.copy(0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            // CREW PRESENCE & RADAR ROW
            item {
                Text("CREW PRESENCE & RADAR", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(activePlan.members) { member ->
                        Surface(
                            color = cardBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = member.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(member.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(member.etaText, color = activeGreen, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // GROUP VOTE SECTION
            val vote = activePlan.activeVote
            if (vote != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161C2C)),
                        border = BorderStroke(1.dp, warmAmber)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🗳️ LIVE GROUP POLL", color = warmAmber, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(vote.question, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            Spacer(modifier = Modifier.height(10.dp))

                            vote.options.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { TonightRepository.castVote(activePlan.id, option.id) },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${if (option.votedByUser) "✓ " else ""}${option.venueName} (${option.area})", color = Color.White, fontSize = 13.sp)
                                    Text("${option.votes} votes", color = neonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // LIVE WORKSPACE CHAT
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, Color.White.copy(0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💬 PLAN CHAT WORKSPACE", color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activePlan.chatMessages.takeLast(4).forEach { msg ->
                                Text("${msg.senderName}: ${msg.text}", color = if (msg.isSystem) neonCyan else Color.White, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("Chat with plan crew...", color = Color.White.copy(0.4f), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    TonightRepository.addPlanMessage(activePlan.id, chatInput)
                                    chatInput = ""
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = neonCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 2: REQUESTS & INVITATIONS
// -------------------------------------------------------------------------
@Composable
fun RequestsAndInvitesTab(state: TonightState) {
    val context = LocalContext.current
    val cardBg = Color(0xFF0F1626)
    val neonCyan = Color(0xFF00E5FF)
    val activeGreen = Color(0xFF00E676)
    val accentPurple = Color(0xFF9D4EDD)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("INCOMING DUO & GROUP PLAN REQUESTS", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        items(state.planRequests) { req ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.White.copy(0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = req.hostAvatar,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("${req.hostName} invited you to:", color = Color.White.copy(0.7f), fontSize = 12.sp)
                            Text(req.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${req.dateText} • ${req.budgetText}", color = neonCyan, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Stops: ${req.venuesSummary}", color = Color.White.copy(0.8f), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    if (req.status == "PENDING") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    TonightRepository.respondToPlanRequest(req.id, true)
                                    Toast.makeText(context, "✅ Plan Accepted & Synced to Device Calendar!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = activeGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Accept + Calendar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    TonightRepository.respondToPlanRequest(req.id, false)
                                    Toast.makeText(context, "Declined invitation", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Decline", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    } else {
                        Surface(
                            color = activeGreen.copy(0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("STATUS: ${req.status}", color = activeGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 3: AI CONCIERGE & TEMPLATES GALLERY
// -------------------------------------------------------------------------
@Composable
fun AiAndTemplatesTab(
    tonightState: TonightState,
    aiQueryText: String,
    onAiQueryChange: (String) -> Unit,
    onGenerateAi: () -> Unit
) {
    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val warmAmber = Color(0xFFFFB703)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1430)),
                border = BorderStroke(1.dp, warmAmber)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("🤖 AI NIGHTLIFE CONCIERGE", color = warmAmber, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text("Type what you want to experience and AI will build a complete, time-optimized itinerary.", color = Color.White.copy(0.7f), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = aiQueryText,
                        onValueChange = onAiQueryChange,
                        placeholder = { Text("e.g. Luxury dinner then dancing until sunrise in Sandton", color = Color.White.copy(0.4f), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onGenerateAi,
                        colors = ButtonDefaults.buttonColors(containerColor = warmAmber),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Generate AI Plan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("PLANNING TEMPLATES", color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        items(tonightState.templates) { tmpl ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tmpl.emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tmpl.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(tmpl.category, color = neonCyan, fontSize = 11.sp)
                            }
                        }
                        Text(tmpl.estBudget, color = warmAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tmpl.description, color = Color.White.copy(0.7f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { TonightRepository.createPlanFromTemplate(tmpl.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use Template", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 4: SHARED EXPENSE WALLET
// -------------------------------------------------------------------------
@Composable
fun SharedWalletTab(activePlan: NightPlan?, state: TonightState, onAddExpenseClick: () -> Unit) {
    val cardBg = Color(0xFF0F1626)
    val neonCyan = Color(0xFF00E5FF)
    val activeGreen = Color(0xFF00E676)
    val accentPurple = Color(0xFF9D4EDD)

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
                border = BorderStroke(1.dp, Color.White.copy(0.12f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("SHARED EXPENSE SPLITTER & WALLET", color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val totalSpent = activePlan?.expenses?.sumOf { it.amountRands } ?: 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Night Expenses", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("R${totalSpent.toInt()}", color = activeGreen, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onAddExpenseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Add Expense to Split", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("ITEMIZED PLAN EXPENSES", color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        val expenses = activePlan?.expenses ?: emptyList()
        items(expenses) { exp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.White.copy(0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(exp.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Paid by ${exp.paidBy} • Split ${exp.splitCount} ways", color = Color.White.copy(0.6f), fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("R${exp.amountRands.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("R${exp.perPersonRands.toInt()} pp", color = neonCyan, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 5: PUBLIC PLANS & MEMORIES
// -------------------------------------------------------------------------
@Composable
fun PublicPlansAndMemoriesTab(state: TonightState) {
    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("PUBLIC COMMUNITY PLANS & CRAWLS", color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        items(state.publicPlans) { pub ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.White.copy(0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(pub.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("By ${pub.creatorName} • ${pub.savesCount} Saves", color = neonCyan, fontSize = 11.sp)
                    Text("Stops: ${pub.stopsList.joinToString(" ➔ ")}", color = Color.White.copy(0.7f), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { TonightRepository.remixPublicPlan(pub.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Remix & Duplicate Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text("COMPLETED NIGHT MEMORIES", color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        items(state.memoryRecaps) { mem ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2B)),
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(mem.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${mem.dateText} • ${mem.friendsCount} Friends • R${mem.totalExpenseRands.toInt()} Total", color = neonCyan, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(mem.recapSummary, color = Color.White.copy(0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// QUICK HELPER PILL
// -------------------------------------------------------------------------
@Composable
fun QuickPill(label: String, valText: String, color: Color, onClick: () -> Unit) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color),
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(valText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// DIALOGS
@Composable
fun AddExpenseDialog(planId: String, onDismiss: () -> Unit, onAddExpense: (title: String, amount: Double, paidBy: String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf("You") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1626),
        title = { Text("Add Expense to Split", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title (e.g. Dinner, Drinks)", color = Color.White.copy(0.6f)) })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount in Rands (e.g. 1200)", color = Color.White.copy(0.6f)) })
                OutlinedTextField(value = paidBy, onValueChange = { paidBy = it }, label = { Text("Paid By", color = Color.White.copy(0.6f)) })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) onAddExpense(title, amt, paidBy)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD))
            ) {
                Text("Add Expense")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(0.6f)) } }
    )
}

@Composable
fun AddStopDialog(onDismiss: () -> Unit, onAddStop: (name: String, area: String, time: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("Sandton") }
    var time by remember { mutableStateOf("11:30 PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1626),
        title = { Text("Add Stop to Itinerary", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Venue / Spot Name", color = Color.White.copy(0.6f)) })
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area / District", color = Color.White.copy(0.6f)) })
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Target Time", color = Color.White.copy(0.6f)) })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onAddStop(name, area, time)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD))
            ) {
                Text("Add Stop")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(0.6f)) } }
    )
}
