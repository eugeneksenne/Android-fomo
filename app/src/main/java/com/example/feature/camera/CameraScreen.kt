package com.example.feature.camera

import com.example.core.data.feed.FeedRepository
import com.example.core.data.media.MediaUploader
import com.example.core.data.venue.VenueIntelligence
import com.example.feature.camera.live.LiveReadiness
import com.example.feature.camera.live.LiveSessionStore
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
    var zoomFactor by remember { mutableStateOf(1.0f) } // 0.5x, 1.0x, 2.0x, 5.0x
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var isGridEnabled by remember { mutableStateOf(false) }
    var isVenuePillExpanded by remember { mutableStateOf(false) }

    // Sound Aware States
    var selectedTemplate by remember { mutableStateOf("None") }
    var selectedEffect by remember { mutableStateOf("None") }
    var soundTheme by remember { mutableStateOf("Party") }

    // Live Streams States
    var isLiveReadinessChecking by remember { mutableStateOf(false) }
    var isCountdownActive by remember { mutableStateOf(false) }
    var countdownCount by remember { mutableStateOf(3) }
    var isBroadcasting by remember { mutableStateOf(false) }
    // Real viewer count. Starts at 0 and only moves when a streaming backend
    // reports actual viewers; it was previously seeded to a fictional 2,410.
    var watcherCount by remember { mutableStateOf(0) }
    val liveComments = remember { mutableStateListOf<String>() }
    var userCommentInput by remember { mutableStateOf("") }
    val floatingEmojis = remember { mutableStateListOf<FloatingEmoji>() }
    // Polls start at zero. These were seeded to 142/84 votes despite there
    // being no viewers able to cast one.
    var showLivePoll by remember { mutableStateOf(true) }
    var pollVotesDJ1 by remember { mutableStateOf(0) }
    var pollVotesDJ2 by remember { mutableStateOf(0) }

    // Video Capture States
    var isRecordingVideo by remember { mutableStateOf(false) }
    var videoDurationSeconds by remember { mutableStateOf(0) }

    // Real CameraX pipeline (replaces the previous static-image simulation).
    val cameraController = remember { CameraCaptureController(context) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(CameraCaptureController.hasCameraPermission(context))
    }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // ---- Venue Intelligence Engine ----------------------------------------
    // GPS -> Offline Venue Pack -> Confidence Score -> Venue Identified.
    // Replaces the hardcoded "Truth Nightclub / Confidence: 99%" label.
    var venueState by remember {
        mutableStateOf<VenueIntelligence.VenueState>(VenueIntelligence.VenueState.Detecting)
    }
    var manualVenueName by remember { mutableStateOf<String?>(null) }

    // ---- Live: readiness, local-first recording, crash recovery -----------
    val liveSessionStore = remember { LiveSessionStore.getInstance(context) }
    var readinessReport by remember { mutableStateOf<LiveReadiness.Report?>(null) }
    var activeLiveSessionId by remember { mutableStateOf<String?>(null) }
    var peakViewers by remember { mutableStateOf(0) }
    var recoverableSession by remember {
        mutableStateOf(liveSessionStore.findRecoverable())
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            coroutineScope.launch { venueState = VenueIntelligence.detect(context) }
        } else {
            venueState = VenueIntelligence.VenueState.PermissionDenied
        }
    }

    LaunchedEffect(Unit) {
        if (VenueIntelligence.hasLocationPermission(context)) {
            venueState = VenueIntelligence.detect(context)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Venue actually attached to a published moment.
    val detectedVenueName: String = manualVenueName
        ?: (venueState as? VenueIntelligence.VenueState.Identified)?.match?.venueName
        ?: ""

    val venueNeedsAttention: Boolean = manualVenueName == null &&
        venueState !is VenueIntelligence.VenueState.Identified &&
        venueState !is VenueIntelligence.VenueState.Detecting

    val venueSubtitle: String = when {
        manualVenueName != null -> "Selected manually"
        else -> when (val st = venueState) {
            is VenueIntelligence.VenueState.Detecting -> "Detecting venue..."
            is VenueIntelligence.VenueState.PermissionDenied -> "Tap to choose a venue"
            is VenueIntelligence.VenueState.NoLocation -> "No GPS signal - tap to choose"
            is VenueIntelligence.VenueState.NoVenueNearby -> "No venue nearby - tap to choose"
            is VenueIntelligence.VenueState.LowConfidence ->
                "Not sure - tap to confirm (${st.candidates.firstOrNull()?.confidencePercent ?: 0}%)"
            is VenueIntelligence.VenueState.Identified ->
                "Confidence: ${st.match.confidencePercent}%"
        }
    }
    // Real signed-in identity, rather than a hardcoded stock portrait.
    val currentUserAvatarUrl = remember {
        runCatching {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
        }.getOrNull().orEmpty()
    }
    // Content Uri of the media actually captured on this device.
    var capturedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var capturedIsVideo by remember { mutableStateOf(false) }

    // Photo/Video Capture & Publish Pipeline
    var capturedPhotoUrl by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var processingStep by remember { mutableStateOf(0) }
    var isPublishing by remember { mutableStateOf(false) }
    var publishStep by remember { mutableStateOf(0) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var showPublishPreviewScreen by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("") }
    var selectedVisibility by remember { mutableStateOf("Public") }
    val selectedDestinations = remember { mutableStateOf(setOf("Feed", "Venue", "Club Lobby")) }
    var isShareLocationEnabled by remember { mutableStateOf(true) }
    var isUploadFinishedSuccess by remember { mutableStateOf(false) }

    // Screen Flash effect (Photo Capture trigger)
    var showFlashOverlay by remember { mutableStateOf(false) }

    // Viewer count is intentionally NOT simulated.
    //
    // This previously ran `watcherCount += (-15..20).random()` on a timer and
    // displayed an invented "2,410 watching" to the broadcaster. With no
    // streaming transport there is no audience, so that was fabricated social
    // proof shown to the creator about their own broadcast.
    //
    // `watcherCount` now stays at its real value (0) until a streaming backend
    // reports actual viewers. See docs/LIVE_ARCHITECTURE.md.
    LaunchedEffect(isBroadcasting) {
        if (isBroadcasting) peakViewers = maxOf(peakViewers, watcherCount)
    }

    // Sound Aware Engine: real on-device audio analysis. Previously this block
    // assigned a random integer (e.g. (120..128).random()) and never opened the
    // microphone, so the "SOUND AWARE: n BPM" readout was fabricated.
    val soundAware = remember { SoundAwareEngine(context) }
    val soundState by soundAware.state.collectAsState()

    DisposableEffect(Unit) {
        soundAware.start(coroutineScope)
        onDispose { soundAware.stop() }
    }

    // Null until enough onsets have been observed to be confident.
    val bpmValue: Int? = soundState.bpm

    // Live comments are intentionally NOT simulated.
    //
    // A fixed pool of ten invented messages ("Amanda: This is absolutely
    // crazy!", ...) used to be replayed at random 2.5-5 s intervals to imitate
    // an audience. There is no chat backend, so every one of those was fake.
    // Only genuine local events are shown now.
    LaunchedEffect(isBroadcasting) {
        if (isBroadcasting) {
            liveComments.clear()
            liveComments.add(
                if (detectedVenueName.isNotBlank()) "Recording locally at $detectedVenueName"
                else "Recording locally"
            )
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
        // CAMERA VIEWFINDER (live CameraX preview)
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
            // Real camera feed. Zoom is applied as a preview transform; the
            // look/effect overlays below composite on top of it.
            CameraViewfinder(
                controller = cameraController,
                useFrontCamera = isFrontCamera,
                flashMode = when (flashMode) {
                    "On" -> ImageCapture.FLASH_MODE_ON
                    "Auto" -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                },
                onPermissionResult = { granted -> hasCameraPermission = granted },
                onError = { message ->
                    cameraError = message
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                },
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

            // NOTE: the "Dual Shot" picture-in-picture selfie bubble was removed.
            // It rendered a stock Unsplash portrait of an unrelated person rather
            // than a second camera feed. True simultaneous front+back capture
            // requires CameraX concurrent-camera support, which is only available
            // on a subset of devices and must be feature-detected via
            // ProcessCameraProvider.availableConcurrentCameraInfos before being
            // offered in the UI. Tracked in docs/LAUNCH_READINESS.md.

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
                    // Driven by real measured audio when the mic is live, so the
                    // meter reflects the room instead of looping regardless of
                    // whether any music is playing.
                    val liveBar1 = if (soundState.isListening)
                        (soundState.bassLevel * 6f).coerceIn(0.15f, 1f) else barHeight1
                    val liveBar2 = if (soundState.isListening)
                        (soundState.level * 5f).coerceIn(0.15f, 1f) else barHeight2
                    val liveBar3 = if (soundState.isListening)
                        (soundState.level * 3.5f).coerceIn(0.15f, 1f) else barHeight3
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(liveBar1).background(Color(0xFFFF2D55)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(liveBar2).background(Color(0xFF00FF7F)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(liveBar3).background(Color(0xFF00F0FF)))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        !soundState.isListening -> "SOUND AWARE: mic off"
                        bpmValue == null -> "SOUND AWARE: listening..."
                        else -> "SOUND AWARE: $bpmValue BPM"
                    },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // CAMERA ERROR BANNER
            // cameraError was previously assigned in six places but never
            // rendered, so hardware failures were invisible once the Toast
            // faded. Surface it persistently until the next successful action.
            cameraError?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 96.dp, start = 20.dp, end = 20.dp)
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.92f), RoundedCornerShape(12.dp))
                        .clickable { cameraError = null }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
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
                                    text = detectedVenueName.ifBlank { "No venue" },
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                // Real detection state and a computed confidence
                                // score. This previously read a hardcoded
                                // "Amapiano Fridays - Confidence: 99%" no matter
                                // where the device actually was.
                                Text(
                                    text = venueSubtitle,
                                    color = if (venueNeedsAttention) Color(0xFFFF9F43) else Color(0xFFFFD700),
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

                                // Real detection result, with the spec's
                                // low-confidence fallback: Nearby / Search / Skip.
                                when (val st = venueState) {
                                    is VenueIntelligence.VenueState.Identified -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text(st.match.venueName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(
                                                    "${st.match.distanceMetres.roundToInt()} m away • ${st.match.confidencePercent}% match",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    is VenueIntelligence.VenueState.LowConfidence -> {
                                        Text(
                                            "Not sure which venue you're at. Pick one:",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 11.sp
                                        )
                                        st.candidates.forEach { candidate ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        manualVenueName = candidate.venueName
                                                        isVenuePillExpanded = false
                                                    }
                                                    .padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(candidate.venueName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${candidate.distanceMetres.roundToInt()} m",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        TextButton(onClick = {
                                            manualVenueName = ""
                                            isVenuePillExpanded = false
                                        }) {
                                            Text("Skip - don't attach a venue", color = Color(0xFFFF9F43), fontSize = 11.sp)
                                        }
                                    }

                                    is VenueIntelligence.VenueState.PermissionDenied -> {
                                        Text(
                                            "Location is off, so FOMO can't detect your venue.",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 11.sp
                                        )
                                        TextButton(onClick = {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }) {
                                            Text("Enable location", color = Color(0xFFFF2D55), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    is VenueIntelligence.VenueState.Detecting -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                color = Color(0xFFFF2D55),
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Detecting venue...", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                        }
                                    }

                                    else -> {
                                        Text(
                                            "No venue detected nearby.",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 11.sp
                                        )
                                        TextButton(onClick = {
                                            coroutineScope.launch { venueState = VenueIntelligence.detect(context) }
                                        }) {
                                            Text("Retry detection", color = Color(0xFFFF2D55), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
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
                                            Toast.makeText(context, "Routing to ${detectedVenueName.ifBlank { "the venue" }}...", Toast.LENGTH_SHORT).show()
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

        // FLIP CAMERA (front / back). Previously this icon only toggled the
        // decorative "dual shot" overlay and never changed lens.
        IconButton(
            onClick = {
                if (isRecordingVideo || isBroadcasting) {
                    Toast.makeText(context, "Can't flip the camera while recording.", Toast.LENGTH_SHORT).show()
                } else {
                    isFrontCamera = !isFrontCamera
                }
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
                contentDescription = if (isFrontCamera) "Switch to rear camera" else "Switch to front camera",
                tint = Color.White,
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
            // Horizontal Mode Selector (PHOTO, VIDEO, LIVE).
            // The spec requires swiping horizontally to switch modes; only
            // tapping was wired up before.
            val modes = listOf("PHOTO", "VIDEO", "LIVE")
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .pointerInput(isRecordingVideo, isBroadcasting) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            // Mode changes are unsafe mid-capture.
                            if (isRecordingVideo || isBroadcasting) return@detectHorizontalDragGestures
                            if (kotlin.math.abs(dragAmount) < 12f) return@detectHorizontalDragGestures
                            val index = modes.indexOf(selectedMode)
                            // Drag left -> advance, drag right -> go back.
                            val next = if (dragAmount < 0) index + 1 else index - 1
                            if (next in modes.indices) selectedMode = modes[next]
                        }
                    }
            ) {
                modes.forEach { mode ->
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
                                    if (!hasCameraPermission) {
                                        Toast.makeText(context, "Allow camera access to capture a moment.", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    cameraError = null
                                    coroutineScope.launch {
                                        showFlashOverlay = true
                                        delay(100)
                                        showFlashOverlay = false

                                        isProcessing = true
                                        processingStep = 1

                                        // Real capture to the device gallery.
                                        val result = cameraController.capturePhoto()
                                        processingStep = 2

                                        result.fold(
                                            onSuccess = { uri ->
                                                processingStep = 3
                                                delay(250)
                                                processingStep = 4
                                                delay(200)
                                                isProcessing = false
                                                capturedMediaUri = uri
                                                capturedIsVideo = false
                                                capturedPhotoUrl = uri.toString()
                                                showPublishPreviewScreen = true
                                            },
                                            onFailure = { error ->
                                                isProcessing = false
                                                processingStep = 0
                                                val msg = error.message ?: "Couldn't capture the photo."
                                                cameraError = msg
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                                "VIDEO" -> {
                                    if (!hasCameraPermission) {
                                        Toast.makeText(context, "Allow camera access to record video.", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    if (isRecordingVideo) {
                                        // Stop the real recording. The saved Uri is
                                        // delivered asynchronously via the callback
                                        // registered in startRecording().
                                        isRecordingVideo = false
                                        isProcessing = true
                                        processingStep = 1
                                        cameraController.stopRecording()
                                    } else {
                                        val started = cameraController.startRecording { result ->
                                            result.fold(
                                                onSuccess = { uri ->
                                                    isProcessing = false
                                                    processingStep = 0
                                                    capturedMediaUri = uri
                                                    capturedIsVideo = true
                                                    capturedPhotoUrl = uri.toString()
                                                    showPublishPreviewScreen = true
                                                },
                                                onFailure = { error ->
                                                    isProcessing = false
                                                    processingStep = 0
                                                    isRecordingVideo = false
                                                    val msg = error.message ?: "Couldn't save the video."
                                                    cameraError = msg
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                        started.fold(
                                            onSuccess = { isRecordingVideo = true },
                                            onFailure = { error ->
                                                val msg = error.message ?: "Couldn't start recording."
                                                cameraError = msg
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                                "LIVE" -> {
                                    // NOTE: real-time broadcast to viewers requires
                                    // streaming infrastructure (RTMP/WebRTC ingest +
                                    // CDN) that does not exist yet - see
                                    // docs/LAUNCH_READINESS.md. What IS real here is
                                    // the capture: the session is recorded on-device
                                    // and published as a genuine replay rather than
                                    // a hardcoded stock photo.
                                    if (isBroadcasting) {
                                        isBroadcasting = false
                                        isProcessing = true
                                        processingStep = 1
                                        cameraController.stopRecording()
                                    } else {
                                        if (!hasCameraPermission) {
                                            Toast.makeText(context, "Allow camera access to go live.", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }
                                        // Query the real device state before offering to broadcast.
                                        readinessReport = LiveReadiness.evaluate(
                                            context = context,
                                            venueIdentified = detectedVenueName.isNotBlank()
                                        )
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
                        // "REC" not "LIVE": nothing is being transmitted to
                        // viewers yet, so claiming LIVE would misrepresent the
                        // broadcast state to the creator.
                        Text(
                            text = if (watcherCount > 0) "LIVE" else "REC",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
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
                        // Honest state: with no streaming transport wired up
                        // there is no audience to count, so we say so rather
                        // than displaying an invented figure.
                        Text(
                            text = if (watcherCount > 0) "$watcherCount WATCHING" else "REC · LOCAL",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
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
        // CRASH RECOVERY  ("Recovered Live Found" - spec requirement)
        // A session left in RECORDING state means the process died mid-
        // broadcast. Previously the recording was simply orphaned.
        // -------------------------------------------------------------
        recoverableSession?.let { session ->
            AlertDialog(
                onDismissRequest = { /* deliberate: force an explicit choice */ },
                containerColor = Color(0xFF141414),
                title = {
                    Text(
                        "Recovered Live found",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "FOMO closed unexpectedly during a Live" +
                                (if (session.venueName.isNotBlank()) " at ${session.venueName}" else "") + ".",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                        Text(
                            "Recorded ${LiveReadiness.formatDuration(session.durationSeconds)} before the interruption.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        liveSessionStore.recover(session.id, session.localUri)
                        session.localUri?.let { uri ->
                            capturedMediaUri = android.net.Uri.parse(uri)
                            capturedIsVideo = true
                            capturedPhotoUrl = uri
                            showPublishPreviewScreen = true
                        }
                        recoverableSession = null
                    }) {
                        Text("Recover", color = Color(0xFFFF2D55), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        liveSessionStore.delete(session.id)
                        recoverableSession = null
                    }) {
                        Text("Delete", color = Color.White.copy(alpha = 0.6f))
                    }
                }
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
                            text = if (readinessReport?.canGoLive == false)
                                "Some requirements aren't met yet."
                            else
                                "Checking camera, mic, network, GPS, storage, battery and temperature...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        // Real device diagnostics. Every row used to be
                        // hardcoded `to true`, so this dialog showed all-green
                        // on a device with a full disk or no permissions.
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            readinessReport?.checks?.forEach { check ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            check.label,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            check.detail,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = when (check.severity) {
                                            LiveReadiness.Severity.PASS -> Icons.Default.CheckCircle
                                            LiveReadiness.Severity.WARN -> Icons.Default.Warning
                                            LiveReadiness.Severity.BLOCK -> Icons.Default.Cancel
                                        },
                                        contentDescription = check.severity.name,
                                        tint = when (check.severity) {
                                            LiveReadiness.Severity.PASS -> Color(0xFF32C759)
                                            LiveReadiness.Severity.WARN -> Color(0xFFFF9F43)
                                            LiveReadiness.Severity.BLOCK -> Color(0xFFFF3B30)
                                        },
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
                                enabled = readinessReport?.canGoLive == true,
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

                                        // Record the session for real so the
                                        // published replay is genuine footage.
                                        // Local-first: register the session before
                                        // recording so an unclean shutdown is
                                        // detectable and recoverable next launch.
                                        val sessionId = "live_${System.currentTimeMillis()}"
                                        val started = cameraController.startRecording { result ->
                                            result.fold(
                                                onSuccess = { uri ->
                                                    isProcessing = false
                                                    processingStep = 0
                                                    liveSessionStore.markEnded(sessionId, uri.toString(), peakViewers)
                                                    // Keep the id so the publish
                                                    // step can complete this
                                                    // session's lifecycle.
                                                    activeLiveSessionId = sessionId
                                                    capturedMediaUri = uri
                                                    capturedIsVideo = true
                                                    capturedPhotoUrl = uri.toString()
                                                    showPublishPreviewScreen = true
                                                },
                                                onFailure = { error ->
                                                    isProcessing = false
                                                    processingStep = 0
                                                    isBroadcasting = false
                                                    val msg = error.message ?: "Couldn't save the replay."
                                                    liveSessionStore.markFailed(sessionId, msg)
                                                    activeLiveSessionId = null
                                                    cameraError = msg
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                        started.fold(
                                            onSuccess = {
                                                isBroadcasting = true
                                                peakViewers = 0
                                                activeLiveSessionId = sessionId
                                                liveSessionStore.startSession(
                                                    id = sessionId,
                                                    venueName = detectedVenueName
                                                )
                                                Toast.makeText(context, "Recording your live session.", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { error ->
                                                val msg = error.message ?: "Couldn't start the live session."
                                                cameraError = msg
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        )
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
                        text = "Anchor: ${detectedVenueName.ifBlank { "No venue" }}",
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
                            // Recommendation derived from what the mic actually
                            // hears, rather than a fixed "128 BPM" string.
                            Text(
                                text = when {
                                    !soundState.isListening ->
                                        "Enable microphone access to get music-matched Look suggestions."
                                    bpmValue == null ->
                                        "Listening for a beat..."
                                    soundState.bassLevel > 0.12f ->
                                        "Heavy bass detected ($bpmValue BPM). Suggested: Bass Shake + Pulse."
                                    bpmValue >= 128 ->
                                        "Fast tempo detected ($bpmValue BPM). Suggested: Strobe Sync + Neon."
                                    bpmValue >= 100 ->
                                        "Steady groove detected ($bpmValue BPM). Suggested: Pulse + Light Trails."
                                    else ->
                                        "Relaxed tempo detected ($bpmValue BPM). Suggested: Glow + Bokeh."
                                },
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
                            // Coil renders a frame from a local video Uri as well
                            // as a still image, so this works for both modes.
                            AsyncImage(
                                model = capturedMediaUri ?: capturedPhotoUrl,
                                contentDescription = if (capturedIsVideo) "Captured video preview" else "Captured photo preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (capturedIsVideo) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
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
                                Text("Add '${detectedVenueName.ifBlank { "venue" }}' landmark and badge tags", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
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
                                val localUri = capturedMediaUri
                                if (localUri == null) {
                                    Toast.makeText(context, "Nothing captured to publish.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isPublishing = true
                                publishStep = 1
                                coroutineScope.launch {
                                    // Upload the real captured file to object
                                    // storage. Publishing the local content:// Uri
                                    // (the previous behaviour) produced moments that
                                    // only rendered on the capturing device.
                                    publishStep = 2
                                    activeLiveSessionId?.let { liveSessionStore.markUploading(it) }
                                    val upload = MediaUploader.uploadMoment(
                                        localUri = localUri,
                                        isVideo = capturedIsVideo,
                                        onProgress = { fraction ->
                                            uploadProgress = fraction
                                            publishStep = if (fraction < 0.99f) 2 else 3
                                        }
                                    )

                                    upload.fold(
                                        onSuccess = { downloadUrl ->
                                            publishStep = 4
                                            val formattedCaption =
                                                captionText.ifEmpty {
                                                    if (isShareLocationEnabled) "Vibing at $detectedVenueName" else "Tonight"
                                                }

                                            // Honour the author's publish settings.
                                            // These were previously collected and
                                            // then discarded: a moment marked
                                            // "Private" with "Hide Venue" was still
                                            // posted publicly with the venue attached.
                                            val destinations = selectedDestinations.value

                                            if (destinations.contains("Profile Story")) {
                                                MyCircleRepository.addStory(
                                                    userName = "You",
                                                    mediaUrl = downloadUrl,
                                                    text = formattedCaption,
                                                    type = if (selectedMode == "LIVE") "Live" else "Story"
                                                )
                                            }
                                            FeedRepository.addMoment(
                                                username = "You",
                                                avatarUrl = currentUserAvatarUrl,
                                                momentType = if (capturedIsVideo) "VIDEO" else "PHOTO",
                                                mediaUrl = downloadUrl,
                                                captionOriginal = formattedCaption,
                                                locationName = if (isShareLocationEnabled) detectedVenueName else "",
                                                visibility = selectedVisibility,
                                                destinations = destinations,
                                                isVenueShared = isShareLocationEnabled
                                            )
                                            activeLiveSessionId?.let {
                                                liveSessionStore.markPublished(it, downloadUrl)
                                                activeLiveSessionId = null
                                            }
                                            isPublishing = false
                                            isUploadFinishedSuccess = true
                                        },
                                        onFailure = { error ->
                                            isPublishing = false
                                            publishStep = 0
                                            uploadProgress = 0f
                                            val msg = error.message ?: "Couldn't publish your moment."
                                            // Keep it in the queue so the replay
                                            // is never lost on a failed upload.
                                            activeLiveSessionId?.let { liveSessionStore.markFailed(it, msg) }
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
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
                        // Real upload progress reported by Firebase Storage.
                        // Indeterminate until the first progress callback lands.
                        if (uploadProgress > 0f) {
                            CircularProgressIndicator(
                                progress = { uploadProgress.coerceIn(0f, 1f) },
                                color = Color(0xFFFF2D55),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                color = Color(0xFFFF2D55),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Text("UPLOADING MOMENT...", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(
                            text = if (uploadProgress > 0f) {
                                "${(uploadProgress.coerceIn(0f, 1f) * 100).toInt()}% uploaded"
                            } else {
                                "Preparing your capture..."
                            },
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        // Step-by-step Upload Engine Checkpoints
                        val uploadStages = listOf(
                            "Uploading HDR media assets safely..." to 1,
                            (if (isShareLocationEnabled && detectedVenueName.isNotBlank())
                                "Adding venue anchoring to $detectedVenueName..."
                             else "Preparing moment metadata...") to 2,
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
                            text = buildString {
                                append("Your moment has been uploaded successfully.")
                                if (isShareLocationEnabled && detectedVenueName.isNotBlank()) {
                                    append(" It is pinned to $detectedVenueName.")
                                }
                                append(" Published to: ")
                                append(selectedDestinations.value.joinToString(", "))
                                append(".")
                            },
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
                                    Text(
                                        if (detectedVenueName.isNotBlank()) "+25 Ripple Points earned at $detectedVenueName"
                                        else "+25 Ripple Points earned",
                                        color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp
                                    )
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
