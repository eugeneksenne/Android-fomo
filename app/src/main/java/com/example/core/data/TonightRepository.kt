package com.example.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class PlanType { SOLO, DUO, GROUP }

data class PlanStop(
    val id: String,
    val venueName: String,
    val venueId: String? = null,
    val area: String,
    val time: String, // e.g. "8:30 PM"
    val status: String = "Upcoming", // "Active", "Completed", "Upcoming"
    val voteCount: Int = 0
)

data class PlanMember(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val status: String = "Joined", // "Arrived", "In Transit", "Invited", "Accepted"
    val etaText: String = "10 min away"
)

data class GroupVoteOption(
    val id: String,
    val venueName: String,
    val area: String,
    val votes: Int,
    val votedByUser: Boolean = false
)

data class GroupVote(
    val id: String,
    val question: String,
    val options: List<GroupVoteOption>
)

data class PlanMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val timeText: String,
    val isSystem: Boolean = false
)

data class PlanExpense(
    val id: String,
    val title: String,
    val amountRands: Double,
    val paidBy: String,
    val splitCount: Int,
    val perPersonRands: Double = amountRands / splitCount,
    val isSettled: Boolean = false
)

data class PlanTemplate(
    val id: String,
    val emoji: String,
    val title: String,
    val category: String, // "Luxury", "Date Night", "Birthday", "Bar Crawl", "Club Hop"
    val description: String,
    val sampleStops: List<String>,
    val estBudget: String
)

data class PlanRequestItem(
    val id: String,
    val hostName: String,
    val hostAvatar: String,
    val title: String,
    val planType: PlanType,
    val membersCount: Int,
    val venuesSummary: String,
    val dateText: String = "This Friday • 19:00",
    val budgetText: String = "R1,200 pp",
    val status: String = "PENDING" // "PENDING", "ACCEPTED", "DECLINED", "SUGGESTED"
)

data class PublicPlanItem(
    val id: String,
    val title: String,
    val creatorName: String,
    val creatorAvatar: String,
    val category: String,
    val savesCount: Int,
    val stopsCount: Int,
    val previewImage: String = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=600&auto=format&fit=crop",
    val stopsList: List<String>
)

data class MemoryRecapItem(
    val id: String,
    val title: String,
    val dateText: String,
    val visitedVenues: List<String>,
    val friendsCount: Int,
    val totalExpenseRands: Double,
    val recapSummary: String
)

data class NightPlan(
    val id: String,
    val title: String,
    val type: PlanType,
    val dateText: String = "Tonight",
    val stops: List<PlanStop>,
    val members: List<PlanMember>,
    val currentStopIndex: Int = 0,
    val activeVote: GroupVote? = null,
    val chatMessages: List<PlanMessage> = emptyList(),
    val isUserCreator: Boolean = true,
    val inviteCode: String = "MOVE-8821",
    val budgetRands: Double = 2500.0,
    val isCalendarSynced: Boolean = true,
    val reservationStatus: String = "VIP Table #4 Confirmed",
    val playlistTracksCount: Int = 38,
    val rideStatus: String = "Bolt XL • Arriving in 4 min",
    val nightGuardActive: Boolean = true,
    val expenses: List<PlanExpense> = emptyList()
)

data class SmartNightSuggestion(
    val id: String,
    val iconEmoji: String,
    val title: String,
    val description: String,
    val actionText: String,
    val type: String // "queue", "traffic", "surge", "energy"
)

data class TonightState(
    val plans: List<NightPlan> = emptyList(),
    val suggestions: List<SmartNightSuggestion> = emptyList(),
    val planRequests: List<PlanRequestItem> = emptyList(),
    val templates: List<PlanTemplate> = emptyList(),
    val publicPlans: List<PublicPlanItem> = emptyList(),
    val memoryRecaps: List<MemoryRecapItem> = emptyList(),
    val activeMoveCount: Int = 2,
    val isLiveStatusActive: Boolean = true,
    val splitFareAmount: Double = 450.0,
    val currentSelectedPlanId: String? = "p1",
    val globalPlanContextTarget: String? = null,
    val isAiGenerating: Boolean = false
)

object TonightRepository {

