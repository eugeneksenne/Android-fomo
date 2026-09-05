package com.example.feature.map.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.feature.map.engine.*

@Composable
fun RegionDownloadDialog(
    offlineMapEngine: OfflineMapEngine,
    currentMode: MapDataMode,
    onClose: () -> Unit
) {
    val countries = remember { offlineMapEngine.availableCountries() }
    var selectedCountry by remember { mutableStateOf(countries.first()) }

    // Access download manager if default engine
    val defaultEngine = offlineMapEngine as? DefaultOfflineMapEngine
    val downloadManager = defaultEngine?.downloadManager
    val states by downloadManager?.states?.collectAsState() ?: remember { mutableStateOf(emptyMap()) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFF0F1524)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offline Region Packs",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "FOMO Map Engine v2.0 • Offline Routing & Lifestyle POIs",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Operating Mode Indicator Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B2436),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (currentMode) {
                                                MapDataMode.HYBRID -> Color(0xFFC084FC)
                                                MapDataMode.ONLINE -> Color(0xFF32C759)
                                                MapDataMode.OFFLINE -> Color(0xFFFF9500)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Current Engine State: ${currentMode.name}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = when (currentMode) {
                                            MapDataMode.HYBRID -> "Local Region Pack + Live FOMO Flash Drops & Events"
                                            MapDataMode.ONLINE -> "Streaming OSM & Online FOMO Server Connection"
                                            MapDataMode.OFFLINE -> "Using Offline Regional Pack (Zero Data Usage)"
                                        },
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Country Selection Chips
                    Text(
                        text = "SELECT COUNTRY",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        items(countries) { country ->
                            val isSelected = selectedCountry.id == country.id
                            Surface(
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1B2436),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color.White.copy(0.12f)),
                                modifier = Modifier.clickable { selectedCountry = country }
                            ) {
                                Text(
                                    text = "${country.name} (${country.regions.size} Regions)",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))

                    // Regions List
                    Text(
                        text = "REGIONAL PACKS (${selectedCountry.name})",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(selectedCountry.regions) { region ->
                            val state = states[region.id] ?: DownloadState.NotDownloaded
                            RegionCardItem(
                                region = region,
                                state = state,
                                onDownload = { downloadManager?.download(region) },
                                onDelete = { downloadManager?.delete(region) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionCardItem(
    region: FomoRegion,
    state: DownloadState,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val sizeMb = region.downloadSizeBytes / (1024 * 1024)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF161E2E),
        border = BorderStroke(
            1.dp,
            if (state is DownloadState.Installed) Color(0xFF32C759).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (state is DownloadState.Installed) Color(0xFF32C759).copy(alpha = 0.2f)
                                else Color(0xFF00E5FF).copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state is DownloadState.Installed) Icons.Default.CheckCircle else Icons.Default.Map,
                            contentDescription = null,
                            tint = if (state is DownloadState.Installed) Color(0xFF32C759) else Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = region.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Size: ${sizeMb} MB • OSM Map + FOMO POI DB",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                when (state) {
                    is DownloadState.Installed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF32C759).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "INSTALLED",
                                    color = Color(0xFF32C759),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }

                    is DownloadState.Downloading -> {
                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    else -> {
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (state is DownloadState.Downloading) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00E5FF),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}
