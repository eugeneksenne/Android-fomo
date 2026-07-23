package com.example.feature.chats

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.data.story.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryComposerModal(
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Camera viewfinder state
    var isFrontCamera by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Golden Hour") }
    var captionText by remember { mutableStateOf("") }

    // Stickers added
    var activeStickers by remember { mutableStateOf<List<StorySticker>>(emptyList()) }
    var showStickerModal by remember { mutableStateOf(false) }

    // Privacy setting
    var privacy by remember { mutableStateOf(StoryPrivacy.PUBLIC) }

    // Media background captured image
    val capturedMediaUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=600"

    // Publishing state
    var isPublishing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Viewfinder Simulator Feed
            AsyncImage(
                model = capturedMediaUrl,
                contentDescription = "Story Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top Control Bar (Close, Camera Flip, Add Sticker, Filter)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = { isFrontCamera = !isFrontCamera }, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                        Icon(Icons.Default.FlipCameraIos, contentDescription = "Flip", tint = Color.White)
                    }
                    IconButton(onClick = { showStickerModal = true }, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                        Icon(Icons.Default.StickyNote2, contentDescription = "Add Sticker", tint = Color.White)
                    }
                }
            }

            // On-Screen Active Stickers Preview Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                activeStickers.forEach { sticker ->
                    StoryStickerOverlay(sticker = sticker)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Bottom Controls Bar (Filter Selector, Privacy Pill, Publish Button)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                    .align(Alignment.BottomCenter)
            ) {
                // Filter selector row
                val filters = listOf("Normal", "Golden Hour", "Cyber Neon", "Midnight Vibe", "Stage Lights")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(filters) { flt ->
                        val isSelected = selectedFilter == flt
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable { selectedFilter = flt }
                        ) {
                            Text(
                                flt,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Privacy Pill & Publish Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Privacy Selector Button
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable {
                            privacy = when (privacy) {
                                StoryPrivacy.PUBLIC -> StoryPrivacy.MY_CIRCLE
                                StoryPrivacy.MY_CIRCLE -> StoryPrivacy.FOLLOWERS_ONLY
                                else -> StoryPrivacy.PUBLIC
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                when (privacy) {
                                    StoryPrivacy.PUBLIC -> "Public Story"
                                    StoryPrivacy.MY_CIRCLE -> "My Circle Only"
                                    StoryPrivacy.FOLLOWERS_ONLY -> "Followers Only"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Publish Action Button
                    Button(
                        onClick = {
                            isPublishing = true
                            coroutineScope.launch {
                                delay(1200)
                                StoryRepository.publishNewStorySegment(
                                    mediaUrl = capturedMediaUrl,
                                    locationName = "Rosebank Nightlife Zone",
                                    filterName = selectedFilter,
                                    stickers = activeStickers,
                                    privacy = privacy
                                )
                                isPublishing = false
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publishing...")
                        } else {
                            Text("Share Story")
                        }
                    }
                }
            }

            // Sticker Selector Modal
            if (showStickerModal) {
                StickerPickerModal(
                    onDismiss = { showStickerModal = false },
                    onStickerSelected = { sticker ->
                        activeStickers = activeStickers + sticker
                        showStickerModal = false
                    }
                )
            }
        }
    }
}

// Sticker Picker Dialog
@Composable
fun StickerPickerModal(
    onDismiss: () -> Unit,
    onStickerSelected: (StorySticker) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Interactive Sticker", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    StorySticker("st_opt_1", StoryStickerType.VENUE, "Cocoon Club Rosebank", "VIP Guest List Open"),
                    StorySticker("st_opt_2", StoryStickerType.FLASH_DROP, "Free Tequila Voucher", "Claim Voucher"),
                    StorySticker("st_opt_3", StoryStickerType.LOCATION, "Rosebank Rooftop Lounge"),
                    StorySticker("st_opt_4", StoryStickerType.POLL, "Are you coming tonight?", "Vote Yes / No")
                ).forEach { sticker ->
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onStickerSelected(sticker) }
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.StickyNote2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(sticker.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                sticker.subtitle?.let { Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) } },
        containerColor = Color(0xFF1E1E26)
    )
}
