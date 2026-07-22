package com.example.feature.chats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.data.NightGuardRepository
import com.example.core.data.chat.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(
    chat: ChatData,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val repoState by ChatRepository.state.collectAsState()

    // Active conversation ID resolve
    val conversationId = remember(chat.name) {
        repoState.conversations.firstOrNull { it.name == chat.name }?.id ?: "conv_1"
    }

    // Messages flow from ChatRepository
    val messagesList = remember(repoState.activeMessages, conversationId) {
        repoState.activeMessages[conversationId] ?: emptyList()
    }

    // UI Local State
    var isOffline by remember { mutableStateOf(!repoState.isNetworkOnline) }
    var disappearingTime by remember { mutableStateOf("Off") }
    var presenceText by remember { mutableStateOf(if (chat.isOnline) "Online" else "Last seen 2h ago") }
    
    // Call overlays
    var activeCallType by remember { mutableStateOf<CallType?>(null) }
    
    // Search in chat
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Reply and Edit
    var replyingToMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMsg by remember { mutableStateOf<ChatMessage?>(null) }
    
    // Attachments & Drawers
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showEmojiDrawer by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    
    // Buddy Pair Overlays
    var showBuddyPairInviteModal by remember { mutableStateOf(false) }
    var showBuddyPairDashboardModal by remember { mutableStateOf(false) }
    
    // Walk Me Home Overlays
    var showWalkMeHomeInviteModal by remember { mutableStateOf(false) }
    var showWalkMeHomeDashboardModal by remember { mutableStateOf(false) }
    
    // Safety Check Overlays
    var showSafetyCheckInviteModal by remember { mutableStateOf(false) }
    var showSafetyCheckDashboardModal by remember { mutableStateOf(false) }
    
    // Group Info Sheet
    var showGroupInfoSheet by remember { mutableStateOf(false) }
    
    // E2E Key Verification Dialog
    var showKeyVerificationDialog by remember { mutableStateOf(false) }

    // Custom Modal Display Targets (Ticket QR, Flash Drop, Route Card)
    var activeTicketQrPayload by remember { mutableStateOf<Map<String, String>?>(null) }
    var activeFlashDropPayload by remember { mutableStateOf<Map<String, String>?>(null) }
    var activeRoutePayload by remember { mutableStateOf<Map<String, String>?>(null) }

    // Message Details Action Sheet
    var activeDetailsMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // Undo Send Banner
    var lastSentMsgId by remember { mutableStateOf<String?>(null) }
    var showUndoBanner by remember { mutableStateOf(false) }

    // Message dispatch callback
    val handleSendMessage: (String, RichMessageType, Map<String, String>?, GroupPoll?) -> Unit = { text, type, metadata, poll ->
        ChatRepository.sendMessage(
            conversationId = conversationId,
            type = type,
            content = text,
            metadata = metadata ?: emptyMap(),
            replyToMsg = replyingToMsg,
            poll = poll
        )
        replyingToMsg = null

        // Trigger Undo Send Banner for 3 seconds
        showUndoBanner = true
        coroutineScope.launch {
            delay(3000)
            showUndoBanner = false
        }

        // Scroll to bottom
        coroutineScope.launch {
            delay(100)
            if (messagesList.isNotEmpty()) {
                listState.animateScrollToItem(messagesList.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            ConversationHeader(
                chat = chat,
                presenceText = presenceText,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onToggleSearch = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) searchQuery = ""
                },
                onBack = onBack,
                onVoiceCall = { activeCallType = CallType.VOICE },
                onVideoCall = { activeCallType = CallType.VIDEO },
                onOfflineToggle = {
                    isOffline = !isOffline
                    ChatRepository.setNetworkStatus(!isOffline)
                },
                isOffline = isOffline,
                disappearingTime = disappearingTime,
                onToggleDisappearing = {
                    disappearingTime = when (disappearingTime) {
                        "Off" -> "24h"
                        "24h" -> "7d"
                        else -> "Off"
                    }
                },
                onShowGroupInfo = { showGroupInfoSheet = true },
                onVerifyKeys = { showKeyVerificationDialog = true }
            )
        },
        containerColor = Color(0xFF101014),
        bottomBar = {
            Column {
                // Background Upload Progress Queue Bar
                if (repoState.activeUploads.isNotEmpty()) {
                    UploadQueueBar(uploads = repoState.activeUploads)
                }

                // Undo Send Toast Banner
                if (showUndoBanner) {
                    UndoSendBanner(onUndo = {
                        showUndoBanner = false
                        // Undo last message
                    })
                }

                if (!isRecordingVoice) {
                    MessageComposer(
                        conversationId = conversationId,
                        onSend = { text ->
                            if (editingMsg != null) {
                                // Edit message
                                editingMsg = null
                            } else {
                                handleSendMessage(text, RichMessageType.TEXT, null, null)
                            }
                        },
                        replyingTo = replyingToMsg,
                        onCancelReply = { replyingToMsg = null },
                        editingMsg = editingMsg,
                        onCancelEdit = { editingMsg = null },
                        onAttachmentClick = { showAttachmentSheet = true },
                        onEmojiClick = { showEmojiDrawer = !showEmojiDrawer },
                        onRecordStart = { isRecordingVoice = true }
                    )

                    // Emoji/Sticker Drawer Panel
                    if (showEmojiDrawer) {
                        EmojiStickerDrawer(
                            onEmojiSelected = { emoji ->
                                handleSendMessage(emoji, RichMessageType.EMOJI, null, null)
                                showEmojiDrawer = false
                            },
                            onStickerSelected = { stickerUrl ->
                                handleSendMessage("Sticker", RichMessageType.STICKER, mapOf("url" to stickerUrl), null)
                                showEmojiDrawer = false
                            }
                        )
                    }
                } else {
                    VoiceRecordingPanel(
                        onCancel = { isRecordingVoice = false },
                        onSend = { durationStr ->
                            isRecordingVoice = false
                            handleSendMessage("Voice note", RichMessageType.VOICE_NOTE, mapOf("duration" to durationStr), null)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pinned Messages Top Bar
            PinnedMessagesBar(
                pinnedSnippet = "Cocoon Club Entry: Pre-sales close in 30 minutes!",
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )

            // Filtered messages
            val displayMessages = if (searchQuery.isEmpty()) {
                messagesList
            } else {
                messagesList.filter { it.content.contains(searchQuery, ignoreCase = true) }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(displayMessages, key = { _, item -> item.id }) { index, msg ->
                    val prevMsg = if (index > 0) displayMessages[index - 1] else null
                    val isConsecutive = prevMsg != null && prevMsg.senderId == msg.senderId

                    MessageBubble(
                        message = msg,
                        isConsecutive = isConsecutive,
                        onLongPress = { activeDetailsMessage = msg },
                        onReplyClick = { replyId ->
                            val idx = messagesList.indexOfFirst { it.id == replyId }
                            if (idx != -1) {
                                coroutineScope.launch { listState.animateScrollToItem(idx) }
                            }
                        },
                        onTicketQrClick = { activeTicketQrPayload = it },
                        onFlashDropClick = { activeFlashDropPayload = it },
                        onRouteClick = { activeRoutePayload = it },
                        onPollVote = { optionId ->
                            ChatRepository.voteInPoll(conversationId, msg.id, optionId)
                        },
                        onBuddyPairClick = { showBuddyPairDashboardModal = true },
                        onWalkMeHomeClick = { showWalkMeHomeDashboardModal = true },
                        onSafetyCheckClick = { showSafetyCheckDashboardModal = true }
                    )
                }
            }

            // --- Dialog Overlays ---

            if (showBuddyPairInviteModal) {
                BuddyPairInviteDialog(
                    onDismiss = { showBuddyPairInviteModal = false },
                    onSendInvite = { sessionName, durationText, durationMins, message ->
                        handleSendMessage(
                            "Buddy Pair Session Invited",
                            RichMessageType.BUDDY_PAIR_CARD,
                            mapOf(
                                "session" to sessionName,
                                "duration" to durationText,
                                "note" to message
                            ),
                            null
                        )
                    }
                )
            }

            if (showBuddyPairDashboardModal) {
                BuddyPairDashboardModal(
                    onDismiss = { showBuddyPairDashboardModal = false }
                )
            }

            if (showWalkMeHomeInviteModal) {
                WalkMeHomeInviteDialog(
                    onDismiss = { showWalkMeHomeInviteModal = false },
                    onStartJourney = { destination, mode, companions ->
                        NightGuardRepository.startJourney(destination, mode, companions)
                        handleSendMessage(
                            "Started Walk Me Home Journey to $destination",
                            RichMessageType.WALK_ME_HOME_CARD,
                            mapOf(
                                "destination" to destination,
                                "mode" to mode
                            ),
                            null
                        )
                    }
                )
            }

            if (showWalkMeHomeDashboardModal) {
                WalkMeHomeDashboardModal(
                    onDismiss = { showWalkMeHomeDashboardModal = false }
                )
            }

            if (showSafetyCheckInviteModal) {
                SafetyCheckInviteDialog(
                    onDismiss = { showSafetyCheckInviteModal = false },
                    onStartCheck = { mins, durationText, contacts ->
                        NightGuardRepository.scheduleSafetyCheck(mins, contacts)
                        handleSendMessage(
                            "Scheduled Safety Check ($durationText)",
                            RichMessageType.SAFETY_CHECK_CARD,
                            mapOf(
                                "duration" to durationText
                            ),
                            null
                        )
                    }
                )
            }

            if (showSafetyCheckDashboardModal) {
                SafetyCheckDashboardModal(
                    onDismiss = { showSafetyCheckDashboardModal = false }
                )
            }

            // Call Simulation Overlay
            activeCallType?.let { type ->
                CallOverlay(
                    name = chat.name,
                    imgUrl = chat.imgUrl,
                    callType = type,
                    onEndCall = { activeCallType = null }
                )
            }

            // Attachment Picker Grid Drawer (13 Options)
            if (showAttachmentSheet) {
                AttachmentGridSheet(
                    onDismiss = { showAttachmentSheet = false },
                    onOptionClick = { option ->
                        showAttachmentSheet = false
                        when (option) {
                            "Ticket QR" -> {
                                handleSendMessage(
                                    "VIP Entry QR Pass",
                                    RichMessageType.TICKET_QR_CARD,
                                    mapOf(
                                        "event" to "Friday Fever Night",
                                        "code" to "FOMO-TKT-884920",
                                        "holder" to "Sarah Jenkins",
                                        "vipTier" to "Platinum Access"
                                    ),
                                    null
                                )
                            }
                            "Flash Drop" -> {
                                handleSendMessage(
                                    "Tequila Flash Drop Voucher",
                                    RichMessageType.FLASH_DROP_CARD,
                                    mapOf(
                                        "title" to "Free Tequila Shots Voucher",
                                        "code" to "FLASH-COCOON-99",
                                        "expires" to "15 minutes remaining",
                                        "venue" to "Cocoon Rosebank"
                                    ),
                                    null
                                )
                            }
                            "Route" -> {
                                handleSendMessage(
                                    "Shared Safe Route",
                                    RichMessageType.ROUTE_CARD,
                                    mapOf(
                                        "routeTitle" to "Rosebank Strip -> VIP Hotel",
                                        "distance" to "2.4 km",
                                        "safetyScore" to "98% NightGuard Verified"
                                    ),
                                    null
                                )
                            }
                            "Venue" -> {
                                handleSendMessage(
                                    "Shared Venue",
                                    RichMessageType.VENUE_CARD,
                                    mapOf(
                                        "image" to "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=300",
                                        "name" to "Cocoon Club Rosebank",
                                        "distance" to "0.8 km",
                                        "status" to "Open • Until 6:00 AM",
                                        "crowd" to "92% Full"
                                    ),
                                    null
                                )
                            }
                            "Event" -> {
                                handleSendMessage(
                                    "Shared Event",
                                    RichMessageType.EVENT_CARD,
                                    mapOf(
                                        "image" to "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=300",
                                        "name" to "Neon Techno Night",
                                        "date" to "Sat, 10:00 PM - 6:00 AM",
                                        "venue" to "Industrial Warehouse",
                                        "going" to "3.4K going"
                                    ),
                                    null
                                )
                            }
                            "Live Location" -> {
                                handleSendMessage(
                                    "Live Location Sharing",
                                    RichMessageType.LIVE_LOCATION,
                                    mapOf(
                                        "battery" to "94%",
                                        "status" to "Moving • Speed: 8km/h",
                                        "duration" to "Sharing for 1 hour",
                                        "safe" to "NightGuard Secured"
                                    ),
                                    null
                                )
                            }
                            "Buddy Pair" -> {
                                showBuddyPairInviteModal = true
                            }
                            "Walk Me Home" -> {
                                showWalkMeHomeInviteModal = true
                            }
                            "Safety Check" -> {
                                showSafetyCheckInviteModal = true
                            }
                            "Poll" -> {
                                val poll = GroupPoll(
                                    id = "poll_${System.currentTimeMillis()}",
                                    question = "Which club are we starting at tonight?",
                                    options = listOf(
                                        PollOption("o1", "Cocoon Club", 2, listOf("https://i.pravatar.cc/150?img=5")),
                                        PollOption("o2", "Omnia Bar", 1, emptyList()),
                                        PollOption("o3", "Kabu Skybar", 0, emptyList())
                                    )
                                )
                                handleSendMessage("Group Poll Created", RichMessageType.POLL_CARD, null, poll)
                            }
                            else -> {
                                handleSendMessage("Attachment: $option", RichMessageType.DOCUMENT, null, null)
                            }
                        }
                    }
                )
            }

            // Ticket QR Code Display Modal
            activeTicketQrPayload?.let { payload ->
                TicketQrModal(payload = payload, onDismiss = { activeTicketQrPayload = null })
            }

            // Flash Drop Claim Modal
            activeFlashDropPayload?.let { payload ->
                FlashDropClaimModal(payload = payload, onDismiss = { activeFlashDropPayload = null })
            }

            // Route Map Modal
            activeRoutePayload?.let { payload ->
                RouteMapModal(payload = payload, onDismiss = { activeRoutePayload = null })
            }

            // Group Info Sheet
            if (showGroupInfoSheet) {
                GroupInfoSheet(chat = chat, onDismiss = { showGroupInfoSheet = false })
            }

            // E2E Key Verification Sheet
            if (showKeyVerificationDialog) {
                E2eKeyVerificationModal(onDismiss = { showKeyVerificationDialog = false })
            }

            // Message Action Sheet
            activeDetailsMessage?.let { msg ->
                MessageActionSheet(
                    message = msg,
                    onDismiss = { activeDetailsMessage = null },
                    onReact = { emoji ->
                        ChatRepository.toggleReaction(conversationId, msg.id, emoji)
                        activeDetailsMessage = null
                    },
                    onReply = {
                        replyingToMsg = msg
                        activeDetailsMessage = null
                    },
                    onDelete = {
                        activeDetailsMessage = null
                    }
                )
            }
        }
    }
}

// Top Conversation Header
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHeader(
    chat: ChatData,
    presenceText: String,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onBack: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onOfflineToggle: () -> Unit,
    isOffline: Boolean,
    disappearingTime: String,
    onToggleDisappearing: () -> Unit,
    onShowGroupInfo: () -> Unit,
    onVerifyKeys: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF18181E),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 10.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search messages...", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onToggleSearch) {
                            Icon(Icons.Default.Close, contentDescription = "Close search", tint = Color.White)
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onShowGroupInfo() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(42.dp)) {
                        AsyncImage(
                            model = chat.imgUrl,
                            contentDescription = chat.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(if (chat.type == ChatType.VENUE || chat.type == ChatType.GROUP) RoundedCornerShape(12.dp) else CircleShape)
                        )
                        if (chat.isOnline && chat.type == ChatType.PERSONAL) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759))
                                    .border(1.5.dp, Color(0xFF18181E), CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = chat.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // E2E Lock Indicator
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "E2E Encrypted",
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(12.dp).clickable { onVerifyKeys() }
                            )
                        }
                        Text(
                            text = presenceText,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onVoiceCall) {
                    Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color.White)
                }
                IconButton(onClick = onVideoCall) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF282830))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Group Info & Permissions", color = Color.White) },
                            onClick = { showMenu = false; onShowGroupInfo() }
                        )
                        DropdownMenuItem(
                            text = { Text("Verify Encryption Keys", color = Color.White) },
                            onClick = { showMenu = false; onVerifyKeys() }
                        )
                        DropdownMenuItem(
                            text = { Text("Search Chat History", color = Color.White) },
                            onClick = { showMenu = false; onToggleSearch() }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isOffline) "Disable Offline Mode" else "Simulate Offline Queue", color = Color.White) },
                            onClick = { showMenu = false; onOfflineToggle() }
                        )
                        DropdownMenuItem(
                            text = { Text("Disappearing: $disappearingTime", color = Color.White) },
                            onClick = { showMenu = false; onToggleDisappearing() }
                        )
                    }
                }
            }
        }
    }
}

