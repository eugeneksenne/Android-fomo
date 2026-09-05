package com.example.feature.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

data class MapCategoryChipData(
    val id: String,
    val label: String,
    val icon: String
)

@Composable
fun CountryPackChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        MapCategoryChipData("Live", "Live", "((•))"),
        MapCategoryChipData("Nightlife", "Nightlife", "🌙"),
        MapCategoryChipData("Events", "Events", "🏷️"),
        MapCategoryChipData("Prep", "Prep", "🛠️"),
        MapCategoryChipData("Food", "Food", "🍴"),
        MapCategoryChipData("Luxury", "Luxury", "💎")
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            val isSelected = selectedCategory.contains(cat.id, ignoreCase = true) ||
                    (selectedCategory == "All" && cat.id == "Nightlife")

            Surface(
                color = if (isSelected) Color(0xFF26123D) else Color(0xFF111622).copy(alpha = 0.9f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable { onCategorySelected(cat.label) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = cat.icon,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cat.label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

