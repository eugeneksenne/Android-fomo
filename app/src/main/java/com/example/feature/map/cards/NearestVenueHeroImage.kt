package com.example.feature.map.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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

@Composable
internal fun NearestVenueHeroImage(
    venue: ExploreVenue,
    normalizedCategory: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.height(120.dp)) {
        // Full venue photo
        AsyncImage(
            model = venue.imageUrl,
            contentDescription = venue.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
        )

        // "HOT TONIGHT" badge top-left
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "HOT TONIGHT",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Glowing neon "V" logo centered in the photo
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp)
                .background(Color(0xFF0D0F18).copy(alpha = 0.75f), CircleShape)
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

        // Tags bottom row
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isNightlife = normalizedCategory == "NIGHTLIFE" || normalizedCategory == "ALL"
            val tag1 = if (isNightlife) "Techno" else "Gourmet"
            val tag2 = if (isNightlife) "Cocktails" else "Drinks"
            val tag3 = if (isNightlife) "21+" else "Verified"

            HeroTag(icon = Icons.Default.MusicNote, text = tag1)
            HeroTag(icon = Icons.Default.LocalBar, text = tag2)
            HeroTag(icon = Icons.Default.Person, text = tag3)
        }
    }
}

@Composable
private fun HeroTag(icon: ImageVector, text: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(9.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(text, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

