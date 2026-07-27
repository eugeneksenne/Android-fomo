package com.example.feature.discover

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage


@Composable
fun ChannelsSection(onOpenClick: () -> Unit) {
    Column {
        SectionHeader(
            title = "Channels",
            subtitle = "Live nightlife operating system",
            actionText = "See all",
            onActionClick = onOpenClick
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(4) { index ->
                val (title, members, emoji, bgImg) = when (index) {
                    0 -> Quadruple("Johannesburg Nights", "24.2K Active", "🌆", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=400&auto=format&fit=crop")
                    1 -> Quadruple("Cape Town After Dark", "18.9K Active", "🌊", "https://images.unsplash.com/photo-1576485375217-d6a95e34d043?q=80&w=400&auto=format&fit=crop")
                    2 -> Quadruple("Konka Club Channel", "3.4K Inside", "🔥", "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=400&auto=format&fit=crop")
                    else -> Quadruple("And Club Channel", "1.8K Raving", "⚡", "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=400&auto=format&fit=crop")
                }
                ChannelCard(
                    title = title,
                    membersText = members,
                    emoji = emoji,
                    bgImage = bgImg,
                    onClick = onOpenClick
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ChannelCard(
    title: String,
    membersText: String,
    emoji: String,
    bgImage: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .width(170.dp)
            .height(130.dp)
            .semantics {
                contentDescription = "$title channel, $membersText"
                role = Role.Button
            }
            .clickable {
                DiscoverAnalytics.cardOpened("channels", title.lowercase().replace(" ", "_"), "channel")
                onClick()
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = bgImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(emoji, fontSize = 22.sp)
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(membersText, color = Color(0xFF00E5FF), fontWeight = FontWeight.Medium, fontSize = 11.sp)
                }
            }
        }
    }
}
