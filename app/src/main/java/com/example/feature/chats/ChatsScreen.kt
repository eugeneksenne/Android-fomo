package com.example.feature.chats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.core.data.chat.*
import com.example.core.data.story.*
import com.example.core.data.NightGuardRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen() {
    val repoState by ChatRepository.state.collectAsState()
    val storyState by StoryRepository.state.collectAsState()

    // UI State local management
    var isCallsTabSelected by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Story Overlays
    var showStoryComposer by remember { mutableStateOf(false) }
    var selectedUserStoryGroup by remember { mutableStateOf<UserStoryGroup?>(null) }

    // Active conversation detail target
    var selectedConversationItem by remember { mutableStateOf<ConversationItem?>(null) }

    // Context Action Sheet target for swiped/long-pressed items
    var contextActionConversation by remember { mutableStateOf<ConversationItem?>(null) }

    // New Group/Chat Wizard
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupDashboardItem by remember { mutableStateOf<ConversationItem?>(null) }

    // Calling Session state
    var activeCallSession by remember { mutableStateOf<FomoCallSession?>(null) }
    var showStartCallDialog by remember { mutableStateOf(false) }

    // Buddy Pair Overlays
    var showBuddyPairInviteDialog by remember { mutableStateOf(false) }
    var showBuddyPairDashboardModal by remember { mutableStateOf(false) }

    // Walk Me Home Overlays
    var showWalkMeHomeInviteDialog by remember { mutableStateOf(false) }
    var showWalkMeHomeDashboardModal by remember { mutableStateOf(false) }

    // Safety Check Overlays
    var showSafetyCheckInviteDialog by remember { mutableStateOf(false) }
    var showSafetyCheckDashboardModal by remember { mutableStateOf(false) }

    // Universal search query sync
    LaunchedEffect(searchQuery) {
        ChatRepository.setSearchQuery(searchQuery)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D12))) {
        Scaffold(
            topBar = {
                ChatsTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onNewChatClick = { showNewChatDialog = true }
                )
            },
            containerColor = Color(0xFF0D0D12),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (isCallsTabSelected) {
                            showStartCallDialog = true
                        } else if (repoState.activeCategory == ChatCategory.GROUPS) {
                            showCreateGroupDialog = true
                        } else {
                            showNewChatDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isCallsTabSelected) {
                        Icon(Icons.Default.AddIcCall, contentDescription = "New Call")
                    } else if (repoState.activeCategory == ChatCategory.GROUPS) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
                    } else {
                        Icon(Icons.Default.Edit, contentDescription = "New Chat")
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top Full-Stack Stories Row
                StoriesSection(
                    storyGroups = storyState.userStoryGroups,
                    onAddStoryClick = { showStoryComposer = true },
                    onStoryGroupClick = { group -> selectedUserStoryGroup = group }
                )

                // Navigation Category Tabs Bar
                CategoryTabsSection(
                    activeCategory = repoState.activeCategory,
                    isCallsSelected = isCallsTabSelected,
                    onCategorySelect = { category ->
                        isCallsTabSelected = false
                        ChatRepository.setCategory(category)
                    },
                    onCallsToggle = {
                        isCallsTabSelected = true
                    }
                )

                // Render List based on mode selection
                if (isCallsTabSelected) {
                    CallsListSection(
                        repoState = repoState,
                        onStartCallClick = { showStartCallDialog = true },
                        onCallStart = { name, img, type, group, guard, lobby ->
                            activeCallSession = FomoCallSession(
                                name = name,
                                imgUrl = img,
                                callType = type,
                                isGroup = group,
                                isNightGuard = guard,
                                isClubLobby = lobby,
                                initialState = if (guard || lobby) FomoCallState.CONNECTED_VOICE else FomoCallState.OUTGOING
                            )
                        }
                    )
                } else if (repoState.activeCategory == ChatCategory.GROUPS) {
                    GroupsListSection(
                        repoState = repoState,
                        onCreateGroupClick = { showCreateGroupDialog = true },
                        onGroupClick = { selectedConversationItem = it },
                        onGroupLongPress = { contextActionConversation = it },
                        onGroupDetailClick = { selectedGroupDashboardItem = it },
                        onGroupCallClick = { group ->
                            activeCallSession = FomoCallSession(
                                name = group.name,
                                imgUrl = group.avatarUrl,
                                callType = "VOICE",
                                isGroup = true,
                                isNightGuard = group.name.contains("NightGuard") || group.name.contains("Safety"),
                                isClubLobby = group.name.contains("Lobby") || group.name.contains("VIP"),
                                initialState = FomoCallState.OUTGOING
                            )
                        }
                    )
                } else {
                    // Filtered Conversations List
                    val filteredConversations = remember(repoState.conversations, repoState.activeCategory, repoState.searchQuery) {
                        repoState.conversations.filter { conv ->
                            val matchesSearch = repoState.searchQuery.isEmpty() ||
                                    conv.name.contains(repoState.searchQuery, ignoreCase = true) ||
                                    conv.lastMessage.contains(repoState.searchQuery, ignoreCase = true)

                            val matchesCategory = when (repoState.activeCategory) {
                                ChatCategory.ALL -> !conv.isArchived
                                ChatCategory.UNREAD -> conv.unreadCount > 0 && !conv.isArchived
                                ChatCategory.PINNED -> conv.isPinned && !conv.isArchived
                                ChatCategory.PERSONAL -> conv.type == ConversationType.PERSONAL && !conv.isArchived
                                ChatCategory.GROUPS -> (conv.type == ConversationType.GROUP || conv.type == ConversationType.BUDDY_PAIR) && !conv.isArchived
                                ChatCategory.BUSINESSES -> conv.type == ConversationType.BUSINESS && !conv.isArchived
                                ChatCategory.VENUES -> conv.type == ConversationType.VENUE && !conv.isArchived
                                ChatCategory.ARCHIVED -> conv.isArchived
                            }

                            matchesSearch && matchesCategory
                        }
                    }

                    if (filteredConversations.isEmpty()) {
                        EmptyStatePanel(category = repoState.activeCategory)
                    } else {
                        ConversationListSection(
                            conversations = filteredConversations,
                            onConversationClick = { selectedConversationItem = it },
                            onConversationLongPress = { contextActionConversation = it }
                        )
                    }
                }
            }
        }

        // --- Overlays & Dialogs ---

        // Interactive Camera & Filter Story Composer
        if (showStoryComposer) {
            StoryComposerModal(onDismiss = { showStoryComposer = false })
        }

        // Immersive Story Player Viewer
        selectedUserStoryGroup?.let { group ->
            StoryViewerModal(
                userStoryGroup = group,
                onDismiss = { selectedUserStoryGroup = null },
                onReplyToChat = { storyUserId, storyUserName, segment, replyText ->
                    selectedUserStoryGroup = null
                    StoryRepository.replyToStory(storyUserId, storyUserName, segment, replyText)

                    // Open target direct message conversation
                    val targetConv = repoState.conversations.firstOrNull {
                        it.name.contains(storyUserName, ignoreCase = true)
                    } ?: repoState.conversations.first()
                    selectedConversationItem = targetConv
                }
            )
        }

        // Context Action Menu for Swiped/Long-pressed Item
        contextActionConversation?.let { conv ->
            ConversationContextDialog(
                conversation = conv,
                onDismiss = { contextActionConversation = null },
                onPinToggle = {
                    ChatRepository.togglePinConversation(conv.id)
                    contextActionConversation = null
                },
                onMuteToggle = {
                    ChatRepository.toggleMuteConversation(conv.id)
                    contextActionConversation = null
                },
                onArchiveToggle = {
                    ChatRepository.toggleArchiveConversation(conv.id)
                    contextActionConversation = null
                },
                onReadToggle = {
                    if (conv.unreadCount > 0) ChatRepository.markConversationRead(conv.id) else ChatRepository.markConversationUnread(conv.id)
                    contextActionConversation = null
                },
                onDelete = {
                    ChatRepository.deleteConversation(conv.id)
                    contextActionConversation = null
                }
            )
        }

        // New Chat & Group Creation Modal
        if (showNewChatDialog) {
            NewChatCreationWizard(
                onDismiss = { showNewChatDialog = false },
                onChatCreated = { newConv ->
                    showNewChatDialog = false
                    selectedConversationItem = newConv
                }
            )
        }

        // Dedicated Create Group Dialog
        if (showCreateGroupDialog) {
            CreateGroupDialog(
                onDismiss = { showCreateGroupDialog = false },
                onGroupCreated = { newGroup ->
                    showCreateGroupDialog = false
                    selectedConversationItem = newGroup
                }
            )
        }

        // Group Detail Dashboard Modal
        selectedGroupDashboardItem?.let { group ->
            GroupDetailDashboardModal(
                group = group,
                onDismiss = { selectedGroupDashboardItem = null },
                onOpenChat = {
                    selectedGroupDashboardItem = null
                    selectedConversationItem = it
                },
                onStartCall = { g ->
                    selectedGroupDashboardItem = null
                    activeCallSession = FomoCallSession(
                        name = g.name,
                        imgUrl = g.avatarUrl,
                        callType = "VOICE",
                        isGroup = true,
                        isNightGuard = g.name.contains("NightGuard"),
                        isClubLobby = g.name.contains("Lobby") || g.name.contains("VIP"),
                        initialState = FomoCallState.OUTGOING
                    )
                }
            )
        }

        // Active Full Screen Conversation Overlay
        selectedConversationItem?.let { conv ->
            ConversationScreenWrapper(
                conversationItem = conv,
                onBack = { selectedConversationItem = null }
            )
        }

        // Call Launcher Picker
        if (showStartCallDialog) {
            StartCallDialog(
                onDismiss = { showStartCallDialog = false },
                onCallInitiated = { name, img, type, group, guard, lobby ->
                    showStartCallDialog = false
                    activeCallSession = FomoCallSession(
                        name = name,
                        imgUrl = img,
                        callType = type,
                        isGroup = group,
                        isNightGuard = guard,
                        isClubLobby = lobby,
                        initialState = FomoCallState.OUTGOING
                    )
                }
            )
        }

        // Active Calling Simulation Session
        activeCallSession?.let { session ->
            FomoCallOverlay(
                session = session,
                onEndCall = { activeCallSession = null }
            )
        }

        // Buddy Pair Modals
        if (showBuddyPairInviteDialog) {
            BuddyPairInviteDialog(
                onDismiss = { showBuddyPairInviteDialog = false },
                onSendInvite = { name, text, mins, msg ->
                    NightGuardRepository.startBuddyPair(name, mins, text)
                    showBuddyPairInviteDialog = false
                    showBuddyPairDashboardModal = true
                }
            )
        }

        if (showBuddyPairDashboardModal) {
            BuddyPairDashboardModal(
                onDismiss = { showBuddyPairDashboardModal = false }
            )
        }

        // Walk Me Home Modals
        if (showWalkMeHomeInviteDialog) {
            WalkMeHomeInviteDialog(
                onDismiss = { showWalkMeHomeInviteDialog = false },
                onStartJourney = { destination, mode, companions ->
                    NightGuardRepository.startJourney(destination, mode, companions)
                    showWalkMeHomeInviteDialog = false
                    showWalkMeHomeDashboardModal = true
                }
            )
        }

        if (showWalkMeHomeDashboardModal) {
            WalkMeHomeDashboardModal(
                onDismiss = { showWalkMeHomeDashboardModal = false }
            )
        }

        // Safety Check Modals
        if (showSafetyCheckInviteDialog) {
            SafetyCheckInviteDialog(
                onDismiss = { showSafetyCheckInviteDialog = false },
                onStartCheck = { mins, durationText, contacts ->
                    NightGuardRepository.scheduleSafetyCheck(mins, contacts)
                    showSafetyCheckInviteDialog = false
                    showSafetyCheckDashboardModal = true
                }
            )
        }

        if (showSafetyCheckDashboardModal) {
            SafetyCheckDashboardModal(
                onDismiss = { showSafetyCheckDashboardModal = false }
            )
        }
    }
}

