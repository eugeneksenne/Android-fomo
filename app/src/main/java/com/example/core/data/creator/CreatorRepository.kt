package com.example.core.data.creator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class CreatorOrder(
    val id: String,
    val buyer: String,
    val ticketType: String,
    val price: String,
    val status: String,
    val checkedIn: Boolean
)

data class CreatorStaff(
    val id: String,
    val name: String,
    val role: String,
    val isOnline: Boolean
)

data class CreatorReview(
    val id: String,
    val author: String,
    val rating: Float,
    val content: String,
    val reply: String? = null
)

data class CreatorCampaign(
    val id: String,
    val name: String,
    val budget: String,
    val spent: String,
    val reach: Int,
    val routes: Int,
    val conversions: Int,
    val status: String
)

data class AiAdvisorMessage(
    val content: String,
    val isUser: Boolean
)

data class CreatorStudioState(
    val verifiedRole: String? = null,
    val fomoWalletBalance: Int = 450,
    val orders: List<CreatorOrder> = emptyList(),
    val staff: List<CreatorStaff> = emptyList(),
    val reviews: List<CreatorReview> = emptyList(),
    val campaigns: List<CreatorCampaign> = emptyList(),
    val aiChatHistory: List<AiAdvisorMessage> = emptyList(),
    val aiIsTyping: Boolean = false,
    val enable2FA: Boolean = true,
    val defaultPayoutMethod: String = "Standard Bank (****8294)"
)

object CreatorRepository {
    private val _state = MutableStateFlow(
        CreatorStudioState(
            orders = listOf(
                CreatorOrder("ORD-4820", "Thabo Molefe", "General Admission", "R150", "Paid", false),
                CreatorOrder("ORD-4821", "Lerato Ndlovu", "VIP Lounge Deck", "R500", "Paid", false),
                CreatorOrder("ORD-4822", "Matthew Wilson", "Early Bird Pass", "R100", "Paid", true),
                CreatorOrder("ORD-4823", "Zanele Khumalo", "General Admission", "R150", "Refunded", false),
                CreatorOrder("ORD-4824", "Pieter DeVries", "VIP Lounge Deck", "R500", "Paid", true)
            ),
            staff = listOf(
                CreatorStaff(UUID.randomUUID().toString(), "Eugene (Owner)", "Venue Owner", true),
                CreatorStaff(UUID.randomUUID().toString(), "Marcus", "Door Staff / Scanner", true),
                CreatorStaff(UUID.randomUUID().toString(), "Sarah", "Promoter / Hostess", false),
                CreatorStaff(UUID.randomUUID().toString(), "Nico", "VIP Bar Manager", true)
            ),
            reviews = listOf(
                CreatorReview("rev_1", "Sipho K.", 4.8f, "Best sound system in JHB! VIP booths are stunning but pricey.", "Thank you Sipho! We pride ourselves on acoustic excellence. See you next weekend!"),
                CreatorReview("rev_2", "Mbali Z.", 4.2f, "Love the Amapiano selection. Front queue took almost 20 mins around midnight though.", null),
                CreatorReview("rev_3", "Chad S.", 3.5f, "Great visual mapping, but air conditioning inside was struggling on Friday.", null)
            ),
            campaigns = listOf(
                CreatorCampaign("cmp_1", "Winter Launch Blast", "R500", "R320", 8450, 420, 48, "Running"),
                CreatorCampaign("cmp_2", "Amapiano Friday Flash", "R250", "R250", 12100, 1140, 94, "Completed"),
                CreatorCampaign("cmp_3", "VIP Table Push", "R900", "R150", 2100, 95, 8, "Paused")
            ),
            aiChatHistory = listOf(
                AiAdvisorMessage("Welcome Eugene. Analyze ticket trends, request pricing suggestions, or optimize Flash Drops below.", false)
            )
        )
    )
    val state: StateFlow<CreatorStudioState> = _state.asStateFlow()

    fun verifyRole(role: String) {
        _state.update { it.copy(verifiedRole = role) }
    }

    fun addStaff(name: String, role: String) {
        val newStaff = CreatorStaff(UUID.randomUUID().toString(), name, role, true)
        _state.update { it.copy(staff = it.staff + newStaff) }
    }
    
    fun removeStaff(staffId: String) {
        _state.update { state -> 
            state.copy(staff = state.staff.filter { it.id != staffId }) 
        }
    }

    fun scanTicket(orderId: String): Boolean {
        var success = false
        _state.update { state ->
            val updatedOrders = state.orders.map { order ->
                if (order.id == orderId && order.status == "Paid" && !order.checkedIn) {
                    success = true
                    order.copy(checkedIn = true)
                } else {
                    order
                }
            }
            state.copy(orders = updatedOrders)
        }
        return success
    }

    fun replyToReview(reviewId: String, replyText: String) {
        _state.update { state ->
            state.copy(
                reviews = state.reviews.map {
                    if (it.id == reviewId) it.copy(reply = replyText) else it
                }
            )
        }
    }

    fun toggle2FA(enabled: Boolean) {
        _state.update { it.copy(enable2FA = enabled) }
    }

    fun updatePayoutMethod(method: String) {
        _state.update { it.copy(defaultPayoutMethod = method) }
    }
    
    fun addDoorOrder(price: String) {
        _state.update { state ->
            val newOrder = CreatorOrder(
                id = "DOOR-${state.orders.size + 100}",
                buyer = "Door Walk-in",
                ticketType = "General Admission",
                price = "R$price",
                status = "Paid",
                checkedIn = true
            )
            state.copy(orders = listOf(newOrder) + state.orders)
        }
    }
    
    fun addAiMessage(content: String, isUser: Boolean) {
        _state.update { state ->
            state.copy(
                aiChatHistory = state.aiChatHistory + AiAdvisorMessage(content, isUser)
            )
        }
    }
    
    fun setAiTyping(isTyping: Boolean) {
        _state.update { it.copy(aiIsTyping = isTyping) }
    }

    fun deductWalletAndAddCampaign(cost: Int, campaign: CreatorCampaign) {
        _state.update { state ->
            state.copy(
                fomoWalletBalance = state.fomoWalletBalance - cost,
                campaigns = listOf(campaign) + state.campaigns
            )
        }
    }
}
