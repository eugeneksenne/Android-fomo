package com.example.feature.discover

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import java.util.Calendar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage


@Composable
fun ImmersiveStoryViewer(
    stories: List<com.example.core.data.CircleStory>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onNavigateToLobby: (String) -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val currentStory = stories.getOrNull(currentIndex) ?: return
    BackHandler { onDismiss() }
    val context = LocalContext.current

    remember(currentIndex) {
        com.example.core.data.MyCircleRepository.markStoryViewed(currentStory.id)
        Unit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {}
            .testTag("story_viewer_container")
    ) {
        AsyncImage(
            model = currentStory.mediaUrl,
            contentDescription = "Story Media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (currentIndex > 0) currentIndex-- else onDismiss()
                    }
                    .testTag("story_nav_prev")
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (currentIndex < stories.size - 1) currentIndex++ else onDismiss()
                    }
                    .testTag("story_nav_next")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { idx, _ ->
                    val progressColor = if (idx < currentIndex) {
                        Color.White
                    } else if (idx == currentIndex) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.3f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(progressColor, RoundedCornerShape(1.5.dp))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = currentStory.userAvatar,
                    contentDescription = currentStory.userName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentStory.userName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = currentStory.timestamp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val emoji = when (currentStory.badgeText) {
                            "Story" -> "📸"
                            "Live" -> "🔴"
                            "Event" -> "🎉"
                            "Venue" -> "📍"
                            "Close Friends" -> "⭐"
                            else -> "📸"
                        }
                        Text("$emoji ${currentStory.badgeText}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("story_viewer_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        if (currentStory.musicPlaying != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Music",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentStory.musicPlaying,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            if (currentStory.venueName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentStory.venueName,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            } else if (currentStory.eventName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentStory.eventName,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStory.venueId != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToLobby(currentStory.venueId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Enter Lobby", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            openRoute(currentStory.venueName ?: "Johannesburg", context)
                        },
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Get Route")
                    }
                } else if (currentStory.eventName != null) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Opening Event: ${currentStory.eventName}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Event", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Ticket Purchase Initiated!", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Buy Ticket")
                    }
                } else if (currentStory.storyType == "Live") {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Entering Live Broadcaster Channel...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Watch Live Broadcaster", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Message sent to ${currentStory.userName}!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Quick Reply", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Added to Close Friends!", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Star ⭐")
                    }
                }
            }
        }
    }
}
