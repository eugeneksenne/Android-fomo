package com.example.core.data.feed

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CommentItem(
    val id: String,
    val author: String,
    val avatar: String,
    val text: String,
    val time: String
)

data class InvitationData(
    val venueName: String,
    val isVenueVerified: Boolean,
    val creatorName: String,
    val initialHours: Int,
    val initialMinutes: Int,
    val venueClosedText: String = "Opens Friday 18:00",
    var status: String = "ACTIVE" // ACTIVE, ENDED, CLOSED
)

data class Moment(
    val id: String,
    val username: String,
    val avatarUrl: String,
    val isVerified: Boolean,
    val isFollowing: Boolean,
    val momentType: String, // "PHOTO", "VIDEO", "LIVE", "REPLAY", "SPONSORED"
    val mediaUrl: String,
    val captionOriginal: String,
    val captionTranslation: String,
    val timeAgo: String,
    val locationName: String,
    val distanceText: String,
    val ripplesCount: Int,
    val likesCount: Int,
    val isLiked: Boolean,
    val isSaved: Boolean,
    val comments: List<CommentItem>,
    val friendActivityText: String,
    val invitation: InvitationData?,
    val momentumState: String = "Quiet", // "Quiet", "Active", "Heating", "Hot", "Viral"
    val currentVelocity: Float = 0.5f, // ripples per minute
    val isReplayProcessed: Boolean = false,
    val liveViewers: Int = 0
)

data class FeedState(
    val moments: List<Moment> = emptyList(),
    val activeTab: String = "For You", // "For You", "Following", "Nearby", "Live"
    val isRefreshing: Boolean = false
)

object FeedRepository {
    private val _state = MutableStateFlow(FeedState(moments = getInitialMoments()))
    val state: StateFlow<FeedState> = _state.asStateFlow()

    fun setActiveTab(tab: String) {
        _state.update { it.copy(activeTab = tab) }
    }

    fun toggleFollow(momentId: String) {
        _state.update { currentState ->
            val updatedMoments = currentState.moments.map { moment ->
                if (moment.id == momentId) moment.copy(isFollowing = !moment.isFollowing) else moment
            }
            currentState.copy(moments = updatedMoments)
        }
    }

    fun toggleLike(momentId: String) {
        _state.update { currentState ->
            val updatedMoments = currentState.moments.map { moment ->
                if (moment.id == momentId) {
                    val newLiked = !moment.isLiked
                    val newLikesCount = if (newLiked) moment.likesCount + 1 else moment.likesCount - 1
                    moment.copy(isLiked = newLiked, likesCount = newLikesCount)
                } else moment
            }
            currentState.copy(moments = updatedMoments)
        }
    }

    fun rippleMoment(momentId: String) {
        _state.update { currentState ->
            val updatedMoments = currentState.moments.map { moment ->
                if (moment.id == momentId) {
                    val newRipples = moment.ripplesCount + 1
                    val newVelocity = moment.currentVelocity + 2.5f
                    val newMomentum = when {
                        newVelocity > 15f -> "Viral"
                        newVelocity > 8f -> "Hot"
                        newVelocity > 3f -> "Heating"
                        else -> "Active"
                    }
                    moment.copy(
                        ripplesCount = newRipples,
                        currentVelocity = newVelocity,
                        momentumState = newMomentum
                    )
                } else moment
            }
            currentState.copy(moments = updatedMoments)
        }
    }

    fun toggleSave(momentId: String) {
        _state.update { currentState ->
            val updatedMoments = currentState.moments.map { moment ->
                if (moment.id == momentId) moment.copy(isSaved = !moment.isSaved) else moment
            }
            currentState.copy(moments = updatedMoments)
        }
    }

    fun addComment(momentId: String, text: String, author: String = "Me", avatar: String = "https://i.pravatar.cc/150?img=12") {
        _state.update { currentState ->
            val updatedMoments = currentState.moments.map { moment ->
                if (moment.id == momentId) {
                    val newComment = CommentItem(
                        id = "c_${System.currentTimeMillis()}",
                        author = author,
                        avatar = avatar,
                        text = text,
                        time = "Just now"
                    )
                    moment.copy(comments = listOf(newComment) + moment.comments)
                } else moment
            }
            currentState.copy(moments = updatedMoments)
        }
    }
    
