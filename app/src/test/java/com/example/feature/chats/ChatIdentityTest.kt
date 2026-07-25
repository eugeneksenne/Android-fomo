package com.example.feature.chats

import com.example.core.data.chat.ChatMessage
import com.example.core.data.chat.ChatRepository
import com.example.core.data.chat.RichMessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

/**
 * Regression tests for message identity, ownership and timestamps.
 *
 * Three defects motivated these:
 *  1. Every client wrote `senderId = "me"` into a shared Firestore collection,
 *     while the UI decided ownership with `senderId == "me"`. Once two people
 *     shared a conversation, BOTH saw every message as their own.
 *  2. Outgoing messages were stamped with the literal string "10:08 PM".
 *  3. Messages were marked read the instant they were sent.
 */
@RunWith(RobolectricTestRunner::class)
class ChatIdentityTest {

    @Before
    fun setUp() {
        ChatRepository.setNetworkStatus(true)
    }

    private fun message(senderId: String, senderName: String = "Someone") = ChatMessage(
        id = "m1",
        conversationId = "conv_1",
        senderId = senderId,
        senderName = senderName,
        senderAvatarUrl = "",
        type = RichMessageType.TEXT,
        content = "hello",
        timestamp = "10:00 PM",
    )

    // ---- ownership ---------------------------------------------------------

    @Test
    fun `a message from another account is not mine`() {
        val peer = message(senderId = "some_other_firebase_uid", senderName = "Sarah")
        assertFalse(
            "a peer's message must never render as the reader's own",
            ChatRepository.isFromCurrentUser(peer)
        )
    }

    @Test
    fun `distinct peers are all correctly attributed`() {
        listOf("uid_alice", "uid_bob", "uid_carol").forEach { uid ->
            assertFalse(
                "$uid must not be treated as the current user",
                ChatRepository.isFromCurrentUser(message(uid))
            )
        }
    }

    @Test
    fun `unauthenticated sessions still resolve a stable local identity`() {
        // With no Firebase user, currentUserId falls back to LOCAL_USER_ID so
        // the app remains usable offline rather than crashing.
        val id = ChatRepository.currentUserId()
        assertTrue("identity must never be blank", id.isNotBlank())
        assertEquals(ChatRepository.LOCAL_USER_ID, id)
    }

    @Test
    fun `legacy seed messages remain attributed to the local user`() {
        // Seed data predates real identities and uses the literal "me".
        assertTrue(ChatRepository.isFromCurrentUser(message(ChatRepository.LOCAL_USER_ID)))
    }

    // ---- timestamps --------------------------------------------------------

    @Test
    fun `clock formatting reflects the supplied instant`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 8)
            set(Calendar.SECOND, 0)
        }
        val formatted = ChatRepository.formatClockTime(cal.timeInMillis)
        assertTrue("expected a 10:08 reading, got $formatted", formatted.contains("10:08"))
    }

    @Test
    fun `different instants produce different timestamps`() {
        val base = System.currentTimeMillis()
        val later = base + 90 * 60 * 1000 // +1h30
        assertNotEquals(
            "timestamps must track real time, not a constant",
            ChatRepository.formatClockTime(base),
            ChatRepository.formatClockTime(later)
        )
    }

    @Test
    fun `a sent message carries a real timestamp not a hardcoded string`() {
        val convId = "conv_1"
        ChatRepository.sendMessage(
            conversationId = convId,
            type = RichMessageType.TEXT,
            content = "timestamp probe"
        )
        val sent = ChatRepository.state.value.activeMessages[convId]
            ?.last { it.content == "timestamp probe" }

        requireNotNull(sent)
        assertNotEquals(
            "the literal placeholder must be gone",
            "10:08 PM",
            sent.timestamp
        )
        assertTrue("timestamp must not be blank", sent.timestamp.isNotBlank())
    }

    // ---- read receipts -----------------------------------------------------

    @Test
    fun `a newly sent message is not marked read`() {
        val convId = "conv_1"
        ChatRepository.sendMessage(
            conversationId = convId,
            type = RichMessageType.TEXT,
            content = "read receipt probe"
        )
        val sent = ChatRepository.state.value.activeMessages[convId]
            ?.last { it.content == "read receipt probe" }

        requireNotNull(sent)
        assertFalse(
            "claiming the recipient read it immediately misrepresents them",
            sent.isRead
        )
    }

    @Test
    fun `an offline message is neither delivered nor read`() {
        ChatRepository.setNetworkStatus(false)
        val convId = "conv_1"
        ChatRepository.sendMessage(
            conversationId = convId,
            type = RichMessageType.TEXT,
            content = "offline probe"
        )
        val sent = ChatRepository.state.value.activeMessages[convId]
            ?.last { it.content == "offline probe" }

        requireNotNull(sent)
        assertFalse("cannot be delivered with no connection", sent.isDelivered)
        assertFalse("cannot be read with no connection", sent.isRead)
        assertTrue("must be flagged for the offline queue", sent.isPendingOffline)
    }

    // ---- security-rule alignment -------------------------------------------

    @Test
    fun `a created group lists its creator as a participant`() {
        val group = ChatRepository.createGroupConversation(groupName = "Rules Probe")
        assertTrue(
            "firestore.rules denies access unless the uid is in participants",
            group.participants.contains(ChatRepository.currentUserId())
        )
        assertEquals(ChatRepository.currentUserId(), group.createdBy)
    }
}
