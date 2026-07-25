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

/**
 * A Moment Invitation: a *temporary* invitation to join the creator where they
 * are right now. Per the spec this is not a venue information card - only its
 * state changes as the invitation runs down.
 *
 * The previous model stored `initialHours`/`initialMinutes` and a mutable
 * status string that nothing ever updated, so the card rendered a hardcoded
 * "Available for 02:44:18" that never moved and a state that only changed via
 * a debug button. Expiry is now an absolute timestamp, which is the only way a
 * countdown can survive recomposition, backgrounding and process death.
 */
data class InvitationData(
    val venueName: String,
    val isVenueVerified: Boolean,
    val creatorName: String,
    /** Wall-clock instant the invitation stops being active. */
    val expiresAtMs: Long,
    /** Set when the creator explicitly ends sharing before expiry. */
    val endedEarlyAtMs: Long? = null,
    /** Opening hours copy shown when the venue itself is closed. */
    val venueClosedText: String = "",
    /** True when venue intelligence reports the venue is currently shut. */
    val isVenueClosed: Boolean = false,
) {
    /** Spec states 1-3, derived rather than stored so they cannot disagree. */
    enum class State { ACTIVE, ENDED, VENUE_CLOSED }

    fun stateAt(nowMs: Long = System.currentTimeMillis()): State = when {
        isVenueClosed -> State.VENUE_CLOSED
        endedEarlyAtMs != null -> State.ENDED
        nowMs >= expiresAtMs -> State.ENDED
        else -> State.ACTIVE
    }

    fun remainingMs(nowMs: Long = System.currentTimeMillis()): Long =
        (expiresAtMs - nowMs).coerceAtLeast(0L)

    /** "Until I Leave" invitations are modelled as a far-future expiry. */
    val isOpenEnded: Boolean get() = expiresAtMs >= OPEN_ENDED_THRESHOLD_MS

    companion object {
        /** Durations the Camera offers when publishing an invitation. */
        val DURATION_OPTIONS_MINUTES = listOf(15, 30, 60, 120)

        /** Sentinel horizon representing "Until I Leave". */
        const val OPEN_ENDED_THRESHOLD_MS = 1_000L * 60 * 60 * 24 * 365

        fun expiryFor(durationMinutes: Int, nowMs: Long = System.currentTimeMillis()): Long =
            nowMs + durationMinutes * 60_000L

        /** Formats remaining time as MM:SS, or HH:MM:SS beyond an hour. */
        fun formatRemaining(remainingMs: Long): String {
            val total = remainingMs / 1000
            val h = total / 3600
            val m = (total % 3600) / 60
            val sec = total % 60
            return if (h > 0) String.format("%02d:%02d:%02d", h, m, sec)
            else String.format("%02d:%02d", m, sec)
        }
    }
}

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
    val audioTrackName: String = "Original Nightlife Audio",
    // Publish settings chosen by the author on the Camera preview screen.
    // These were previously collected in the UI and then discarded, so a
    // moment marked "Private" was still published publicly with its venue
    // attached. They are now carried on the model and enforced.
    val visibility: String = VISIBILITY_PUBLIC, // Public | Followers | Private
    val destinations: Set<String> = setOf(DESTINATION_FEED),
    val isVenueShared: Boolean = true
)

const val VISIBILITY_PUBLIC = "Public"
const val VISIBILITY_FOLLOWERS = "Followers"
const val VISIBILITY_PRIVATE = "Private"

const val DESTINATION_FEED = "Feed"
const val DESTINATION_VENUE = "Venue"
const val DESTINATION_CLUB_LOBBY = "Club Lobby"
const val DESTINATION_EVENT = "Event Details"
const val DESTINATION_PROFILE = "Profile Story"

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
                    // Seeding demo content from the client is DEBUG-ONLY.
                    // See ChatRepository for rationale.
                    if (com.example.BuildConfig.DEBUG) {
                        seedFirestoreMoments(db)
                    }
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

    /** Display name of the signed-in user, or a neutral fallback. */
    private fun currentUserName(): String =
        runCatching {
            val u = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            u?.displayName?.takeIf { it.isNotBlank() } ?: u?.email?.substringBefore("@")
        }.getOrNull() ?: "You"

    private fun currentUserAvatar(): String =
        runCatching {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
        }.getOrNull().orEmpty()

    fun addMoment(
        username: String,
        avatarUrl: String,
        momentType: String,
        mediaUrl: String,
        captionOriginal: String,
        locationName: String,
        visibility: String = VISIBILITY_PUBLIC,
        destinations: Set<String> = setOf(DESTINATION_FEED),
        isVenueShared: Boolean = true
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
            // Respect the author's "Hide Venue" choice.
            locationName = when {
                !isVenueShared -> ""
                locationName.isBlank() -> "Truth Nightclub"
                else -> locationName
            },
            distanceText = if (isVenueShared) "Right here" else "",
            ripplesCount = 25,
            likesCount = 1,
            isLiked = true,
            isSaved = false,
            comments = emptyList(),
            friendActivityText = "⚡ You captured this moment",
            invitation = null,
            momentumState = "Heating",
            currentVelocity = 8.5f,
            visibility = visibility,
            destinations = destinations,
            isVenueShared = isVenueShared
        )

        // Only surface the moment in the local feed when the author actually
        // chose to publish it there.
        if (destinations.contains(DESTINATION_FEED)) {
            _state.update { current ->
                current.copy(moments = listOf(newMoment) + current.moments)
            }
        }

        // A "Private" moment must never reach the shared `moments` collection.
        // It stays local to this device until a per-user private collection
        // exists. Publishing it here would make it world-readable.
        if (visibility == VISIBILITY_PRIVATE) {
            return newMoment
        }

        firestore?.let { db ->
            try {
                val doc = mapOf(
                    "id" to newMoment.id,
                    "visibility" to newMoment.visibility,
                    "destinations" to newMoment.destinations.toList(),
                    "isVenueShared" to newMoment.isVenueShared,
                    "authorId" to (
                        runCatching {
                            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        }.getOrNull() ?: ""
                    ),
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

    /**
     * Adds a comment authored by the signed-in user.
     *
     * The author previously defaulted to the literal "Me" with a stock avatar,
     * so every comment in a shared feed was attributed to the same fictional
     * person regardless of who wrote it.
     */
    fun addComment(
        momentId: String,
        text: String,
        author: String = currentUserName(),
        avatar: String = currentUserAvatar(),
    ) {
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
    
    /**
     * Ends an invitation early, per the spec's "creator manually ends sharing".
     * State is derived from timestamps, so this records *when* rather than
     * writing a status string that could contradict the clock.
     */
    fun endInvitation(momentId: String) {
        _state.update { current ->
            current.copy(
                moments = current.moments.map { moment ->
                    if (moment.id == momentId && moment.invitation != null &&
                        moment.invitation.endedEarlyAtMs == null
                    ) {
                        moment.copy(
                            invitation = moment.invitation.copy(
                                endedEarlyAtMs = System.currentTimeMillis()
                            )
                        )
                    } else moment
                }
            )
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
                    expiresAtMs = InvitationData.expiryFor(durationMinutes = 105)
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
                    // Already expired -> renders State 2 (Invitation Ended).
                    expiresAtMs = System.currentTimeMillis() - 60_000L
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