    private val initialTemplates = listOf(
        PlanTemplate("t1", "👑", "Luxury Night", "VIP & Fine Dining", "Multi-course rooftop dinner followed by premium lounge VIP booth", listOf("Marble Rosebank", "Saint Lounge", "LIV Sandton"), "R3,500 pp"),
        PlanTemplate("t2", "🕯️", "Date Night", "Romantic & Speakeasy", "Intimate craft cocktail bar, cozy bistro, and quiet rooftop lounge", listOf("Sin+Tax Rosebank", "Mesh Club", "Gigi Rooftop"), "R1,800 pp"),
        PlanTemplate("t3", "🎂", "Birthday Celebration", "Group Party", "High-energy club night with table service, sparklers, and group rides", listOf("Rockets Bryanston", "Truth Nightclub", "LIV Sandton"), "R2,200 pp"),
        PlanTemplate("t4", "💃", "Girls Night Out", "Nightlife & Dancing", "Sunset bubbly lounge, trendy tapas, and dancing until sunrise", listOf("Alto234 Rooftop", "Zioux Sandton", "Saint Lounge"), "R2,000 pp"),
        PlanTemplate("t5", "🍺", "Bar Crawl & Crawls", "Casual Social", "Craft beer tour across local speakeasies and live DJ lounges", listOf("Hell's Kitchen Melville", "The Jolly Roger", "Kitcheners Braamfontein"), "R900 pp"),
        PlanTemplate("t6", "🔥", "Amapiano & Culture Tour", "Soweto Nightlife", "Authentic shisanyama, live Amapiano DJ sets, and VIP courtyard vibe", listOf("Chaf Pozi Soweto", "Soweto Towers Lounge", "Konka Soweto"), "R1,500 pp")
    )

    private val initialRequests = listOf(
        PlanRequestItem(
            id = "req_1",
            hostName = "Koketso M.",
            hostAvatar = "https://i.pravatar.cc/150?img=33",
            title = "Friday Sandton Bar Hop",
            planType = PlanType.GROUP,
            membersCount = 6,
            venuesSummary = "Marble ➔ Zioux ➔ LIV Sandton",
            dateText = "This Friday • 19:00",
            budgetText = "R1,400 pp",
            status = "PENDING"
        ),
        PlanRequestItem(
            id = "req_2",
            hostName = "Amanda K.",
            hostAvatar = "https://i.pravatar.cc/150?img=47",
            title = "Late Night Rosebank Cocktails",
            planType = PlanType.DUO,
            membersCount = 2,
            venuesSummary = "Saint Lounge ➔ Mesh Club",
            dateText = "Saturday • 21:30",
            budgetText = "R950 pp",
            status = "PENDING"
        )
    )

    private val initialPublicPlans = listOf(
        PublicPlanItem(
            id = "pub_1",
            title = "Sandton VIP Friday Experience",
            creatorName = "DJ Maphorisa Official",
            creatorAvatar = "https://i.pravatar.cc/150?img=11",
            category = "Nightlife & VIP",
            savesCount = 1420,
            stopsCount = 3,
            stopsList = listOf("Marble Rooftop", "Saint Lounge", "LIV Sandton")
        ),
        PublicPlanItem(
            id = "pub_2",
            title = "Rosebank Speakeasy & Dinner Crawl",
            creatorName = "Joburg Night Guide",
            creatorAvatar = "https://i.pravatar.cc/150?img=22",
            category = "Cocktails & Food",
            savesCount = 980,
            stopsCount = 4,
            stopsList = listOf("Keyes Art Mile", "Sin+Tax Bar", "Marble", "Mesh Club")
        ),
        PublicPlanItem(
            id = "pub_3",
            title = "Soweto Sunday Amapiano Special",
            creatorName = "Konka VIP Crew",
            creatorAvatar = "https://i.pravatar.cc/150?img=52",
            category = "Culture & Party",
            savesCount = 2150,
            stopsCount = 3,
            stopsList = listOf("Vilakazi Street", "Chaf Pozi", "Konka Soweto")
        )
    )

