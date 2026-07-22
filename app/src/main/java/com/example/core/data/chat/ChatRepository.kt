package com.example.core.data.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

// Category Filters
enum class ChatCategory {
    ALL, UNREAD, PINNED, PERSONAL, GROUPS, BUSINESSES, VENUES, ARCHIVED
}

// Conversation Classification
enum class ConversationType {
    PERSONAL, GROUP, VENUE, BUSINESS, EVENT, NIGHTGUARD, BUDDY_PAIR
}

// Presence Engine States
enum class PresenceStatus {
    ONLINE, OFFLINE, TYPING, RECORDING, IN_CALL, IN_VIDEO_CALL, LIVE_ON_FOMO, SHARING_LIVE_LOCATION
}

// Group Roles
enum class GroupRole {
    OWNER, ADMIN, MODERATOR, MEMBER, READ_ONLY
}

// Granular Permissions Model
data class GroupPermissions(
    val canSendMessages: Boolean = true,
    val canSendMedia: Boolean = true,
    val canAddMembers: Boolean = true,
    val canChangeInfo: Boolean = false,
    val canPinMessages: Boolean = true,
    val canCreatePolls: Boolean = true,
    val isAnnouncementOnly: Boolean = false
)

// Poll Data Model
data class PollOption(
    val id: String,
    val text: String,
    val votesCount: Int = 0,
    val voterAvatars: List<String> = emptyList()
)

data class GroupPoll(
    val id: String,
    val question: String,
    val options: List<PollOption>,
    val userVotedOptionId: String? = null,
    val totalVotes: Int = 0,
    val isClosed: Boolean = false
)

// Rich Message Types
enum class RichMessageType {
    TEXT, EMOJI, GIF, STICKER, IMAGE, VIDEO, AUDIO, VOICE_NOTE,
    DOCUMENT, CONTACT, LOCATION, LIVE_LOCATION,
    VENUE_CARD, EVENT_CARD, MOMENT_CARD, ROUTE_CARD, FLASH_DROP_CARD, TICKET_QR_CARD, POLL_CARD, BUDDY_PAIR_CARD, WALK_ME_HOME_CARD, SAFETY_CHECK_CARD
}

// Message Data Model
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String, // "me" or peer id
    val senderName: String,
    val senderAvatarUrl: String,
    val type: RichMessageType,
    val content: String,
    val timestamp: String,
    val isDelivered: Boolean = true,
    val isRead: Boolean = true,
    val isPendingOffline: Boolean = false,
    val isEdited: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reactions: Map<String, Int> = emptyMap(), // emoji -> count
    val myReactions: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(), // Custom card payload
    val poll: GroupPoll? = null
)

// Attachment Upload Status
enum class UploadStatus {
    QUEUED, UPLOADING, PAUSED, COMPLETED, FAILED
}

data class AttachmentUploadItem(
    val id: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val progressPercent: Int,
    val status: UploadStatus,
    val thumbnailUrl: String? = null
)

// Call Management Models
enum class CallDirection {
    INCOMING, OUTGOING, MISSED, REJECTED
}

enum class CallMediaType {
    VOICE, VIDEO, GROUP_VOICE, GROUP_VIDEO, NIGHTGUARD_EMERGENCY, CLUB_LOUNGE
}

data class CallLogItem(
    val id: String,
    val participantName: String,
    val participantAvatarUrl: String,
    val mediaType: CallMediaType,
    val direction: CallDirection,
    val timestampText: String,
    val durationSeconds: Int = 0,
    val isVerified: Boolean = false,
    val isFavorite: Boolean = false,
    val roomName: String? = null,
    val participantCount: Int = 2
)

// Conversation Item Model
data class ConversationItem(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val type: ConversationType,
    val category: ChatCategory,
    val lastMessage: String,
    val lastMessageTimestamp: String,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val presenceStatus: PresenceStatus = PresenceStatus.OFFLINE,
    val typingIndicatorText: String? = null,
    val draftMessageText: String? = null,
    val groupRole: GroupRole? = null,
    val memberCount: Int = 1,
    val e2eEncrypted: Boolean = true,
    val e2eKeyFingerprint: String = "4F89-A2C3-90BF-1102"
)