// Pinned Messages Top Banner
@Composable
fun PinnedMessagesBar(pinnedSnippet: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF22222E),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = pinnedSnippet,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Composer with Morphing Mic / Send button
@Composable
fun MessageComposer(
    conversationId: String,
    onSend: (String) -> Unit,
    replyingTo: ChatMessage?,
    onCancelReply: () -> Unit,
    editingMsg: ChatMessage?,
    onCancelEdit: () -> Unit,
    onAttachmentClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onRecordStart: () -> Unit
) {
    var textState by remember { mutableStateOf("") }

    // Draft autosave
    LaunchedEffect(textState) {
        ChatRepository.saveDraft(conversationId, textState)
    }

    Surface(color = Color(0xFF18181E), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Reply Preview Banner
            if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(3.dp).height(32.dp).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Replying to ${replyingTo.senderName}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(replyingTo.content, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Composer Controls
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                IconButton(
                    onClick = onAttachmentClick,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Attachment", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onEmojiClick,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = "Emoji Drawer", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(6.dp))

                TextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("Message...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(6.dp))

                val isSending = textState.isNotBlank()
                IconButton(
                    onClick = {
                        if (isSending) {
                            onSend(textState)
                            textState = ""
                        } else {
                            onRecordStart()
                        }
                    },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isSending) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                        contentDescription = "Action",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// Emoji / Sticker / GIF Drawer
@Composable
fun EmojiStickerDrawer(
    onEmojiSelected: (String) -> Unit,
    onStickerSelected: (String) -> Unit
) {
    var activeDrawerTab by remember { mutableStateOf("EMOJI") }
    val emojis = listOf("🔥", "🎉", "🍸", "🍹", "🍾", "🥂", "🕺", "💃", "😎", "💯", "🚀", "❤️", "🙌", "👑", "✨", "🎟️")

    Surface(color = Color(0xFF1E1E26), modifier = Modifier.fillMaxWidth().height(220.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Emojis",
                    color = if (activeDrawerTab == "EMOJI") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { activeDrawerTab = "EMOJI" }
                )
                Text(
                    "Stickers",
                    color = if (activeDrawerTab == "STICKERS") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { activeDrawerTab = "STICKERS" }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (activeDrawerTab == "EMOJI") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(emojis) { emoji ->
                        Text(
                            emoji,
                            fontSize = 32.sp,
                            modifier = Modifier.clickable { onEmojiSelected(emoji) }
                        )
                    }
                }
            } else {
                Text("Sticker Search Engine (Trending FOMO Stickers Available)", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
    }
}

// Voice Note Panel with Dynamic Local Audio Waveform Canvas
@Composable
fun VoiceRecordingPanel(
    onCancel: () -> Unit,
    onSend: (String) -> Unit
) {
    var seconds by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(1000)
            seconds++
        }
    }

    val formattedTime = String.format("%d:%02d", seconds / 60, seconds % 60)

    Surface(color = Color(0xFF18181E), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Red))
                Spacer(modifier = Modifier.width(8.dp))
                Text(formattedTime, color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Local Audio Waveform Canvas
            Canvas(modifier = Modifier.width(100.dp).height(24.dp)) {
                val barWidth = 4f
                val gap = 4f
                repeat(12) { i ->
                    val h = (10..22).random().toFloat()
                    drawRect(
                        color = Color(0xFF34C759),
                        topLeft = Offset(i * (barWidth + gap), (size.height - h) / 2),
                        size = androidx.compose.ui.geometry.Size(barWidth, h)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { isPaused = !isPaused }) {
                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
                }
                Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))) {
                    Text("Cancel", color = Color.White)
                }
                Button(onClick = { onSend(formattedTime) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Send")
                }
            }
        }
    }
}

// Message Bubble supporting Custom Native Cards
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isConsecutive: Boolean,
    onLongPress: () -> Unit,
    onReplyClick: (String) -> Unit,
    onTicketQrClick: (Map<String, String>) -> Unit,
    onFlashDropClick: (Map<String, String>) -> Unit,
    onRouteClick: (Map<String, String>) -> Unit,
    onPollVote: (String) -> Unit,
    onBuddyPairClick: () -> Unit = {},
    onWalkMeHomeClick: () -> Unit = {},
    onSafetyCheckClick: () -> Unit = {}
) {
    val isMe = message.senderId == "me"
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else Color(0xFF24242C)

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = if (isConsecutive) 2.dp else 8.dp),
        horizontalAlignment = alignment
    ) {
        if (!isMe && !isConsecutive) {
            Text(message.senderName, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(start = 10.dp, bottom = 2.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 310.dp).combinedClickable(onClick = {}, onLongClick = onLongPress),
            shape = RoundedCornerShape(16.dp),
            color = bubbleColor
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Reply Preview
                if (message.replyToMessageId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.1f)).padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(3.dp).height(24.dp).background(Color.White))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(message.replyToSender ?: "Reply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(message.replyToText ?: "", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Render Type Content
                when (message.type) {
                    RichMessageType.TEXT -> Text(message.content, color = Color.White, fontSize = 15.sp)
                    RichMessageType.EMOJI -> Text(message.content, fontSize = 32.sp)
                    RichMessageType.VOICE_NOTE -> VoiceMessageBubbleContent(duration = message.metadata["duration"] ?: "0:18")
                    RichMessageType.TICKET_QR_CARD -> TicketQrCardSnippet(message.metadata) { onTicketQrClick(message.metadata) }
                    RichMessageType.FLASH_DROP_CARD -> FlashDropSnippet(message.metadata) { onFlashDropClick(message.metadata) }
                    RichMessageType.ROUTE_CARD -> RouteCardSnippet(message.metadata) { onRouteClick(message.metadata) }
                    RichMessageType.VENUE_CARD -> VenueCardSnippet(message.metadata)
                    RichMessageType.EVENT_CARD -> EventCardSnippet(message.metadata)
                    RichMessageType.LIVE_LOCATION -> LiveLocationSnippet(message.metadata)
                    RichMessageType.BUDDY_PAIR_CARD -> BuddyPairCardSnippet(
                        metadata = message.metadata,
                        onOpenDashboard = onBuddyPairClick,
                        onMeetupClick = onBuddyPairClick,
                        onFindFriendClick = onBuddyPairClick
                    )
                    RichMessageType.WALK_ME_HOME_CARD -> WalkMeHomeCardSnippet(
                        metadata = message.metadata,
                        onOpenDashboard = onWalkMeHomeClick,
                        onSendSupportReaction = { NightGuardRepository.addCompanionMessage(it) }
                    )
                    RichMessageType.SAFETY_CHECK_CARD -> SafetyCheckCardSnippet(
                        metadata = message.metadata,
                        onOpenDashboard = onSafetyCheckClick,
                        onConfirmSafe = { NightGuardRepository.confirmImsafe() },
                        onSnooze = { NightGuardRepository.snoozeSafetyCheck(15) }
                    )
                    RichMessageType.POLL_CARD -> message.poll?.let { GroupPollCardSnippet(it, onPollVote) }
                    else -> Text(message.content, color = Color.White, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp & Delivery State
                Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text(message.timestamp, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (message.isPendingOffline) Icons.Default.Schedule else Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Reactions
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2C2C36)).padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                message.reactions.forEach { (emoji, count) ->
                    Text("$emoji $count", color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}

// Custom Native Cards Components
@Composable
fun TicketQrCardSnippet(metadata: Map<String, String>, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.3f)).clickable { onClick() }.padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("VIP Ticket QR Pass", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text("Event: ${metadata["event"] ?: "Party"}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        Text("Pass Code: ${metadata["code"] ?: "TKT-001"}", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("View Entry QR Code")
        }
    }
}

@Composable
fun FlashDropSnippet(metadata: Map<String, String>, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.3f)).clickable { onClick() }.padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalActivity, contentDescription = null, tint = Color(0xFFFFCC00), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(metadata["title"] ?: "Flash Drop Voucher", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text("Expires: ${metadata["expires"] ?: "Soon"}", color = Color(0xFFFFCC00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Claim Voucher", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RouteCardSnippet(metadata: Map<String, String>, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.3f)).clickable { onClick() }.padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(metadata["routeTitle"] ?: "Safe Route", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Text("Safety Level: ${metadata["safetyScore"] ?: "98%"}", color = Color(0xFF34C759), fontSize = 11.sp)
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Open Interactive Route Map")
        }
    }
}

@Composable
fun GroupPollCardSnippet(poll: GroupPoll, onVote: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
        Text(poll.question, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        poll.options.forEach { option ->
            val isSelected = option.id == poll.userVotedOptionId
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().clickable { onVote(option.id) }.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option.text, color = Color.White, fontSize = 13.sp)
                    Text("${option.votesCount} votes", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun VoiceMessageBubbleContent(duration: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf("1.0x") }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(200.dp)) {
        IconButton(onClick = { isPlaying = !isPlaying }) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(duration, color = Color.White, fontSize = 12.sp)
        }
        Text(speed, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
            speed = if (speed == "1.0x") "1.5x" else "1.0x"
        })
    }
}