    fun setInvitationStatus(momentId: String, status: String) {
        _state.update { currentState ->
            val updatedMoments = currentState.moments.map { moment ->
                if (moment.id == momentId && moment.invitation != null) {
                    moment.copy(invitation = moment.invitation.copy(status = status))
                } else moment
            }
            currentState.copy(moments = updatedMoments)
        }
    }

    private fun getInitialMoments(): List<Moment> {
        return listOf(
            Moment(
                id = "m1",
                username = "Amanda",
                avatarUrl = "https://i.pravatar.cc/150?img=47",
                isVerified = true,
                isFollowing = false,
                momentType = "VIDEO",
                mediaUrl = "https://images.unsplash.com/photo-1545128485-c400e7702796",
                captionOriginal = "VIP Booths at Cocoon are unmatched 🔥 #Nightlife #JHB",
                captionTranslation = "",
                timeAgo = "2h ago",
                locationName = "Cocoon Nightclub",
                distanceText = "280m away",
                ripplesCount = 842,
                likesCount = 2105,
                isLiked = false,
                isSaved = false,
                comments = listOf(
                    CommentItem("c1", "Sarah K", "https://i.pravatar.cc/150?img=5", "We need to go here this weekend!!", "1h ago"),
                    CommentItem("c2", "Musa", "https://i.pravatar.cc/150?img=11", "Dope vibe fr", "30m ago")
                ),
                friendActivityText = "👥 8 friends rippled this",
                invitation = InvitationData(
                    venueName = "Cocoon Nightclub",
                    isVenueVerified = true,
                    creatorName = "Amanda",
                    initialHours = 1,
                    initialMinutes = 45
                ),
                momentumState = "Heating",
                currentVelocity = 4.2f
            ),
            Moment(
                id = "m2",
                username = "DJ Zinhle",
                avatarUrl = "https://i.pravatar.cc/150?img=45",
                isVerified = true,
                isFollowing = true,
                momentType = "LIVE",
                mediaUrl = "https://images.unsplash.com/photo-1574169208507-84376144848b",
                captionOriginal = "Live from the Main Stage 🎧",
                captionTranslation = "",
                timeAgo = "LIVE",
                locationName = "Sandton Convention Centre",
                distanceText = "4.2km away",
                ripplesCount = 15200,
                likesCount = 42890,
                isLiked = true,
                isSaved = false,
                comments = emptyList(),
                friendActivityText = "🔥 Your circle is pulling up",
                invitation = null,
                momentumState = "Viral",
                currentVelocity = 18.5f,
                liveViewers = 2400
            ),
            Moment(
                id = "m3",
                username = "Thabo",
                avatarUrl = "https://i.pravatar.cc/150?img=33",
                isVerified = false,
                isFollowing = true,
                momentType = "PHOTO",
                mediaUrl = "https://images.unsplash.com/photo-1551043047-1d2adf00f3fd",
                captionOriginal = "Quiet dinner before the storm 🍷",
                captionTranslation = "",
                timeAgo = "5h ago",
                locationName = "Marble",
                distanceText = "1.1km away",
                ripplesCount = 12,
                likesCount = 45,
                isLiked = false,
                isSaved = false,
                comments = listOf(),
                friendActivityText = "",
                invitation = InvitationData(
                    venueName = "Marble",
                    isVenueVerified = true,
                    creatorName = "Thabo",
                    initialHours = 0,
                    initialMinutes = 0,
                    status = "ENDED"
                ),
                momentumState = "Quiet",
                currentVelocity = 0.1f
            ),
            Moment(
                id = "m4",
                username = "Club 55",
                avatarUrl = "https://i.pravatar.cc/150?img=18",
                isVerified = true,
                isFollowing = false,
                momentType = "REPLAY",
                mediaUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67",
                captionOriginal = "Insane close out to last night!!",
                captionTranslation = "",
                timeAgo = "Replay • Ended 5m ago",
                locationName = "Club 55",
                distanceText = "3.4km away",
                ripplesCount = 4320,
                likesCount = 11200,
                isLiked = false,
                isSaved = true,
                comments = listOf(),
                friendActivityText = "✨ 3 friends routed here tonight",
                invitation = null,
                momentumState = "Hot",
                currentVelocity = 9.2f,
                isReplayProcessed = true
            )
        )
    }
}
