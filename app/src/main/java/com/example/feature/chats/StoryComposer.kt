package com.example.feature.chats

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.data.story.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PrivacyOption(
    val privacy: StoryPrivacy,
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryComposerModal(
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Privacy selection state
    val privacyOptions = remember {
        listOf(
            PrivacyOption(StoryPrivacy.PUBLIC, "Everyone", "Anyone on FOMO"),
            PrivacyOption(StoryPrivacy.MY_CIRCLE, "My Circle", "Only your close circle"),
            PrivacyOption(StoryPrivacy.FOLLOWERS_ONLY, "Followers", "Only people following you")
        )
    }
    var selectedPrivacy by remember { mutableStateOf(privacyOptions[0]) }
    var showPrivacyMenu by remember { mutableStateOf(false) }

    // Sample images
    val sampleImages = remember {
        listOf(
            "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800", // Snowy Mountain Lake matching screenshot
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=800",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=800",
            "https://images.unsplash.com/photo-1519681393784-d120267933ba?q=80&w=800"
        )
    }
    var currentImageIndex by remember { mutableIntStateOf(0) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Pick photo from gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            capturedBitmap = null
        }
    }

    // Take camera photo launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUri = null
        }
    }

    // Publishing state
    var isPublishing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Main Bottom Sheet Content Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFF13121B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Header ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "Add Story",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // --- Camera & Gallery Cards Row ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Camera Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .clickable {
                                    try {
                                        cameraLauncher.launch(null)
                                    } catch (e: Exception) {
                                        // Fallback: switch sample photo
                                        currentImageIndex = (currentImageIndex + 1) % sampleImages.size
                                        selectedImageUri = null
                                        capturedBitmap = null
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1B1A26),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF381A5E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Camera",
                                        tint = Color(0xFFC084FC),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Camera",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Gallery Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .clickable {
                                    try {
                                        galleryLauncher.launch("image/*")
                                    } catch (e: Exception) {
                                        // Fallback: switch sample photo
                                        currentImageIndex = (currentImageIndex + 1) % sampleImages.size
                                        selectedImageUri = null
                                        capturedBitmap = null
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1B1A26),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF381A5E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Collections,
                                        contentDescription = "Gallery",
                                        tint = Color(0xFFC084FC),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Gallery",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // --- STORY PREVIEW Section Header ---
                    Text(
                        text = "STORY PREVIEW",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    // --- Circular Image Preview with Dashed Border ---
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .clickable {
                                // Tap preview circle to cycle sample image
                                currentImageIndex = (currentImageIndex + 1) % sampleImages.size
                                selectedImageUri = null
                                capturedBitmap = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Dashed circle outline
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 2.dp.toPx()
                            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.45f),
                                radius = (size.minDimension / 2) - strokeWidth,
                                style = Stroke(width = strokeWidth, pathEffect = dashPathEffect)
                            )
                        }

                        // Circular Cropped Image
                        Box(
                            modifier = Modifier
                                .size(212.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray)
                        ) {
                            if (capturedBitmap != null) {
                                AsyncImage(
                                    model = capturedBitmap,
                                    contentDescription = "Captured Story Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Story Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = sampleImages[currentImageIndex],
                                    contentDescription = "Story Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Your story will appear for 24 hours",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // --- Privacy Dropdown Card ---
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPrivacyMenu = true },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1B1A26),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF381A5E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = Color(0xFFC084FC),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = selectedPrivacy.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = selectedPrivacy.subtitle,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Privacy",
                                    tint = Color.Gray
                                )
                            }
                        }

                        // Privacy Options Dropdown Menu
                        DropdownMenu(
                            expanded = showPrivacyMenu,
                            onDismissRequest = { showPrivacyMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF1F1E2E))
                        ) {
                            privacyOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = option.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = option.subtitle,
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPrivacy = option
                                        showPrivacyMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // --- Share Story Action Button ---
                    Button(
                        onClick = {
                            isPublishing = true
                            coroutineScope.launch {
                                delay(1000)
                                val finalMediaUrl = if (selectedImageUri != null) {
                                    selectedImageUri.toString()
                                } else {
                                    sampleImages[currentImageIndex]
                                }
                                StoryRepository.publishNewStorySegment(
                                    mediaUrl = finalMediaUrl,
                                    locationName = "FOMO Live Feed",
                                    filterName = "Normal",
                                    stickers = emptyList(),
                                    privacy = selectedPrivacy.privacy
                                )
                                isPublishing = false
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B21A8)
                        )
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sharing Story...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Text(
                                text = "Share Story",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
