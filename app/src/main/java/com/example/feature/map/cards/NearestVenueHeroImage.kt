package com.example.feature.map.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.data.ExploreVenue

/**
 * The left-hand hero photo column of [NearestVenueCard]: full-height venue
 * photo, "HOT TONIGHT" badge, the glowing neon "V" watermark, and the
 * bottom-left adaptive tag stack (music/drink/age or category-appropriate
 * equivalents).
 *
 * Extracted verbatim from `MapScreen.kt`'s inline `NearestVenueCard` left
 * column `Box`. The caller supplies the `.weight(...)` modifier since that
 * only applies inside the parent `Row`'s scope.
 */
@Composable
internal fun NearestVenueHeroImage(
    venue: ExploreVenue,
    normalizedCategory: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.height(165.dp)) {
        // Full-height venue photo
        AsyncImage(
            model = venue.imageUrl,
            contentDescription = venue.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )

        // "HOT TONIGHT" badge top-left
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF4500),
                    modifier = Modifier.size(9.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "HOT TONIGHT",
                    color = Color.White,
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Large glowing neon "V" logo centered in the photo
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp)
                .background(Color(0xFF0C0F19).copy(alpha = 0.75f), CircleShape)
                .border(1.5.dp, Color(0xFFC026D3), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "V",
                color = Color(0xFFF472B6),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }

        // Adaptive Tags bottom-left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val isNightlife = normalizedCategory == "NIGHTLIFE"
            val tag1 = if (isNightlife) "Techno" else if (normalizedCategory == "FOOD") "Gourmet" else "Premium"
            val tag2 = if (isNightlife) "Cocktails" else if (normalizedCategory == "FOOD") "Drinks" else "Booking"
            val tag3 = if (isNightlife) "21+" else if (normalizedCategory == "FOOD") "All Ages" else "Verified"

            HeroTag(icon = Icons.Default.MusicNote, tint = Color(0xFF00E5FF), text = tag1)
            HeroTag(icon = Icons.Default.LocalBar, tint = Color(0xFFFF9500), text = tag2)
            HeroTag(icon = Icons.Default.Person, tint = Color(0xFF32D74B), text = tag3)
        }
    }
}

@Composable
private fun HeroTag(icon: ImageVector, tint: Color, text: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.80f),
        shape = RoundedCornerShape(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(8.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(text, color = Color.White, fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