    private val initialMemories = listOf(
        MemoryRecapItem(
            id = "mem_1",
            title = "Last Friday Sandton Crew Move",
            dateText = "July 18, 2026",
            visitedVenues = listOf("Marble", "Zioux", "LIV Sandton"),
            friendsCount = 7,
            totalExpenseRands = 4800.0,
            recapSummary = "7 friends, 3 venues visited, 42 photos shared in live album. Reached home safely at 03:45 AM via Bolt XL."
        ),
        MemoryRecapItem(
            id = "mem_2",
            title = "Rosebank Cocktail Date Night",
            dateText = "July 11, 2026",
            visitedVenues = listOf("Sin+Tax", "Saint Lounge"),
            friendsCount = 2,
            totalExpenseRands = 1950.0,
            recapSummary = "Duo date plan with Amanda. 2 venues visited, reserved table at Saint. Completed at 01:15 AM."
        )
    )

    private val initialPlans = listOf(
        NightPlan(
            id = "p1",
            title = "Sandton Rooftop & LIV Night",
            type = PlanType.GROUP,
            dateText = "Tonight",
            stops = listOf(
                PlanStop("s1", "Marble Restaurant", "v1", "Rosebank", "8:30 PM", "Completed"),
                PlanStop("s2", "LIV Sandton", "v2", "Sandton", "11:45 PM", "Active"),
                PlanStop("s3", "Konka Soweto", "v3", "Soweto", "2:00 AM", "Upcoming")
            ),
            members = listOf(
                PlanMember("m1", "You (Eugene)", "https://i.pravatar.cc/150?img=68", "Arrived", "At Venue"),
                PlanMember("m2", "Amanda", "https://i.pravatar.cc/150?img=47", "Arrived", "At Venue"),
                PlanMember("m3", "Thabo", "https://i.pravatar.cc/150?img=12", "Arrived", "At Venue"),
                PlanMember("m4", "Kgomotso", "https://i.pravatar.cc/150?img=49", "In Transit", "5 min away"),
                PlanMember("m5", "Sarah", "https://i.pravatar.cc/150?img=32", "Arrived", "At Venue"),
                PlanMember("m6", "Junior", "https://i.pravatar.cc/150?img=11", "Arrived", "At Venue"),
                PlanMember("m7", "Lerato", "https://i.pravatar.cc/150?img=25", "Invited", "Pending"),
                PlanMember("m8", "David", "https://i.pravatar.cc/150?img=53", "In Transit", "12 min away")
            ),
            currentStopIndex = 1,
            activeVote = GroupVote(
                id = "v1",
                question = "Where are we heading after LIV?",
                options = listOf(
                    GroupVoteOption("vo1", "Konka Soweto", "Soweto", 4, true),
                    GroupVoteOption("vo2", "Truth Nightclub", "Midrand", 2, false),
                    GroupVoteOption("vo3", "Rockets Menlyn", "Pretoria", 1, false)
                )
            ),
            chatMessages = listOf(
                PlanMessage("c1", "Thabo", "VIP Table 4 is reserved under Amanda!", "21:15"),
                PlanMessage("c2", "Sarah", "Rideshare arriving in 3 mins at Marble 🚘", "21:30"),
                PlanMessage("c3", "System", "6/8 members arrived at LIV Sandton 🎉", "21:45", isSystem = true),
                PlanMessage("c4", "Amanda", "Door queue is moving fast! Get inside!", "21:50")
            ),
            isUserCreator = true,
            inviteCode = "FOMO-SANDTON-2026",
            budgetRands = 3500.0,
            isCalendarSynced = true,
            reservationStatus = "VIP Table #4 Confirmed",
            playlistTracksCount = 38,
            rideStatus = "Bolt XL • Arriving in 4 min",
            nightGuardActive = true,
            expenses = listOf(
                PlanExpense("exp1", "Marble Dinner & Drinks", 2400.0, "Thabo", 6, 400.0, false),
                PlanExpense("exp2", "LIV VIP Table Deposit", 1200.0, "You", 6, 200.0, false)
            )
        ),
        NightPlan(
            id = "p2",
            title = "Late Night Cocktails",
            type = PlanType.DUO,
            dateText = "Tonight",
            stops = listOf(
                PlanStop("s10", "Saint Lounge", "v4", "Rosebank", "10:15 PM", "Active"),
                PlanStop("s11", "Mesh Club", "v5", "Rosebank", "12:30 AM", "Upcoming")
            ),
            members = listOf(
                PlanMember("m1", "You (Eugene)", "https://i.pravatar.cc/150?img=68", "Arrived", "At Venue"),
                PlanMember("m2", "Amanda", "https://i.pravatar.cc/150?img=47", "In Transit", "8 min away")
            ),
            currentStopIndex = 0,
            activeVote = null,
            chatMessages = listOf(
                PlanMessage("c10", "Amanda", "Almost there! Save a couch spot!", "22:05")
            ),
            isUserCreator = false,
            inviteCode = "DUO-ROSEBANK-99",
            budgetRands = 1200.0,
            isCalendarSynced = true,
            reservationStatus = "Bar Couch Reserved",
            playlistTracksCount = 18,
            rideStatus = "Uber Comfort • En Route",
            nightGuardActive = true,
            expenses = listOf(
                PlanExpense("exp3", "Saint Drinks Round 1", 650.0, "Amanda", 2, 325.0, false)
            )
        )
    )