@Composable
fun VenueCardSnippet(metadata: Map<String, String>) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))) {
        AsyncImage(model = metadata["image"], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(100.dp))
        Column(modifier = Modifier.padding(6.dp)) {
            Text(metadata["name"] ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(metadata["status"] ?: "", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
fun EventCardSnippet(metadata: Map<String, String>) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))) {
        AsyncImage(model = metadata["image"], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(100.dp))
        Column(modifier = Modifier.padding(6.dp)) {
            Text(metadata["name"] ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(metadata["date"] ?: "", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
fun LiveLocationSnippet(metadata: Map<String, String>) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp)) {
        Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text("Live Location Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(metadata["status"] ?: "", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

// Ticket QR Modal
@Composable
fun TicketQrModal(payload: Map<String, String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("VIP Entry Barcode", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(160.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Scan at VIP Entrance", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Holder: ${payload["holder"] ?: "VIP Guest"}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
        containerColor = Color(0xFF1E1E24)
    )
}

// Flash Drop Claim Modal
@Composable
fun FlashDropClaimModal(payload: Map<String, String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(payload["title"] ?: "Flash Voucher", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = Color(0xFFFFCC00), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Present to Bartender:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(payload["code"] ?: "CODE-123", color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Dismiss") } },
        containerColor = Color(0xFF1E1E24)
    )
}

// Route Map Modal
@Composable
fun RouteMapModal(payload: Map<String, String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NightGuard Route Map", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(payload["routeTitle"] ?: "Route", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Color(0xFF2C2C36), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("Interactive Leaflet Route Active", color = Color.White.copy(alpha = 0.6f))
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close Map") } },
        containerColor = Color(0xFF1E1E24)
    )
}

// Group Info & Granular Permissions Sheet
@Composable
fun GroupInfoSheet(chat: ChatData, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${chat.name} Info", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Role: Admin", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Permissions Matrix:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("• Can Send Messages: Yes", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("• Can Send Media & Attachments: Yes", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("• Can Pin Messages: Yes", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } },
        containerColor = Color(0xFF1E1E24)
    )
}

// E2E Key Verification Modal
@Composable
fun E2eKeyVerificationModal(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("E2E Session Security Keys", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Verified Identity Key Fingerprint:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("4F89 - A2C3 - 90BF - 1102", color = Color(0xFF34C759), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Encrypted via AES-256-GCM / Signal Protocol Session Keys.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
        containerColor = Color(0xFF1E1E24)
    )
}

// Attachment Grid Sheet (13 options)
@Composable
fun AttachmentGridSheet(onDismiss: () -> Unit, onOptionClick: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1E1E26)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Share Content", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                val options = listOf(
                    Triple("Safety Check", Icons.Default.Security, Color(0xFF5856D6)),
                    Triple("Walk Me Home", Icons.Default.DirectionsWalk, Color(0xFF32D74B)),
                    Triple("Buddy Pair", Icons.Default.Group, Color(0xFF007AFF)),
                    Triple("Ticket QR", Icons.Default.QrCode, Color(0xFF007AFF)),
                    Triple("Flash Drop", Icons.Default.LocalActivity, Color(0xFFFFCC00)),
                    Triple("Route", Icons.Default.Place, Color(0xFF34C759)),
                    Triple("Venue", Icons.Default.Storefront, Color(0xFFFF9500)),
                    Triple("Event", Icons.Default.Event, Color(0xFFFF2D55)),
                    Triple("Live Location", Icons.Default.MyLocation, Color(0xFF34C759)),
                    Triple("Poll", Icons.Default.Poll, Color(0xFFAF52DE)),
                    Triple("Contact", Icons.Default.Person, Color(0xFF007AFF))
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(options) { (title, icon, color) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onOptionClick(title) }) {
                            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(title, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// Upload Progress Bar Component
@Composable
fun UploadQueueBar(uploads: List<AttachmentUploadItem>) {
    Surface(color = Color(0xFF242430), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding( horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Uploading ${uploads.size} file(s)...", color = Color.White, fontSize = 12.sp)
        }
    }
}

// Undo Send Toast Banner
@Composable
fun UndoSendBanner(onUndo: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Message sent", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("UNDO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clickable { onUndo() })
        }
    }
}
