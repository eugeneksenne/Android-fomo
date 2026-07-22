package com.example.feature.creatorstudio

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.VenueRepository
import com.example.core.data.EventRepository
import com.example.core.data.creator.CreatorRepository
import com.example.core.data.creator.CreatorStudioState
import com.example.core.data.creator.CreatorOrder
import com.example.core.data.creator.CreatorStaff
import com.example.core.data.creator.CreatorReview
import com.example.core.data.creator.CreatorCampaign
import com.example.core.data.creator.AiAdvisorMessage
import coil.compose.AsyncImage

// We rely on CreatorRepository models now

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorStudioScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val venueState by com.example.core.data.VenueRepository.venueState.collectAsState()
    val fomoEvents by com.example.core.data.EventRepository.eventsState.collectAsState()
    val creatorState by CreatorRepository.state.collectAsState()

    // -------------------------------------------------------------------------
    // CRITICAL CREATOR IDENTITY & ROLE STATES
    // -------------------------------------------------------------------------
    var selectedClaimMethod by remember { mutableStateOf<String?>(null) } // "Google", "Email", "Manual"
    var showVerificationFlow by remember { mutableStateOf(false) }

    // Verification Inputs
    var emailInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var verificationStatus by remember { mutableStateOf("Not Started") } // "InReview", "Approved"
    val documentUploads = remember { mutableStateListOf<String>() }

    // Navigation & Workspace control
    var activeTab by remember { mutableStateOf("home") } // "home", "events", "entry", "promote", "lobby", "flash", "intelligence", "settings"

    // Interactive local states for Creator Studio features
    var liveOccupancy by remember { mutableIntStateOf(142) }
    var maxCapacity by remember { mutableIntStateOf(350) }
    var currentScannerTarget by remember { mutableStateOf("GA_VALID") }
    var scannerLogMessage by remember { mutableStateOf("Ready to scan event pass...") }
    var scannerLogType by remember { mutableStateOf("IDLE") }
    var showEventWizard by remember { mutableStateOf(false) }
    var liveBroadcastIsLive by remember { mutableStateOf(true) }
    var liveBroadcastTitle by remember { mutableStateOf("DJ Maphorisa Live Amapiano Launch") }
    var liveUpdateText by remember { mutableStateOf("") }
    var liveUpdateRole by remember { mutableStateOf("Venue") }
    var wizardStep by remember { mutableIntStateOf(1) }
    var wizTitle by remember { mutableStateOf("") }
    var wizDescription by remember { mutableStateOf("") }
    var wizTimeText by remember { mutableStateOf("21:00 — 04:00") }
    var wizGenresText by remember { mutableStateOf("Amapiano, Afro House") }
    var wizArtistsText by remember { mutableStateOf("Kabza De Small, Uncle Waffles") }
    var wizHasFlashDrop by remember { mutableStateOf(false) }
    var wizFlashDropText by remember { mutableStateOf("") }
    var wizEntryMethod by remember { mutableStateOf("QR Pass") }
    var wizPrice by remember { mutableStateOf("R150") }

    // -------------------------------------------------------------------------
    // TRANSACTIONAL DATABASES (State Holders from Repository)
    // -------------------------------------------------------------------------
    val verifiedRole = creatorState.verifiedRole
    val fomoWalletBalance = creatorState.fomoWalletBalance
    val simOrders = creatorState.orders
    val simStaff = creatorState.staff
    val simReviews = creatorState.reviews
    val simCampaigns = creatorState.campaigns
    val aiAdvisorChatHistory = creatorState.aiChatHistory.map { if (it.isUser) "User: ${it.content}" else "Advisor: ${it.content}" }
    val aiAdvisorIsTyping = creatorState.aiIsTyping
    val enable2FA = creatorState.enable2FA
    val defaultPayoutMethod = creatorState.defaultPayoutMethod

    // Define color codes matching luxury cyberpunk dark theme
    val themeBg = Color(0xFF0B0F19)
    val cardBg = Color(0xFF0F1524)
    val neonCyan = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF2D55)
    val successGreen = Color(0xFF32D74B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (verifiedRole != null && !showVerificationFlow) {
                            Surface(
                                color = when (verifiedRole) {
                                    "Venue" -> neonCyan.copy(alpha = 0.15f)
                                    "Organiser" -> neonPink.copy(alpha = 0.15f)
                                    else -> Color.White.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, when (verifiedRole) {
                                    "Venue" -> neonCyan
                                    "Organiser" -> neonPink
                                    else -> Color.White.copy(alpha = 0.4f)
                                }),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    text = verifiedRole!!.uppercase() + " PRO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (verifiedRole) {
                                        "Venue" -> neonCyan
                                        "Organiser" -> neonPink
                                        else -> Color.White
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = if (showVerificationFlow) "Identity Verification"
                            else if (showEventWizard) "Event Creation Studio"
                            else when (activeTab) {
                                "home" -> "Creator Workspace"
                                "events" -> "Events & Lineups"
                                "entry" -> "Front Door Access"
                                "promote" -> "FOMO Promote Console"
                                "lobby" -> "Club Lobby Twin"
                                "flash" -> "Flash Drops Hub"
                                "intelligence" -> "FOMO Intelligence"
                                "settings" -> "Administrative Settings"
                                else -> "Creator Studio"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showEventWizard) {
                            showEventWizard = false
                        } else if (showVerificationFlow) {
                            showVerificationFlow = false
                        } else if (activeTab != "home") {
                            activeTab = "home"
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeBg),
                actions = {
                    if (verifiedRole != null && !showVerificationFlow) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet", tint = neonCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "R$fomoWalletBalance",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )
        },
        containerColor = themeBg
    ) { paddingValues ->

        // -------------------------------------------------------------------------
        // GATEWAY: ROLE ONBOARDING & TRUST VERIFICATION FLOW
        // -------------------------------------------------------------------------
        if (verifiedRole == null || showVerificationFlow) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(themeBg)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Verify Professional Identity",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Select your verified nightlife profile to unlock high-fidelity Creator Studio analytics, ticketing, and push promotion tools.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Simulated Ownership Intelligence Matching Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = "Intelligence", tint = neonCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "OWNERSHIP INTELLIGENCE",
                                color = neonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "We scanned regional Google Places listings matching your authenticated profile. Tap below to claim ownership instantly.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Surface(
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, successGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Sky Lounge & FOMO Club", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Johannesburg • Verified Match 100%", color = successGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = {
                                        CreatorRepository.verifyRole("Venue")
                                        showVerificationFlow = false
                                        activeTab = "home"
                                        Toast.makeText(context, "🎉 Verified match approved! Claimed FOMO Club.", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = successGreen),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Claim", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 4 Interactive Manual Verification Roles
                Text(
                    "Or Apply manually for a Professional Role:",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Start)
                )

                RoleClaimItem(
                    icon = Icons.Default.Business,
                    title = "Venue Owner & Operator",
                    description = "Claim a physical venue, setup entry capacities, and broadcast active lobbies.",
                    onSelect = {
                        selectedClaimMethod = "Venue"
                        showVerificationFlow = true
                    }
                )

                RoleClaimItem(
                    icon = Icons.Default.ConfirmationNumber,
                    title = "Event Organiser",
                    description = "Verify with your official company email to host massive ticketed events.",
                    onSelect = {
                        selectedClaimMethod = "Organiser"
                        showVerificationFlow = true
                    }
                )

                RoleClaimItem(
                    icon = Icons.Default.Headphones,
                    title = "Professional DJ",
                    description = "Authenticate Spotify for Artists to sync performance schedules.",
                    onSelect = {
                        CreatorRepository.verifyRole("DJ")
                        showVerificationFlow = false
                        activeTab = "home"
                        Toast.makeText(context, "🎧 DJ profile verified via Spotify APIs!", Toast.LENGTH_SHORT).show()
                    }
                )

                RoleClaimItem(
                    icon = Icons.Default.Mic,
                    title = "Music Artist",
                    description = "Sync verified catalog and receive direct booking inquiries.",
                    onSelect = {
                        CreatorRepository.verifyRole("Artist")
                        showVerificationFlow = false
                        activeTab = "home"
                        Toast.makeText(context, "🎤 Artist profile verified successfully!", Toast.LENGTH_SHORT).show()
                    }
                )

                // Interactive Expandable Verification Workflow panel if selected
                if (selectedClaimMethod != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, neonPink.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Verify ${selectedClaimMethod} Identity",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            if (selectedClaimMethod == "Organiser") {
                                Text(
                                    "We verify organisations using custom business domains to prevent fraudulent ticketing.",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Business Email (No @gmail)") },
                                    placeholder = { Text("e.g. events@ultra.co.za") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = neonCyan,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )

                                if (otpSent) {
                                    OutlinedTextField(
                                        value = otpInput,
                                        onValueChange = { otpInput = it },
                                        label = { Text("6-Digit OTP Code") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (!otpSent) {
                                            if (emailInput.contains("@gmail") || emailInput.contains("@yahoo") || !emailInput.contains(".")) {
                                                Toast.makeText(context, "Please use an official custom domain email!", Toast.LENGTH_LONG).show()
                                            } else {
                                                otpSent = true
                                                Toast.makeText(context, "Verification OTP code sent to $emailInput", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            if (otpInput.length >= 4) {
                                                CreatorRepository.verifyRole("Organiser")
                                                showVerificationFlow = false
                                                activeTab = "home"
                                                Toast.makeText(context, "🎉 Organiser verified successfully!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = neonPink),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (otpSent) "Verify OTP & Access" else "Send Domain OTP")
                                }
                            } else {
                                // Venue Owner file uploads
                                Text(
                                    "Please upload your business registration and valid liquor/municipal license to secure this listing.",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            documentUploads.add("CK1_Company_Reg.pdf")
                                            Toast.makeText(context, "Uploaded CK1 Company PDF", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("CK1 Business PDF", fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = {
                                            documentUploads.add("Liquor_License_2026.png")
                                            Toast.makeText(context, "Uploaded Liquor License Image", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Liquor License", fontSize = 10.sp)
                                    }
                                }

                                if (documentUploads.isNotEmpty()) {
                                    Column {
                                        Text("Uploaded Files:", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        documentUploads.forEach { doc ->
                                            Text("✓ $doc", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (documentUploads.isEmpty()) {
                                            Toast.makeText(context, "Please upload at least one verify file", Toast.LENGTH_SHORT).show()
                                        } else {
                                            CreatorRepository.verifyRole("Venue")
                                            showVerificationFlow = false
                                            activeTab = "home"
                                            Toast.makeText(context, "🎉 Documents registered! Venue access approved.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Submit for Review & Claim", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Emergency Bypass / Demo Quick Access Link
                Text(
                    text = "Bypass to Demo Hub",
                    style = MaterialTheme.typography.bodySmall,
                    color = neonCyan,
                    modifier = Modifier
                        .clickable {
                            CreatorRepository.verifyRole("Venue")
                            showVerificationFlow = false
                            activeTab = "home"
                        }
                        .padding(12.dp)
                        .testTag("bypass_verification_cta")
                )
            }
        } else {
            // -------------------------------------------------------------------------
            // VERIFIED CREATOR WORKSPACE INTERFACE
            // -------------------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(themeBg)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    
                    // Responsive Navigation tabs aligning standard M3 layouts
                    ScrollableTabRow(
                        selectedTabIndex = when (activeTab) {
                            "home" -> 0
                            "events" -> 1
                            "entry" -> 2
                            "promote" -> 3
                            "lobby" -> 4
                            "flash" -> 5
                            "intelligence" -> 6
                            "settings" -> 7
                            else -> 0
                        },
                        containerColor = themeBg,
                        contentColor = Color.White,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[
                                    when (activeTab) {
                                        "home" -> 0
                                        "events" -> 1
                                        "entry" -> 2
                                        "promote" -> 3
                                        "lobby" -> 4
                                        "flash" -> 5
                                        "intelligence" -> 6
                                        "settings" -> 7
                                        else -> 0
                                    }
                                ]),
                                color = neonCyan
                            )
                        }
                    ) {
                        Tab(selected = activeTab == "home", onClick = { activeTab = "home" }, text = { Text("🏠 Home Hub", fontSize = 13.sp) })
                        Tab(selected = activeTab == "events", onClick = { activeTab = "events" }, text = { Text("📅 Events Studio", fontSize = 13.sp) })
                        Tab(selected = activeTab == "entry", onClick = { activeTab = "entry" }, text = { Text("🚪 Entry Scanner", fontSize = 13.sp) })
                        Tab(selected = activeTab == "promote", onClick = { activeTab = "promote" }, text = { Text("🚀 Promote Spend", fontSize = 13.sp) })
                        Tab(selected = activeTab == "lobby", onClick = { activeTab = "lobby" }, text = { Text("🔮 Club Lobby", fontSize = 13.sp) })
                        Tab(selected = activeTab == "flash", onClick = { activeTab = "flash" }, text = { Text("⚡ Flash Drops", fontSize = 13.sp) })
                        Tab(selected = activeTab == "intelligence", onClick = { activeTab = "intelligence" }, text = { Text("🧠 AI Brain", fontSize = 13.sp) })
                        Tab(selected = activeTab == "settings", onClick = { activeTab = "settings" }, text = { Text("⚙️ Settings", fontSize = 13.sp) })
                    }

                    // MAIN PANEL ROUTER BASED ON TABS
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        when (activeTab) {
                            "home" -> CreatorHomeTab(
                                balance = fomoWalletBalance,
                                occupancy = liveOccupancy,
                                maxCap = maxCapacity,
                                fomoEventsCount = fomoEvents.size,
                                activeCampaigns = simCampaigns.count { it.status == "Running" },
                                onQuickActionClick = { tab -> activeTab = tab }
                            )

                            "events" -> CreatorEventsTab(
                                events = fomoEvents,
                                onCreateClick = { showEventWizard = true }
                            )

                            "entry" -> CreatorEntryControlTab(
                                currentTarget = currentScannerTarget,
                                onTargetChange = { currentScannerTarget = it },
                                logMessage = scannerLogMessage,
                                logType = scannerLogType,
                                onScanClick = {
                                    when (currentScannerTarget) {
                                        "GA_VALID" -> {
                                            scannerLogMessage = "✅ ENTRY APPROVED: General Admission Ticket\nHolder: Thabo Molefe | Verified Paid"
                                            scannerLogType = "SUCCESS"
                                            liveOccupancy = (liveOccupancy + 1).coerceAtMost(maxCapacity)
                                        }
                                        "VIP_VALID" -> {
                                            scannerLogMessage = "✅ VIP APPROVED: Rooftop Glasshouse Access\nHolder: Lerato Ndlovu | Fast-lane Ticket"
                                            scannerLogType = "SUCCESS"
                                            liveOccupancy = (liveOccupancy + 1).coerceAtMost(maxCapacity)
                                        }
                                        "ALREADY_SCANNED" -> {
                                            scannerLogMessage = "❌ DUPLICATE QR DETECTED\nTicket ORD-4822 already scanned at 20:15 by Gate Marcus."
                                            scannerLogType = "ERROR"
                                        }
                                        "INVALID_TOKEN" -> {
                                            scannerLogMessage = "❌ INVALID QR SECURITY TOKEN\nCryptographic signature mismatch. Possible duplicate/fake pass."
                                            scannerLogType = "ERROR"
                                        }
                                        "REFUNDED" -> {
                                            scannerLogMessage = "❌ FRAUD ALERT: REFUNDED TICKET\nOrder ORD-4823 was fully refunded to buyer Zanele Khumalo."
                                            scannerLogType = "ERROR"
                                        }
                                    }
                                },
                                occupancy = liveOccupancy,
                                maxCap = maxCapacity,
                                orders = simOrders,
                                onAddDoorSale = { qty, price ->
                                    val cost = qty * price
                                    if (fomoWalletBalance >= cost) {
                                        liveOccupancy = (liveOccupancy + qty).coerceAtMost(maxCapacity)
                                        CreatorRepository.addDoorOrder(price.toString())
                                        Toast.makeText(context, "✓ Checked in $qty walk-in guests. Ticket revenue added.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Insufficient Wallet Balance for door simulator preloads", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            "promote" -> CreatorPromoteTab(
                                balance = fomoWalletBalance,
                                campaigns = simCampaigns,
                                onBoostClick = { cost ->
                                    if (fomoWalletBalance >= cost) {
                                        CreatorRepository.deductWalletAndAddCampaign(
                                            cost = cost,
                                            campaign = CreatorCampaign("cmp_${System.currentTimeMillis()}", "Dynamic Promo Boost", "R$cost", "R$cost", 4500, 210, 18, "Running")
                                        )
                                        Toast.makeText(context, "🚀 Nightlife Promo Boost Activated live! Wallet updated.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Insufficient funds! Please top up your Creator wallet.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            "lobby" -> CreatorClubLobbyTab(
                                broadcastIsLive = liveBroadcastIsLive,
                                onBroadcastChange = { liveBroadcastIsLive = it },
                                broadcastTitle = liveBroadcastTitle,
                                onTitleChange = { liveBroadcastTitle = it },
                                liveUpdateText = liveUpdateText,
                                onUpdateTextChange = { liveUpdateText = it },
                                liveUpdateRole = liveUpdateRole,
                                onRoleChange = { liveUpdateRole = it },
                                staffList = simStaff,
                                onPushUpdate = {
                                    if (liveUpdateText.isBlank()) {
                                        Toast.makeText(context, "Please enter update text!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        VenueRepository.addLiveUpdate(
                                            author = if (liveUpdateRole == "Venue") venueState.name else "Resident Guest DJ",
                                            role = liveUpdateRole,
                                            content = liveUpdateText
                                        )
                                        liveUpdateText = ""
                                        Toast.makeText(context, "Live update broadcasted to public Club Lobby!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onIssueNotice = { title, desc, type ->
                                    VenueRepository.addNotice(type, title, desc)
                                    Toast.makeText(context, "⚠️ High-priority operational notice pinned to lobby!", Toast.LENGTH_SHORT).show()
                                }
                            )

                            "flash" -> CreatorFlashDropsTab(
                                activeDrops = venueState.flashDrops,
                                onCreateDrop = { title, sub, mins, stock ->
                                    VenueRepository.addFlashDrop(title, sub, mins, stock)
                                    Toast.makeText(context, "⚡ Flash Drop active inside city radius!", Toast.LENGTH_SHORT).show()
                                }
                            )

                            "intelligence" -> CreatorIntelligenceTab(
                                simReviews = simReviews,
                                chatHistory = aiAdvisorChatHistory,
                                isTyping = aiAdvisorIsTyping,
                                onQuestionAsk = { question ->
                                    CreatorRepository.addAiMessage(question, true)
                                    CreatorRepository.setAiTyping(true)
                                    
                                    val answer = when {
                                        question.contains("sales") || question.contains("ticket") -> {
                                            "Ticket sales velocity for Amapiano Winter Edition is 28% higher than typical Friday Bashes. Suggest closing Early Bird tiers immediately and allocating 15% more capacity to the VIP Lounge Deck."
                                        }
                                        question.contains("flash") || question.contains("drop") -> {
                                            "Ladies Night Cocktail Drop generates the highest conversion rate (82% arrivals). To increase early check-ins, schedule a ⚡ Free Welcome Drink Flash Drop precisely 45 minutes before doors open."
                                        }
                                        question.contains("dj") || question.contains("crowd") -> {
                                            "DJ Kent's sunset sessions attract peak arrivals between 18:30 and 19:30, with a 94% retention rate into midnight. Leverage performance credits to boost this set in a 10km radius."
                                        }
                                        else -> {
                                            "Based on live metrics, your JHB Vibe Health Index is excellent at 92/100. Recommend enabling a 2-hour Dynamic Ticket discount for standard ticket holder check-ins."
                                        }
                                    }
                                    
                                    CreatorRepository.setAiTyping(false)
                                    CreatorRepository.addAiMessage(answer, false)
                                }
                            )

                            "settings" -> CreatorSettingsTab(
                                staffList = simStaff,
                                enable2FA = enable2FA,
                                on2FAToggle = { CreatorRepository.toggle2FA(it) },
                                payoutMethod = defaultPayoutMethod,
                                onPayoutChange = { CreatorRepository.updatePayoutMethod(it) }
                            )
                        }
                    }
                }

                // -------------------------------------------------------------------------
                // FULL-FIDELITY EVENT CREATION WIZARD (STAGED POPUP)
                // -------------------------------------------------------------------------
                if (showEventWizard) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(themeBg),
                        color = themeBg
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Progress steps indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Step $wizardStep of 4",
                                    color = neonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (wizardStep) {
                                        1 -> "Basic Concept"
                                        2 -> "Venue & Schedule"
                                        3 -> "Lineup & Talents"
                                        4 -> "Ticketing & Publish"
                                        else -> ""
                                    },
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Dynamic visual progress bar
                            LinearProgressIndicator(
                                progress = { wizardStep / 4f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = neonCyan,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            when (wizardStep) {
                                1 -> {
                                    Text("Event Basics", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    OutlinedTextField(
                                        value = wizTitle,
                                        onValueChange = { wizTitle = it },
                                        label = { Text("Event Name") },
                                        placeholder = { Text("e.g. Amapiano Summit: JHB Takeover") },
                                        modifier = Modifier.fillMaxWidth().testTag("wiz_title_input"),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    OutlinedTextField(
                                        value = wizDescription,
                                        onValueChange = { wizDescription = it },
                                        label = { Text("Description") },
                                        minLines = 3,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }

                                2 -> {
                                    Text("Venue & Schedule", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    OutlinedTextField(
                                        value = wizTimeText,
                                        onValueChange = { wizTimeText = it },
                                        label = { Text("Operating Hours") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    OutlinedTextField(
                                        value = wizGenresText,
                                        onValueChange = { wizGenresText = it },
                                        label = { Text("Nightlife Music Genres") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }

                                3 -> {
                                    Text("Artist Lineups", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    OutlinedTextField(
                                        value = wizArtistsText,
                                        onValueChange = { wizArtistsText = it },
                                        label = { Text("Confirmed Headliners (comma separated)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = wizHasFlashDrop, onCheckedChange = { wizHasFlashDrop = it })
                                        Text("Include Real-time Flash Drop Reward", color = Color.White, fontSize = 14.sp)
                                    }

                                    if (wizHasFlashDrop) {
                                        OutlinedTextField(
                                            value = wizFlashDropText,
                                            onValueChange = { wizFlashDropText = it },
                                            label = { Text("Flash Drop description") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )
                                    }
                                }

                                4 -> {
                                    Text("Entry Options & Monetisation", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { wizEntryMethod = "Online & Door" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (wizEntryMethod == "Online & Door") neonCyan else Color(0xFF1E293B)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Online + Door", color = if (wizEntryMethod == "Online & Door") Color.Black else Color.White, fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = { wizEntryMethod = "Door Only" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (wizEntryMethod == "Door Only") neonCyan else Color(0xFF1E293B)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Door Sales Only", color = if (wizEntryMethod == "Door Only") Color.Black else Color.White, fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = { wizEntryMethod = "Free RSVP" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (wizEntryMethod == "Free RSVP") neonCyan else Color(0xFF1E293B)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Free RSVP", color = if (wizEntryMethod == "Free RSVP") Color.Black else Color.White, fontSize = 10.sp)
                                        }
                                    }

                                    if (wizEntryMethod != "Free RSVP") {
                                        OutlinedTextField(
                                            value = wizPrice,
                                            onValueChange = { wizPrice = it },
                                            label = { Text("Standard Pass Ticket Price") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )
                                    }

                                    // Display Smart Pricing Recommendation
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(0.5.dp, neonCyan.copy(alpha = 0.5f))
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Recommend, contentDescription = "Recommended", tint = neonCyan)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Pricing Recommendation: Standard Amapiano summits inside Johannesburg average R150 - R200 for optimum conversion velocity.",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Wizard action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (wizardStep > 1) {
                                    OutlinedButton(
                                        onClick = { wizardStep-- },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                    ) {
                                        Text("Previous")
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Button(
                                    onClick = {
                                        if (wizardStep < 4) {
                                            if (wizardStep == 1 && wizTitle.isBlank()) {
                                                Toast.makeText(context, "Please enter an Event Name!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                wizardStep++
                                            }
                                        } else {
                                            // Finish creation & Inject live into EventRepository
                                            EventRepository.addNewEvent(
                                                title = wizTitle,
                                                venueId = "fomo_club",
                                                venueName = venueState.name,
                                                genres = wizGenresText.split(",").map { it.trim() },
                                                artists = wizArtistsText.split(",").map { it.trim() },
                                                ticketPrice = wizPrice,
                                                timeText = wizTimeText,
                                                description = wizDescription,
                                                hasFlashDrop = wizHasFlashDrop,
                                                flashDropText = if (wizHasFlashDrop) wizFlashDropText else null
                                            )
                                            showEventWizard = false
                                            wizardStep = 1
                                            activeTab = "events"
                                            Toast.makeText(context, "🎉 Event published live on FOMO Network!", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                    modifier = Modifier.testTag("wiz_next_button")
                                ) {
                                    Text(
                                        text = if (wizardStep == 4) "Publish Event Live" else "Continue",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENTS & WIDGET CARDS FOR VERIFICATION
// =============================================================================

@Composable
fun RoleClaimItem(icon: ImageVector, title: String, description: String, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        color = Color(0xFF0F1524),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF00E5FF))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
        }
    }
}

// =============================================================================
// TAB VISUALS
// =============================================================================

@Composable
fun CreatorHomeTab(
    balance: Int,
    occupancy: Int,
    maxCap: Int,
    fomoEventsCount: Int,
    activeCampaigns: Int,
    onQuickActionClick: (String) -> Unit
) {
    val neonCyan = Color(0xFF00E5FF)
    val successGreen = Color(0xFF32D74B)

    // AI Daily briefing panel
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = "Intelligence", tint = neonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI DAILY BRIEFING", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text(
                text = "Welcome back, Eugene. Sunday VIP Special tickets are selling 28% faster than previous weeks. Launch a R19 Quick Flash Drop to incentivize walk-in arrivals before 9 PM tonight.",
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }

    // Grid stats using simple Rows
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatsCompactCard(
                title = "Vibe Occupancy",
                value = "$occupancy / $maxCap",
                subText = "${(occupancy * 100) / maxCap}% Capacity Live",
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )
            StatsCompactCard(
                title = "FOMO Wallet",
                value = "R$balance",
                subText = "Ready for Ads Boost",
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatsCompactCard(
                title = "Total Events",
                value = "$fomoEventsCount",
                subText = "Live FOMO Schedules",
                icon = Icons.Default.Event,
                modifier = Modifier.weight(1f)
            )
            StatsCompactCard(
                title = "Active Boosts",
                value = "$activeCampaigns",
                subText = "Promote campaigns live",
                icon = Icons.Default.Campaign,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Interactive Quick Actions Deck
    Text("Quick Operations Shortcuts", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onQuickActionClick("events") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1524)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(Icons.Default.AddBox, contentDescription = null, tint = neonCyan)
                Spacer(modifier = Modifier.height(4.dp))
                Text("New Event", fontSize = 11.sp, color = Color.White)
            }
        }

        Button(
            onClick = { onQuickActionClick("entry") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1524)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = neonCyan)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Open Scanner", fontSize = 11.sp, color = Color.White)
            }
        }

        Button(
            onClick = { onQuickActionClick("flash") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1524)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = neonCyan)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Flash Drop", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun StatsCompactCard(title: String, value: String, subText: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0F1524),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(subText, color = Color(0xFF32D74B), fontSize = 11.sp)
        }
    }
}

@Composable
fun CreatorEventsTab(
    events: List<com.example.core.data.Event>,
    onCreateClick: () -> Unit
) {
    val neonCyan = Color(0xFF00E5FF)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Events & Lineups", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
            modifier = Modifier.testTag("create_event_wizard_trigger")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Create Event", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events) { evt ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = evt.posterUrl,
                        contentDescription = "Poster",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(evt.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("📍 ${evt.venueName} • ${evt.timeText}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = evt.ticketPrice,
                                    color = neonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (evt.hasFlashDrop) {
                                Surface(
                                    color = Color(0xFFFF2D55).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "⚡ Flash Drop",
                                        color = Color(0xFFFF2D55),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
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
fun CreatorEntryControlTab(
    currentTarget: String,
    onTargetChange: (String) -> Unit,
    logMessage: String?,
    logType: String,
    onScanClick: () -> Unit,
    occupancy: Int,
    maxCap: Int,
    orders: List<CreatorOrder>,
    onAddDoorSale: (Int, Int) -> Unit
) {
    val neonCyan = Color(0xFF00E5FF)
    val successGreen = Color(0xFF32D74B)
    val neonPink = Color(0xFFFF2D55)

    Text("Front Door Scanner Simulator", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

    // Simulated Viewfinder
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(2.dp, if (logType == "SUCCESS") successGreen else if (logType == "ERROR") neonPink else neonCyan, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = neonCyan, modifier = Modifier.size(48.dp))
            Text("Simulated Camera Viewfinder Active", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
            ) {
                Text("Simulate Scan QR", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Log messages
    if (logMessage != null) {
        Surface(
            color = if (logType == "SUCCESS") successGreen.copy(alpha = 0.15f) else neonPink.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, if (logType == "SUCCESS") successGreen else neonPink),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = logMessage,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    // Interactive target switch
    Column {
        Text("Scanner Target Selector (Demo Mock):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onTargetChange("GA_VALID") },
                colors = ButtonDefaults.buttonColors(containerColor = if (currentTarget == "GA_VALID") neonCyan else Color(0xFF1E293B))
            ) { Text("GA Ticket", color = if (currentTarget == "GA_VALID") Color.Black else Color.White) }
            Button(
                onClick = { onTargetChange("VIP_VALID") },
                colors = ButtonDefaults.buttonColors(containerColor = if (currentTarget == "VIP_VALID") neonCyan else Color(0xFF1E293B))
            ) { Text("VIP Ticket", color = if (currentTarget == "VIP_VALID") Color.Black else Color.White) }
            Button(
                onClick = { onTargetChange("ALREADY_SCANNED") },
                colors = ButtonDefaults.buttonColors(containerColor = if (currentTarget == "ALREADY_SCANNED") neonPink else Color(0xFF1E293B))
            ) { Text("Duplicate QR", color = Color.White) }
            Button(
                onClick = { onTargetChange("INVALID_TOKEN") },
                colors = ButtonDefaults.buttonColors(containerColor = if (currentTarget == "INVALID_TOKEN") neonPink else Color(0xFF1E293B))
            ) { Text("Invalid QR", color = Color.White) }
        }
    }

    // Door walk-in sales
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Front-Gate Walk-in Sales", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Text("Simulate cash or external POS purchases at the entrance queue.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onAddDoorSale(1, 150) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+1 Guest (R150)", fontSize = 11.sp, color = Color.White)
                }
                Button(
                    onClick = { onAddDoorSale(2, 150) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+2 Guests (R300)", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CreatorPromoteTab(
    balance: Int,
    campaigns: List<CreatorCampaign>,
    onBoostClick: (Int) -> Unit
) {
    val neonCyan = Color(0xFF00E5FF)
    val successGreen = Color(0xFF32D74B)

    Text("Promote Your Vibe", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

    // Promote Pricing Table Options
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PREMIUM SPONSOR PACKS", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            PromotePackItem(
                title = "Promote Flash Drop (City Wide)",
                price = "R39",
                reachText = "Est. 12,000 localized feeds tonight",
                onBuy = { onBoostClick(39) }
            )
            PromotePackItem(
                title = "Promote Event (Province Reach)",
                price = "R199",
                reachText = "Est. 55,000 premium feeds & push alerts",
                onBuy = { onBoostClick(199) }
            )
            PromotePackItem(
                title = "Featured Discover Banner (7 Days)",
                price = "R299",
                reachText = "Locked premium spot in discover feeds",
                onBuy = { onBoostClick(299) }
            )
        }
    }

    // Active campaigns progress table
    Text("Active Ads Performance Tracker", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
    campaigns.forEach { cmp ->
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = successGreen.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(cmp.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text("Budget: ${cmp.budget} | Spent: ${cmp.spent}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${cmp.reach} Reach", color = neonCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("${cmp.conversions} Conversions", color = successGreen, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PromotePackItem(title: String, price: String, reachText: String, onBuy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(reachText, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Button(
            onClick = onBuy,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(price, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun CreatorClubLobbyTab(
    broadcastIsLive: Boolean,
    onBroadcastChange: (Boolean) -> Unit,
    broadcastTitle: String,
    onTitleChange: (String) -> Unit,
    liveUpdateText: String,
    onUpdateTextChange: (String) -> Unit,
    liveUpdateRole: String,
    onRoleChange: (String) -> Unit,
    staffList: List<CreatorStaff>,
    onPushUpdate: () -> Unit,
    onIssueNotice: (String, String, String) -> Unit
) {
    val neonCyan = Color(0xFF00E5FF)
    val successGreen = Color(0xFF32D74B)

    var noticeTitle by remember { mutableStateOf("") }
    var noticeContent by remember { mutableStateOf("") }
    var noticeType by remember { mutableStateOf("Alert") }

    Text("Lobby Broadcast Controls", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Simulate Live Broadcaster", color = Color.White, fontSize = 14.sp)
            Text("Fans see live streams directly inside Discover Map", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Switch(checked = broadcastIsLive, onCheckedChange = onBroadcastChange)
    }

    if (broadcastIsLive) {
        OutlinedTextField(
            value = broadcastTitle,
            onValueChange = onTitleChange,
            label = { Text("Livestream Title") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
    }

    // 24h Update publisher
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Push 24h Live Update", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = liveUpdateText,
                onValueChange = onUpdateTextChange,
                placeholder = { Text("e.g. DJ Maphorisa just took the stage, main floor is absolutely packed!") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Post As:", color = Color.White, fontSize = 12.sp)
                RadioButton(selected = liveUpdateRole == "Venue", onClick = { onRoleChange("Venue") })
                Text("Venue", color = Color.White, fontSize = 12.sp)
                RadioButton(selected = liveUpdateRole == "DJ", onClick = { onRoleChange("DJ") })
                Text("DJ Guest", color = Color.White, fontSize = 12.sp)
            }

            Button(
                onClick = onPushUpdate,
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Broadcast Live Update", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Operational Notice board
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Issue Critical Notice", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = noticeTitle,
                onValueChange = { noticeTitle = it },
                label = { Text("Notice Title (e.g. VIP Bookings Sold Out)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = noticeContent,
                onValueChange = { noticeContent = it },
                label = { Text("Details (e.g. Standard entry queue is moving fast)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Button(
                onClick = {
                    if (noticeTitle.isNotBlank()) {
                        onIssueNotice(noticeTitle, noticeContent, noticeType)
                        noticeTitle = ""
                        noticeContent = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Issue Warning Bulletin", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CreatorFlashDropsTab(
    activeDrops: List<com.example.core.data.FlashDrop>,
    onCreateDrop: (String, String, Int, Int) -> Unit
) {
    val neonCyan = Color(0xFF00E5FF)

    var dropTitle by remember { mutableStateOf("") }
    var dropSubtitle by remember { mutableStateOf("") }
    var dropStock by remember { mutableStateOf("50") }

    Text("Active Flash Drops Tracker", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

    activeDrops.forEach { drop ->
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFF2D55).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF2D55))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(drop.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(drop.subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text("Expires in ${drop.expiresMinutes}m | Remaining Stock: ${drop.currentStock}/${drop.initialStock}", color = neonCyan, fontSize = 11.sp)
                }
            }
        }
    }

    // Launch New Flash Drop form
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("LAUNCH TIME-SENSITIVE REWARD", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = dropTitle,
                onValueChange = { dropTitle = it },
                label = { Text("Reward Title (e.g. Ladies Night Cocktail Voucher)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = dropSubtitle,
                onValueChange = { dropSubtitle = it },
                label = { Text("Terms (e.g. Free for first 50 guests inside)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = dropStock,
                onValueChange = { dropStock = it },
                label = { Text("Initial Stock") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Button(
                onClick = {
                    if (dropTitle.isNotBlank()) {
                        onCreateDrop(dropTitle, dropSubtitle, 45, dropStock.toIntOrNull() ?: 50)
                        dropTitle = ""
                        dropSubtitle = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pulse Reward to City Map", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CreatorIntelligenceTab(
    simReviews: List<CreatorReview>,
    chatHistory: List<String>,
    isTyping: Boolean,
    onQuestionAsk: (String) -> Unit
) {
    val neonCyan = Color(0xFF00E5FF)
    val successGreen = Color(0xFF32D74B)

    Text("AI Performance Intelligence", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

    // JHB Vibe Health Rating gauge using Canvas
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx())
                    )
                    drawArc(
                        color = successGreen,
                        startAngle = 135f,
                        sweepAngle = 270f * 0.92f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
                Text("92%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("JHB VIBE HEALTH INDEX", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Elite Nightlife Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Top 5% converting venues in Rosebank hub this month.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }

    // Conversational Business Advisor
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🧠 AI CO-PILOT CHAT", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            // Conversation history container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chatHistory.forEach { msg ->
                        val isUser = msg.startsWith("User:")
                        Text(
                            text = if (isUser) msg.substring(5) else msg,
                            color = if (isUser) neonCyan else Color.White,
                            textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 12.sp,
                            fontWeight = if (isUser) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (isTyping) {
                        Text("Advisor typing suggestion...", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    }
                }
            }

            // Simple question chip presets
            Text("Ask Business Questions:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onQuestionAsk("Why did ticket sales slow down?") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Analyze Sales Velocity", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { onQuestionAsk("When is the best time to drop a Flash Drop?") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Optimize Flash Drops", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { onQuestionAsk("Which DJ attracts the biggest crowd?") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Query Headliner retention", fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }

    // Guest Review management
    Text("Latest Guest Reviews & Feedback", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
    simReviews.forEach { rev ->
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(rev.author, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                        Text(" ${rev.rating}", color = Color.White, fontSize = 12.sp)
                    }
                }
                Text(rev.content, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                
                if (rev.reply != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "My Reply: ${rev.reply}",
                            color = neonCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorSettingsTab(
    staffList: List<CreatorStaff>,
    enable2FA: Boolean,
    on2FAToggle: (Boolean) -> Unit,
    payoutMethod: String,
    onPayoutChange: (String) -> Unit
) {
    val neonCyan = Color(0xFF00E5FF)
    val successGreen = Color(0xFF32D74B)

    Text("Creator Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

    // Security & Payout
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("ADMIN SECURITY & BILLING", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Two-Factor Authentication (2FA)", color = Color.White, fontSize = 13.sp)
                    Text("Secure payouts with custom SMS codes", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
                Switch(checked = enable2FA, onCheckedChange = on2FAToggle)
            }

            OutlinedTextField(
                value = payoutMethod,
                onValueChange = onPayoutChange,
                label = { Text("Default Ticket Payout Bank Account") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }
    }

    // Role Permission Matrix list
    Text("Team Permissions List", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
    staffList.forEach { staff ->
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (staff.isOnline) successGreen else Color.Gray)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(staff.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(staff.role, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                Text(
                    text = "✓ Active",
                    color = neonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
