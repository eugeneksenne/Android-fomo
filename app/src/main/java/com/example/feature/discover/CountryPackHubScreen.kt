package com.example.feature.discover

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPackHubScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val packState by CountryPackRepository.state.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("📍 Onboarding Pipeline", "🌐 Creation Platform", "📦 SQLite Packs", "🏷️ Map Filters", "🏛️ Venue Registry")

    var isAddVenueModalOpen by remember { mutableStateOf(false) }

    val themeBg = Color(0xFF090D16)
    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val warmAmber = Color(0xFFFFB703)
    val activeGreen = Color(0xFF00E676)
    val alertRed = Color(0xFFFF1744)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = themeBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Country Pack Intelligence", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                        Text("${packState.currentCountry} • ${packState.sqliteDatabaseSizeMb} MB Local Engine", fontSize = 11.sp, color = neonCyan)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("country_pack_back")) {
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
                            "LIVE PACKS v18",
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
            // TAB SELECTOR
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

            // TAB CONTENTS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTabIndex) {
                    0 -> OnboardingPipelineTab(packState)
                    1 -> CreationPlatformTab(packState)
                    2 -> SqlitePacksTab(packState)
                    3 -> MapFiltersTab(packState)
                    4 -> VenueRegistryTab(packState) { isAddVenueModalOpen = true }
                }
            }
        }
    }

    if (isAddVenueModalOpen) {
        AddPermanentVenueDialog(
            onDismiss = { isAddVenueModalOpen = false },
            onAddVenue = { name, cat, packType, district, address, phone ->
                CountryPackRepository.createVenueRecord(name, cat, packType, district, address, phone)
                Toast.makeText(context, "✅ Venue '$name' permanently created with FOMO ID!", Toast.LENGTH_SHORT).show()
                isAddVenueModalOpen = false
            }
        )
    }
}

