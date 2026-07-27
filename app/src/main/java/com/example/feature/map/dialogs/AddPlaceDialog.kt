package com.example.feature.map.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.data.ExploreVenue

/**
 * The Map screen's "Add to FOMO" dialog: three tabs (Venue / Event / Temp
 * Party) that each generate a session-scoped [ExploreVenue] and hand it back
 * via [onSubmitVenue].
 *
 * Extracted verbatim from `MapScreen.kt`'s inline `AddPlaceOverlayDialog`
 * composable - same three tabs, same generated venue defaults (rating 4.8,
 * "🎁 Dynamic Pin" attributes, `evt_<timestamp>` id). The tab forms
 * themselves live in [AddPlaceFormTab] to keep this root composable under
 * the Map architecture's 250-line guideline.
 */
@Composable
fun AddPlaceOverlayDialog(
    onClose: () -> Unit,
    onSubmitVenue: (ExploreVenue) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Venue, 1 = Event, 2 = Temp Party
    val formState = remember { AddPlaceFormState() }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            color = Color(0xFF0F1524),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add to FOMO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close add form", tint = Color.White)
                    }
                }

                AddPlaceTabSelector(activeTab = activeTab, onTabSelected = { activeTab = it })

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AddPlaceFormTab(activeTab = activeTab, formState = formState)
                }

                Button(
                    onClick = {
                        val generatedVenue = buildGeneratedVenue(activeTab, formState)
                        if (generatedVenue != null) {
                            onSubmitVenue(generatedVenue)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Post Live to Map Radar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AddPlaceTabSelector(activeTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("Venue", "Event", "Temp Party").forEachIndexed { index, title ->
            val active = activeTab == index
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .background(if (active) Color(0xFF00E5FF) else Color(0xFF151D30), RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(title, color = if (active) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Builds the session-scoped [ExploreVenue] for whichever tab is active, or
 * `null` when the required name field is still empty (mirrors the original
 * inline `if (finalName.isNotEmpty())` guard).
 */
private fun buildGeneratedVenue(activeTab: Int, formState: AddPlaceFormState): ExploreVenue? {
    val finalName = when (activeTab) {
        0 -> formState.name
        1 -> formState.eventTitle
        else -> formState.partyName
    }
    if (finalName.isEmpty()) return null

    val finalSub = when (activeTab) {
        0 -> formState.subcategory
        1 -> "Special Event Hosted tonight"
        else -> "Temporary Secret Party"
    }
    val finalCat = when (activeTab) {
        0 -> formState.categorySelect
        1 -> "Events"
        else -> "Nightlife"
    }

    val newId = "evt_${System.currentTimeMillis()}"
    return ExploreVenue(
        id = newId,
        name = finalName,
        category = finalCat,
        subcategory = finalSub,
        imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop",
        isVerified = true,
        rating = 4.8f,
        reviewCount = 10,
        address = formState.address.ifEmpty { "Johannesburg" },
        area = "Sandton",
        distanceText = "0.5 km away",
        attributes = listOf("🎁 Dynamic Pin", "🟢 Active", "Simulated"),
        openDays = "Active Now",
        startHour = 18,
        endHour = 4,
        is24Hours = false,
        websiteUrl = "https://fomoapp.live",
        hasClubLobby = true
    )
}
