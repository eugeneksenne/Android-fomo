package com.example.feature.map.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import com.example.core.data.CircleFriend

/**
 * The compact friend preview card shown above the bottom carousel sheet when
 * a friend pin is selected on the map.
 *
 * Extracted verbatim from `MapScreen.kt`'s inline `FriendDetailsPanel`
 * composable. Hosted by [com.example.feature.map.overlays.VenuePreviewOverlay]
 * alongside [VenueDetailsPanel] since both render into the same "selected
 * pin" slot in the Map screen layout.
 */
@Composable
fun FriendDetailsPanel(friend: CircleFriend, onClose: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = Color(0xFF0F1524).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFF32D74B).copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (friend.status == "Online") "ONLINE" else "OFFLINE", color = Color(0xFF32D74B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close friend details", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = friend.avatarUrl,
                    contentDescription = friend.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(friend.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(friend.username, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text("🎵 ${friend.currentActivity}", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
            }
        }
    }
}