// -------------------------------------------------------------------------
// TAB 1: ONBOARDING & INSTALLATION PIPELINE
// -------------------------------------------------------------------------
@Composable
fun OnboardingPipelineTab(state: CountryPackState) {
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
        item {
            // LOCATION DETECTION BANNER
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📍", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Country Detection Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(state.detectedLocation, color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Surface(
                            color = activeGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("VERIFIED", color = activeGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "FOMO uses GPS with IP fallback to immediately auto-detect the user's country and fetch the corresponding Country Pack Manifest.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Text("INSTALLATION SEQUENCE (ONBOARDING)", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        // STEP 1: CORE & NIGHTLIFE PACK (IMMEDIATE)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
                border = BorderStroke(1.dp, activeGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = activeGreen,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("1", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stage 1 — Foreground Immediate Install", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Core Pack + NIGHTLIFE Pack installed during onboarding. Guarantees instant social density and immediate nightlife discovery.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }

                    Surface(
                        color = activeGreen.copy(0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("COMPLETE", color = activeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
        }

        // STEP 2: REALTIME MAP ACTIVATION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1433)),
                border = BorderStroke(1.dp, accentPurple.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = accentPurple,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("2", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stage 2 — Realtime & Social Activation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Activates glowing nightlife pins, WebSocket crowd streams, DJ live indicators, and NightGuard protection engine.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }

                    Surface(
                        color = neonCyan.copy(0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ACTIVE", color = neonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
        }

        // STEP 3: BACKGROUND PERMANENT PACK QUEUE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, warmAmber.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = warmAmber,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("3", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stage 3 — Background Pack Installation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Progressively installs FOOD, PREP, WELLNESS, and TRAVEL packs in background queue to optimize battery and bandwidth.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }

                    Surface(
                        color = warmAmber.copy(0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SYNCING", color = warmAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
        }

        // STEP 4: PROGRESSIVE DISTRICT EXPANSION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Stage 4 — Progressive District Expansion", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Expands geographic detail dynamically per district (Sandton -> Rosebank -> Soweto) to eliminate storage bloat.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Sandton Central", "Rosebank Node", "Soweto Orlando").forEach { dist ->
                            val isExp = state.expandedDistricts.contains(dist.split(" ").first())
                            Surface(
                                color = if (isExp) accentPurple else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable { CountryPackRepository.expandDistrictLayer(dist.split(" ").first()) }
                            ) {
                                Text(
                                    "${if (isExp) "✓ " else "+ "}$dist",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 2: GLOBAL PACK CREATION PLATFORM
// -------------------------------------------------------------------------
@Composable
fun CreationPlatformTab(state: CountryPackState) {
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
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("REGIONAL CELL ENGINE & COVERAGE", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("National Coverage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${state.totalCoveragePercent}%", color = activeGreen, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (state.totalCoveragePercent / 100.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = activeGreen,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }

        item {
            Text("ACTIVE REGIONAL CELLS SCANNING PIPELINE", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        items(state.regionalCells) { cell ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${cell.district} (${cell.cellId})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${cell.city}, ${cell.province} • Postal: ${cell.postalCode}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }

                        Surface(
                            color = when (cell.scanStatus) {
                                CellScanStatus.COMPLETE -> activeGreen.copy(0.2f)
                                CellScanStatus.MONITORING -> neonCyan.copy(0.2f)
                                else -> warmAmber.copy(0.2f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                cell.scanStatus.name,
                                color = when (cell.scanStatus) {
                                    CellScanStatus.COMPLETE -> activeGreen
                                    CellScanStatus.MONITORING -> neonCyan
                                    else -> warmAmber
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Venues: ${cell.venueCount} | Last Scan: ${cell.lastScanDate}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)

                        if (cell.scanStatus != CellScanStatus.COMPLETE) {
                            Button(
                                onClick = { CountryPackRepository.triggerCellScan(cell.cellId) },
                                colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Trigger Full Scan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 3: SQLITE PACKS & DELTA PATCH ENGINE
// -------------------------------------------------------------------------
@Composable
fun SqlitePacksTab(state: CountryPackState) {
    val context = LocalContext.current
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
        // DELTA UPDATE BANNER IF AVAILABLE
        val patch = state.activeDeltaPatch
        if (patch != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1528)),
                    border = BorderStroke(1.dp, warmAmber)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ DELTA PATCH UPDATE AVAILABLE", color = warmAmber, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${patch.packType.displayName} (v${patch.fromVersion} ➔ v${patch.toVersion})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(patch.releaseNotes, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                CountryPackRepository.applyDeltaUpdate(patch.packType)
                                Toast.makeText(context, "⚡ Delta patch applied! SQLite database updated.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = warmAmber),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Apply Lightweight Delta Patch (${patch.patchSizeBytes / 1000} KB)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("PERMANENT SQLITE COUNTRY PACKS", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        items(state.countryPacks) { pack ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(pack.packType.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pack.packType.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("v${pack.version}", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${pack.sizeMb} MB • ${pack.venueCount} Venues Stored", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        Text(pack.packType.description, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (pack.status == PackInstallStatus.INSTALLED) {
                        Surface(
                            color = activeGreen.copy(0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("INSTALLED", color = activeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    } else {
                        Button(
                            onClick = { CountryPackRepository.installPack(pack.packType) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Install", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 4: MAP FILTERS & CATEGORY VISIBILITY
// -------------------------------------------------------------------------
@Composable
fun MapFiltersTab(state: CountryPackState) {
    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)

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
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("CATEGORY-BASED MAP FILTERING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Permanent venues remain stored in local SQLite databases, but map pins dynamically toggle based on enabled category filters.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Text("TOGGLE ACTIVE MAP CATEGORIES", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        items(CountryPackType.entries) { packType ->
            val isActive = state.activeCategoryFilters.contains(packType)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { CountryPackRepository.toggleCategoryFilter(packType) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFF191330) else cardBg),
                border = BorderStroke(1.dp, if (isActive) neonCyan else Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(packType.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(packType.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(packType.description, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    Switch(
                        checked = isActive,
                        onCheckedChange = { CountryPackRepository.toggleCategoryFilter(packType) },
                        colors = SwitchDefaults.colors(checkedThumbColor = neonCyan, checkedTrackColor = accentPurple)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// TAB 5: PERMANENT VENUE REGISTRY & IDENTITY
// -------------------------------------------------------------------------
@Composable
fun VenueRegistryTab(state: CountryPackState, onAddClick: () -> Unit) {
    val cardBg = Color(0xFF0F1626)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val activeGreen = Color(0xFF00E676)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PERMANENT VENUE IDENTIFIERS", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp)

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Venue Record", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(state.permanentVenues) { venue ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(venue.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(venue.fomoVenueId, color = neonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Surface(
                            color = activeGreen.copy(0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("${venue.completenessScore}% COMPLETE", color = activeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("${venue.streetAddress} • ${venue.district}, ${venue.city}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text("Hours: ${venue.openingHours} | Phone: ${venue.phone}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        venue.staticAmenities.forEach { amenity ->
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(amenity, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// ADD PERMANENT VENUE DIALOG
// -------------------------------------------------------------------------
@Composable
fun AddPermanentVenueDialog(
    onDismiss: () -> Unit,
    onAddVenue: (name: String, cat: String, packType: CountryPackType, district: String, address: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Nightclub") }
    var selectedPackType by remember { mutableStateOf(CountryPackType.NIGHTLIFE) }
    var district by remember { mutableStateOf("Sandton") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1626),
        title = { Text("Add Permanent Venue Record", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Venue Name", color = Color.White.copy(0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Subcategory (e.g. Lounge, Fine Dining)", color = Color.White.copy(0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("District/Suburb (e.g. Sandton, Rosebank)", color = Color.White.copy(0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street Address", color = Color.White.copy(0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone", color = Color.White.copy(0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAddVenue(name, category, selectedPackType, district, address, phone)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD))
            ) {
                Text("Generate Permanent FOMO ID", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(0.6f))
            }
        }
    )
}
