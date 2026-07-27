package com.example.feature.map.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.data.CircleFriend
import com.example.core.data.ExploreVenue
import com.example.feature.map.state.SelectedMapItem

/**
 * The Map screen's universal search dialog: free-text query across venues
 * and friends, with "All" / "Hotspots" / "Friends" tab filters.
 *
 * Extracted verbatim from `MapScreen.kt`'s inline `SearchOverlayDialog`
 * composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOverlayDialog(
    venues: List<ExploreVenue>,
    friends: List<CircleFriend>,
    onClose: () -> Unit,
    onSelectItem: (SelectedMapItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchTab by remember { mutableStateOf("All") } // "All", "Hotspots", "Friends"

    val filteredVenues = remember(query, venues) {
        if (query.isEmpty()) venues else venues.filter { it.name.contains(query, ignoreCase = true) || it.subcategory.contains(query, ignoreCase = true) }
    }
    val filteredFriends = remember(query, friends) {
        if (query.isEmpty()) friends else friends.filter { it.name.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            color = Color(0xFF0F1524),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Universal Discovery", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close search", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search places, events, people...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = Color(0xFF0A0F19),
                        unfocusedContainerColor = Color(0xFF0A0F19)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tab filters
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("All", "Hotspots", "Friends").forEach { tab ->
                        val active = searchTab == tab
                        Box(
                            modifier = Modifier
                                .background(if (active) Color(0xFF00E5FF) else Color(0xFF151D30), RoundedCornerShape(14.dp))
                                .clickable { searchTab = tab }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(tab, color = if (active) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text("SEARCH RESULTS", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (filteredVenues.isEmpty() && filteredFriends.isEmpty()) {
                            Text(
                                "No matches found. Try typing another venue or friend name.",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Center),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Column {
                                if (searchTab == "All" || searchTab == "Hotspots") {
                                    filteredVenues.take(4).forEach { venue ->
                                        SearchResultVenueRow(venue = venue, onClick = { onSelectItem(SelectedMapItem.Venue(venue)) })
                                    }
                                }
                                if (searchTab == "All" || searchTab == "Friends") {
                                    filteredFriends.take(4).forEach { friend ->
                                        SearchResultFriendRow(friend = friend, onClick = { onSelectItem(SelectedMapItem.Friend(friend)) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultVenueRow(venue: ExploreVenue, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(venue.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(venue.subcategory, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun SearchResultFriendRow(friend: CircleFriend, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF32D74B), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(friend.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(friend.currentActivity, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, maxLines = 1)
        }
    }
}
