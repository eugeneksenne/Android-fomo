package com.example.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class BuddyMember(
    val id: String,
    val name: String,
    val imageUrl: String,
    val status: String, // At Venue, Walking, Driving, Home, Offline, Idle, Dancing
    val batteryPercent: Int,
    val distanceText: String,
    val isSelectedForLostFriend: Boolean = false,
    val connectionState: String = "Online",
    val latOffset: Double = 0.0,
    val lngOffset: Double = 0.0,
    val etaText: String = "3 min walk"
)

data class MeetupPoint(
    val title: String,
    val distanceText: String,
    val etaMinutes: Int,
    val active: Boolean = true,
    val countdownMinutes: Int = 15
)

data class BuddyTimelineItem(
    val id: String,
    val timestamp: String,
    val title: String,
    val iconEmoji: String = "🟢"
)

data class BuddyPairRequest(
    val id: String,
    val hostName: String,
    val hostAvatar: String,
    val durationText: String,
    val messageText: String,
    val status: String = "PENDING"
)

data class NightGuardState(
    // SOS state
    val isSosActive: Boolean = false,
    val sosCountdown: Int = 0,
    val isEmergencyCallConnected: Boolean = false,
    val isEvidenceRecorded: Boolean = false,

    // Buddy Pair states
    val isBuddyActive: Boolean = false,
    val buddySessionName: String = "Festival Crew",
    val buddyDurationText: String = "Until Event Ends",
    val buddyRemainingMinutes: Int = 136, // 2h 16m
    val buddyInviteMessage: String = "Let's stay together tonight.",
    val buddyPendingRequests: List<BuddyPairRequest> = listOf(
        BuddyPairRequest("req_bp1", "Sarah Jenkins", "https://i.pravatar.cc/150?img=32", "2 Hours", "Let's stay together tonight at Saint Lounge!", "PENDING")
    ),
    val buddies: List<BuddyMember> = listOf(
        BuddyMember("b1", "Sarah", "https://i.pravatar.cc/150?img=32", "At Venue", 84, "18 m away", latOffset = 0.0001, lngOffset = 0.0002, etaText = "1 min walk"),
        BuddyMember("b2", "James", "https://i.pravatar.cc/150?img=12", "Walking", 62, "120 m away", latOffset = -0.0003, lngOffset = 0.0005, etaText = "3 min walk"),
        BuddyMember("b3", "Peter", "https://i.pravatar.cc/150?img=11", "Driving", 48, "450 m away", latOffset = 0.0012, lngOffset = -0.0010, etaText = "6 min drive"),
        BuddyMember("b4", "Kgomotso", "https://i.pravatar.cc/150?img=49", "Home", 98, "2.4 km away", latOffset = 0.0050, lngOffset = 0.0040, etaText = "12 min drive")
    ),
    val meetupPoint: MeetupPoint? = MeetupPoint("Main Stage Bar", "85 m away", 1, true, 12),
    val timeline: List<BuddyTimelineItem> = listOf(
        BuddyTimelineItem("tl1", "21:03", "Buddy Pair Started", "🤝"),
        BuddyTimelineItem("tl2", "21:08", "Sarah Arrived at Saint Lounge", "📍"),
        BuddyTimelineItem("tl3", "21:20", "James Joined Session", "👥"),
        BuddyTimelineItem("tl4", "22:04", "Meet-Up Point Created", "🎯")
    ),
    val shareExactLocation: Boolean = true,
    val shareBattery: Boolean = true,
    val shareEta: Boolean = true,
    val shareMovement: Boolean = true,
    val isSharingPaused: Boolean = false,

    // Walk Me Home states
    val isJourneyActive: Boolean = false,
    val journeyDestination: String = "Home",
    val journeyMode: String = "Walking", // Walking, Driving, Ride-share, Cycling
    val journeyEtaMinutes: Int = 18,
    val journeyRemainingDistanceKm: Double = 1.8,
    val companionsWatching: List<String> = listOf("Sarah", "James"),
    val companionMessages: List<String> = listOf("Watching you!", "Get home safe!"),
    val routeDeviationDetected: Boolean = false,

    // Safety Check states
    val isSafetyCheckActive: Boolean = false,
    val safetyCheckRemainingMinutes: Int = 45,
    val safetyCheckContacts: List<String> = listOf("Sarah", "Kgomotso"),
    val showSafetyCheckPrompt: Boolean = false,
    val isEscalationActive: Boolean = false
)

