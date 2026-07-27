package com.example.feature.discover

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.data.*

// CREATE PLAN MODAL DIALOG
// -------------------------------------------------------------------------
@Composable
fun CreatePlanModalDialog(
    onDismiss: () -> Unit,
    onCreatePlan: (title: String, type: PlanType, venues: List<Pair<String, String>>, invited: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PlanType.GROUP) }
    var venue1 by remember { mutableStateOf("Marble Restaurant") }
    var area1 by remember { mutableStateOf("Rosebank") }
    var venue2 by remember { mutableStateOf("LIV Sandton") }
    var area2 by remember { mutableStateOf("Sandton") }
    var invitedNames by remember { mutableStateOf("Amanda, Thabo, Sarah") }

    val cardBg = Color(0xFF141C2E)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Night Move", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Move Title (e.g. Sandton Party)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanType.values().forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.name, fontSize = 11.sp, color = if (selectedType == t) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = neonCyan)
                        )
                    }
                }

                OutlinedTextField(
                    value = venue1,
                    onValueChange = { venue1 = it },
                    label = { Text("First Venue Stop") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = venue2,
                    onValueChange = { venue2 = it },
                    label = { Text("Second Venue Stop") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = invitedNames,
                    onValueChange = { invitedNames = it },
                    label = { Text("Invite Friends (Comma separated)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val venuesList = listOf(Pair(venue1, area1), Pair(venue2, area2)).filter { it.first.isNotBlank() }
                    val friendsList = invitedNames.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onCreatePlan(title, selectedType, venuesList, friendsList)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentPurple)
            ) {
                Text("Launch Move", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(0.6f)) }
        },
        containerColor = cardBg
    )
}

// -------------------------------------------------------------------------
// SPLIT FARE DIALOG
// -------------------------------------------------------------------------
@Composable
fun SplitFareDialog(
    onDismiss: () -> Unit,
    onConfirmSplit: (Double) -> Unit
) {
    var amountInput by remember { mutableStateOf("450") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Rideshare / Table Bill", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter total bill amount (ZAR) to split equally among group members:", color = Color.White.copy(0.7f), fontSize = 12.sp)
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount (R)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 450.0
                    onConfirmSplit(amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Request Split", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(0.6f)) }
        },
        containerColor = Color(0xFF141C2E)
    )
}
