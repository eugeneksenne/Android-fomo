package com.example.feature.map.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The Map screen's stacked floating action buttons: live Overpass POI query,
 * Add Place, SOS/NightGuard, heatmap toggle, and recenter.
 *
 * Extracted verbatim from `MapScreen.kt`'s inline `Column` of
 * `FloatingActionButton`s at the bottom-end of the screen - same colors,
 * borders, icons and content descriptions, same stacking order. Toast
 * side-effects and the actual `evaluateJavascript` calls now live with the
 * caller ([com.example.feature.map.MapScreen] /
 * [com.example.feature.map.map.MarkerRenderer]) since this component has no
 * knowledge of the `WebView` or `MapScreenState`.
 */
@Composable
fun MapFloatingButtons(
    isHeatmapEnabled: Boolean,
    onQueryOverpass: () -> Unit,
    onAddPlace: () -> Unit,
    onSosClick: () -> Unit,
    onToggleHeatmap: () -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Live Overpass API OSM POIs button
        FloatingActionButton(
            onClick = onQueryOverpass,
            containerColor = Color(0xFF0F1524),
            contentColor = Color(0xFF76FF03),
            modifier = Modifier
                .size(48.dp)
                .border(1.5.dp, Color(0xFF76FF03), CircleShape),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Public, contentDescription = "Query Overpass API OSM POIs", modifier = Modifier.size(22.dp))
        }

        // Add Place or Event (➕) button
        FloatingActionButton(
            onClick = onAddPlace,
            containerColor = Color(0xFF0F1524),
            contentColor = Color(0xFF00E5FF),
            modifier = Modifier
                .size(48.dp)
                .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Place to Map", modifier = Modifier.size(24.dp))
        }

        // SOS Urgency panic button
        FloatingActionButton(
            onClick = onSosClick,
            containerColor = Color(0xFFFF2D55),
            contentColor = Color.White,
            modifier = Modifier
                .size(48.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Warning, contentDescription = "SOS Rescue Guard", modifier = Modifier.size(22.dp))
        }

        // Vibe heat indicator switch
        FloatingActionButton(
            onClick = onToggleHeatmap,
            containerColor = Color(0xFF0F1524),
            contentColor = if (isHeatmapEnabled) Color(0xFFFF2D55) else Color.White,
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, if (isHeatmapEnabled) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.15f), CircleShape),
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (isHeatmapEnabled) Icons.Default.LocalFireDepartment else Icons.Outlined.LocalFireDepartment,
                contentDescription = "Vibe Hotspots Map Heat",
                modifier = Modifier.size(22.dp)
            )
        }

        // Recenter onto Sandton / Rosebank Location
        FloatingActionButton(
            onClick = onRecenter,
            containerColor = Color(0xFF0F1524),
            contentColor = Color(0xFF00E5FF),
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), CircleShape),
            shape = CircleShape
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Recenter user location", modifier = Modifier.size(20.dp))
        }
    }
}
