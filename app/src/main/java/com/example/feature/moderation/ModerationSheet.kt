package com.example.feature.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.moderation.ModerationRepository

/**
 * Report / block / hide sheet for a piece of user-generated content.
 *
 * Required by the Google Play User Generated Content policy, which mandates an
 * in-app way to report objectionable content and block abusive users. The app
 * previously had no such affordance anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationSheet(
    authorName: String,
    onDismiss: () -> Unit,
    onHide: () -> Unit,
    onBlock: () -> Unit,
    onReport: (ModerationRepository.ReportReason, String, Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showReasons by remember { mutableStateOf(false) }
    var selectedReason by remember {
        mutableStateOf<ModerationRepository.ReportReason?>(null)
    }
    var details by remember { mutableStateOf("") }
    var blockToo by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!showReasons) {
                Text(
                    text = "Manage this post",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                ModerationAction(
                    icon = { Icon(Icons.Default.VisibilityOff, null, tint = Color.White) },
                    title = "Hide this post",
                    subtitle = "You won't see it again",
                    onClick = onHide,
                )

                ModerationAction(
                    icon = { Icon(Icons.Default.Flag, null, tint = Color(0xFFFF9F43)) },
                    title = "Report",
                    subtitle = "Tell us what's wrong with this post",
                    onClick = { showReasons = true },
                )

                ModerationAction(
                    icon = { Icon(Icons.Default.Block, null, tint = Color(0xFFFF3B30)) },
                    title = "Block $authorName",
                    subtitle = "You won't see their posts or messages",
                    onClick = onBlock,
                )
            } else {
                Text(
                    text = "Why are you reporting this?",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = "Your report is anonymous and helps keep FOMO safe.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                ModerationRepository.ReportReason.entries.forEach { reason ->
                    val selected = selectedReason == reason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) Color(0xFF2D0A40) else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { selectedReason = reason }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = reason.label,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 500) details = it },
                    placeholder = { Text("Add details (optional)", color = Color.White.copy(alpha = 0.4f)) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { blockToo = !blockToo }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                if (blockToo) Color(0xFFFF2D55) else Color.Transparent,
                                RoundedCornerShape(4.dp),
                            ),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Also block $authorName",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        selectedReason?.let { onReport(it, details, blockToo) }
                    },
                    enabled = selectedReason != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text("Submit report", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { showReasons = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Back", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun ModerationAction(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}
