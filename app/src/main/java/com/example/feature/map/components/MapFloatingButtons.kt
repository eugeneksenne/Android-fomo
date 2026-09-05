package com.example.feature.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MapFloatingButtons(
    onAddPlace: () -> Unit,
    onSosClick: () -> Unit,
    onRecenter: () -> Unit,
    onDownloadMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0D111D).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.width(68.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Add Business
            ActionButtonItem(
                icon = Icons.Default.Add,
                iconTint = Color(0xFF8B5CF6),
                label = "Add\nBusiness",
                onClick = onAddPlace
            )

            // 2. Nightguard
            ActionButtonItem(
                icon = Icons.Default.Shield,
                iconTint = Color(0xFF8B5CF6),
                label = "Nightguard",
                onClick = onSosClick
            )

            // 3. Recenter
            ActionButtonItem(
                icon = Icons.Default.MyLocation,
                iconTint = Color.White,
                label = "Recenter",
                onClick = onRecenter
            )

            // 4. Download map
            ActionButtonItem(
                icon = Icons.Default.Download,
                iconTint = Color.White,
                label = "Download\nmap",
                onClick = onDownloadMapClick
            )
        }
    }
}

@Composable
private fun ActionButtonItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp
        )
    }
}

