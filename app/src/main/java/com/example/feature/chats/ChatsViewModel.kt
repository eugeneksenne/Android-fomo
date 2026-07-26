package com.example.feature.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.chat.AttachmentUploadItem
import com.example.core.data.chat.CallLogItem
import com.example.core.data.chat.ChatCategory
import com.example.core.data.chat.ChatMessage
import com.example.core.data.chat.ChatRepository
import com.example.core.data.chat.ConversationItem
import com.example.core.data.chat.GroupPoll
import com.example.core.data.chat.RichMessageType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State for the Chats feature screens.
 */
data class ChatsUiState(
    val activeCategory: ChatCategory = ChatCategory.ALL,
    val searchQuery: String = "",
    val conversations: List<ConversationItem> = emptyList(),
    val filteredConversations: List<ConversationItem> = emptyList(),
    val activeConversationId: String? = null,
    val activeConversation: ConversationItem? = null,
    val activeMessages: List<ChatMessage> = emptyList(),
    val callLogs: List<CallLogItem> = emptyList(),
    val isNetworkOnline: Boolean = true,
    val activeUploads: List<AttachmentUploadItem> = emptyList(),
    val unreadTotalCount: Int = 0
)

/**
 * ViewModel layer for the Chats feature.
 * Connects Jetpack Compose UI with real-time Firestore message streams, presence state,
 * message operations, media uploads, and safety features in ChatRepository.
 */
class ChatsViewModel : ViewModel() {

    private val repository = ChatRepository

    val uiState: StateFlow<ChatsUiState> = repository.state
        .map { repoState ->
            val query = repoState.searchQuery.trim().lowercase()
            val filtered = repoState.conversations.filter { conv ->
                val matchesCategory = when (repoState.activeCategory) {
                    ChatCategory.ALL -> !conv.isArchived
                    ChatCategory.UNREAD -> !conv.isArchived && conv.unreadCount > 0
                    ChatCategory.PINNED -> !conv.isArchived && conv.isPinned
                    ChatCategory.PERSONAL -> !conv.isArchived && conv.category == ChatCategory.PERSONAL
                    ChatCategory.GROUPS -> !conv.isArchived && conv.category == ChatCategory.GROUPS
                    ChatCategory.BUSINESSES -> !conv.isArchived && conv.category == ChatCategory.BUSINESSES
                    ChatCategory.VENUES -> !conv.isArchived && conv.category == ChatCategory.VENUES
                    ChatCategory.ARCHIVED -> conv.isArchived
                }
                val matchesQuery = query.isEmpty() ||
                        conv.name.lowercase().contains(query) ||
                        conv.lastMessage.lowercase().contains(query)

                matchesCategory && matchesQuery
            }

            val activeConv = repoState.activeConversationId?.let { id ->
                repoState.conversations.find { it.id == id }
            }

            val currentMsgs = repoState.activeConversationId?.let { id ->
                repoState.activeMessages[id] ?: emptyList()
            } ?: emptyList()

            val totalUnread = repoState.conversations
                .filterNot { it.isArchived }
                .sumOf { it.unreadCount }

            ChatsUiState(
                activeCategory = repoState.activeCategory,
                searchQuery = repoState.searchQuery,
                conversations = repoState.conversations,
                filteredConversations = filtered,
                activeConversationId = repoState.activeConversationId,
                activeConversation = activeConv,
                activeMessages = currentMsgs,
                callLogs = repoState.callLogs,
                isNetworkOnline = repoState.isNetworkOnline,
                activeUploads = repoState.activeUploads,
                unreadTotalCount = totalUnread
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatsUiState()
        )

    fun setCategory(category: ChatCategory) {
        repository.setCategory(category)
    }

    fun setSearchQuery(query: String) {
        repository.setSearchQuery(query)
    }

    fun selectConversation(conversationId: String?) {
        repository.setActiveConversation(conversationId)
        if (conversationId != null) {
            repository.markAsRead(conversationId)
        }
    }

    fun sendMessage(
        conversationId: String,
        type: RichMessageType,
        content: String,
        metadata: Map<String, String>? = null,
        poll: GroupPoll? = null
    ) {
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = conversationId,
                type = type,
                content = content,
                metadata = metadata ?: emptyMap(),
                poll = poll
            )
        }
    }

    fun editMessage(conversationId: String, messageId: String, newContent: String) {
        viewModelScope.launch {
            repository.editMessage(conversationId, messageId, newContent)
        }
    }

    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(conversationId, messageId)
        }
    }

    fun addReaction(conversationId: String, messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.addReaction(conversationId, messageId, emoji)
        }
    }

    fun voteInPoll(conversationId: String, messageId: String, optionId: String) {
        viewModelScope.launch {
            repository.voteInPoll(conversationId, messageId, optionId)
        }
    }

    fun updateDraft(conversationId: String, draftText: String) {
        repository.saveDraft(conversationId, draftText)
    }

    fun togglePin(conversationId: String) {
        repository.togglePin(conversationId)
    }

    fun toggleMute(conversationId: String) {
        repository.toggleMute(conversationId)
    }

    fun toggleArchive(conversationId: String) {
        repository.toggleArchive(conversationId)
    }

    fun markAsRead(conversationId: String) {
        repository.markAsRead(conversationId)
    }

    fun deleteConversation(conversationId: String) {
        repository.deleteConversation(conversationId)
    }

    fun createGroupConversation(name: String, category: ChatCategory = ChatCategory.GROUPS) {
        viewModelScope.launch {
            repository.createConversation(name, category)
        }
    }
}