    private val initialSuggestions = listOf(
        SmartNightSuggestion("sug1", "⏱️", "Queue Surge Alert", "LIV Sandton queue is swelling. Expected door wait: 25 mins. Leave now or get FastPass.", "Get FastPass", "queue"),
        SmartNightSuggestion("sug2", "🚕", "Surge Pricing Detected", "Uber & Bolt fares around Rosebank rising by 1.4x due to high nightlife volume.", "Lock Fare", "surge"),
        SmartNightSuggestion("sug3", "🔥", "Venue Vibe Peaking", "Saint Lounge vibe sentiment hit 95% fire rating right now. Crowd level high.", "View Live Vibe", "energy"),
        SmartNightSuggestion("sug4", "🛡️", "Group Separation Warning", "Kgomotso and David are currently 1.4 km away from your group at LIV.", "Check Radar", "safety")
    )

    private val _state = MutableStateFlow(
        TonightState(
            plans = initialPlans,
            suggestions = initialSuggestions,
            planRequests = initialRequests,
            templates = initialTemplates,
            publicPlans = initialPublicPlans,
            memoryRecaps = initialMemories,
            activeMoveCount = initialPlans.size
        )
    )
    val state: StateFlow<TonightState> = _state.asStateFlow()

    fun selectPlan(planId: String) {
        _state.update { current ->
            current.copy(currentSelectedPlanId = planId)
        }
    }

    fun castVote(planId: String, optionId: String) {
        _state.update { current ->
            val updatedPlans = current.plans.map { plan ->
                if (plan.id == planId && plan.activeVote != null) {
                    val updatedOptions = plan.activeVote.options.map { option ->
                        if (option.id == optionId) {
                            val wasVoted = option.votedByUser
                            option.copy(
                                votes = if (wasVoted) option.votes - 1 else option.votes + 1,
                                votedByUser = !wasVoted
                            )
                        } else {
                            if (option.votedByUser) option.copy(votes = option.votes - 1, votedByUser = false) else option
                        }
                    }
                    plan.copy(activeVote = plan.activeVote.copy(options = updatedOptions))
                } else plan
            }
            current.copy(plans = updatedPlans)
        }
    }

    fun addPlanMessage(planId: String, text: String) {
        if (text.isBlank()) return
        _state.update { current ->
            val updatedPlans = current.plans.map { plan ->
                if (plan.id == planId) {
                    val newMsg = PlanMessage(
                        id = UUID.randomUUID().toString(),
                        senderName = "You",
                        text = text,
                        timeText = "Just now",
                        isSystem = false
                    )
                    plan.copy(chatMessages = plan.chatMessages + newMsg)
                } else plan
            }
            current.copy(plans = updatedPlans)
        }
    }

    fun createPlan(
        title: String,
        type: PlanType,
        venues: List<Pair<String, String>>,
        invitedNames: List<String>
    ) {
        val stops = venues.mapIndexed { index, pair ->
            PlanStop(
                id = UUID.randomUUID().toString(),
                venueName = pair.first,
                area = pair.second,
                time = when (index) {
                    0 -> "9:00 PM"
                    1 -> "11:30 PM"
                    else -> "1:45 AM"
                },
                status = if (index == 0) "Active" else "Upcoming"
            )
        }

        val members = listOf(
            PlanMember("m1", "You (Eugene)", "https://i.pravatar.cc/150?img=68", "Arrived", "At Venue")
        ) + invitedNames.mapIndexed { idx, name ->
            PlanMember("inv_$idx", name, "https://i.pravatar.cc/150?img=${(idx + 10) * 3}", "Invited", "Pending")
        }

        val newPlan = NightPlan(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Tonight's Nightlife Move" },
            type = type,
            dateText = "Tonight",
            stops = stops,
            members = members,
            currentStopIndex = 0,
            chatMessages = listOf(
                PlanMessage("sys_1", "System", "Move created! Invitations sent out.", "Just now", isSystem = true)
            ),
            isUserCreator = true,
            inviteCode = "FOMO-${(1000..9999).random()}"
        )

        _state.update { current ->
            val newPlans = listOf(newPlan) + current.plans
            current.copy(plans = newPlans, activeMoveCount = newPlans.size, currentSelectedPlanId = newPlan.id)
        }
    }

