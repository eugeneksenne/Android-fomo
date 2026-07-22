package com.example.feature.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

enum class ChatType {
    PERSONAL, GROUP, VENUE, NIGHTGUARD
}

enum class CallType {
    VOICE, VIDEO
}

data class ChatData(
    val name: String,
    val message: String = "",
    val time: String = "",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val type: ChatType = ChatType.PERSONAL,
    val imgUrl: String = ""
)

@Composable
fun CallOverlay(
    callType: CallType,
    name: String,
    imgUrl: String,
    onEndCall: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.9f)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(model = imgUrl, contentDescription = null, modifier = Modifier.size(100.dp).clip(CircleShape))
            Spacer(modifier = Modifier.height(16.dp))
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(if (callType == CallType.VIDEO) "Video Call..." else "Voice Call...", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onEndCall,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("End Call", color = Color.White)
            }
        }
    }
}

@Composable
fun MessageActionSheet(
    message: com.example.core.data.chat.ChatMessage? = null,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit = {},
    onReply: () -> Unit = {},
    onCopy: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Message Actions", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("🔥", "❤️", "👍", "😮", "🎉").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 22.sp,
                            modifier = Modifier.clickable { onReact(emoji) }.padding(4.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onReply(); onDismiss() }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reply to Message", color = Color.White)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onCopy(); onDismiss() }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Copy Text", color = Color.White)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onDelete(); onDismiss() }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Delete Message", color = Color.Red)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = Color(0xFF1E1E24)
    )
}
