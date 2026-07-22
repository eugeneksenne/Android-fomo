package com.example.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CircleStory(
    val id: String,
    val userName: String,
    val userAvatar: String,
    val storyType: String, // "Story", "Live", "Event", "Venue", "Close Friends", "Viewed"
    val ringColor: String, // Hex color
    val badgeText: String, // "Story", "Live", "Event", "Venue", "Close Friends"
    val timestamp: String,
    val mediaUrl: String,
    val venueId: String? = null,
    val venueName: String? = null,
    val eventName: String? = null,
    val musicPlaying: String? = null,
    val isViewed: Boolean = false,
    val isUserStory: Boolean = false
)

data class ActivityItem(
    val id: String,
    val userName: String,
    val userAvatar: String,
    val activityType: String, // "Moment", "Live", "Event", "Nearby"
    val description: String,
    val timestamp: String,
    val mediaUrl: String? = null,
    val venueId: String? = null,
    val venueName: String? = null,
    val eventId: String? = null,
    val eventName: String? = null,
    val distanceText: String? = null,
    val isLive: Boolean = false,
    val watchersCount: Int? = null
)

data class CircleFriend(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String,
    val isVerified: Boolean = false,
    val mutualFriendsCount: Int,
    val currentActivity: String,
    val status: String, // "Online", "Offline"
    val isCloseFriend: Boolean = false,
    val distanceText: String = "1.2 km away",
    val venueName: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class DiscoverPerson(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val mutualFriendsCount: Int,
    val reason: String,
    val category: String, // "People You May Know", "Met Tonight", "Same Venue Lovers", "Same Event Attendees", "Nearby People", "Trending People", "Recently Joined", "Suggested for You"
    val isFollowing: Boolean = false,
    val isFriendRequested: Boolean = false,
    val isVerified: Boolean = false
)

data class FriendRequest(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val mutualFriendsCount: Int,
    val reason: String,
    val type: String // "Incoming", "Outgoing"
)

object MyCircleRepository {

    // Initial Stories list
    private val initialStories = listOf(
        CircleStory(
            id = "story_amanda",
            userName = "Amanda",
            userAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=200&auto=format&fit=crop",
            storyType = "Story",
            ringColor = "#8A2BE2", // Purple
            badgeText = "Story",
            timestamp = "10m ago",
            mediaUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=800&auto=format&fit=crop",
            musicPlaying = "Kelebe - Lojay & Sarz"
        ),
        CircleStory(
            id = "story_jason",
            userName = "Jason",
            userAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=200&auto=format&fit=crop",
            storyType = "Live",
            ringColor = "#FF2D55", // Red
            badgeText = "Live",
            timestamp = "Just Now",
            mediaUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=800&auto=format&fit=crop",
            venueId = "taboo_sandton",
            venueName = "Taboo Lounge",
            musicPlaying = "Amapiano Heat Live DJ Set"
        ),
        CircleStory(
            id = "story_sarah",
            userName = "Sarah",
            userAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop",
            storyType = "Event",
            ringColor = "#FFD700", // Gold
            badgeText = "Event",
            timestamp = "1h ago",
            mediaUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=800&auto=format&fit=crop",
            eventName = "Amapiano Fridays",
            musicPlaying = "Mnike - Tyler ICU"
        ),
        CircleStory(
            id = "story_tyler",
            userName = "Tyler",
            userAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=200&auto=format&fit=crop",
            storyType = "Venue",
            ringColor = "#007AFF", // Blue
            badgeText = "Venue",
            timestamp = "2h ago",
            mediaUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=800&auto=format&fit=crop",
            venueId = "konka_soweto",
            venueName = "Konka Soweto"
        ),
        CircleStory(
            id = "story_jessica",
            userName = "Jessica",
            userAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=200&auto=format&fit=crop",
            storyType = "Close Friends",
            ringColor = "#34C759", // Green
            badgeText = "Close Friends",
            timestamp = "3h ago",
            mediaUrl = "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=800&auto=format&fit=crop",
            musicPlaying = "Sgudi Snyc - De Mthuda"
        )
    )

    private val _storiesState = MutableStateFlow<List<CircleStory>>(initialStories)
    val storiesState: StateFlow<List<CircleStory>> = _storiesState.asStateFlow()

    // Activity Feed items
    private val initialActivityItems = listOf(
        ActivityItem(
            id = "act_1",
            userName = "Amanda",
            userAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=200&auto=format&fit=crop",
            activityType = "Moment",
            description = "Sharing a Moment at Altitude Club",
            timestamp = "3 min ago",
            mediaUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop",
            venueId = "altitude_club",
            venueName = "Altitude Club"
        ),
        ActivityItem(
            id = "act_2",
            userName = "Jason",
            userAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=200&auto=format&fit=crop",
            activityType = "Live",
            description = "Live at Taboo Lounge",
            timestamp = "Just Now",
            venueId = "taboo_sandton",
            venueName = "Taboo Lounge",
            isLive = true,
            watchersCount = 182
        ),
        ActivityItem(
            id = "act_3",
            userName = "Sarah",
            userAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop",
            activityType = "Event",
            description = "Going to Amapiano Fridays Tonight",
            timestamp = "15 min ago",
            eventId = "amapiano_fridays",
            eventName = "Amapiano Fridays"
        ),
        ActivityItem(
            id = "act_4",
            userName = "Mike",
            userAvatar = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?q=80&w=200&auto=format&fit=crop",
            activityType = "Nearby",
            description = "Spotted 650 m away at Rockets Lounge",
            timestamp = "20 min ago",
            distanceText = "650 m away",
            venueId = "rockets_lounge",
            venueName = "Rockets Lounge"
        )
    )

    private val _activityItemsState = MutableStateFlow<List<ActivityItem>>(initialActivityItems)
    val activityItemsState: StateFlow<List<ActivityItem>> = _activityItemsState.asStateFlow()

    // Friends list
    private val initialFriends = listOf(
        CircleFriend(
            id = "friend_amanda",
            name = "Amanda Smith",
            username = "@amanda_s",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=200&auto=format&fit=crop",
            isVerified = true,
            mutualFriendsCount = 14,
            currentActivity = "Chilling at Altitude Club",
            status = "Online",
            isCloseFriend = true,
            distanceText = "850 m away",
            venueName = "Altitude Club",
            latitude = -26.1075,
            longitude = 28.0567
        ),
        CircleFriend(
            id = "friend_jason",
            name = "Jason Bourne",
            username = "@j_bourne",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=200&auto=format&fit=crop",
            isVerified = false,
            mutualFriendsCount = 8,
            currentActivity = "Streaming Live from Taboo Lounge",
            status = "Online",
            distanceText = "1.4 km away",
            venueName = "Taboo Lounge",
            latitude = -26.1102,
            longitude = 28.0612
        ),
        CircleFriend(
            id = "friend_sarah",
            name = "Sarah Connor",
            username = "@sarah_c",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop",
            isVerified = true,
            mutualFriendsCount = 22,
            currentActivity = "Planning Amapiano Fridays",
            status = "Online",
            isCloseFriend = true,
            distanceText = "2.1 km away",
            latitude = -26.0988,
            longitude = 28.0495
        ),
        CircleFriend(
            id = "friend_tyler",
            name = "Tyler Durden",
            username = "@t_durden",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=200&auto=format&fit=crop",
            isVerified = false,
            mutualFriendsCount = 5,
            currentActivity = "Spotted at Konka Soweto",
            status = "Offline",
            distanceText = "12.5 km away",
            venueName = "Konka Soweto",
            latitude = -26.2389,
            longitude = 27.9056
        ),
        CircleFriend(
            id = "friend_jessica",
            name = "Jessica Alba",
            username = "@jess_alba",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=200&auto=format&fit=crop",
            isVerified = true,
            mutualFriendsCount = 31,
            currentActivity = "Listening to Sgudi Snyc",
            status = "Online",
            distanceText = "3.2 km away",
            latitude = -26.1156,
            longitude = 28.0512
        )
    )

    private val _friendsState = MutableStateFlow<List<CircleFriend>>(initialFriends)
    val friendsState: StateFlow<List<CircleFriend>> = _friendsState.asStateFlow()

    // Discover (People You May Know / Suggested)
    private val initialDiscoverPeople = listOf(
        DiscoverPerson(
            id = "disc_1",
            name = "Michael B",
            avatarUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 12,
            reason = "12 Mutual Friends • Loves Amapiano",
            category = "People You May Know",
            isVerified = true
        ),
        DiscoverPerson(
            id = "disc_2",
            name = "Sinethemba Khumalo",
            avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 3,
            reason = "Met at Rockets Lounge Tonight",
            category = "Met Tonight"
        ),
        DiscoverPerson(
            id = "disc_3",
            name = "Zama Dlamini",
            avatarUrl = "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 5,
            reason = "Frequents Konka Soweto",
            category = "Same Venue Lovers"
        ),
        DiscoverPerson(
            id = "disc_4",
            name = "Tshepo Maseko",
            avatarUrl = "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 9,
            reason = "Going to Amapiano Fridays",
            category = "Same Event Attendees"
        ),
        DiscoverPerson(
            id = "disc_5",
            name = "Lerato Molefe",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 1,
            reason = "Spotted 400 m Away",
            category = "Nearby People"
        ),
        DiscoverPerson(
            id = "disc_6",
            name = "Kabelo M",
            avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 42,
            reason = "Trending Creator Tonight",
            category = "Trending People",
            isVerified = true
        ),
        DiscoverPerson(
            id = "disc_7",
            name = "Naledi Ndlovu",
            avatarUrl = "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 0,
            reason = "Just Joined FOMO South Africa",
            category = "Recently Joined"
        ),
        DiscoverPerson(
            id = "disc_8",
            name = "Sipho D",
            avatarUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 18,
            reason = "AI Vibe Match: 98% (Loves Afro House & Rooftops)",
            category = "Suggested for You",
            isVerified = true
        )
    )

    private val _discoverPeopleState = MutableStateFlow<List<DiscoverPerson>>(initialDiscoverPeople)
    val discoverPeopleState: StateFlow<List<DiscoverPerson>> = _discoverPeopleState.asStateFlow()

    // Friend Requests list
    private val initialRequests = listOf(
        FriendRequest(
            id = "req_1",
            name = "Bongiwe Mkhize",
            avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 7,
            reason = "Met at Altitude Club",
            type = "Incoming"
        ),
        FriendRequest(
            id = "req_2",
            name = "Neo Rametsi",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 11,
            reason = "Loves Hip Hop Nights",
            type = "Incoming"
        ),
        FriendRequest(
            id = "req_3",
            name = "Karabo S",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop",
            mutualFriendsCount = 4,
            reason = "Added via Phone Contacts",
            type = "Outgoing"
        )
    )

    private val _friendRequestsState = MutableStateFlow<List<FriendRequest>>(initialRequests)
    val friendRequestsState: StateFlow<List<FriendRequest>> = _friendRequestsState.asStateFlow()

    // Actions
    fun markStoryViewed(id: String) {
        _storiesState.update { current ->
            current.map { story ->
                if (story.id == id) story.copy(isViewed = true, ringColor = "#8E8E93") else story
            }
        }
    }

    fun addStory(userName: String, mediaUrl: String, text: String, type: String = "Story") {
        val newStory = CircleStory(
            id = "story_user_${System.currentTimeMillis()}",
            userName = userName,
            userAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200&auto=format&fit=crop",
            storyType = type,
            ringColor = when(type) {
                "Live" -> "#FF2D55"
                "Event" -> "#FFD700"
                "Venue" -> "#007AFF"
                "Close Friends" -> "#34C759"
                else -> "#8A2BE2"
            },
            badgeText = type,
            timestamp = "Just Now",
            mediaUrl = mediaUrl,
            musicPlaying = if (text.isNotEmpty()) text else null,
            isUserStory = true
        )
        _storiesState.update { listOf(newStory) + it }
    }

    fun handleAcceptRequest(id: String) {
        val request = _friendRequestsState.value.find { it.id == id } ?: return
        
        // Remove from requests
        _friendRequestsState.update { it.filter { r -> r.id != id } }

        // Add to friends
        val newFriend = CircleFriend(
            id = "friend_${request.id}",
            name = request.name,
            username = "@${request.name.lowercase().replace(" ", "_")}",
            avatarUrl = request.avatarUrl,
            mutualFriendsCount = request.mutualFriendsCount,
            currentActivity = "Just connected on FOMO",
            status = "Online"
        )
        _friendsState.update { listOf(newFriend) + it }
    }

    fun handleDeclineRequest(id: String) {
        _friendRequestsState.update { it.filter { r -> r.id != id } }
    }

    fun toggleCloseFriend(friendId: String) {
        _friendsState.update { current ->
            current.map { f ->
                if (f.id == friendId) f.copy(isCloseFriend = !f.isCloseFriend) else f
            }
        }
    }

    fun handleRemoveFriend(friendId: String) {
        _friendsState.update { it.filter { f -> f.id != friendId } }
    }

    fun handleFollowDiscover(id: String) {
        _discoverPeopleState.update { current ->
            current.map { person ->
                if (person.id == id) person.copy(isFollowing = !person.isFollowing) else person
            }
        }
    }

    fun handleAddFriendDiscover(id: String) {
        val person = _discoverPeopleState.value.find { person -> person.id == id } ?: return
        
        // Mark as requested or add straight to outgoing requests
        _discoverPeopleState.update { current ->
            current.map { p ->
                if (p.id == id) p.copy(isFriendRequested = true) else p
            }
        }

        val newRequest = FriendRequest(
            id = "req_${person.id}",
            name = person.name,
            avatarUrl = person.avatarUrl,
            mutualFriendsCount = person.mutualFriendsCount,
            reason = person.reason,
            type = "Outgoing"
        )
        _friendRequestsState.update { listOf(newRequest) + it }
    }
}