// State for Chat Management Repository
data class ChatRepositoryState(
    val activeCategory: ChatCategory = ChatCategory.ALL,
    val searchQuery: String = "",
    val conversations: List<ConversationItem> = emptyList(),
    val callLogs: List<CallLogItem> = emptyList(),
    val activeConversationId: String? = null,
    val activeMessages: Map<String, List<ChatMessage>> = emptyMap(), // conversationId -> list
    val pendingOfflineQueue: List<ChatMessage> = emptyList(),
    val isNetworkOnline: Boolean = true,
    val activeUploads: List<AttachmentUploadItem> = emptyList(),
    val e2eVerificationVerified: Boolean = true,
    val draftMap: Map<String, String> = emptyMap() // conversationId -> draft
)

object ChatRepository {
    private val _state = MutableStateFlow(
        ChatRepositoryState(
            conversations = seedInitialConversations(),
            callLogs = seedInitialCallLogs(),
            activeMessages = seedInitialMessages()
        )
    )
    val state: StateFlow<ChatRepositoryState> = _state.asStateFlow()

    // --- Group Creation & Management Actions ---
    fun createGroupConversation(
        groupName: String,
        description: String = "",
        avatarUrl: String = "",
        isPublic: Boolean = false,
        memberNames: List<String> = listOf("You", "Sarah Jenkins", "Kgomotso Mokoena")
    ): ConversationItem {
        val newGroupId = "group_${System.currentTimeMillis()}"
        val defaultAvatar = if (avatarUrl.isNotBlank()) avatarUrl else "https://images.unsplash.com/photo-1522071820081-009f0129c71c?q=80&w=200"
        val newGroup = ConversationItem(
            id = newGroupId,
            name = groupName,
            avatarUrl = defaultAvatar,
            type = ConversationType.GROUP,
            category = ChatCategory.GROUPS,
            lastMessage = "Group created: $groupName. Welcome everyone!",
            lastMessageTimestamp = "Just now",
            unreadCount = 0,
            isPinned = false,
            isMuted = false,
            isArchived = false,
            isVerified = true,
            groupRole = GroupRole.OWNER,
            memberCount = memberNames.size,
            e2eEncrypted = true
        )

        val initialMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = newGroupId,
            senderId = "user_me",
            senderName = "You",
            senderAvatarUrl = "https://i.pravatar.cc/150?img=33",
            type = RichMessageType.TEXT,
            content = "Group '$groupName' was created. $description",
            timestamp = "Just now",
            isDelivered = true,
            isRead = true
        )

        _state.update { current ->
            val updatedConvs = listOf(newGroup) + current.conversations
            val updatedMsgs = current.activeMessages.toMutableMap()
            updatedMsgs[newGroupId] = listOf(initialMsg)
            current.copy(conversations = updatedConvs, activeMessages = updatedMsgs)
        }

