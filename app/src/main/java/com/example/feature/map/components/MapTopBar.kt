package com.example.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Top HUD Bar matching the mockup exactly:
 * - Avatar with purple ring & crown badge
 * - FOMO Score (🔥 92%)
 * - Search & Notification rounded square action buttons
 */
@Composable
fun MapTopBar(
    avatarUrl: String,
    fomoScore: Int = 92,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Profile Avatar with purple border & Crown Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onAvatarClick() }
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "User profile options",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF8B5CF6), CircleShape)
                )

                // Purple crown badge
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👑", fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // FOMO Score block
            Column {
                Text(
                    text = "FOMO Score",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$fomoScore%",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Action Buttons: Search & Notifications (Rounded Squares)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Search button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131722).copy(alpha = 0.9f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Universal Search",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Notification bell with purple dot badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131722).copy(alpha = 0.9f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable { onNotificationsClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6))
                    )
                }
            }
        }
    }
}

