package com.example.feature.map.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * The Map screen's "Radar Notifications" dialog: a dismissable list of
 * Flash Drop / friend activity / event / NightGuard alerts, each with a
 * "Locate Venue" deep-link back into the map.
 *
 * Extracted verbatim from `MapScreen.kt`'s inline `NotificationsOverlayDialog`
 * composable, including its seeded mock notification list.
 */
@Composable
fun NotificationsOverlayDialog(
    onClose: () -> Unit,
    onLocateVenue: (String) -> Unit
) {
    val initialNotifications = remember {
        mutableStateListOf(
            Triple("fd1", "🎁 FLASH DROP: Welcome tequila shot details at FOMO Club. 12 min left!", "fomo_club"),
            Triple("fd2", "👥 FRIEND ACTIVITY: Sarah checked-in at Taboo Lounge Sandton.", "taboo_sandton"),
            Triple("fd3", "🎫 EVENT LIVE: Amapiano Fridays live stream starts in 30 mins!", "fomo_club"),
            Triple("fd4", "🟢 NIGHTGUARD ESCORT: Safe guards active in Rosebank zone.", "fomo_club")
        )
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f),
            color = Color(0xFF0F1524),
            border = BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.35f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Radar Notifications", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close alerts", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (initialNotifications.isEmpty()) {
                        Text(
                            "You have cleared all alerts.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 40.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        initialNotifications.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.second, color = Color.White, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Locate Venue",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { onLocateVenue(item.third) }
                                    )
                                }
                                IconButton(
                                    onClick = { initialNotifications.remove(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear Alert", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