        return newGroup
    }

    // --- Calls Repository Actions ---
    fun addCallLog(
        participantName: String,
        avatarUrl: String,
        mediaType: CallMediaType,
        direction: CallDirection,
        durationSeconds: Int = 0,
        roomName: String? = null,
        isVerified: Boolean = false
    ) {
        val newLog = CallLogItem(
            id = "call_${System.currentTimeMillis()}",
            participantName = participantName,
            participantAvatarUrl = avatarUrl,
            mediaType = mediaType,
            direction = direction,
            timestampText = "Just now",
            durationSeconds = durationSeconds,
            isVerified = isVerified,
            roomName = roomName
        )
        _state.update { current ->
            current.copy(callLogs = listOf(newLog) + current.callLogs)
        }
    }

    fun deleteCallLog(id: String) {
        _state.update { current ->
            current.copy(callLogs = current.callLogs.filterNot { it.id == id })
        }
    }

    fun clearAllCallLogs() {
        _state.update { current ->
            current.copy(callLogs = emptyList())
        }
    }

    fun toggleCallFavorite(id: String) {
        _state.update { current ->
            current.copy(
                callLogs = current.callLogs.map {
                    if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
                }
            )
        }
    }

    // --- Category Selection ---
    fun setCategory(category: ChatCategory) {
        _state.update { it.copy(activeCategory = category) }
    }

    // --- Global Indexed Search ---
    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    // --- Swipe & Management Actions ---
    fun togglePinConversation(id: String) {
        _state.update { current ->
            current.copy(
                conversations = current.conversations.map {
                    if (it.id == id) it.copy(isPinned = !it.isPinned) else it
                }
            )
        }
    }

    fun toggleMuteConversation(id: String) {
        _state.update { current ->
            current.copy(
                conversations = current.conversations.map {
                    if (it.id == id) it.copy(isMuted = !it.isMuted) else it
                }
            )
        }
    }

    fun toggleArchiveConversation(id: String) {
        _state.update { current ->
            current.copy(
                conversations = current.conversations.map {
                    if (it.id == id) it.copy(isArchived = !it.isArchived) else it
                }
            )
        }
    }

    fun markConversationRead(id: String) {
        _state.update { current ->
            current.copy(
                conversations = current.conversations.map {
                    if (it.id == id) it.copy(unreadCount = 0) else it
                }
            )
        }
    }

    fun markConversationUnread(id: String) {
        _state.update { current ->
            current.copy(
                conversations = current.conversations.map {
                    if (it.id == id) it.copy(unreadCount = 1) else it
                }
            )
        }
    }

    fun deleteConversation(id: String) {
        _state.update { current ->
            current.copy(
                conversations = current.conversations.filter { it.id != id },
                activeMessages = current.activeMessages.filterKeys { it != id }
            )
        }
    }

    // --- Draft Actions ---
    fun saveDraft(conversationId: String, text: String) {
        _state.update { current ->
            val updatedDrafts = current.draftMap.toMutableMap()
            if (text.isBlank()) {
                updatedDrafts.remove(conversationId)
            } else {
                updatedDrafts[conversationId] = text
            }
            val updatedConvs = current.conversations.map {
                if (it.id == conversationId) {
                    it.copy(draftMessageText = if (text.isBlank()) null else text)
                } else it
            }
            current.copy(draftMap = updatedDrafts, conversations = updatedConvs)
        }
    }

    // --- Messaging Engine ---
    fun sendMessage(
        conversationId: String,
        type: RichMessageType,
        content: String,
        metadata: Map<String, String> = emptyMap(),
        replyToMsg: ChatMessage? = null,
        poll: GroupPoll? = null
    ) {
        val currentIsOnline = _state.value.isNetworkOnline
        val newId = UUID.randomUUID().toString()
        val timestamp = "10:08 PM"

        val newMsg = ChatMessage(
            id = newId,
            conversationId = conversationId,
            senderId = "me",
            senderName = "Me",
            senderAvatarUrl = "https://i.pravatar.cc/150?img=12",
            type = type,
            content = content,
            timestamp = timestamp,
            isDelivered = currentIsOnline,
            isRead = currentIsOnline,
            isPendingOffline = !currentIsOnline,
            replyToMessageId = replyToMsg?.id,
            replyToText = replyToMsg?.content,
            replyToSender = replyToMsg?.senderName,
            metadata = metadata,
            poll = poll
        )

        _state.update { current ->
            val conversationMsgs = current.activeMessages[conversationId]?.toMutableList() ?: mutableListOf()
            conversationMsgs.add(newMsg)

            val updatedMap = current.activeMessages.toMutableMap()
            updatedMap[conversationId] = conversationMsgs

            val updatedQueue = if (!currentIsOnline) current.pendingOfflineQueue + newMsg else current.pendingOfflineQueue

            // Update conversation snippet
            val updatedConvs = current.conversations.map {
                if (it.id == conversationId) {
                    it.copy(
                        lastMessage = if (type == RichMessageType.TEXT) content else "[${type.name.lowercase().replace('_', ' ')}]",
                        lastMessageTimestamp = timestamp,
                        draftMessageText = null
                    )
                } else it
            }

            // Clear draft
            val drafts = current.draftMap.toMutableMap()
            drafts.remove(conversationId)

            current.copy(
                activeMessages = updatedMap,
                pendingOfflineQueue = updatedQueue,
                conversations = updatedConvs,
                draftMap = drafts
            )
        }
    }

    // --- Message Reactions ---
    fun toggleReaction(conversationId: String, messageId: String, emoji: String) {
        _state.update { current ->
            val msgs = current.activeMessages[conversationId]?.toMutableList() ?: return@update current
            val idx = msgs.indexOfFirst { it.id == messageId }
            if (idx == -1) return@update current

            val target = msgs[idx]
            val reactions = target.reactions.toMutableMap()
            val myReactions = target.myReactions.toMutableList()

            if (myReactions.contains(emoji)) {
                myReactions.remove(emoji)
                val count = reactions[emoji] ?: 1
                if (count <= 1) reactions.remove(emoji) else reactions[emoji] = count - 1
            } else {
                myReactions.add(emoji)
                reactions[emoji] = (reactions[emoji] ?: 0) + 1
            }

            msgs[idx] = target.copy(reactions = reactions, myReactions = myReactions)
            val updatedMap = current.activeMessages.toMutableMap()
            updatedMap[conversationId] = msgs
            current.copy(activeMessages = updatedMap)
        }
    }

    // --- Group Poll Voting ---
    fun voteInPoll(conversationId: String, messageId: String, optionId: String) {
        _state.update { current ->
            val msgs = current.activeMessages[conversationId]?.toMutableList() ?: return@update current
            val idx = msgs.indexOfFirst { it.id == messageId }
            if (idx == -1) return@update current

            val target = msgs[idx]
            val existingPoll = target.poll ?: return@update current

            val updatedOptions = existingPoll.options.map { option ->
                if (option.id == optionId) {
                    option.copy(
                        votesCount = option.votesCount + 1,
                        voterAvatars = option.voterAvatars + "https://i.pravatar.cc/150?img=12"
                    )
                } else if (option.id == existingPoll.userVotedOptionId) {
                    option.copy(
                        votesCount = (option.votesCount - 1).coerceAtLeast(0),
                        voterAvatars = option.voterAvatars.filter { it != "https://i.pravatar.cc/150?img=12" }
                    )
                } else option
            }

            val updatedPoll = existingPoll.copy(
                options = updatedOptions,
                userVotedOptionId = optionId,
                totalVotes = existingPoll.totalVotes + (if (existingPoll.userVotedOptionId == null) 1 else 0)
            )

            msgs[idx] = target.copy(poll = updatedPoll)
            val updatedMap = current.activeMessages.toMutableMap()
            updatedMap[conversationId] = msgs
            current.copy(activeMessages = updatedMap)
        }
    }

    // --- Offline Network Simulation & Automatic Queue Sync ---
    fun setNetworkStatus(isOnline: Boolean) {
        _state.update { current ->
            if (isOnline && !current.isNetworkOnline) {
                // Perform sync merge of pending queue
                val queue = current.pendingOfflineQueue
                val updatedMessages = current.activeMessages.mapValues { (_, msgList) ->
                    msgList.map { msg ->
                        if (msg.isPendingOffline) msg.copy(isPendingOffline = false, isDelivered = true, isRead = true) else msg
                    }
                }
                current.copy(
                    isNetworkOnline = true,
                    activeMessages = updatedMessages,
                    pendingOfflineQueue = emptyList()
                )
            } else {
                current.copy(isNetworkOnline = isOnline)
            }
        }
    }

    // --- Attachments & Background Upload Manager ---
    fun addAttachmentToQueue(fileName: String, sizeBytes: Long, mimeType: String) {
        val newUpload = AttachmentUploadItem(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            fileSizeBytes = sizeBytes,
            mimeType = mimeType,
            progressPercent = 10,
            status = UploadStatus.UPLOADING,
            thumbnailUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=200"
        )
        _state.update { current ->
            current.copy(activeUploads = current.activeUploads + newUpload)
        }
    }

    fun updateUploadProgress(uploadId: String, progressPercent: Int, status: UploadStatus) {
        _state.update { current ->
            current.copy(
                activeUploads = current.activeUploads.map {
                    if (it.id == uploadId) it.copy(progressPercent = progressPercent, status = status) else it
                }
            )
        }
    }

    // --- Seed Helpers ---
    private fun seedInitialConversations(): List<ConversationItem> {
        return listOf(
            ConversationItem(
                id = "conv_1",
                name = "Sarah Jenkins",
                avatarUrl = "https://i.pravatar.cc/150?img=5",
                type = ConversationType.PERSONAL,
                category = ChatCategory.PERSONAL,
                lastMessage = "Are we still on for tonight at Cocoon?",
                lastMessageTimestamp = "2m",
                unreadCount = 3,
                isPinned = true,
                isOnline = true,
                presenceStatus = PresenceStatus.ONLINE
            ),
            ConversationItem(
                id = "conv_2",
                name = "NightGuard: Sarah's Companion",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200",
                type = ConversationType.NIGHTGUARD,
                category = ChatCategory.ALL,
                lastMessage = "Walk Me Home route initialized • ETA 12 mins",
                lastMessageTimestamp = "15m",
                unreadCount = 1,
                isPinned = true,
                isOnline = true,
                presenceStatus = PresenceStatus.SHARING_LIVE_LOCATION
            ),
            ConversationItem(
                id = "conv_3",
                name = "Friday Night VIP Crew 🔥",
                avatarUrl = "https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=200",
                type = ConversationType.GROUP,
                category = ChatCategory.GROUPS,
                lastMessage = "David: I bought 5 VIP Flash Drop Passes!",
                lastMessageTimestamp = "45m",
                unreadCount = 0,
                isPinned = false,
                groupRole = GroupRole.ADMIN,
                memberCount = 8
            ),
            ConversationItem(
                id = "conv_4",
                name = "Cocoon Club Official",
                avatarUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=200",
                type = ConversationType.VENUE,
                category = ChatCategory.VENUES,
                lastMessage = "Guest list closing in 20 minutes!",
                lastMessageTimestamp = "2h",
                unreadCount = 0,
                isVerified = true,
                isPinned = false
            ),
            ConversationItem(
                id = "conv_5",
                name = "Mike Ross",
                avatarUrl = "https://i.pravatar.cc/150?img=11",
                type = ConversationType.PERSONAL,
                category = ChatCategory.PERSONAL,
                lastMessage = "Check out this Route Card for Rosebank",
                lastMessageTimestamp = "4h",
                unreadCount = 0,
                isOnline = false,
                presenceStatus = PresenceStatus.OFFLINE
            ),
            ConversationItem(
                id = "conv_6",
                name = "Ultra Music Festival Organizers",
                avatarUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=200",
                type = ConversationType.EVENT,
                category = ChatCategory.BUSINESSES,
                lastMessage = "Your VIP Access QR Code is ready!",
                lastMessageTimestamp = "1d",
                unreadCount = 0,
                isVerified = true
            ),
            ConversationItem(
                id = "conv_7",
                name = "Archived Summer Squad",
                avatarUrl = "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?q=80&w=200",
                type = ConversationType.GROUP,
                category = ChatCategory.ARCHIVED,
                lastMessage = "Memories from last season!",
                lastMessageTimestamp = "2w",
                isArchived = true,
                memberCount = 12
            )
        )
    }

    private fun seedInitialMessages(): Map<String, List<ChatMessage>> {
        val map = mutableMapOf<String, List<ChatMessage>>()

        map["conv_1"] = listOf(
            ChatMessage(
                id = "m1",
                conversationId = "conv_1",
                senderId = "peer_sarah",
                senderName = "Sarah Jenkins",
                senderAvatarUrl = "https://i.pravatar.cc/150?img=5",
                type = RichMessageType.TEXT,
                content = "Hey! Are you excited for tonight's VIP launch?",
                timestamp = "10:00 PM"
            ),
            ChatMessage(
                id = "m2",
                conversationId = "conv_1",
                senderId = "me",
                senderName = "Me",
                senderAvatarUrl = "https://i.pravatar.cc/150?img=12",
                type = RichMessageType.TEXT,
                content = "Absolutely! Where are we meeting first?",
                timestamp = "10:01 PM"
            ),
            ChatMessage(
                id = "m3",
                conversationId = "conv_1",
                senderId = "peer_sarah",
                senderName = "Sarah Jenkins",
                senderAvatarUrl = "https://i.pravatar.cc/150?img=5",
                type = RichMessageType.VENUE_CARD,
                content = "Cocoon Club Rosebank",
                timestamp = "10:02 PM",
                metadata = mapOf(
                    "image" to "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=300",
                    "name" to "Cocoon Club Rosebank",
                    "distance" to "0.8 km",
                    "status" to "Open • Until 6:00 AM",
                    "crowd" to "92% Full"
                )
            ),
            ChatMessage(
                id = "m4",
                conversationId = "conv_1",
                senderId = "peer_sarah",
                senderName = "Sarah Jenkins",
                senderAvatarUrl = "https://i.pravatar.cc/150?img=5",
                type = RichMessageType.FLASH_DROP_CARD,
                content = "Free Tequila Shots Voucher",
                timestamp = "10:03 PM",
                metadata = mapOf(
                    "title" to "VIP Tequila Flash Drop",
                    "code" to "FOMO-VIP-992",
                    "expires" to "In 18 minutes",
                    "venue" to "Cocoon Club"
                )
            )
        )

        map["conv_3"] = listOf(
            ChatMessage(
                id = "m301",
                conversationId = "conv_3",
                senderId = "peer_david",
                senderName = "David",
                senderAvatarUrl = "https://i.pravatar.cc/150?img=33",
                type = RichMessageType.POLL_CARD,
                content = "Pre-party Location Poll",
                timestamp = "09:30 PM",
                poll = GroupPoll(
                    id = "poll_1",
                    question = "Where are we having pre-drinks?",
                    options = listOf(
                        PollOption("o1", "Rooftop Lounge Rosebank", 3, listOf("https://i.pravatar.cc/150?img=5", "https://i.pravatar.cc/150?img=11")),
                        PollOption("o2", "Kabu Skybar", 2, listOf("https://i.pravatar.cc/150?img=9")),
                        PollOption("o3", "Straight to Venue", 1, emptyList())
                    ),
                    userVotedOptionId = "o1",
                    totalVotes = 6
                )
            )
        )

        return map
    }

    private fun seedInitialCallLogs(): List<CallLogItem> {
        return listOf(
            CallLogItem(
                id = "call_1",
                participantName = "Sarah Jenkins",
                participantAvatarUrl = "https://i.pravatar.cc/150?img=5",
                mediaType = CallMediaType.VIDEO,
                direction = CallDirection.INCOMING,
                timestampText = "10 mins ago",
                durationSeconds = 845,
                isVerified = true,
                isFavorite = true
            ),
            CallLogItem(
                id = "call_2",
                participantName = "NightGuard Emergency Line",
                participantAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200",
                mediaType = CallMediaType.NIGHTGUARD_EMERGENCY,
                direction = CallDirection.OUTGOING,
                timestampText = "1 hour ago",
                durationSeconds = 120,
                isVerified = true,
                isFavorite = true
            ),
            CallLogItem(
                id = "call_3",
                participantName = "Kgomotso & Crew",
                participantAvatarUrl = "https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=200",
                mediaType = CallMediaType.GROUP_VOICE,
                direction = CallDirection.MISSED,
                timestampText = "2 hours ago",
                durationSeconds = 0,
                isFavorite = false,
                participantCount = 5
            ),
            CallLogItem(
                id = "call_4",
                participantName = "Cocoon VIP Club Lounge",
                participantAvatarUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=200",
                mediaType = CallMediaType.CLUB_LOUNGE,
                direction = CallDirection.INCOMING,
                timestampText = "Yesterday, 11:30 PM",
                durationSeconds = 2100,
                roomName = "Stage 1 • VIP Pre-Party Lounge",
                participantCount = 18
            ),
            CallLogItem(
                id = "call_5",
                participantName = "Mike Ross",
                participantAvatarUrl = "https://i.pravatar.cc/150?img=11",
                mediaType = CallMediaType.VOICE,
                direction = CallDirection.OUTGOING,
                timestampText = "Yesterday, 08:15 PM",
                durationSeconds = 312,
                isFavorite = true
            ),
            CallLogItem(
                id = "call_6",
                participantName = "Emma Stone",
                participantAvatarUrl = "https://i.pravatar.cc/150?img=49",
                mediaType = CallMediaType.VIDEO,
                direction = CallDirection.MISSED,
                timestampText = "2 days ago",
                durationSeconds = 0,
                isFavorite = false
            )
        )
    }
}
