package com.example.feature.feed

import com.example.core.data.feed.FeedRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FeedRepositoryTest {

    @Before
    fun setUp() {
        // Assume default state
    }

    @Test
    fun testInitialFeedMomentsLoaded() {
        val state = FeedRepository.state.value
        assertTrue("Initial feed moments should not be empty", state.moments.isNotEmpty())
    }

    @Test
    fun testSetActiveTab() {
        FeedRepository.setActiveTab("Live")
        assertEquals("Live", FeedRepository.state.value.activeTab)
    }

    @Test
    fun testToggleFollow() {
        val targetMoment = FeedRepository.state.value.moments.first()
        val initialFollowState = targetMoment.isFollowing

        FeedRepository.toggleFollow(targetMoment.id)
        
        val updatedMoment = FeedRepository.state.value.moments.first { it.id == targetMoment.id }
        assertEquals(!initialFollowState, updatedMoment.isFollowing)
    }

    @Test
    fun testToggleLike() {
        val targetMoment = FeedRepository.state.value.moments.first()
        val initialLikeState = targetMoment.isLiked
        val initialLikesCount = targetMoment.likesCount

        FeedRepository.toggleLike(targetMoment.id)

        val updatedMoment = FeedRepository.state.value.moments.first { it.id == targetMoment.id }
        assertEquals(!initialLikeState, updatedMoment.isLiked)
        if (!initialLikeState) {
            assertEquals(initialLikesCount + 1, updatedMoment.likesCount)
        } else {
            assertEquals(initialLikesCount - 1, updatedMoment.likesCount)
        }
    }

    @Test
    fun testRippleMoment() {
        val targetMoment = FeedRepository.state.value.moments.first()
        val initialRipplesCount = targetMoment.ripplesCount
        val initialVelocity = targetMoment.currentVelocity

        FeedRepository.rippleMoment(targetMoment.id)

        val updatedMoment = FeedRepository.state.value.moments.first { it.id == targetMoment.id }
        assertEquals(initialRipplesCount + 1, updatedMoment.ripplesCount)
        assertTrue(updatedMoment.currentVelocity > initialVelocity)
    }

    @Test
    fun testToggleSave() {
        val targetMoment = FeedRepository.state.value.moments.first()
        val initialSaveState = targetMoment.isSaved

        FeedRepository.toggleSave(targetMoment.id)

        val updatedMoment = FeedRepository.state.value.moments.first { it.id == targetMoment.id }
        assertEquals(!initialSaveState, updatedMoment.isSaved)
    }

    @Test
    fun testAddComment() {
        val targetMoment = FeedRepository.state.value.moments.first()
        val initialCommentsCount = targetMoment.comments.size
        val newText = "This looks amazing!"

        FeedRepository.addComment(targetMoment.id, newText)

        val updatedMoment = FeedRepository.state.value.moments.first { it.id == targetMoment.id }
        assertEquals(initialCommentsCount + 1, updatedMoment.comments.size)
        assertEquals(newText, updatedMoment.comments.first().text)
    }

    @Test
    fun testSetInvitationStatus() {
        val targetMoment = FeedRepository.state.value.moments.first { it.invitation != null }
        val newStatus = "CLOSED"

        FeedRepository.setInvitationStatus(targetMoment.id, newStatus)

        val updatedMoment = FeedRepository.state.value.moments.first { it.id == targetMoment.id }
        assertEquals(newStatus, updatedMoment.invitation?.status)
    }
}
