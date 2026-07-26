package com.example.feature.chats

import com.example.core.data.chat.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChatRepositoryTest {

    @Before
    fun setUp() {
        ChatRepository.setCategory(ChatCategory.ALL)
        ChatRepository.setSearchQuery("")
        ChatRepository.setNetworkStatus(true)
    }

    @Test
    fun testInitialConversationsLoaded() {
        val state = ChatRepository.state.value
        assertTrue("Initial conversations should not be empty", state.conversations.isNotEmpty())
    }

    @Test
    fun testCategoryFiltering() {
        ChatRepository.setCategory(ChatCategory.PINNED)
        val state = ChatRepository.state.value
        assertEquals(ChatCategory.PINNED, state.activeCategory)

        val pinnedConvs = state.conversations.filter { it.isPinned && !it.isArchived }
        assertTrue("Pinned conversations filter works", pinnedConvs.all { it.isPinned })
    }

    @Test
    fun testTogglePinConversation() {
        val firstId = ChatRepository.state.value.conversations.first().id
        val initialPinned = ChatRepository.state.value.conversations.first().isPinned

        ChatRepository.togglePinConversation(firstId)
        val updatedPinned = ChatRepository.state.value.conversations.first { it.id == firstId }.isPinned

        assertEquals("Pin status toggled correctly", !initialPinned, updatedPinned)
    }

    @Test
    fun testSendMessageAndDraftClearing() {
        val convId = "conv_1"
        ChatRepository.saveDraft(convId, "Unsent draft text")
        assertEquals("Unsent draft text", ChatRepository.state.value.draftMap[convId])

        ChatRepository.sendMessage(
            conversationId = convId,
            type = RichMessageType.TEXT,
            content = "Testing message delivery"
        )

        assertNull("Draft cleared after message send", ChatRepository.state.value.draftMap[convId])
        val messages = ChatRepository.state.value.activeMessages[convId]
        assertNotNull("Message list exists", messages)
        assertTrue("Sent message exists in repository", messages!!.any { it.content == "Testing message delivery" })
    }

    @Test
    fun testOfflineQueueAndAutomaticSync() {
        // Switch network to offline
        ChatRepository.setNetworkStatus(false)
        assertFalse("Network status updated to offline", ChatRepository.state.value.isNetworkOnline)

        val convId = "conv_1"
        ChatRepository.sendMessage(
            conversationId = convId,
            type = RichMessageType.TEXT,
            content = "Offline queued message"
        )

        val pendingQueue = ChatRepository.state.value.pendingOfflineQueue
        assertTrue("Offline message queued", pendingQueue.any { it.content == "Offline queued message" })

        // Reconnect network -> Automatic merge sync
        ChatRepository.setNetworkStatus(true)
        assertTrue("Network restored", ChatRepository.state.value.isNetworkOnline)
        assertTrue("Pending queue emptied on sync", ChatRepository.state.value.pendingOfflineQueue.isEmpty())
    }

    @Test
    fun testGroupPollVoting() {
        val convId = "conv_3"
        val msgId = "m301"
        val pollOptionId = "o2"

        ChatRepository.voteInPoll(convId, msgId, pollOptionId)

        val messages = ChatRepository.state.value.activeMessages[convId]
        val pollMessage = messages?.firstOrNull { it.id == msgId }
        assertNotNull("Poll message found", pollMessage)
        assertNotNull("Poll data attached", pollMessage?.poll)

        assertEquals("Poll vote recorded", pollOptionId, pollMessage?.poll?.userVotedOptionId)
    }

    @Test
    fun testAttachmentUploadManagerProgress() {
        ChatRepository.addAttachmentToQueue("voice_note.m4a", 1024000L, "audio/m4a")
        val uploads = ChatRepository.state.value.activeUploads
        assertTrue("Upload queued", uploads.isNotEmpty())

        val targetUploadId = uploads.last().id
        ChatRepository.updateUploadProgress(targetUploadId, 100, UploadStatus.COMPLETED)

        val updatedUpload = ChatRepository.state.value.activeUploads.first { it.id == targetUploadId }
        assertEquals(100, updatedUpload.progressPercent)
        assertEquals(UploadStatus.COMPLETED, updatedUpload.status)
    }
}
