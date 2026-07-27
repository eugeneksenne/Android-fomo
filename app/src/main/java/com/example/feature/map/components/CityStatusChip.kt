package com.example.feature.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The pulsing "city vibe" status badge shown centered in [MapTopBar].
 *
 * Extracted verbatim from the pre-refactor `MapTopBar`'s inline `Surface` -
 * same red pulse dot, same pill shape, same uppercase label styling. Tapping
 * it cycles through the city status list (handled by the caller via
 * [onClick]; see `MapScreenState.cycleCityStatus`).
 */
@Composable
fun CityStatusChip(
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F1524).copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color(0xFFFF2D55).copy(alpha = 0.45f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFFFF2D55), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status.uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
