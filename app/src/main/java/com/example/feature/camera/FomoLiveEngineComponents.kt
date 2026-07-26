package com.example.feature.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * FOMO Live Engine Data Models & Enums
 */
enum class LiveLinkMode {
    MANUAL,
    AI_DIRECTOR,
    PIP,
    QUAD
}

data class LiveCamera(
    val id: String,
    val handle: String,
    val name: String,
    val cameraAngle: String,
    val avatarUrl: String,
    val streamImageUrl: String,
    val watcherCount: Int,
    val bpm: Int,
    val isPrimary: Boolean = false,
    val energyLevel: Int = 85
)

// Default Linked Live Cameras at Truth Nightclub
val defaultTruthNightclubCameras = listOf(
    LiveCamera(
        id = "cam_1",
        handle = "@You",
        name = "Main Stage",
        cameraAngle = "Front Row",
        avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200&auto=format&fit=crop",
        streamImageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=800&auto=format&fit=crop",
        watcherCount = 2410,
        bpm = 128,
        isPrimary = true,
        energyLevel = 94
    ),
    LiveCamera(
        id = "cam_2",
        handle = "@Neo",
        name = "DJ Booth",
        cameraAngle = "Overhead Deck",
        avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?q=80&w=200&auto=format&fit=crop",
        streamImageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=800&auto=format&fit=crop",
        watcherCount = 1890,
        bpm = 130,
        energyLevel = 98
    ),
    LiveCamera(
        id = "cam_3",
        handle = "@Kaybee",
        name = "VIP Terrace",
        cameraAngle = "Mezzanine Level",
        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop",
        streamImageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=800&auto=format&fit=crop",
        watcherCount = 1240,
        bpm = 126,
        energyLevel = 88
    ),
    LiveCamera(
        id = "cam_4",
        handle = "@Thando",
        name = "Dance Floor",
        cameraAngle = "Center Pit",
        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=200&auto=format&fit=crop",
        streamImageUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?q=80&w=800&auto=format&fit=crop",
        watcherCount = 980,
        bpm = 128,
        energyLevel = 91
    ),
    LiveCamera(
        id = "cam_5",
        handle = "@Alfred",
        name = "Outdoor Courtyard",
        cameraAngle = "Firepit Lounge",
        avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=200&auto=format&fit=crop",
        streamImageUrl = "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?q=80&w=800&auto=format&fit=crop",
        watcherCount = 650,
        bpm = 112,
        energyLevel = 76
    ),
    LiveCamera(
        id = "cam_6",
        handle = "@Lebo",
        name = "Bar Lounge",
        cameraAngle = "Main Bar",
        avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=200&auto=format&fit=crop",
        streamImageUrl = "https://images.unsplash.com/photo-1572116469696-31de0f17cc34?q=80&w=800&auto=format&fit=crop",
        watcherCount = 420,
        bpm = 115,
        energyLevel = 70
    )
)

/**
 * Sound Aware Live Render Canvas Effect Overlay
 * Generates pulse glows, neon trails, and beat zooms synced with live audio BPM.
 */
@Composable
fun SoundAwareLiveRender(
    bpm: Int = 128,
    activeEffect: String = "Pulse Glow",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundAware")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (60000 / bpm.coerceAtLeast(60)), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing)
        ),
        label = "rotate"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        when (activeEffect) {
            "Pulse Glow" -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF2D55).copy(alpha = 0.25f * pulseAnim),
                            Color(0xFF7D7AFF).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = (width.coerceAtLeast(height) * 0.6f) * pulseAnim
                    )
                )
            }
            "Neon Pulse" -> {
                val strokeWidth = 6.dp.toPx() * pulseAnim
                drawCircle(
                    color = Color(0xFF00FF7F).copy(alpha = 0.4f * pulseAnim),
                    radius = (width * 0.35f) * pulseAnim,
                    center = center,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 20f), rotateAnim)
                    )
                )
                drawCircle(
                    color = Color(0xFFFF2D55).copy(alpha = 0.3f),
                    radius = (width * 0.45f) * pulseAnim,
                    center = center,
                    style = Stroke(width = strokeWidth / 2f)
                )
            }
            "Beat Zoom" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF2D55).copy(alpha = 0.18f * pulseAnim),
                            Color.Transparent,
                            Color(0xFF00E5FF).copy(alpha = 0.22f * pulseAnim)
                        )
                    )
                )
            }
            "Lens Bloom" -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f * pulseAnim),
                            Color(0xFFFFD700).copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.8f, height * 0.2f),
                        radius = width * 0.5f * pulseAnim
                    )
                )
            }
            "Light Trails" -> {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFF2D55).copy(alpha = 0.3f),
                            Color(0xFF7D7AFF).copy(alpha = 0.3f),
                            Color(0xFF00FF7F).copy(alpha = 0.3f),
                            Color(0xFFFF2D55).copy(alpha = 0.3f)
                        ),
                        center = center
                    ),
                    radius = width * 0.5f * pulseAnim,
                    center = center,
                    style = Stroke(width = 8.dp.toPx())
                )
            }
        }
    }
}

