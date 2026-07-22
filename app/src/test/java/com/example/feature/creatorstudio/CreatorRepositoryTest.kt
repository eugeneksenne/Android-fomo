package com.example.feature.creatorstudio

import com.example.core.data.creator.CreatorRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CreatorRepositoryTest {

    @Before
    fun setUp() {
        // Assume default state
    }

    @Test
    fun testVerifyRoleUpdatesState() {
        CreatorRepository.verifyRole("Organiser")
        assertEquals("Organiser", CreatorRepository.state.value.verifiedRole)
    }

    @Test
    fun testAddStaffMember() {
        val initialCount = CreatorRepository.state.value.staff.size
        CreatorRepository.addStaff("Dj K", "Headliner")
        val updatedState = CreatorRepository.state.value
        assertEquals(initialCount + 1, updatedState.staff.size)
        assertTrue(updatedState.staff.any { it.name == "Dj K" && it.role == "Headliner" })
    }

    @Test
    fun testRemoveStaffMember() {
        val initialStaff = CreatorRepository.state.value.staff
        assertTrue(initialStaff.isNotEmpty())
        val staffToRemove = initialStaff.first()

        CreatorRepository.removeStaff(staffToRemove.id)
        
        val updatedStaff = CreatorRepository.state.value.staff
        assertFalse(updatedStaff.any { it.id == staffToRemove.id })
    }

    @Test
    fun testScanTicketUpdatesOrderStatus() {
        val order = CreatorRepository.state.value.orders.first { it.status == "Paid" && !it.checkedIn }
        val success = CreatorRepository.scanTicket(order.id)
        
        assertTrue(success)
        val updatedOrder = CreatorRepository.state.value.orders.first { it.id == order.id }
        assertTrue(updatedOrder.checkedIn)
    }

    @Test
    fun testReplyToReview() {
        val review = CreatorRepository.state.value.reviews.first()
        val replyText = "We are glad you enjoyed it!"

        CreatorRepository.replyToReview(review.id, replyText)

        val updatedReview = CreatorRepository.state.value.reviews.first { it.id == review.id }
        assertEquals(replyText, updatedReview.reply)
    }

    @Test
    fun testToggle2FA() {
        CreatorRepository.toggle2FA(false)
        assertFalse(CreatorRepository.state.value.enable2FA)
        
        CreatorRepository.toggle2FA(true)
        assertTrue(CreatorRepository.state.value.enable2FA)
    }

    @Test
    fun testUpdatePayoutMethod() {
        val newMethod = "FNB (****1234)"
        CreatorRepository.updatePayoutMethod(newMethod)
        assertEquals(newMethod, CreatorRepository.state.value.defaultPayoutMethod)
    }

    @Test
    fun testAddAiMessageAndTyping() {
        val initialCount = CreatorRepository.state.value.aiChatHistory.size
        CreatorRepository.setAiTyping(true)
        assertTrue(CreatorRepository.state.value.aiIsTyping)

        CreatorRepository.addAiMessage("Test user message", true)
        CreatorRepository.setAiTyping(false)
        
        val state = CreatorRepository.state.value
        assertFalse(state.aiIsTyping)
        assertEquals(initialCount + 1, state.aiChatHistory.size)
        assertEquals("Test user message", state.aiChatHistory.last().content)
        assertTrue(state.aiChatHistory.last().isUser)
    }
}
