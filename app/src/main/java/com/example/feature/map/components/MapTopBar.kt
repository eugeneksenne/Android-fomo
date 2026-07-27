package com.example.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * The Map screen's floating top HUD bar: profile avatar, the [CityStatusChip]
 * vibe badge, and the search/notifications action toolbar.
 *
 * Extracted verbatim from `MapScreen.kt`'s inline `MapTopBar` composable -
 * same layout, spacing, colors and accessibility labels, only the city
 * status badge markup moved into [CityStatusChip] for reuse/testability.
 */
@Composable
fun MapTopBar(
    avatarUrl: String,
    cityStatus: String,
    onCityStatusClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Profile Avatar with custom border
        AsyncImage(
            model = avatarUrl,
            contentDescription = "User profile options",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF00E5FF), CircleShape)
                .clickable { onAvatarClick() }
        )

        // Center Pulsing dynamic context status badge
        CityStatusChip(status = cityStatus, onClick = onCityStatusClick)

        // Action Toolbar matching accessibility targets (48.dp area via padding)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F1524).copy(alpha = 0.9f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = "Universal Search", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F1524).copy(alpha = 0.9f))
                    .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), CircleShape)
                    .clickable { onNotificationsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Flash Drop Alerts", tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
            }
        }
    }
}