    fun addStopToPlan(planId: String, venueName: String, area: String, time: String) {
        _state.update { current ->
            val updatedPlans = current.plans.map { plan ->
                if (plan.id == planId) {
                    val newStop = PlanStop(
                        id = UUID.randomUUID().toString(),
                        venueName = venueName,
                        area = area,
                        time = time,
                        status = "Upcoming"
                    )
                    plan.copy(stops = plan.stops + newStop)
                } else plan
            }
            current.copy(plans = updatedPlans)
        }
    }

    fun addItemToActivePlan(name: String, area: String) {
        val activeId = _state.value.currentSelectedPlanId ?: "p1"
        addStopToPlan(activeId, name, area, "11:15 PM")
    }

    fun respondToPlanRequest(requestId: String, accept: Boolean) {
        _state.update { current ->
            val updatedReqs = current.planRequests.map { req ->
                if (req.id == requestId) {
                    req.copy(status = if (accept) "ACCEPTED" else "DECLINED")
                } else req
            }
            current.copy(planRequests = updatedReqs)
        }
    }

    fun createPlanFromTemplate(templateId: String) {
        val tmpl = _state.value.templates.find { it.id == templateId } ?: return
        createPlan(
            title = "${tmpl.title} Plan",
            type = PlanType.GROUP,
            venues = tmpl.sampleStops.map { Pair(it, "Sandton/Rosebank") },
            invitedNames = listOf("Amanda", "Thabo", "Kgomotso")
        )
    }

    fun remixPublicPlan(publicPlanId: String) {
        val pub = _state.value.publicPlans.find { it.id == publicPlanId } ?: return
        createPlan(
            title = "Remix: ${pub.title}",
            type = PlanType.GROUP,
            venues = pub.stopsList.map { Pair(it, "Nightlife Spot") },
            invitedNames = listOf("Amanda", "Sarah")
        )
    }

    fun generateAiPlan(prompt: String) {
        _state.update { it.copy(isAiGenerating = true) }
        val generatedTitle = if (prompt.contains("dinner", true)) "AI Custom Dinner & Dancing" else "AI Nightlife Itinerary"
        createPlan(
            title = generatedTitle,
            type = PlanType.GROUP,
            venues = listOf(
                Pair("Marble Restaurant", "Rosebank"),
                Pair("Saint Lounge", "Rosebank"),
                Pair("LIV Nightclub", "Sandton")
            ),
            invitedNames = listOf("Amanda", "Thabo")
        )
        _state.update { it.copy(isAiGenerating = false) }
    }

    fun addExpense(planId: String, title: String, amount: Double, paidBy: String) {
        _state.update { current ->
            val updatedPlans = current.plans.map { plan ->
                if (plan.id == planId) {
                    val split = plan.members.size.coerceAtLeast(1)
                    val newExp = PlanExpense(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        amountRands = amount,
                        paidBy = paidBy,
                        splitCount = split
                    )
                    plan.copy(expenses = plan.expenses + newExp)
                } else plan
            }
            current.copy(plans = updatedPlans)
        }
    }

    fun toggleCalendarSync(planId: String) {
        _state.update { current ->
            val updatedPlans = current.plans.map { plan ->
                if (plan.id == planId) plan.copy(isCalendarSynced = !plan.isCalendarSynced) else plan
            }
            current.copy(plans = updatedPlans)
        }
    }

    fun setGlobalPlanContext(targetName: String?) {
        _state.update { it.copy(globalPlanContextTarget = targetName) }
    }

    fun splitFare(amount: Double) {
        _state.update { current ->
            current.copy(splitFareAmount = amount)
        }
    }
}