/**
 * Live Link Engine Bar: Lists all connected cameras at the same venue
 */
@Composable
fun LiveLinkBar(
    cameras: List<LiveCamera>,
    selectedCameraId: String,
    currentMode: LiveLinkMode,
    onSelectCamera: (LiveCamera) -> Unit,
    onModeChange: (LiveLinkMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.82f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Venue Cam Count & Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Live Link",
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LIVE LINK • 6 CAMERAS AT TRUTH",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Mode Toggles (MANUAL, AI, PIP, QUAD)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveLinkMode.values().forEach { mode ->
                        val isSel = currentMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.1f))
                                .clickable { onModeChange(mode) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = when (mode) {
                                    LiveLinkMode.MANUAL -> "Manual"
                                    LiveLinkMode.AI_DIRECTOR -> "AI Director"
                                    LiveLinkMode.PIP -> "PiP"
                                    LiveLinkMode.QUAD -> "Quad (4)"
                                },
                                color = if (isSel) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal Scrollable Cams List
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cameras) { cam ->
                    val isSelected = cam.id == selectedCameraId
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFFF2D55).copy(alpha = 0.25f) else Color(0xFF1C1C1C),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .width(105.dp)
                            .clickable { onSelectCamera(cam) }
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(55.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                AsyncImage(
                                    model = cam.streamImageUrl,
                                    contentDescription = cam.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Live Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .background(Color.Red, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("LIVE", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                }

                                // BPM Indicator
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("${cam.bpm} BPM", color = Color(0xFFFFD700), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = cam.name,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${cam.handle} • ${cam.watcherCount} watching",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quad View Mode: Renders 4 live place cameras in a 2x2 grid simultaneously
 */
@Composable
fun QuadViewGrid(
    cameras: List<LiveCamera>,
    activeAudioCameraId: String,
    onSelectAudioCamera: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quadCams = cameras.take(4)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            quadCams.getOrNull(0)?.let { cam ->
                QuadTile(cam, activeAudioCameraId == cam.id) { onSelectAudioCamera(cam.id) }
            }
            quadCams.getOrNull(1)?.let { cam ->
                QuadTile(cam, activeAudioCameraId == cam.id) { onSelectAudioCamera(cam.id) }
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            quadCams.getOrNull(2)?.let { cam ->
                QuadTile(cam, activeAudioCameraId == cam.id) { onSelectAudioCamera(cam.id) }
            }
            quadCams.getOrNull(3)?.let { cam ->
                QuadTile(cam, activeAudioCameraId == cam.id) { onSelectAudioCamera(cam.id) }
            }
        }
    }
}

@Composable
private fun RowScope.QuadTile(
    camera: LiveCamera,
    hasAudio: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable { onClick() }
            .border(
                if (hasAudio) 2.dp else 0.5.dp,
                if (hasAudio) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.2f)
            )
    ) {
        AsyncImage(
            model = camera.streamImageUrl,
            contentDescription = camera.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Badges
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Red, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(camera.name.uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Text(camera.handle, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (hasAudio) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Audio Active",
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier.size(14.dp)
                )
            }
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("${camera.watcherCount} 👥", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Picture-in-Picture Floating Window for Secondary Live Cam
 */
@Composable
fun PipOverlayView(
    secondaryCamera: LiveCamera,
    onSwapPrimary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black,
        border = BorderStroke(2.dp, Color(0xFFFF2D55)),
        modifier = modifier
            .size(110.dp, 160.dp)
            .clickable { onSwapPrimary() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = secondaryCamera.streamImageUrl,
                contentDescription = secondaryCamera.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = secondaryCamera.name,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .background(Color(0xFFFF2D55), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Tap to Swap", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * AI Live Director Real-time Banner Highlight
 */
@Composable
fun AIDirectorBadge(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E0B36).copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFD438FF)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFD438FF), Color(0xFFFF2D55))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
            }
        }
    }
}

/**
 * Replay Verification & Background Upload Queue Modal
 * Triggered after broadcaster ends the live stream.
 */
@Composable
fun ReplayVerificationModal(
    replayImageUrl: String,
    venueName: String,
    eventName: String,
    durationSeconds: Int,
    totalWatchers: Int,
    ripplePointsEarned: Int,
    onPublishReplay: (caption: String, destinations: List<String>) -> Unit,
    onDiscard: () -> Unit
) {
    var captionText by remember { mutableStateOf("Unforgettable night at $venueName! 🔥 #$eventName") }
    val selectedDestinations = remember { mutableStateListOf("Feed", "Venue", "Club Lobby") }
    var selectedThumbnailIndex by remember { mutableIntStateOf(0) }
    var uploadProgress by remember { mutableFloatStateOf(0.1f) }
    var isUploading by remember { mutableStateOf(false) }

    val sampleThumbnails = listOf(
        replayImageUrl,
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?q=80&w=800&auto=format&fit=crop"
    )

    LaunchedEffect(isUploading) {
        if (isUploading) {
            while (uploadProgress < 1.0f) {
                kotlinx.coroutines.delay(200)
                uploadProgress += 0.08f
            }
            onPublishReplay(captionText, selectedDestinations)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF00FF7F).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Verified, null, tint = Color(0xFF00FF7F), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("REPLAY VERIFICATION", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("FOMO Broadcast Engine • Recorded Locally", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                    }

                    IconButton(onClick = onDiscard) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.6f))
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Stats Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("PEAK WATCHERS", color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("$totalWatchers 👥", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("DURATION", color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            val mins = durationSeconds / 60
                            val secs = durationSeconds % 60
                            Text(String.format("%02d:%02d ⏱️", mins, secs), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Surface(
                        color = Color(0xFF2A1502),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFFFD700)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("RIPPLE SCORE", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("+$ripplePointsEarned ⚡", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // AI Smart Thumbnails Selector
                Text("Select Smart AI Thumbnail:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sampleThumbnails.size) { idx ->
                        val isSel = selectedThumbnailIndex == idx
                        Box(
                            modifier = Modifier
                                .size(70.dp, 50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    2.dp,
                                    if (isSel) Color(0xFFFF2D55) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedThumbnailIndex = idx }
                        ) {
                            AsyncImage(
                                model = sampleThumbnails[idx],
                                contentDescription = "Thumbnail $idx",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSel) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = Color(0xFFFF2D55),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Caption TextField
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("Replay Caption", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF2D55),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Destinations Toggles
                Text("Publish Destinations:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Feed", "Venue", "Club Lobby", "Story").forEach { dest ->
                        val isSel = selectedDestinations.contains(dest)
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                if (isSel) selectedDestinations.remove(dest)
                                else selectedDestinations.add(dest)
                            },
                            label = { Text(dest, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF2D55),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E1E1E),
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Upload Progress Bar
                if (isUploading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Background Hardware Encoding & Edge CDN...", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text(String.format("%.0f%%", uploadProgress * 100), color = Color(0xFF00FF7F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            color = Color(0xFF00FF7F),
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDiscard,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Discard", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { isUploading = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                        enabled = !isUploading,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(if (isUploading) "Publishing..." else "Publish Replay 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