object NightGuardRepository {
    private val _state = MutableStateFlow(NightGuardState())
    val state: StateFlow<NightGuardState> = _state.asStateFlow()

    // Buddy Pair Actions
    fun startBuddyPair(sessionName: String, durationMinutes: Int, durationText: String = "2 Hours") {
        _state.update { current ->
            val newTimeline = current.timeline + BuddyTimelineItem(
                id = "tl_${System.currentTimeMillis()}",
                timestamp = "Just now",
                title = "Started '$sessionName' ($durationText)",
                iconEmoji = "🤝"
            )
            current.copy(
                isBuddyActive = true,
                buddySessionName = sessionName,
                buddyDurationText = durationText,
                buddyRemainingMinutes = durationMinutes,
                timeline = newTimeline
            )
        }
    }

    fun endBuddyPair() {
        _state.update { current ->
            current.copy(
                isBuddyActive = false,
                meetupPoint = null,
                buddies = current.buddies.map { it.copy(isSelectedForLostFriend = false) }
            )
        }
    }

    fun highlightFriendForLostFriend(buddyId: String) {
        _state.update { current ->
            current.copy(
                buddies = current.buddies.map { buddy ->
                    if (buddy.id == buddyId) {
                        buddy.copy(isSelectedForLostFriend = !buddy.isSelectedForLostFriend)
                    } else {
                        buddy.copy(isSelectedForLostFriend = false)
                    }
                }
            )
        }
    }

    fun setMeetupPoint(title: String) {
        _state.update { current ->
            val newTimeline = current.timeline + BuddyTimelineItem(
                id = "tl_${System.currentTimeMillis()}",
                timestamp = "Just now",
                title = "Meet-Up Point set: $title",
                iconEmoji = "🎯"
            )
            current.copy(
                meetupPoint = MeetupPoint(
                    title = title,
                    distanceText = "85 m away",
                    etaMinutes = 1,
                    countdownMinutes = 15
                ),
                timeline = newTimeline
            )
        }
    }

    fun removeMeetupPoint() {
        _state.update { current ->
            current.copy(meetupPoint = null)
        }
    }

    fun respondToBuddyRequest(requestId: String, accept: Boolean) {
        _state.update { current ->
            val updatedReqs = current.buddyPendingRequests.map { req ->
                if (req.id == requestId) req.copy(status = if (accept) "ACCEPTED" else "DECLINED") else req
            }
            current.copy(
                buddyPendingRequests = updatedReqs,
                isBuddyActive = accept || current.isBuddyActive
            )
        }
    }

    fun togglePrivacy(exactLoc: Boolean? = null, battery: Boolean? = null, eta: Boolean? = null, movement: Boolean? = null) {
        _state.update { current ->
            current.copy(
                shareExactLocation = exactLoc ?: current.shareExactLocation,
                shareBattery = battery ?: current.shareBattery,
                shareEta = eta ?: current.shareEta,
                shareMovement = movement ?: current.shareMovement
            )
        }
    }

    fun togglePauseSharing() {
        _state.update { current ->
            current.copy(isSharingPaused = !current.isSharingPaused)
        }
    }

    fun extendSession(extraMinutes: Int = 30) {
        _state.update { current ->
            current.copy(buddyRemainingMinutes = current.buddyRemainingMinutes + extraMinutes)
        }
    }

    fun updateBuddyLocation() {
        _state.update { current ->
            current.copy(
                buddies = current.buddies.map { buddy ->
                    if (buddy.id == "b1" || buddy.id == "b2") {
                        val newDist = Random.nextInt(5, 50)
                        buddy.copy(distanceText = "$newDist m away", batteryPercent = (buddy.batteryPercent - 1).coerceAtLeast(1))
                    } else {
                        buddy
                    }
                }
            )
        }
    }

