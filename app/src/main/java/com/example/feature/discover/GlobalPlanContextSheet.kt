package com.example.feature.discover

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.PlanType
import com.example.core.data.TonightRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalPlanContextSheet(
    targetName: String,
    targetArea: String = "Sandton / Joburg",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tonightState by TonightRepository.state.collectAsState()

    var selectedFriend by remember { mutableStateOf("Amanda K.") }
    var aiPromptInput by remember { mutableStateOf("Plan an epic night around $targetName starting at 20:00") }

    val themeBg = Color(0xFF0D121F)
    val cardBg = Color(0xFF141C2E)
    val accentPurple = Color(0xFF9D4EDD)
    val neonCyan = Color(0xFF00E5FF)
    val activeGreen = Color(0xFF00E676)
    val warmAmber = Color(0xFFFFB703)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("global_plan_context_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // SHEET HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📅", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Plan with $targetName",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "$targetArea • Integrated Planning Engine",
                            color = neonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // OPTION 1: ADD TO CURRENT ACTIVE PLAN
                item {
                    val activePlan = tonightState.plans.find { it.id == tonightState.currentSelectedPlanId } ?: tonightState.plans.firstOrNull()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (activePlan != null) {
                                    TonightRepository.addStopToPlan(activePlan.id, targetName, targetArea, "11:30 PM")
                                    Toast.makeText(context, "✅ Added '$targetName' to ${activePlan.title}!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2338)),
                        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(accentPurple, neonCyan)))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = activeGreen, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Add to Active Plan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = activePlan?.let { "${it.title} (${it.stops.size} stops currently)" } ?: "Active Plan",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                            Surface(
                                color = activeGreen.copy(0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("QUICK ADD", color = activeGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }

                // OPTION 2: CREATE DUO PLAN REQUEST
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                TonightRepository.createPlan(
                                    title = "Duo Outing at $targetName",
                                    type = PlanType.DUO,
                                    venues = listOf(Pair(targetName, targetArea)),
                                    invitedNames = listOf(selectedFriend)
                                )
                                Toast.makeText(context, "📩 Duo Plan Request sent to $selectedFriend for $targetName!", Toast.LENGTH_LONG).show()
                                onDismiss()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2C)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = neonCyan, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Plan Duo Move with $selectedFriend", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Sends interactive Duo Plan invitation & calendar sync request", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(0.4f))
                        }
                    }
                }

                // OPTION 3: AI NIGHT CONCIERGE GENERATION
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A122E)),
                        border = BorderStroke(1.dp, warmAmber.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🤖 AI Night Concierge Itinerary Generator", color = warmAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = aiPromptInput,
                                onValueChange = { aiPromptInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = warmAmber,
                                    unfocusedBorderColor = Color.White.copy(0.2f)
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    TonightRepository.generateAiPlan(aiPromptInput)
                                    Toast.makeText(context, "⚡ AI Itinerary generated around $targetName!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = warmAmber),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Generate Full AI Night Plan", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // OPTION 4: TEMPLATES QUICK SELECT
                item {
                    Text("START FROM TEMPLATE WITH THIS VENUE", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                items(tonightState.templates.take(3)) { tmpl ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                TonightRepository.createPlan(
                                    title = "${tmpl.title} at $targetName",
                                    type = PlanType.GROUP,
                                    venues = listOf(Pair(targetName, targetArea)) + tmpl.sampleStops.drop(1).map { Pair(it, "Sandton") },
                                    invitedNames = listOf("Amanda", "Thabo")
                                )
                                Toast.makeText(context, "🎉 Plan created using '${tmpl.title}' template!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tmpl.emoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tmpl.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(tmpl.description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
                            }
                            Text(tmpl.estBudget, color = neonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
