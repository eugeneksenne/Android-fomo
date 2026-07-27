package com.example.feature.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Map screen's horizontally-scrolling category chip selector
 * ("All", "🌙 Nightlife", "🍔 Food", "✨ Prep", "☕ Wellness", "✈ Travel",
 * "🎫 Events"). Selecting a chip drives both the venue-list filter
 * ([com.example.feature.map.util.VenueFilter]) and the Leaflet map's own
 * `filterCategory` / `fetchOverpassPOIs` JS calls.
 *
 * Extracted verbatim from `MapScreen.kt`'s inline `CategoryChips` composable;
 * file name matches the Map architecture doc's `CountryPackChips` slot since
 * these categories are backed by the same Country Pack taxonomy
 * (Nightlife/Food/Prep/Wellness/Travel) used in `CountryPackRepository`.
 */
@Composable
fun CountryPackChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("All", "🌙 Nightlife", "🍔 Food", "✨ Prep", "☕ Wellness", "✈ Travel", "🎫 Events")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            Surface(
                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF0F1524).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable { onCategorySelected(category) }
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}