    // Walk Me Home Actions
    fun startJourney(destination: String, mode: String, companions: List<String>) {
        _state.update { current ->
            current.copy(
                isJourneyActive = true,
                journeyDestination = destination,
                journeyMode = mode,
                journeyEtaMinutes = if (mode == "Driving") 6 else 18,
                journeyRemainingDistanceKm = if (mode == "Driving") 4.2 else 1.8,
                companionsWatching = companions,
                companionMessages = listOf("Started watching your ride", "Keep eyes open!"),
                routeDeviationDetected = false
            )
        }
    }

    fun updateJourneyProgress() {
        _state.update { current ->
            if (current.isJourneyActive) {
                val nextDist = (current.journeyRemainingDistanceKm - 0.2).coerceAtLeast(0.0)
                val nextEta = (current.journeyEtaMinutes - 2).coerceAtLeast(0)
                val arrived = nextDist <= 0.0
                current.copy(
                    journeyRemainingDistanceKm = nextDist,
                    journeyEtaMinutes = nextEta,
                    isJourneyActive = !arrived
                )
            } else {
                current
            }
        }
    }

    fun triggerRouteDeviation() {
        _state.update { current ->
            current.copy(
                routeDeviationDetected = true,
                companionMessages = current.companionMessages + "⚠️ You deviated from the safe route! Are you okay?"
            )
        }
    }

    fun addCompanionMessage(msg: String) {
        _state.update { current ->
            current.copy(
                companionMessages = current.companionMessages + msg
            )
        }
    }

    fun endJourney() {
        _state.update { current ->
            current.copy(
                isJourneyActive = false,
                routeDeviationDetected = false
            )
        }
    }

    // Safety Check Actions
    fun scheduleSafetyCheck(minutes: Int, contacts: List<String>) {
        _state.update { current ->
            current.copy(
                isSafetyCheckActive = true,
                safetyCheckRemainingMinutes = minutes,
                safetyCheckContacts = contacts,
                showSafetyCheckPrompt = false,
                isEscalationActive = false
            )
        }
    }

    fun triggerSafetyCheckPrompt() {
        _state.update { current ->
            current.copy(
                showSafetyCheckPrompt = true
            )
        }
    }

    fun confirmImsafe() {
        _state.update { current ->
            current.copy(
                isSafetyCheckActive = false,
                showSafetyCheckPrompt = false,
                isEscalationActive = false
            )
        }
    }

    fun snoozeSafetyCheck(extraMinutes: Int) {
        _state.update { current ->
            current.copy(
                safetyCheckRemainingMinutes = extraMinutes,
                showSafetyCheckPrompt = false
            )
        }
    }

    fun cancelSafetyCheck() {
        _state.update { current ->
            current.copy(
                isSafetyCheckActive = false,
                showSafetyCheckPrompt = false,
                isEscalationActive = false
            )
        }
    }

    fun escalateMissedCheck() {
        _state.update { current ->
            current.copy(
                isEscalationActive = true,
                showSafetyCheckPrompt = false
            )
        }
    }

    // SOS Panic Actions
    fun setSosActive(active: Boolean) {
        _state.update { current ->
            current.copy(
                isSosActive = active,
                sosCountdown = if (active) 5 else 0
            )
        }
    }

    fun decrementSosCountdown() {
        _state.update { current ->
            if (current.isSosActive && current.sosCountdown > 0) {
                current.copy(sosCountdown = current.sosCountdown - 1)
            } else {
                current
            }
        }
    }

    fun connectEmergencyCall(connect: Boolean) {
        _state.update { current ->
            current.copy(
                isEmergencyCallConnected = connect
            )
        }
    }

    fun toggleEvidenceRecording() {
        _state.update { current ->
            current.copy(
                isEvidenceRecorded = !current.isEvidenceRecorded
            )
        }
    }
}
