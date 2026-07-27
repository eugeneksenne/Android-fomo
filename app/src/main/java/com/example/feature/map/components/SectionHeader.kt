package com.example.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.map.state.MapBottomTab

/**
 * Header row for the Map screen's bottom carousel sheet: the "🔥 Live Spots"
 * / "👥 Friends Map" segmented tab switch plus the live online-friends count
 * badge.
 *
 * Extracted verbatim from the inline `Row` at the top of `MapScreen`'s
 * bottom `Surface` sheet, including the underline-indicator tab style
 * (`SegmentedTabButton`) and the green "N LIVE" pill that only appears when
 * at least one friend is online.
 */
@Composable
fun SectionHeader(
    selectedTab: MapBottomTab,
    onTabSelected: (MapBottomTab) -> Unit,
    onlineFriendsCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MapBottomTab.entries.forEach { tab ->
                SegmentedTabButton(
                    title = tab.label,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }

        if (onlineFriendsCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF32D74B).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF32D74B), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("$onlineFriendsCount LIVE", color = Color(0xFF32D74B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * A single underline-style segmented tab button used by [SectionHeader].
 * Extracted verbatim from `MapScreen.kt`'s inline `SegmentedTabButton`.
 */
@Composable
fun SegmentedTabButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(2.5.dp)
                .background(if (isSelected) Color(0xFF00E5FF) else Color.Transparent)
        )
    }
}
