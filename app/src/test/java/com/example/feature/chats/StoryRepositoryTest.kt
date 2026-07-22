package com.example.feature.chats

import com.example.core.data.chat.ChatRepository
import com.example.core.data.story.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StoryRepositoryTest {

    @Before
    fun setUp() {
        // Reset or initialize state if needed
    }

    @Test
    fun testInitialStoryGroupsLoaded() {
        val state = StoryRepository.state.value
        assertTrue("Initial story groups should not be empty", state.userStoryGroups.isNotEmpty())
        assertTrue("My Story exists in initial seed", state.userStoryGroups.any { it.isOwnStory })
    }

    @Test
    fun testMarkSegmentViewed() {
        val targetUser = StoryRepository.state.value.userStoryGroups.first { !it.isOwnStory }
        val targetSegment = targetUser.segments.first()

        StoryRepository.markSegmentViewed(targetUser.userId, targetSegment.id)

        val updatedUser = StoryRepository.state.value.userStoryGroups.first { it.userId == targetUser.userId }
        val updatedSegment = updatedUser.segments.first { it.id == targetSegment.id }
        assertTrue("Segment marked as viewed", updatedSegment.isViewed)
    }

    @Test
    fun testAddReactionToSegment() {
        val targetUser = StoryRepository.state.value.userStoryGroups.first { !it.isOwnStory }
        val targetSegment = targetUser.segments.first()

        StoryRepository.addReactionToSegment(targetUser.userId, targetSegment.id, "🔥")

        val updatedUser = StoryRepository.state.value.userStoryGroups.first { it.userId == targetUser.userId }
        val myViewerEntry = updatedUser.viewersList.firstOrNull { it.userId == "me" }
        assertNotNull("Viewer entry added for user", myViewerEntry)
        assertEquals("🔥", myViewerEntry?.reactionEmoji)
    }

    @Test
    fun testReplyToStoryBridgesToChatRepository() {
        val targetUser = StoryRepository.state.value.userStoryGroups.first { !it.isOwnStory }
        val targetSegment = targetUser.segments.first()
        val replyText = "Insane vibe! Save me a spot!"

        StoryRepository.replyToStory(targetUser.userId, targetUser.userName, targetSegment, replyText)

        // Verify message appended in ChatRepository
        val messages = ChatRepository.state.value.activeMessages["conv_1"]
        assertNotNull("Messages list exists in conv_1", messages)
        val lastMsg = messages!!.last()
        assertTrue("Message content contains reply text", lastMsg.content.contains(replyText))
        assertEquals(targetSegment.mediaUrl, lastMsg.metadata["storyMediaUrl"])
    }

    @Test
    fun testToggleMuteUserStories() {
        val targetUser = StoryRepository.state.value.userStoryGroups.first { !it.isOwnStory }
        val initialMuted = targetUser.isMuted

        StoryRepository.toggleMuteUserStories(targetUser.userId)

        val updatedUser = StoryRepository.state.value.userStoryGroups.first { it.userId == targetUser.userId }
        assertEquals(!initialMuted, updatedUser.isMuted)
    }

    @Test
    fun testPublishNewStorySegment() {
        val initialOwnCount = StoryRepository.state.value.userStoryGroups.first { it.isOwnStory }.segments.size

        StoryRepository.publishNewStorySegment(
            mediaUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819",
            locationName = "VIP Sky Lounge",
            filterName = "Cyber Neon",
            stickers = listOf(StorySticker("st_test", StoryStickerType.LOCATION, "VIP Sky Lounge")),
            privacy = StoryPrivacy.PUBLIC
        )

        val updatedOwnCount = StoryRepository.state.value.userStoryGroups.first { it.isOwnStory }.segments.size
        assertEquals("Segment added to My Story", initialOwnCount + 1, updatedOwnCount)
    }

    @Test
    fun testDeleteOwnStorySegment() {
        val ownGroup = StoryRepository.state.value.userStoryGroups.first { it.isOwnStory }
        val targetSegmentId = ownGroup.segments.first().id

        StoryRepository.deleteOwnStorySegment(targetSegmentId)

        val updatedOwnGroup = StoryRepository.state.value.userStoryGroups.first { it.isOwnStory }
        assertFalse("Target segment deleted", updatedOwnGroup.segments.any { it.id == targetSegmentId })
    }
}
