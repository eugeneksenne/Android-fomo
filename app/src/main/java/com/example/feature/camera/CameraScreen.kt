package com.example.feature.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.data.MyCircleRepository
import com.example.core.data.VenueRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Floating reactions emoji model
data class FloatingEmoji(
    val id: Long,
    val emoji: String,
    val initialX: Float,
    val progress: Animatable<Float, AnimationVector1D>
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CameraScreen(
    onCloseClick: () -> Unit = {},
    onNavigateToLobby: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Navigation & Layout States
    var selectedMode by remember { mutableStateOf("PHOTO") } // PHOTO, VIDEO, LIVE
    var selectedLook by remember { mutableStateOf("Pulse") } // Pulse, Neon, Glow, Midnight, Stage, Electric
    var isStudioOpen by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf("Off") } // Off, On, Auto
    var isDualShotEnabled by remember { mutableStateOf(false) }
    var zoomFactor by remember { mutableStateOf(1.0f) } // 0.5x, 1.0x, 2.0x, 5.0x
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var isGridEnabled by remember { mutableStateOf(false) }
    var isVenuePillExpanded by remember { mutableStateOf(false) }

    // Sound Aware States
    var selectedTemplate by remember { mutableStateOf("None") }
    var selectedEffect by remember { mutableStateOf("None") }
    var soundTheme by remember { mutableStateOf("Party") }
    var bpmValue by remember { mutableStateOf(128) }

    // Live Streams States
    var isLiveReadinessChecking by remember { mutableStateOf(false) }
    var isCountdownActive by remember { mutableStateOf(false) }
    var countdownCount by remember { mutableStateOf(3) }
    var isBroadcasting by remember { mutableStateOf(false) }
    var watcherCount by remember { mutableStateOf(2410) }
    val liveComments = remember { mutableStateListOf<String>() }
    var userCommentInput by remember { mutableStateOf("") }
    val floatingEmojis = remember { mutableStateListOf<FloatingEmoji>() }
    var showLivePoll by remember { mutableStateOf(true) }
    var pollVotesDJ1 by remember { mutableStateOf(142) }
    var pollVotesDJ2 by remember { mutableStateOf(84) }

    // Video Capture States
    var isRecordingVideo by remember { mutableStateOf(false) }
    var videoDurationSeconds by remember { mutableStateOf(0) }

    // Photo/Video Capture & Publish Pipeline
    var capturedPhotoUrl by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var processingStep by remember { mutableStateOf(0) }
    var isPublishing by remember { mutableStateOf(false) }
    var publishStep by remember { mutableStateOf(0) }
    var showPublishPreviewScreen by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("") }
    var selectedVisibility by remember { mutableStateOf("Public") }
    val selectedDestinations = remember { mutableStateOf(setOf("Feed", "Venue", "Club Lobby")) }
    var isShareLocationEnabled by remember { mutableStateOf(true) }
    var isUploadFinishedSuccess by remember { mutableStateOf(false) }

    // Drag-and-drop dual shot window state
    var dualShotOffset by remember { mutableStateOf(Offset(30f, 150f)) }

    // Screen Flash effect (Photo Capture trigger)
    var showFlashOverlay by remember { mutableStateOf(false) }

    // Simulate fluctuation of Watcher count
    LaunchedEffect(isBroadcasting) {
        if (isBroadcasting) {
            while (isBroadcasting) {
                delay(3000)
                watcherCount += (-15..20).random()
            }
        }
    }

    // Simulate fluctuation of BPM value based on Sound Aware theme
    LaunchedEffect(soundTheme) {
        bpmValue = when (soundTheme) {
            "Chill" -> (90..110).random()
            "Party" -> (120..128).random()
            "Festival" -> (128..135).random()
            "Concert" -> (130..142).random()
            else -> 128
        }
    }

    // Live Comment Generation Simulation
    val simulatedComments = listOf(
        "Amanda: This is absolutely crazy! 🔥",
        "Tyler: What event is this? Amapiano Fridays?",
        "Sarah: Best night ever, venue is packed!",
        "Jason: Uncle Waffles is on fire! 🎧",
        "Michael: Ripple multiplier is 2.5x right now!",
        "Bongiwe: FOMO Club is undefeated.",
        "Neo: Lit vibe, arriving in 10 mins!",
        "Jessica: Bass shake effect looks gorgeous!",
        "Lerato: That venue pill info has flash drops, go claim!",
        "Kabelo: Show the DJ booth please!"
    )
    LaunchedEffect(isBroadcasting) {
        if (isBroadcasting) {
            liveComments.clear()
            liveComments.add("System: Broadcast started at Truth Nightclub")
            while (isBroadcasting) {
                delay((2500..5000).random().toLong())
                val comment = simulatedComments.random()
                liveComments.add(comment)
            }
        }
    }

    // Video Recording Timer
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            videoDurationSeconds = 0
            while (isRecordingVideo) {
                delay(1000)
                videoDurationSeconds++
            }
        }
    }

    // Focus point timeout helper
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1500)
            focusPoint = null
        }
    }

    // Floating reaction trigger helper
    fun spawnReactionEmoji(emoji: String) {
        val id = System.currentTimeMillis() + (0..10000).random()
        val initialX = (30..80).random().toFloat()
        val progress = Animatable(1f)
        val reaction = FloatingEmoji(id, emoji, initialX, progress)
        floatingEmojis.add(reaction)
        coroutineScope.launch {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 2200, easing = LinearOutSlowInEasing)
            )
            floatingEmojis.remove(reaction)
        }
    }

    // Camera viewfinder scale factor
    val viewFinderScale by animateFloatAsState(
        targetValue = when (zoomFactor) {
            0.5f -> 0.8f
            1.0f -> 1.0f
            2.0f -> 1.4f
            5.0f -> 2.0f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    // Main Layout container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // -------------------------------------------------------------
        // CAMERA VIEWINDER (Simulated)
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            focusPoint = offset
                        }
                    )
                }
        ) {
            // Main Unsplash Background simulating lens feed
            AsyncImage(
                model = "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=1200&auto=format&fit=crop",
                contentDescription = "Camera Preview Lens Feed",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = viewFinderScale,
                        scaleY = viewFinderScale
                    )
            )

            // LOOK COLOR TINT OVERLAY (Pulse, Neon, Glow, etc.)
            val lookTint = when (selectedLook) {
                "Pulse" -> Color(0xFFFF2D55).copy(alpha = 0.12f)
                "Neon" -> Color(0xFF00F0FF).copy(alpha = 0.16f)
                "Glow" -> Color(0xFFFFD700).copy(alpha = 0.14f)
                "Midnight" -> Color(0xFF0033aa).copy(alpha = 0.22f)
                "Vintage Party" -> Color(0xFFE28B00).copy(alpha = 0.12f)
                "Electric" -> Color(0xFFB026FF).copy(alpha = 0.18f)
                "Luxe" -> Color(0xFFF3E5AB).copy(alpha = 0.10f)
                "Noir" -> Color(0xFF333333).copy(alpha = 0.25f)
                "Stage" -> Color(0xFF666666).copy(alpha = 0.20f)
                "Flash" -> Color(0xFFE0F7FA).copy(alpha = 0.08f)
                "Sunset" -> Color(0xFFFF5722).copy(alpha = 0.15f)
                "Rooftop" -> Color(0xFF3F51B5).copy(alpha = 0.12f)
                else -> Color.Transparent
            }
            if (lookTint != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(lookTint)
                )
            }

            // EFFECT OVERLAY (Motion Blur, scanline scan, cinema bars, prism)
            when (selectedEffect) {
                "VHS" -> {
                    // Vintage VHS CRT Scanline Simulation
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0..40) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.07f))
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(8.dp, Color.Black.copy(alpha = 0.15f))
                    ) {
                        Text(
                            "PLAY ▶  VHS 12:28:40 PM",
                            color = Color(0xFF00FF00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
                    }
                }
                "Cinematic Bars" -> {
                    // Movie Cinema Widescreen letterbox crop
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color.Black)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color.Black)
                        )
                    }
                }
                "Light Trails" -> {
                    // Glowing line trails overlay
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFFFF007F),
                                radius = 250f,
                                center = Offset(this.size.width * 0.3f, this.size.height * 0.4f),
                                alpha = 0.15f
                            )
                            drawCircle(
                                color = Color(0xFF00FFFF),
                                radius = 180f,
                                center = Offset(this.size.width * 0.7f, this.size.height * 0.5f),
                                alpha = 0.12f
                            )
                        }
                    }
                }
                "Bloom" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                                    radius = 800f
                                )
                            )
                    )
                }
            }

            // GRID OVERLAY (If selected)
            if (isGridEnabled) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.25f), thickness = 0.8.dp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.25f), thickness = 0.8.dp)
                }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    VerticalDivider(
                        color = Color.White.copy(alpha = 0.25f),
                        thickness = 0.8.dp,
                        modifier = Modifier.fillMaxHeight()
                    )
                    VerticalDivider(
                        color = Color.White.copy(alpha = 0.25f),
                        thickness = 0.8.dp,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }

            // DUAL SHOT SUB-PREVIEW (Selfie Bubble)
            if (isDualShotEnabled) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(dualShotOffset.x.roundToInt(), dualShotOffset.y.roundToInt()) }
                        .size(110.dp, 160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                dualShotOffset += dragAmount
                            }
                        }
                ) {
                    // Draggable selfie camera preview Unsplash
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=300&auto=format&fit=crop",
                        contentDescription = "Dual Shot Selfie Bubble",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Mini label
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("CREATOR", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // TAP TO FOCUS PULSE RING
            focusPoint?.let { point ->
                val focusTransition = rememberInfiniteTransition(label = "focus")
                val focusScale by focusTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .absoluteOffset {
                            IntOffset(
                                (point.x - 30.dp.toPx()).roundToInt(),
                                (point.y - 30.dp.toPx()).roundToInt()
                            )
                        }
                        .size(60.dp)
                        .graphicsLayer(scaleX = focusScale, scaleY = focusScale)
                        .border(1.5.dp, Color(0xFFFFD700), CircleShape)
                ) {
                    // Small crosshairs
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFFFFD700), CircleShape)
                            .align(Alignment.Center)
                    )
                }
            }

            // SOUND AWARE BEAT-REACTIVE GRAPHICS
            val infiniteTransition = rememberInfiniteTransition(label = "audio")
            val barHeight1 by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(380, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar1"
            )
            val barHeight2 by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(280, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar2"
            )
            val barHeight3 by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(480, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar3"
            )

            // Beat pulsing indicator for Sound Aware mode
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 180.dp, start = 20.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio waves
                Row(
                    modifier = Modifier.size(18.dp, 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(barHeight1).background(Color(0xFFFF2D55)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(barHeight2).background(Color(0xFF00FF7F)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(barHeight3).background(Color(0xFF00F0FF)))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SOUND AWARE: ${bpmValue} BPM",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // TOP SCRIM GRADIENT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
            )

            // BOTTOM SCRIM GRADIENT
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
            )
        }

        // -------------------------------------------------------------
        // TOP HUD / CONTROLS BAR
        // -------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Close Button
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Camera",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Center Venue Pill Indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pill shape
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.clickable { isVenuePillExpanded = !isVenuePillExpanded }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = Color(0xFFFF2D55),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Truth Nightclub",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Amapiano Fridays • Confidence: 99%",
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isVenuePillExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // EXPANDED INTEL OVERLAY DROPDOWN
                    AnimatedVisibility(
                        visible = isVenuePillExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.88f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .width(280.dp)
                                .padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "VENUE INTELLIGENCE",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )

                                Divider(color = Color.White.copy(alpha = 0.15f))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("FOMO Club / Truth", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Rosebank, JHB • 1.2 km away", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFF2D55), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("LIVE NOW", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            isVenuePillExpanded = false
                                            onNavigateToLobby("fomo_club")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D0A40)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Forum, null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Club Lobby", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            isVenuePillExpanded = false
                                            Toast.makeText(context, "Routing to Truth Nightclub Entrance...", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.DirectionsRun, null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Route Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Divider(color = Color.White.copy(alpha = 0.15f))

                                // Active Flash Drops
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CardGiftcard, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Flash Drop active: Free Tequila Shot (52/100)",
                                        color = Color(0xFFFFD700),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right icons (Grid, Flash, Studio)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Grid toggle
                IconButton(
                    onClick = {
                        isGridEnabled = !isGridEnabled
                        Toast.makeText(context, if (isGridEnabled) "Grid Enabled" else "Grid Disabled", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isGridEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Toggle Grid",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Flash toggle
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            "Off" -> "On"
                            "On" -> "Auto"
                            else -> "Off"
                        }
                        Toast.makeText(context, "Flash: $flashMode", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    val icon = when (flashMode) {
                        "On" -> Icons.Default.FlashOn
                        "Auto" -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Flash Toggle",
                        tint = if (flashMode == "Off") Color.White else Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Studio Button
                IconButton(
                    onClick = { isStudioOpen = !isStudioOpen },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "FOMO Studio Settings",
                        tint = if (isStudioOpen) Color(0xFFFF2D55) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ZOOM RATIO BAR (0.5x, 1x, 2x, 5x) - Sitting dynamically on left side of screen
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            listOf(0.5f, 1.0f, 2.0f, 5.0f).forEach { scale ->
                val isSelected = zoomFactor == scale
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { zoomFactor = scale },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${scale}x",
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // DUAL SHOT HUD TOGGLE
        IconButton(
            onClick = {
                isDualShotEnabled = !isDualShotEnabled
                Toast.makeText(context, if (isDualShotEnabled) "Dual Shot Enabled" else "Dual Shot Disabled", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.FlipCameraIos,
                contentDescription = "Toggle Dual Shot",
                tint = if (isDualShotEnabled) Color(0xFFFF2D55) else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // -------------------------------------------------------------
        // BOTTOM VIEWINDER CONTROLS (Swipe Modes & Capture Buttons)
        // -------------------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Horizontal Mode Swipe Selector (PHOTO, VIDEO, LIVE)
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                listOf("PHOTO", "VIDEO", "LIVE").forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (!isBroadcasting && !isRecordingVideo) {
                                    selectedMode = mode
                                } else {
                                    Toast.makeText(context, "Cannot change mode during active stream!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = mode,
                                color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFD700))
                                )
                            }
                        }
                    }
                }
            }

            // Capture Line Controls (Gallery, Trigger, Looks Carousel)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Option: Gallery Thumbnail or Settings
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(64.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable {
                                Toast.makeText(context, "Opening FOMO Local Moments Drafts...", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?q=80&w=200&auto=format&fit=crop",
                            contentDescription = "Gallery Moments",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("12", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Drafts", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }

                // Middle Option: Capture Button with dynamic style
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable {
                            // Capture action handler
                            when (selectedMode) {
                                "PHOTO" -> {
                                    coroutineScope.launch {
                                        showFlashOverlay = true
                                        delay(100)
                                        showFlashOverlay = false
                                        // Transition to AI Moment Engine processing
                                        isProcessing = true
                                        processingStep = 1
                                        delay(800)
                                        processingStep = 2
                                        delay(800)
                                        processingStep = 3
                                        delay(800)
                                        processingStep = 4
                                        delay(600)
                                        isProcessing = false
                                        capturedPhotoUrl = "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=600&auto=format&fit=crop"
                                        showPublishPreviewScreen = true
                                    }
                                }
                                "VIDEO" -> {
                                    if (isRecordingVideo) {
                                        // Stop Recording
                                        isRecordingVideo = false
                                        isProcessing = true
                                        processingStep = 1
                                        coroutineScope.launch {
                                            delay(700)
                                            processingStep = 2
                                            delay(700)
                                            processingStep = 3
                                            delay(700)
                                            processingStep = 4
                                            delay(500)
                                            isProcessing = false
                                            capturedPhotoUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=600&auto=format&fit=crop"
                                            showPublishPreviewScreen = true
                                        }
                                    } else {
                                        // Start Recording
                                        isRecordingVideo = true
                                    }
                                }
                                "LIVE" -> {
                                    if (isBroadcasting) {
                                        // End Live Broadcast
                                        isBroadcasting = false
                                        isProcessing = true
                                        processingStep = 1
                                        coroutineScope.launch {
                                            delay(900)
                                            processingStep = 2
                                            delay(900)
                                            processingStep = 3
                                            delay(900)
                                            processingStep = 4
                                            delay(600)
                                            isProcessing = false
                                            capturedPhotoUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=600&auto=format&fit=crop"
                                            showPublishPreviewScreen = true
                                        }
                                    } else {
                                        // Trigger live checks setup
                                        isLiveReadinessChecking = true
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val centerColor = if (selectedMode == "LIVE") Color.Red else if (selectedMode == "VIDEO") Color.Red else Color.White
                    val isRecording = isRecordingVideo || isBroadcasting
                    Box(
                        modifier = Modifier
                            .size(if (isRecording) 28.dp else 68.dp)
                            .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                            .background(centerColor)
                    )
                }

                // Right Option: Mini Looks Carousel
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(90.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val looks = listOf("Pulse", "Neon", "Glow", "Midnight", "Vintage Party", "Electric", "Luxe", "Noir", "Stage", "Flash", "Sunset", "Rooftop")
                        items(looks) { look ->
                            val isCurrent = selectedLook == look
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedLook = look }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = look,
                                    color = if (isCurrent) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFD700))
                                    )
                                }
                            }
                        }
                    }
                    Text("Looks Filter", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Video Duration overlay
            if (isRecordingVideo) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = String.format("REC %02d:%02d", videoDurationSeconds / 60, videoDurationSeconds % 60),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // BROADCASTING HUD OVERLAYS (LIVE Chat & reactions stream)
        // -------------------------------------------------------------
        if (isBroadcasting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
            ) {
                // Left Side: Live Broadcast metadata
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 110.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(Color.Red, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.People, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text("${watcherCount} WATCHING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }

                // Center/Right: Floating Reaction Stream rising
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(120.dp, 300.dp)
                ) {
                    floatingEmojis.forEach { reaction ->
                        val yOffset = 300.dp * reaction.progress.value
                        val alpha = reaction.progress.value
                        Text(
                            text = reaction.emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .offset(x = reaction.initialX.dp, y = yOffset)
                                .graphicsLayer(alpha = alpha)
                        )
                    }
                }

                // Lower Left: Comments LazyColumn stream
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.7f)
                        .height(200.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(liveComments.reversed()) { comment ->
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(comment, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Broadcaster typed chat bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = userCommentInput,
                            onValueChange = { userCommentInput = it },
                            placeholder = { Text("Comment as host...", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.8f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.8f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (userCommentInput.isNotEmpty()) {
                                    liveComments.add("Broadcaster (You): $userCommentInput")
                                    userCommentInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFFF2D55), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Lower Right: Floating reaction triggers list
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    listOf("🔥", "❤️", "🙌", "🍻", "🎉").forEach { emoji ->
                        IconButton(
                            onClick = { spawnReactionEmoji(emoji) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }

                // Live Poll system
                if (showLivePoll) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 110.dp)
                            .width(160.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LIVE VIBE POLL", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { showLivePoll = false }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Which DJ sets the mood?", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))

                            // Option A
                            val totalVotes = (pollVotesDJ1 + pollVotesDJ2).toFloat()
                            val pctDJ1 = if (totalVotes > 0) (pollVotesDJ1 / totalVotes) * 100 else 0f
                            val pctDJ2 = if (totalVotes > 0) (pollVotesDJ2 / totalVotes) * 100 else 0f

                            Column(modifier = Modifier.clickable { pollVotesDJ1++ }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Kabza", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                                    Text(String.format("%.0f%%", pctDJ1), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { pctDJ1 / 100f },
                                    color = Color(0xFFFF2D55),
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Option B
                            Column(modifier = Modifier.clickable { pollVotesDJ2++ }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Uncle Waffles", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                                    Text(String.format("%.0f%%", pctDJ2), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { pctDJ2 / 100f },
                                    color = Color(0xFF00FF7F),
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // PHOTO CAPTURE FLASH OVERLAY EFFECT
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = showFlashOverlay,
            enter = fadeIn(animationSpec = tween(50)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // -------------------------------------------------------------
        // PRE-LIVE DEVICE READINESS DIAGNOSTICS SCREEN
        // -------------------------------------------------------------
        if (isLiveReadinessChecking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF2D0A40), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BroadcastOnPersonal, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(28.dp))
                        }

                        Text("GO LIVE READINESS CHECK", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(
                            text = "Verifying telemetry, video hardware encoder, and venue mapping anchors before streaming...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        // Diagnostics List Items
                        val listItems = listOf(
                            "GPS & Venue identified (Truth JHB)" to true,
                            "Camera & Mic permissions verified" to true,
                            "Network Latency (14 ms Jitter-free)" to true,
                            "Local storage safe (24.2 GB / Est. 6h 15m)" to true,
                            "Thermal temperature status (Cool)" to true
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listItems.forEach { (label, ok) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (ok) Color(0xFF32C759) else Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { isLiveReadinessChecking = false },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    isLiveReadinessChecking = false
                                    isCountdownActive = true
                                    countdownCount = 3
                                    coroutineScope.launch {
                                        delay(1000)
                                        countdownCount = 2
                                        delay(1000)
                                        countdownCount = 1
                                        delay(1000)
                                        isCountdownActive = false
                                        isBroadcasting = true
                                        Toast.makeText(context, "Broadcast Live! One Sun. Billion Eyes.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Go Live", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // LIVE COUNTDOWN TIMER OVERLAY (3... 2... 1...)
        // -------------------------------------------------------------
        if (isCountdownActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "STARTING BROADCAST IN...",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "$countdownCount",
                        color = Color.White,
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Anchor: Truth Nightclub",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // STUDIO COMPANION DRAWER (Looks, templates, effects)
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isStudioOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(280.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FOMO STUDIO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        IconButton(onClick = { isStudioOpen = false }) {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.15f))

                    // Looks Section
                    Text("🎨 COLOR LOOKS", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    val looks = listOf("Pulse", "Neon", "Glow", "Midnight", "Vintage Party", "Electric", "Luxe", "Noir", "Stage", "Flash", "Sunset", "Rooftop")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        looks.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { look ->
                                    val isSelected = selectedLook == look
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) Color(0xFF2D0A40) else Color.White.copy(alpha = 0.05f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFFFF2D55) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedLook = look }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(look, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Moment Templates Section
                    Text("🎬 MOMENT TEMPLATES", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    val templates = listOf("Club Recap", "Night Started", "Festival", "Date Night", "Birthday", "Girls Night")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        templates.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { tmpl ->
                                    val isSelected = selectedTemplate == tmpl
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) Color(0xFF2D0A40) else Color.White.copy(alpha = 0.05f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFFFF2D55) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedTemplate = if (isSelected) "None" else tmpl }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(tmpl, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Creative Effects Section
                    Text("✨ NIGHTLIFE EFFECTS", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    val effects = listOf("Motion Blur", "Light Trails", "Bloom", "Lens Flare", "Film Grain", "Neon Reflection", "VHS", "Slow Shutter", "Bokeh", "Cinematic Bars", "Prism")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        effects.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { eff ->
                                    val isSelected = selectedEffect == eff
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) Color(0xFF2D0A40) else Color.White.copy(alpha = 0.05f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFFFF2D55) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedEffect = if (isSelected) "None" else eff }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(eff, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Sound Aware Engine Section
                    Text("🎵 SOUND AWARE INTEL", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Theme Intensity", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Chill", "Party", "Festival", "Concert", "Creator").forEach { theme ->
                                    val isSel = soundTheme == theme
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSel) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.08f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { soundTheme = theme }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(theme, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            Text("AI Recommendation", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Heavy Amapiano Bass Detected (128 BPM). Suggested: Pulse Filter + Light Trails Effect.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // AI MOMENT ENGINE PROCESSING (Screen overlay)
        // -------------------------------------------------------------
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val pulseTransition = rememberInfiniteTransition(label = "pulseLogo")
                    val pulseAlpha by pulseTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "logoAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer(alpha = pulseAlpha)
                            .background(Color(0xFF2D0A40), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFB026FF),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "AI MOMENT ENGINE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = "Processing nightlife capture using GPU acceleration...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stage checklists
                    val stages = listOf(
                        "Selecting best keyframe with face optimization...",
                        "Applying color grade and Look: $selectedLook...",
                        "Synchronizing video visual peaks to beat BPM...",
                        "Generating Ripple metadata boost multipliers..."
                    )

                    Column(
                        modifier = Modifier.width(280.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        stages.forEachIndexed { index, label ->
                            val state = when {
                                processingStep > index + 1 -> "Success"
                                processingStep == index + 1 -> "Running"
                                else -> "Pending"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = if (state == "Running") Color.White else Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    fontWeight = if (state == "Running") FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                when (state) {
                                    "Success" -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF32C759), modifier = Modifier.size(16.dp))
                                    "Running" -> CircularProgressIndicator(color = Color(0xFFFF2D55), strokeWidth = 1.5.dp, modifier = Modifier.size(14.dp))
                                    else -> Icon(Icons.Default.RadioButtonUnchecked, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // MOMENT PUBLISH PREVIEW & SETTINGS SCREEN
        // -------------------------------------------------------------
        if (showPublishPreviewScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    // Top Back
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showPublishPreviewScreen = false }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                        Text(
                            text = "PUBLISH NEW MOMENT",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Media Preview & Settings details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Visual Card
                        Box(
                            modifier = Modifier
                                .size(140.dp, 200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = capturedPhotoUrl,
                                contentDescription = "Captured Media preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Selected Look overlay representation
                            val previewLookTint = when (selectedLook) {
                                "Pulse" -> Color(0xFFFF2D55).copy(alpha = 0.12f)
                                "Neon" -> Color(0xFF00F0FF).copy(alpha = 0.16f)
                                "Glow" -> Color(0xFFFFD700).copy(alpha = 0.14f)
                                "Midnight" -> Color(0xFF0033aa).copy(alpha = 0.22f)
                                "Vintage Party" -> Color(0xFFE28B00).copy(alpha = 0.12f)
                                "Electric" -> Color(0xFFB026FF).copy(alpha = 0.18f)
                                "Luxe" -> Color(0xFFF3E5AB).copy(alpha = 0.10f)
                                "Noir" -> Color(0xFF333333).copy(alpha = 0.25f)
                                "Stage" -> Color(0xFF666666).copy(alpha = 0.20f)
                                "Flash" -> Color(0xFFE0F7FA).copy(alpha = 0.08f)
                                "Sunset" -> Color(0xFFFF5722).copy(alpha = 0.15f)
                                "Rooftop" -> Color(0xFF3F51B5).copy(alpha = 0.12f)
                                else -> Color.Transparent
                            }
                            if (previewLookTint != Color.Transparent) {
                                Box(modifier = Modifier.fillMaxSize().background(previewLookTint))
                            }

                            // Watermark location stamps
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("📍 Truth", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2D0A40), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Amapiano", color = Color(0xFFFFD700), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Caption & AI Assist Form
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "WRITE CAPTION",
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )

                            TextField(
                                value = captionText,
                                onValueChange = { captionText = it },
                                placeholder = { Text("What's the vibe? Share your moment...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFFFF2D55),
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )

                            // AI suggested caption pills
                            Text(
                                "✨ AI SUGGESTED CAPTIONS",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )

                            val suggestions = listOf(
                                "Truth was crazy tonight! 🔥",
                                "Amapiano vibes are immaculate ✨",
                                "Bass is hitting different at Rosebank! 🔊"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                suggestions.forEach { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .clickable { captionText = suggestion }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(suggestion, color = Color.White, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Settings Details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Divider(color = Color.White.copy(alpha = 0.1f))

                        // Visibility Options
                        Text("VISIBILITY SETTINGS", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Public", "Followers", "Private").forEach { vis ->
                                val isSelected = selectedVisibility == vis
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) Color(0xFF2D0A40) else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFFF2D55) else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedVisibility = vis }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(vis, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        // Publish Destinations Checklist
                        Text("PUBLISH DESTINATIONS", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        val destinationsList = listOf("Feed", "Venue", "Club Lobby", "Event Details", "Profile Story")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            destinationsList.forEach { dest ->
                                val isChecked = selectedDestinations.value.contains(dest)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            val current = selectedDestinations.value.toMutableSet()
                                            if (isChecked) current.remove(dest) else current.add(dest)
                                            selectedDestinations.value = current
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dest, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            val current = selectedDestinations.value.toMutableSet()
                                            if (isChecked) current.remove(dest) else current.add(dest)
                                            selectedDestinations.value = current
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFFFF2D55),
                                            uncheckedColor = Color.White.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        // Share location stamp switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Share Location Stamp", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Add 'Truth Nightclub' landmark and badge tags", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                            }
                            Switch(
                                checked = isShareLocationEnabled,
                                onCheckedChange = { isShareLocationEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF2D55)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Trigger Publish Action!
                        Button(
                            onClick = {
                                isPublishing = true
                                publishStep = 1
                                coroutineScope.launch {
                                    delay(700)
                                    publishStep = 2
                                    delay(700)
                                    publishStep = 3
                                    delay(700)
                                    publishStep = 4
                                    delay(700)
                                    isPublishing = false
                                    isUploadFinishedSuccess = true

                                    // Add Story dynamically to global state / MyCircleRepository so that it is FULL STACK and integrated!
                                    val formattedCaption = if (captionText.isEmpty()) "Vibing at Truth Nightclub" else captionText
                                    MyCircleRepository.addStory(
                                        userName = "You",
                                        mediaUrl = capturedPhotoUrl,
                                        text = formattedCaption,
                                        type = if (selectedMode == "LIVE") "Live" else "Story"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("PUBLISH MOMENT NOW", fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // UPLOAD ENGINE STEP-BY-STEP DIALOGUE
        // -------------------------------------------------------------
        if (isPublishing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF2D55),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )

                        Text("UPLOADING MOMENT...", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(
                            text = "Every capture contributes to your local Ripple score!",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        // Step-by-step Upload Engine Checkpoints
                        val uploadStages = listOf(
                            "Uploading HDR media assets safely..." to 1,
                            "Adding venue anchoring to Truth Nightclub..." to 2,
                            "Broadcasting to feed and followers..." to 3,
                            "Syncing global Ripple ranking system..." to 4
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            uploadStages.forEach { (label, stepId) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        color = if (publishStep >= stepId) Color.White else Color.White.copy(alpha = 0.4f),
                                        fontSize = 11.sp,
                                        fontWeight = if (publishStep == stepId) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (publishStep > stepId) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF32C759), modifier = Modifier.size(16.dp))
                                    } else if (publishStep == stepId) {
                                        CircularProgressIndicator(color = Color(0xFFFF2D55), strokeWidth = 1.5.dp, modifier = Modifier.size(12.dp))
                                    } else {
                                        Icon(Icons.Default.RadioButtonUnchecked, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // MOMENT PUBLISHED SUCCESS CONGRATULATIONS DIALOGUE
        // -------------------------------------------------------------
        if (isUploadFinishedSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF2D0A40), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color(0xFF00FF7F), modifier = Modifier.size(36.dp))
                        }

                        Text("MOMENT PUBLISHED!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
                        Text(
                            text = "Your moment has been uploaded successfully. It is now pinned to Truth Nightclub, and published to your Feed story tray!",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        // Ripple Boost card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.TrendingUp, null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("RIPPLE BOOSTED! ⚡", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("+25 Ripple Points earned at Truth Club", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                // Reset capture flows and close settings screen
                                isUploadFinishedSuccess = false
                                showPublishPreviewScreen = false
                                selectedLook = "Pulse"
                                selectedEffect = "None"
                                selectedTemplate = "None"
                                captionText = ""
                                // Go back to Discover tab (close)
                                onCloseClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
