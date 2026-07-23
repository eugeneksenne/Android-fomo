package com.example.core.data.feed

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
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

data class NightlifeStory(
    val id: String,
    val username: String,
    val avatarUrl: String,
    val storyMediaUrl: String,
    val locationName: String,
    val isLiveNow: Boolean = false,
    val hasUnseen: Boolean = true
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
    val liveViewers: Int = 0,
    val audioTrackName: String = "Original Nightlife Audio"
)

data class FeedState(
    val moments: List<Moment> = emptyList(),
    val stories: List<NightlifeStory> = emptyList(),
    val activeTab: String = "For You", // "For You", "Following", "Nearby", "Live"
    val isRefreshing: Boolean = false
)

object FeedRepository {
    private var firestore: FirebaseFirestore? = null

    private val _state = MutableStateFlow(FeedState(moments = getInitialMoments(), stories = getInitialStories()))
    val state: StateFlow<FeedState> = _state.asStateFlow()

    init {
        initFirebaseSync()
    }

    fun initFirebaseSync() {
        try {
            val db = FirebaseFirestore.getInstance()
            firestore = db

            db.collection("moments").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.w("FeedRepository", "Firestore listener warning: $error")
                    return@addSnapshotListener
                }
                if (snapshot.isEmpty) {
                    seedFirestoreMoments(db)
                } else {
                    val firestoreMoments = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val username = doc.getString("username") ?: "User"
                        val avatarUrl = doc.getString("avatarUrl") ?: ""
                        val isVerified = doc.getBoolean("isVerified") ?: false
                        val momentType = doc.getString("momentType") ?: "PHOTO"
                        val mediaUrl = doc.getString("mediaUrl") ?: ""
                        val captionOriginal = doc.getString("captionOriginal") ?: ""
                        val timeAgo = doc.getString("timeAgo") ?: "Just now"
                        val locationName = doc.getString("locationName") ?: "Truth Nightclub"
                        val distanceText = doc.getString("distanceText") ?: "250m away"
                        val ripplesCount = doc.getLong("ripplesCount")?.toInt() ?: 10
                        val likesCount = doc.getLong("likesCount")?.toInt() ?: 5

                        Moment(
                            id = id,
                            username = username,
                            avatarUrl = avatarUrl,
                            isVerified = isVerified,
                            isFollowing = true,
                            momentType = momentType,
                            mediaUrl = mediaUrl,
                            captionOriginal = captionOriginal,
                            captionTranslation = "",
                            timeAgo = timeAgo,
                            locationName = locationName,
                            distanceText = distanceText,
                            ripplesCount = ripplesCount,
                            likesCount = likesCount,
                            isLiked = false,
                            isSaved = false,
                            comments = emptyList(),
                            friendActivityText = "⚡ Real-time Ripple Moment",
                            invitation = null,
                            momentumState = "Hot",
                            currentVelocity = 5.0f
                        )
                    }
                    if (firestoreMoments.isNotEmpty()) {
                        _state.update { current ->
                            val existingIds = firestoreMoments.map { it.id }.toSet()
                            val localOnly = current.moments.filter { it.id !in existingIds }
                            current.copy(moments = firestoreMoments + localOnly)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FeedRepository", "Firestore init skipped or unavailable", e)
        }
    }

    private fun seedFirestoreMoments(db: FirebaseFirestore) {
        try {
            getInitialMoments().forEach { moment ->
                val doc = mapOf(
                    "id" to moment.id,
                    "username" to moment.username,
                    "avatarUrl" to moment.avatarUrl,
                    "isVerified" to moment.isVerified,
                    "momentType" to moment.momentType,
                    "mediaUrl" to moment.mediaUrl,
                    "captionOriginal" to moment.captionOriginal,
                    "timeAgo" to moment.timeAgo,
                    "locationName" to moment.locationName,
                    "distanceText" to moment.distanceText,
                    "ripplesCount" to moment.ripplesCount,
                    "likesCount" to moment.likesCount,
                    "timestampMs" to System.currentTimeMillis()
                )
                db.collection("moments").document(moment.id).set(doc)
            }
        } catch (e: Exception) {
            Log.e("FeedRepository", "Error seeding Firestore moments", e)
        }
    }

    fun addMoment(
        username: String,
        avatarUrl: String,
        momentType: String,
        mediaUrl: String,
        captionOriginal: String,
        locationName: String
    ): Moment {
        val newMoment = Moment(
            id = "m_${System.currentTimeMillis()}",
            username = username,
            avatarUrl = if (avatarUrl.isBlank()) "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200&auto=format&fit=crop" else avatarUrl,
            isVerified = true,
            isFollowing = true,
            momentType = momentType,
            mediaUrl = if (mediaUrl.isBlank()) "https://images.unsplash.com/photo-1545128485-c400e7702796" else mediaUrl,
            captionOriginal = captionOriginal,
            captionTranslation = "",
            timeAgo = "Just now",
            locationName = if (locationName.isBlank()) "Truth Nightclub" else locationName,
            distanceText = "Right here",
            ripplesCount = 25,
            likesCount = 1,
            isLiked = true,
            isSaved = false,
            comments = emptyList(),
            friendActivityText = "⚡ You captured this moment",
            invitation = null,
            momentumState = "Heating",
            currentVelocity = 8.5f
        )

        _state.update { current ->
            current.copy(moments = listOf(newMoment) + current.moments)
        }

        firestore?.let { db ->
            try {
                val doc = mapOf(
                    "id" to newMoment.id,
                    "username" to newMoment.username,
                    "avatarUrl" to newMoment.avatarUrl,
                    "isVerified" to newMoment.isVerified,
                    "momentType" to newMoment.momentType,
                    "mediaUrl" to newMoment.mediaUrl,
                    "captionOriginal" to newMoment.captionOriginal,
                    "timeAgo" to newMoment.timeAgo,
                    "locationName" to newMoment.locationName,
                    "distanceText" to newMoment.distanceText,
                    "ripplesCount" to newMoment.ripplesCount,
                    "likesCount" to newMoment.likesCount,
                    "timestampMs" to System.currentTimeMillis()
                )
                db.collection("moments").document(newMoment.id).set(doc)
            } catch (e: Exception) {
                Log.e("FeedRepository", "Error adding moment to Firestore", e)
            }
        }

        return newMoment
    }

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

    fun markStorySeen(storyId: String) {
        _state.update { currentState ->
            val updatedStories = currentState.stories.map { story ->
                if (story.id == storyId) story.copy(hasUnseen = false) else story
            }
            currentState.copy(stories = updatedStories)
        }
    }

    private fun getInitialStories(): List<NightlifeStory> {
        return listOf(
            NightlifeStory(
                id = "s1",
                username = "Amanda",
                avatarUrl = "https://i.pravatar.cc/150?img=47",
                storyMediaUrl = "https://images.unsplash.com/photo-1545128485-c400e7702796",
                locationName = "Cocoon VIP",
                isLiveNow = true,
                hasUnseen = true
            ),
            NightlifeStory(
                id = "s2",
                username = "DJ Zinhle",
                avatarUrl = "https://i.pravatar.cc/150?img=45",
                storyMediaUrl = "https://images.unsplash.com/photo-1574169208507-84376144848b",
                locationName = "Sandton Stage",
                isLiveNow = true,
                hasUnseen = true
            ),
            NightlifeStory(
                id = "s3",
                username = "AfroHaus",
                avatarUrl = "https://i.pravatar.cc/150?img=12",
                storyMediaUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7",
                locationName = "Rooftop Deck",
                isLiveNow = false,
                hasUnseen = true
            ),
            NightlifeStory(
                id = "s4",
                username = "Club 55",
                avatarUrl = "https://i.pravatar.cc/150?img=18",
                storyMediaUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67",
                locationName = "Main Floor",
                isLiveNow = false,
                hasUnseen = false
            ),
            NightlifeStory(
                id = "s5",
                username = "Truth JHB",
                avatarUrl = "https://i.pravatar.cc/150?img=25",
                storyMediaUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819",
                locationName = "Terrace Lounge",
                isLiveNow = false,
                hasUnseen = true
            )
        )
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