enum class FomoCallState {
    OUTGOING, INCOMING, CONNECTED_VOICE, CONNECTED_VIDEO, ENDED
}

data class FomoCallSession(
    val name: String,
    val imgUrl: String,
    val callType: String,
    val isGroup: Boolean = false,
    val isNightGuard: Boolean = false,
    val isClubLobby: Boolean = false,
    val initialState: FomoCallState = FomoCallState.OUTGOING
)

@Composable
fun ActionRowItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CallsListSection(
    repoState: ChatRepositoryState,
    onStartCallClick: () -> Unit,
    onCallStart: (String, String, String, Boolean, Boolean, Boolean) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, MISSED, VOICE, VIDEO, GROUPS
    var contextLogItem by remember { mutableStateOf<CallLogItem?>(null) }

    val favorites = remember(repoState.callLogs) {
        repoState.callLogs.filter { it.isFavorite }
    }

    val filteredLogs = remember(repoState.callLogs, selectedFilter, repoState.searchQuery) {
        repoState.callLogs.filter { log ->
            val matchesSearch = repoState.searchQuery.isEmpty() ||
                    log.participantName.contains(repoState.searchQuery, ignoreCase = true) ||
                    (log.roomName?.contains(repoState.searchQuery, ignoreCase = true) == true)

            val matchesFilter = when (selectedFilter) {
                "MISSED" -> log.direction == CallDirection.MISSED
                "VOICE" -> log.mediaType == CallMediaType.VOICE || log.mediaType == CallMediaType.NIGHTGUARD_EMERGENCY
                "VIDEO" -> log.mediaType == CallMediaType.VIDEO
                "GROUPS" -> log.mediaType == CallMediaType.GROUP_VOICE || log.mediaType == CallMediaType.GROUP_VIDEO || log.mediaType == CallMediaType.CLUB_LOUNGE
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        // --- Quick Favorites Row ---
        if (favorites.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                    Text(
                        text = "FAVORITES & SPEED DIAL",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(favorites) { fav ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clickable {
                                        val isVideo = fav.mediaType == CallMediaType.VIDEO
                                        val isNightGuard = fav.mediaType == CallMediaType.NIGHTGUARD_EMERGENCY
                                        val isClubLobby = fav.mediaType == CallMediaType.CLUB_LOUNGE
                                        onCallStart(fav.participantName, fav.participantAvatarUrl, if (isVideo) "VIDEO" else "VOICE", false, isNightGuard, isClubLobby)
                                    }
                            ) {
                                Box(modifier = Modifier.size(60.dp)) {
                                    AsyncImage(
                                        model = fav.participantAvatarUrl,
                                        contentDescription = fav.participantName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (fav.mediaType == CallMediaType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fav.participantName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Sub-Filter Segment Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "MISSED", "VOICE", "VIDEO", "GROUPS").forEach { filterKey ->
                    val isSel = selectedFilter == filterKey
                    Surface(
                        color = if (isSel) Color.White.copy(alpha = 0.18f) else Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.clickable { selectedFilter = filterKey }
                    ) {
                        Text(
                            text = filterKey.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // --- Header Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Calls (${filteredLogs.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (repoState.callLogs.isNotEmpty()) {
                    TextButton(onClick = { ChatRepository.clearAllCallLogs() }) {
                        Text("Clear All", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }
        }

        // --- Call History List ---
        if (filteredLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhoneMissed,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No call logs found",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                CallHistoryRowItem(
                    log = log,
                    onVoiceCall = {
                        val isNightGuard = log.mediaType == CallMediaType.NIGHTGUARD_EMERGENCY
                        val isClubLobby = log.mediaType == CallMediaType.CLUB_LOUNGE
                        onCallStart(log.participantName, log.participantAvatarUrl, "VOICE", log.mediaType == CallMediaType.GROUP_VOICE, isNightGuard, isClubLobby)
                    },
                    onVideoCall = {
                        val isNightGuard = log.mediaType == CallMediaType.NIGHTGUARD_EMERGENCY
                        val isClubLobby = log.mediaType == CallMediaType.CLUB_LOUNGE
                        onCallStart(log.participantName, log.participantAvatarUrl, "VIDEO", log.mediaType == CallMediaType.GROUP_VIDEO, isNightGuard, isClubLobby)
                    },
                    onToggleFavorite = { ChatRepository.toggleCallFavorite(log.id) },
                    onDelete = { ChatRepository.deleteCallLog(log.id) },
                    onLongClick = { contextLogItem = log }
                )
            }
        }
    }

    // Context dialog for call log item
    contextLogItem?.let { log ->
        AlertDialog(
            onDismissRequest = { contextLogItem = null },
            title = { Text(log.participantName, color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Manage call log entry:", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    ChatRepository.toggleCallFavorite(log.id)
                    contextLogItem = null
                }) {
                    Text(if (log.isFavorite) "Remove from Speed Dial" else "Add to Speed Dial", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    ChatRepository.deleteCallLog(log.id)
                    contextLogItem = null
                }) {
                    Text("Delete Log", color = Color.Red)
                }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }
}

@Composable
fun CallHistoryRowItem(
    log: CallLogItem,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onVoiceCall)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(modifier = Modifier.size(52.dp)) {
            AsyncImage(
                model = log.participantAvatarUrl,
                contentDescription = log.participantName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(if (log.mediaType == CallMediaType.CLUB_LOUNGE) RoundedCornerShape(16.dp) else CircleShape)
            )

            if (log.isVerified) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).background(Color(0xFF0D0D12), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center Info Block
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = log.participantName,
                    color = if (log.direction == CallDirection.MISSED) Color(0xFFFF453A) else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Call Direction Icon
                when (log.direction) {
                    CallDirection.INCOMING -> Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(14.dp))
                    CallDirection.OUTGOING -> Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(14.dp))
                    CallDirection.MISSED -> Icon(Icons.AutoMirrored.Filled.CallMissed, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(14.dp))
                    CallDirection.REJECTED -> Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Subtitle detail
                val mediaLabel = when (log.mediaType) {
                    CallMediaType.VIDEO -> "Video Call"
                    CallMediaType.GROUP_VOICE -> "Group Voice (${log.participantCount})"
                    CallMediaType.GROUP_VIDEO -> "Group Video (${log.participantCount})"
                    CallMediaType.NIGHTGUARD_EMERGENCY -> "NightGuard Line"
                    CallMediaType.CLUB_LOUNGE -> log.roomName ?: "VIP Audio Lounge"
                    else -> "Voice Call"
                }

                val durationText = if (log.durationSeconds > 0) {
                    val m = log.durationSeconds / 60
                    val s = log.durationSeconds % 60
                    " • ${m}m ${s}s"
                } else ""

                Text(
                    text = "$mediaLabel$durationText • ${log.timestampText}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Quick Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onVoiceCall) {
                Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onVideoCall) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun StartCallDialog(
    onDismiss: () -> Unit,
    onCallInitiated: (String, String, String, Boolean, Boolean, Boolean) -> Unit
) {
    var modeTab by remember { mutableStateOf("DIRECT") } // DIRECT, GROUP, NIGHTGUARD, LOUNGE
    var selectedContactName by remember { mutableStateOf("Sarah Jenkins") }
    var callMediaType by remember { mutableStateOf("VOICE") } // VOICE or VIDEO

    val contacts = listOf(
        Pair("Sarah Jenkins", "https://i.pravatar.cc/150?img=5"),
        Pair("NightGuard Escalation", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200"),
        Pair("Cocoon VIP Lounge Stage", "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=200"),
        Pair("Kgomotso & Crew", "https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=200"),
        Pair("Mike Ross", "https://i.pravatar.cc/150?img=11")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Start New Call", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Mode selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = modeTab == "DIRECT",
                        onClick = { modeTab = "DIRECT" },
                        label = { Text("Direct", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = modeTab == "NIGHTGUARD",
                        onClick = { modeTab = "NIGHTGUARD"; selectedContactName = "NightGuard Escalation" },
                        label = { Text("NightGuard", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = modeTab == "LOUNGE",
                        onClick = { modeTab = "LOUNGE"; selectedContactName = "Cocoon VIP Lounge Stage" },
                        label = { Text("VIP Lounge", fontSize = 11.sp) }
                    )
                }

                // Media type toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Call Mode:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = callMediaType == "VOICE",
                            onClick = { callMediaType = "VOICE" },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Voice") }
                        )
                        FilterChip(
                            selected = callMediaType == "VIDEO",
                            onClick = { callMediaType = "VIDEO" },
                            leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Video") }
                        )
                    }
                }

                Text("Select Target:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                contacts.forEach { (name, img) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedContactName = name }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedContactName == name, onClick = { selectedContactName = name })
                        Spacer(modifier = Modifier.width(8.dp))
                        AsyncImage(model = img, contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(name, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val contact = contacts.firstOrNull { it.first == selectedContactName } ?: contacts.first()
                    val isGuard = modeTab == "NIGHTGUARD" || selectedContactName.contains("NightGuard")
                    val isLobby = modeTab == "LOUNGE" || selectedContactName.contains("Lounge")
                    val isGroup = selectedContactName.contains("Crew") || isLobby

                    ChatRepository.addCallLog(
                        participantName = contact.first,
                        avatarUrl = contact.second,
                        mediaType = if (isGuard) CallMediaType.NIGHTGUARD_EMERGENCY else if (isLobby) CallMediaType.CLUB_LOUNGE else if (callMediaType == "VIDEO") CallMediaType.VIDEO else CallMediaType.VOICE,
                        direction = CallDirection.OUTGOING,
                        durationSeconds = 0,
                        isVerified = true
                    )

                    onCallInitiated(contact.first, contact.second, callMediaType, isGroup, isGuard, isLobby)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Initiate Call", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF1E1E24)
    )
}

@Composable
fun FomoCallOverlay(
    session: FomoCallSession,
    onEndCall: () -> Unit
) {
    var callState by remember { mutableStateOf(session.initialState) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(true) }
    var isCameraOn by remember { mutableStateOf(session.callType == "VIDEO") }
    var isFrontCamera by remember { mutableStateOf(true) }
    var durationSeconds by remember { mutableStateOf(0) }
    var showInCallChat by remember { mutableStateOf(false) }
    var showAddParticipantSheet by remember { mutableStateOf(false) }
    var inCallChatMessage by remember { mutableStateOf("") }
    val callChatList = remember { mutableStateListOf<Pair<String, String>>() }

    // Live call duration timer
    LaunchedEffect(callState) {
        if (callState == FomoCallState.CONNECTED_VOICE || callState == FomoCallState.CONNECTED_VIDEO) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                durationSeconds++
            }
        }
    }

    // Auto connect after ringing delay
    LaunchedEffect(Unit) {
        if (session.initialState == FomoCallState.OUTGOING) {
            kotlinx.coroutines.delay(2000L)
            callState = if (session.callType == "VIDEO") FomoCallState.CONNECTED_VIDEO else FomoCallState.CONNECTED_VOICE
        }
    }

    val formattedDuration = remember(durationSeconds) {
        val m = durationSeconds / 60
        val s = durationSeconds % 60
        String.format("%02d:%02d", m, s)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0D0D12)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // --- Video Stream Background (if VIDEO) ---
            if (callState == FomoCallState.CONNECTED_VIDEO && isCameraOn) {
                AsyncImage(
                    model = session.imgUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Draggable local video camera box
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 150.dp)
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFrontCamera) {
                        Text("Selfie Camera", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Rear Camera", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // --- Voice Background with Pulsing Glow ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), Color(0xFF0D0D12))
                            )
                        )
                )
            }

            // --- Top Banner (Status & NightGuard / Lounge Indicators) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (session.isNightGuard) {
                    Surface(
                        color = Color(0xFFFF2D55).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF2D55))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NightGuard Emergency Escort Connected • GPS Live", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (session.isClubLobby) {
                    Surface(
                        color = Color(0xFF9D00FF).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF9D00FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF9D00FF), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cocoon VIP Lounge Stage • 18 Listeners", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Center Avatar
                AsyncImage(
                    model = session.imgUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(session.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)

                Spacer(modifier = Modifier.height(6.dp))

                val statusText = when (callState) {
                    FomoCallState.OUTGOING -> "Calling E2E Encrypted Line..."
                    FomoCallState.INCOMING -> "Incoming Call..."
                    FomoCallState.CONNECTED_VOICE -> "Voice Call • $formattedDuration"
                    FomoCallState.CONNECTED_VIDEO -> "HD Video Call • $formattedDuration"
                    FomoCallState.ENDED -> "Call Ended"
                }

                Text(
                    text = statusText,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            // --- In-Call Chat Drawer Overlay (if toggled) ---
            if (showInCallChat) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .align(Alignment.Center)
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E1E24).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("In-Call Messages", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            IconButton(onClick = { showInCallChat = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            reverseLayout = true
                        ) {
                            items(callChatList.reversed()) { (sender, msg) ->
                                Text(
                                    text = "$sender: $msg",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inCallChatMessage,
                                onValueChange = { inCallChatMessage = it },
                                placeholder = { Text("Type message...", color = Color.Gray, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (inCallChatMessage.isNotBlank()) {
                                        callChatList.add(Pair("Me", inCallChatMessage))
                                        inCallChatMessage = ""
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // --- Bottom Action Controls Bar ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (callState == FomoCallState.INCOMING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decline
                        IconButton(
                            onClick = onEndCall,
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.Red)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // Accept
                        IconButton(
                            onClick = {
                                callState = if (session.callType == "VIDEO") FomoCallState.CONNECTED_VIDEO else FomoCallState.CONNECTED_VOICE
                            },
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF34C759))
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                } else {
                    // Controls Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute Mic
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = if (isMuted) Color.Black else Color.White
                            )
                        }

                        // Speakerphone
                        IconButton(
                            onClick = { isSpeaker = !isSpeaker },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaker) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Speaker",
                                tint = if (isSpeaker) Color.Black else Color.White
                            )
                        }

                        // Video / Flip Camera
                        if (session.callType == "VIDEO" || callState == FomoCallState.CONNECTED_VIDEO) {
                            IconButton(
                                onClick = { isFrontCamera = !isFrontCamera },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                            }
                        }

                        // In-Call Chat Button
                        IconButton(
                            onClick = { showInCallChat = !showInCallChat },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (showInCallChat) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "In-Call Chat", tint = if (showInCallChat) Color.Black else Color.White)
                        }

                        // Add Participant
                        IconButton(
                            onClick = { showAddParticipantSheet = true },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Participant", tint = Color.White)
                        }

                        // End Call
                        IconButton(
                            onClick = {
                                ChatRepository.addCallLog(
                                    participantName = session.name,
                                    avatarUrl = session.imgUrl,
                                    mediaType = if (session.isNightGuard) CallMediaType.NIGHTGUARD_EMERGENCY else if (session.isClubLobby) CallMediaType.CLUB_LOUNGE else if (session.callType == "VIDEO") CallMediaType.VIDEO else CallMediaType.VOICE,
                                    direction = CallDirection.OUTGOING,
                                    durationSeconds = durationSeconds,
                                    isVerified = true
                                )
                                onEndCall()
                            },
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }

    // Add Participant Modal
    if (showAddParticipantSheet) {
        AlertDialog(
            onDismissRequest = { showAddParticipantSheet = false },
            title = { Text("Add to Active Call", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    listOf("Kgomotso Mokoena", "David Beckham", "Emma Stone").forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    callChatList.add(Pair("System", "$friend joined the call"))
                                    showAddParticipantSheet = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(friend, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddParticipantSheet = false }) {
                    Text("Close", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }
}

// Top Bar with Search Field
@Composable
fun ChatsTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNewChatClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onNewChatClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "New Chat", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Indexed Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search chats, venues, messages...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.6f)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// WhatsApp-style Stories Section connected to StoryRepository
@Composable
fun StoriesSection(
    storyGroups: List<UserStoryGroup>,
    onAddStoryClick: () -> Unit,
    onStoryGroupClick: (UserStoryGroup) -> Unit
) {
    val ownStory = storyGroups.firstOrNull { it.isOwnStory }
    val friendStories = storyGroups.filter { !it.isOwnStory }

    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MyStoryAvatarItem(
                    ownStory = ownStory,
                    onAddStoryClick = onAddStoryClick,
                    onViewOwnStoryClick = { ownGroup -> onStoryGroupClick(ownGroup) }
                )
            }
            items(friendStories) { group ->
                FriendStoryAvatarItem(
                    storyGroup = group,
                    onClick = { onStoryGroupClick(group) }
                )
            }
        }
    }
}

@Composable
fun MyStoryAvatarItem(
    ownStory: UserStoryGroup?,
    onAddStoryClick: () -> Unit,
    onViewOwnStoryClick: (UserStoryGroup) -> Unit
) {
    val hasSegments = ownStory != null && ownStory.segments.isNotEmpty()
    val myImgUrl = ownStory?.userAvatarUrl ?: "https://i.pravatar.cc/150?img=12"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable {
                if (hasSegments) onViewOwnStoryClick(ownStory!!) else onAddStoryClick()
            }
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            if (hasSegments) {
                val gradient = Brush.linearGradient(colors = listOf(Color(0xFF007AFF), Color(0xFF34C759)))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(2.dp, gradient, CircleShape)
                        .padding(3.dp)
                ) {
                    AsyncImage(
                        model = myImgUrl,
                        contentDescription = "My Story",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
            } else {
                AsyncImage(
                    model = myImgUrl,
                    contentDescription = "My Story",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .align(Alignment.Center)
                )
            }

            // Glass Plus Icon Badge
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.5.dp, Color(0xFF0D0D12), CircleShape)
                    .clickable { onAddStoryClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Story", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "My Story",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FriendStoryAvatarItem(
    storyGroup: UserStoryGroup,
    onClick: () -> Unit
) {
    val gradient = Brush.linearGradient(colors = listOf(Color(0xFFFF0080), Color(0xFFFF8C00)))
    val ringBorder = when {
        storyGroup.isMuted -> Modifier.border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
        storyGroup.hasUnviewed -> Modifier.border(2.dp, gradient, CircleShape)
        else -> Modifier.border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .then(ringBorder)
                    .padding(3.dp)
            ) {
                AsyncImage(
                    model = storyGroup.userAvatarUrl,
                    contentDescription = storyGroup.userName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
            if (storyGroup.isVerified) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF0D0D12), CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            storyGroup.userName,
            color = if (storyGroup.isMuted) Color.White.copy(alpha = 0.4f) else Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Category Tabs Bar supporting Chats, Calls, Groups
@Composable
fun CategoryTabsSection(
    activeCategory: ChatCategory,
    isCallsSelected: Boolean,
    onCategorySelect: (ChatCategory) -> Unit,
    onCallsToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chats Tab
        val isChatsSelected = !isCallsSelected && activeCategory != ChatCategory.GROUPS
        Surface(
            color = if (isChatsSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.clickable { onCategorySelect(ChatCategory.ALL) }
        ) {
            Text(
                text = "Chats",
                color = if (isChatsSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                fontWeight = if (isChatsSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Calls Tab
        Surface(
            color = if (isCallsSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.clickable { onCallsToggle() }
        ) {
            Text(
                text = "Calls",
                color = if (isCallsSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                fontWeight = if (isCallsSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Groups Tab
        val isGroupsSelected = !isCallsSelected && activeCategory == ChatCategory.GROUPS
        Surface(
            color = if (isGroupsSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.clickable { onCategorySelect(ChatCategory.GROUPS) }
        ) {
            Text(
                text = "Groups",
                color = if (isGroupsSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                fontWeight = if (isGroupsSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

// Conversation List
@Composable
fun ConversationListSection(
    conversations: List<ConversationItem>,
    onConversationClick: (ConversationItem) -> Unit,
    onConversationLongPress: (ConversationItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(conversations, key = { it.id }) { conv ->
            ConversationCardItem(
                conv = conv,
                onClick = { onConversationClick(conv) },
                onLongPress = { onConversationLongPress(conv) }
            )
        }
    }
}

@Composable
fun ConversationCardItem(
    conv: ConversationItem,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.x < -30) {
                            onLongPress()
                        }
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with status indicators
        Box(modifier = Modifier.size(54.dp)) {
            AsyncImage(
                model = conv.avatarUrl,
                contentDescription = conv.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(if (conv.type == ConversationType.VENUE || conv.type == ConversationType.GROUP) RoundedCornerShape(16.dp) else CircleShape)
            )

            if (conv.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759))
                        .border(2.dp, Color(0xFF0D0D12), CircleShape)
                )
            }

            if (conv.type == ConversationType.NIGHTGUARD) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFFFF2D55))
                        .border(1.5.dp, Color(0xFF0D0D12), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center Info Block
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conv.name,
                        color = Color.White,
                        fontWeight = if (conv.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conv.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                }

                Text(
                    text = conv.lastMessageTimestamp,
                    color = if (conv.unreadCount > 0) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = if (conv.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Snippet / Typing / Draft
                val displaySnippet = when {
                    conv.draftMessageText != null -> "Draft: ${conv.draftMessageText}"
                    conv.presenceStatus == PresenceStatus.TYPING -> "Typing..."
                    conv.presenceStatus == PresenceStatus.RECORDING -> "Recording voice note..."
                    else -> conv.lastMessage
                }

                val snippetColor = when {
                    conv.draftMessageText != null -> Color(0xFFFFCC00)
                    conv.presenceStatus == PresenceStatus.TYPING || conv.presenceStatus == PresenceStatus.RECORDING -> MaterialTheme.colorScheme.primary
                    conv.unreadCount > 0 -> Color.White
                    else -> Color.White.copy(alpha = 0.6f)
                }

                Text(
                    text = displaySnippet,
                    color = snippetColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Icons: Pinned, Muted, Unread badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (conv.isMuted) {
                        Icon(Icons.Default.VolumeOff, contentDescription = "Muted", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                    }
                    if (conv.isPinned) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                    if (conv.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conv.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStatePanel(category: ChatCategory) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Forum,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No conversations in ${category.name.lowercase().replaceFirstChar { it.uppercase() }}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Conversation Context Dialog for Swipe / Long Press
@Composable
fun ConversationContextDialog(
    conversation: ConversationItem,
    onDismiss: () -> Unit,
    onPinToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onArchiveToggle: () -> Unit,
    onReadToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E24)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = conversation.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ActionRowItem(if (conversation.isPinned) "Unpin conversation" else "Pin conversation", Icons.Default.PushPin, onClick = onPinToggle)
                ActionRowItem(if (conversation.isMuted) "Unmute notifications" else "Mute notifications", Icons.Default.VolumeOff, onClick = onMuteToggle)
                ActionRowItem(if (conversation.isArchived) "Unarchive chat" else "Archive chat", Icons.Default.Archive, onClick = onArchiveToggle)
                ActionRowItem(if (conversation.unreadCount > 0) "Mark as read" else "Mark as unread", Icons.Default.MarkChatRead, onClick = onReadToggle)
                ActionRowItem("Delete conversation", Icons.Default.Delete, color = Color.Red, onClick = onDelete)
            }
        }
    }
}

// New Chat & VIP Group Creation Wizard
@Composable
fun NewChatCreationWizard(
    onDismiss: () -> Unit,
    onChatCreated: (ConversationItem) -> Unit
) {
    var mode by remember { mutableStateOf("DIRECT") } // DIRECT or GROUP
    var groupName by remember { mutableStateOf("") }
    var selectedBuddy by remember { mutableStateOf("Sarah Jenkins") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Conversation", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == "DIRECT",
                        onClick = { mode = "DIRECT" },
                        label = { Text("Direct Message") }
                    )
                    FilterChip(
                        selected = mode == "GROUP",
                        onClick = { mode = "GROUP" },
                        label = { Text("VIP Group Chat") }
                    )
                }

                if (mode == "GROUP") {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        placeholder = { Text("Group Name (e.g. VIP Saturday Crew)") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }

                Text("Select Buddy:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                listOf("Sarah Jenkins", "Mike Ross", "Emma Stone", "David Beckham").forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBuddy = name }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedBuddy == name, onClick = { selectedBuddy = name })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newConv = ConversationItem(
                        id = "conv_${System.currentTimeMillis()}",
                        name = if (mode == "GROUP") groupName.ifBlank { "New VIP Group" } else selectedBuddy,
                        avatarUrl = if (mode == "GROUP") "https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=200" else "https://i.pravatar.cc/150?img=5",
                        type = if (mode == "GROUP") ConversationType.GROUP else ConversationType.PERSONAL,
                        category = if (mode == "GROUP") ChatCategory.GROUPS else ChatCategory.PERSONAL,
                        lastMessage = "Conversation created",
                        lastMessageTimestamp = "Just now"
                    )
                    onChatCreated(newConv)
                }
            ) {
                Text("Start Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF1E1E24)
    )
}

// Conversation Wrapper linking active item to full ConversationScreen
@Composable
fun ConversationScreenWrapper(
    conversationItem: ConversationItem,
    onBack: () -> Unit
) {
    val chatData = ChatData(
        name = conversationItem.name,
        message = conversationItem.lastMessage,
        time = conversationItem.lastMessageTimestamp,
        unreadCount = conversationItem.unreadCount,
        isOnline = conversationItem.isOnline,
        type = when (conversationItem.type) {
            ConversationType.GROUP -> ChatType.GROUP
            ConversationType.VENUE -> ChatType.VENUE
            ConversationType.NIGHTGUARD -> ChatType.NIGHTGUARD
            else -> ChatType.PERSONAL
        },
        imgUrl = conversationItem.avatarUrl
    )

    ConversationScreen(chat = chatData, onBack = onBack)
}

// --- Groups & Communities Section Components ---

@Composable
fun GroupsListSection(
    repoState: ChatRepositoryState,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (ConversationItem) -> Unit,
    onGroupLongPress: (ConversationItem) -> Unit,
    onGroupDetailClick: (ConversationItem) -> Unit,
    onGroupCallClick: (ConversationItem) -> Unit
) {
    var selectedGroupSubFilter by remember { mutableStateOf("ALL") } // ALL, PINNED, NIGHTGUARD, VIP, PLANNING

    val allGroups = remember(repoState.conversations) {
        repoState.conversations.filter {
            (it.type == ConversationType.GROUP || it.type == ConversationType.BUDDY_PAIR) && !it.isArchived
        }
    }

    val pinnedGroups = remember(allGroups) {
        allGroups.filter { it.isPinned }
    }

    val filteredGroups = remember(allGroups, selectedGroupSubFilter, repoState.searchQuery) {
        allGroups.filter { group ->
            val matchesSearch = repoState.searchQuery.isEmpty() ||
                    group.name.contains(repoState.searchQuery, ignoreCase = true) ||
                    group.lastMessage.contains(repoState.searchQuery, ignoreCase = true)

            val matchesSub = when (selectedGroupSubFilter) {
                "PINNED" -> group.isPinned
                "NIGHTGUARD" -> group.type == ConversationType.BUDDY_PAIR || group.name.contains("NightGuard") || group.name.contains("Safety")
                "VIP" -> group.name.contains("VIP") || group.name.contains("Lounge") || group.name.contains("Club")
                "PLANNING" -> group.lastMessage.contains("Plan") || group.lastMessage.contains("RSVP") || group.lastMessage.contains("Pass")
                else -> true
            }

            matchesSearch && matchesSub
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        // --- Header Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Groups & Communities",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${allGroups.size} active social crews",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onCreateGroupClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Group", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // --- Pinned Groups Horizontal Cards ---
        if (pinnedGroups.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)) {
                    Text(
                        text = "PINNED CREWS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pinnedGroups, key = { it.id }) { group ->
                            PinnedGroupCardItem(
                                group = group,
                                onClick = { onGroupClick(group) },
                                onCallClick = { onGroupCallClick(group) },
                                onDashboardClick = { onGroupDetailClick(group) }
                            )
                        }
                    }
                }
            }
        }

        // --- Sub-Filter Chips ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "PINNED", "NIGHTGUARD", "VIP", "PLANNING").forEach { subFilter ->
                    val isSel = selectedGroupSubFilter == subFilter
                    Surface(
                        color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.clickable { selectedGroupSubFilter = subFilter }
                    ) {
                        Text(
                            text = when (subFilter) {
                                "NIGHTGUARD" -> "🛡️ NightGuard"
                                "VIP" -> "👑 VIP Lounges"
                                "PLANNING" -> "🗓️ Plans"
                                else -> subFilter.lowercase().replaceFirstChar { it.uppercase() }
                            },
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // --- Vertical List of Groups ---
        if (filteredGroups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No group chats found", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(filteredGroups, key = { it.id }) { group ->
                GroupRowItem(
                    group = group,
                    onClick = { onGroupClick(group) },
                    onLongClick = { onGroupLongPress(group) },
                    onCallClick = { onGroupCallClick(group) },
                    onDashboardClick = { onGroupDetailClick(group) }
                )
            }
        }
    }
}

@Composable
fun PinnedGroupCardItem(
    group: ConversationItem,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onDashboardClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1E24),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(44.dp)) {
                    AsyncImage(
                        model = group.avatarUrl,
                        contentDescription = group.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, tint = Color.Black, modifier = Modifier.size(10.dp))
                    }
                }

                Row {
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDashboardClick,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Dashboard", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = group.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${group.memberCount} members • Live Event",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = group.lastMessage,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GroupRowItem(
    group: ConversationItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCallClick: () -> Unit,
    onDashboardClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group Avatar
            Box(modifier = Modifier.size(54.dp)) {
                AsyncImage(
                    model = group.avatarUrl,
                    contentDescription = group.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                )

                if (group.name.contains("NightGuard") || group.type == ConversationType.BUDDY_PAIR) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFFFF2D55)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                } else if (group.isVerified || group.name.contains("VIP")) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(11.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Info Block
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    group.groupRole?.let { role ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = role.name,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${group.memberCount} members",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = " • ${group.lastMessageTimestamp}",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = group.lastMessage,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons / Unread Badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (group.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = group.unreadCount.toString(),
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Group Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDashboardClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Group Info", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onGroupCreated: (ConversationItem) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPresetImage by remember { mutableStateOf("https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=200") }
    var isAnnouncementOnly by remember { mutableStateOf(false) }
    var isPublicGroup by remember { mutableStateOf(false) }

    val contacts = listOf(
        Pair("Sarah Jenkins", "https://i.pravatar.cc/150?img=5"),
        Pair("Kgomotso Mokoena", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200"),
        Pair("Mike Ross", "https://i.pravatar.cc/150?img=11"),
        Pair("Emma Stone", "https://i.pravatar.cc/150?img=9"),
        Pair("David Beckham", "https://i.pravatar.cc/150?img=12")
    )

    val selectedMembers = remember { mutableStateListOf("Sarah Jenkins", "Kgomotso Mokoena") }

    val presets = listOf(
        Pair("VIP Nightout", "https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=200"),
        Pair("NightGuard Squad", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200"),
        Pair("Festival Crew", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=200"),
        Pair("Campus Lounge", "https://images.unsplash.com/photo-1522071820081-009f0129c71c?q=80&w=200")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Group", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name *") },
                    placeholder = { Text("e.g., Friday Night VIP Crew") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Topic / Description") },
                    placeholder = { Text("What is this group for?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Cover Preset Selection
                Text("Select Cover Image:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { (label, url) ->
                        val isSel = selectedPresetImage == url
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedPresetImage = url }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(if (isSel) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Text(label, color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Select Members
                Text("Add Initial Members (${selectedMembers.size}):", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                contacts.forEach { (name, img) ->
                    val isChecked = selectedMembers.contains(name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedMembers.remove(name) else selectedMembers.add(name)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                if (it) selectedMembers.add(name) else selectedMembers.remove(name)
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AsyncImage(model = img, contentDescription = null, modifier = Modifier.size(24.dp).clip(CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Only Admins can send messages", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Switch(checked = isAnnouncementOnly, onCheckedChange = { isAnnouncementOnly = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        val newGroup = ChatRepository.createGroupConversation(
                            groupName = groupName,
                            description = description,
                            avatarUrl = selectedPresetImage,
                            isPublic = isPublicGroup,
                            memberNames = listOf("You") + selectedMembers
                        )
                        onGroupCreated(newGroup)
                    }
                },
                enabled = groupName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Create Group", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF1E1E24)
    )
}

@Composable
fun GroupDetailDashboardModal(
    group: ConversationItem,
    onDismiss: () -> Unit,
    onOpenChat: (ConversationItem) -> Unit,
    onStartCall: (ConversationItem) -> Unit
) {
    var activeTab by remember { mutableStateOf("INFO") } // INFO, MEMBERS, PLANS, POLLS
    var isNightGuardEnabled by remember { mutableStateOf(group.type == ConversationType.BUDDY_PAIR || group.name.contains("NightGuard")) }
    var selectedRsvp by remember { mutableStateOf("ATTENDING") } // ATTENDING, MAYBE, DECLINED

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = group.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(group.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                        Text("${group.memberCount} members • Encrypted", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("INFO", "MEMBERS", "PLANS", "POLLS").forEach { tab ->
                        val isSel = activeTab == tab
                        Surface(
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { activeTab = tab }
                        ) {
                            Text(
                                text = tab.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (isSel) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                when (activeTab) {
                    "INFO" -> {
                        Column {
                            // NightGuard Safety Escort Card
                            Surface(
                                color = if (isNightGuardEnabled) Color(0xFFFF2D55).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isNightGuardEnabled) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("NightGuard Safety Escort", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Share live GPS location with group members during night outs", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                    Switch(checked = isNightGuardEnabled, onCheckedChange = { isNightGuardEnabled = it })
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Group Invite Link Box
                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Group Invite Link", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("fomo.app/g/${group.id}", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onStartCall(group) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Call", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onOpenChat(group) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Chat", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    "MEMBERS" -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            val memberList = listOf(
                                Triple("You", "Group Owner • Online", "https://i.pravatar.cc/150?img=33"),
                                Triple("Sarah Jenkins", "Admin • Online", "https://i.pravatar.cc/150?img=5"),
                                Triple("Kgomotso Mokoena", "Member • Live GPS", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200"),
                                Triple("Mike Ross", "Member • Offline", "https://i.pravatar.cc/150?img=11"),
                                Triple("Emma Stone", "Member • 10m ago", "https://i.pravatar.cc/150?img=9")
                            )
                            items(memberList) { (name, sub, img) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(model = img, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(sub, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { }) {
                                        Icon(Icons.Default.Message, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    "PLANS" -> {
                        Column {
                            Surface(
                                color = Color(0xFF2A2A35),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Friday Night VIP @ Cocoon Club", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Tonight • 10:30 PM • Zone 7 Rosebank", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text("6 members attending • Table #4 Reserved", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("ATTENDING" to "I'm In! 🎉", "MAYBE" to "Maybe 🤔", "DECLINED" to "Can't Make It").forEach { (key, label) ->
                                            val isSel = selectedRsvp == key
                                            Button(
                                                onClick = { selectedRsvp = key },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(label, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "POLLS" -> {
                        Column {
                            Text("Active Venue Voting Poll:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            listOf(
                                Pair("Cocoon Club Rosebank", 6),
                                Pair("Madison Avenue Rivonia", 2),
                                Pair("Zone 7 Night Lounge", 1)
                            ).forEach { (venue, votes) ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(venue, color = Color.White, fontSize = 12.sp)
                                        Text("$votes votes", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF1E1E24)
    )
}
