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
fun EventsSection(
    events: List<com.example.core.data.Event>,
    isOnline: Boolean = true,
    onNavigateToEvents: () -> Unit,
    onNavigateToEventDetails: (String) -> Unit,
    onRetry: () -> Unit = {}
) {
    Column {
        SectionHeader(
            title = "Events",
            subtitle = "Tonight and upcoming",
            actionText = "See all",
            onActionClick = onNavigateToEvents
        )
        when {
            events.isEmpty() && !isOnline -> DiscoverOfflineState(
                message = "Reconnect to load tonight's events.",
                onRetryClick = onRetry
            )
            events.isEmpty() -> DiscoverEmptyState(
                title = "No events loaded",
                message = "Refresh later to discover tonight's upcoming events."
            )
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = events, key = { it.id }) { event ->
                    EventCard(event = event, onCardClick = { onNavigateToEventDetails(event.id) })
                }
            }
        }
    }
}

@Composable
fun EventCard(event: com.example.core.data.Event, onCardClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .semantics {
                contentDescription = "${event.title} at ${event.venueName}. Event card."
                role = Role.Button
            }
            .clickable {
                DiscoverAnalytics.cardOpened("events", event.id, "event")
                onCardClick()
            }
            .testTag("discover_event_card_${event.id}")
    ) {
        AsyncImage(
            model = event.posterUrl,
            contentDescription = event.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 100f
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(event.dateText.take(3).uppercase(), color = Color(0xFFFF2D55), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(if (event.dateText == "Tonight") "NOW" else "SUN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column {
                Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(event.venueName, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}
