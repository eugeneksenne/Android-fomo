package com.example.feature.chats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.data.story.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerModal(
    userStoryGroup: UserStoryGroup,
    onDismiss: () -> Unit,
    onReplyToChat: (storyUserId: String, storyUserName: String, segment: StoryMediaSegment, replyText: String) -> Unit
) {
    var activeSegmentIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val currentSegment = remember(activeSegmentIndex, userStoryGroup.segments) {
        userStoryGroup.segments.getOrNull(activeSegmentIndex) ?: userStoryGroup.segments.first()
    }

    // Direct Reply Input State
    var replyText by remember { mutableStateOf("") }

    // Floating reaction animations
    var floatingEmojis by remember { mutableStateOf<List<String>>(emptyList()) }

    // Analytics sheet for own story
    var showAnalyticsSheet by remember { mutableStateOf(false) }

    // Overflow context menu
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Mark current segment as viewed
    LaunchedEffect(currentSegment.id) {
        StoryRepository.markSegmentViewed(userStoryGroup.userId, currentSegment.id)
    }

    // 5-Second Timer Progress
    LaunchedEffect(activeSegmentIndex, isPaused) {
        if (!isPaused) {
            progress = 0f
            val stepTimeMs = 50L
            val totalSteps = (currentSegment.durationSeconds * 1000) / stepTimeMs

            for (step in 1..totalSteps) {
                delay(stepTimeMs)
                progress = step.toFloat() / totalSteps.toFloat()
            }

            // Advance segment or dismiss
            if (activeSegmentIndex < userStoryGroup.segments.size - 1) {
                activeSegmentIndex++
            } else {
                onDismiss()
            }
        }
    }

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
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.3f) {
                                if (activeSegmentIndex > 0) activeSegmentIndex--
                            } else {
                                if (activeSegmentIndex < userStoryGroup.segments.size - 1) activeSegmentIndex++ else onDismiss()
                            }
                        }
                    )
                }
        ) {
            // Background Story Media
            AsyncImage(
                model = currentSegment.mediaUrl,
                contentDescription = "Story Media",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Shading Gradients
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    .align(Alignment.TopCenter)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                    .align(Alignment.BottomCenter)
            )

            // On-Screen Interactive Overlay Stickers
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp, bottom = 120.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                currentSegment.stickers.forEach { sticker ->
                    StoryStickerOverlay(sticker = sticker)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Top Header & Progress Bars
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 12.dp, end = 12.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Segment Progress Bars Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    userStoryGroup.segments.forEachIndexed { idx, _ ->
                        val fillRatio = when {
                            idx < activeSegmentIndex -> 1f
                            idx == activeSegmentIndex -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { fillRatio },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                // User Metadata Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = userStoryGroup.userAvatarUrl,
                        contentDescription = userStoryGroup.userName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                userStoryGroup.userName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (userStoryGroup.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                currentSegment.timestampFormatted,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        currentSegment.locationName?.let { loc ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(loc, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                        }
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier.background(Color(0xFF2C2C36))
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (userStoryGroup.isMuted) "Unmute Stories" else "Mute User Stories", color = Color.White) },
                                onClick = {
                                    showOverflowMenu = false
                                    StoryRepository.toggleMuteUserStories(userStoryGroup.userId)
                                }
                            )
                            if (userStoryGroup.isOwnStory) {
                                DropdownMenuItem(
                                    text = { Text("Delete Segment", color = Color.Red) },
                                    onClick = {
                                        showOverflowMenu = false
                                        StoryRepository.deleteOwnStorySegment(currentSegment.id)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Bottom Engagement Bar (Direct Reply + Emoji Pulse / Own Story Views)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                    .align(Alignment.BottomCenter)
            ) {
                // Own Story Viewers Button
                if (userStoryGroup.isOwnStory) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable { showAnalyticsSheet = true }
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${userStoryGroup.totalViewsCount} Views",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // Fast Emoji Reaction Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("🔥", "❤️", "😂", "😮", "🍾", "💯").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clickable {
                                        floatingEmojis = floatingEmojis + emoji
                                        StoryRepository.addReactionToSegment(userStoryGroup.userId, currentSegment.id, emoji)
                                    }
                            )
                        }
                    }

                    // Direct Message Reply Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Reply to ${userStoryGroup.userName}...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                                focusedBorderColor = Color.White.copy(alpha = 0.4f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        if (replyText.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    onReplyToChat(userStoryGroup.userId, userStoryGroup.userName, currentSegment, replyText)
                                    replyText = ""
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Own Story Analytics Sheet
            if (showAnalyticsSheet) {
                StoryViewersAnalyticsSheet(
                    userStoryGroup = userStoryGroup,
                    onDismiss = { showAnalyticsSheet = false }
                )
            }
        }
    }
}

// Overlay Sticker Render Component
@Composable
fun StoryStickerOverlay(sticker: StorySticker) {
    Surface(
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (sticker.type) {
                StoryStickerType.VENUE -> Icons.Default.Storefront
                StoryStickerType.EVENT -> Icons.Default.Event
                StoryStickerType.FLASH_DROP -> Icons.Default.LocalActivity
                StoryStickerType.POLL -> Icons.Default.Poll
                StoryStickerType.LOCATION -> Icons.Default.Place
                StoryStickerType.MUSIC -> Icons.Default.MusicNote
            }
            val iconColor = when (sticker.type) {
                StoryStickerType.FLASH_DROP -> Color(0xFFFFCC00)
                StoryStickerType.LOCATION -> Color(0xFF34C759)
                else -> MaterialTheme.colorScheme.primary
            }

            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(sticker.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                sticker.subtitle?.let {
                    Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
    }
}

// Own Story Analytics Viewers Sheet
@Composable
fun StoryViewersAnalyticsSheet(
    userStoryGroup: UserStoryGroup,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Story Analytics", fontWeight = FontWeight.Bold, color = Color.White)
                Text("${userStoryGroup.totalViewsCount} Total Views", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Box(modifier = Modifier.height(240.dp)) {
                if (userStoryGroup.viewersList.isEmpty()) {
                    Text("No views yet", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(userStoryGroup.viewersList) { viewer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = viewer.userAvatarUrl,
                                        contentDescription = viewer.userName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(36.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(viewer.userName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(viewer.viewedTimestampFormatted, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                    }
                                }

                                viewer.reactionEmoji?.let { emoji ->
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.White) }
        },
        containerColor = Color(0xFF1E1E26)
    )
}
