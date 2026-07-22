package com.example.core.data.story

import com.example.core.data.chat.ChatRepository
import com.example.core.data.chat.RichMessageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

// Story Overlay Sticker Types
enum class StoryStickerType {
    VENUE, EVENT, FLASH_DROP, POLL, LOCATION, MUSIC
}

data class StorySticker(
    val id: String,
    val type: StoryStickerType,
    val title: String,
    val subtitle: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

// Segment level media
data class StoryMediaSegment(
    val id: String,
    val mediaUrl: String,
    val isVideo: Boolean = false,
    val durationSeconds: Int = 5,
    val timestampFormatted: String,
    val isViewed: Boolean = false,
    val stickers: List<StorySticker> = emptyList(),
    val locationName: String? = null,
    val filterName: String? = null
)

// Viewers / Analytics Telemetry
data class StoryViewerItem(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String,
    val viewedTimestampFormatted: String,
    val reactionEmoji: String? = null
)

// Grouped stories by user / venue
data class UserStoryGroup(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String,
    val isVerified: Boolean = false,
    val isMuted: Boolean = false,
    val isOwnStory: Boolean = false,
    val segments: List<StoryMediaSegment>,
    val totalViewsCount: Int = 0,
    val viewersList: List<StoryViewerItem> = emptyList()
) {
    val hasUnviewed: Boolean
        get() = segments.any { !it.isViewed }
}

enum class StoryPrivacy {
    PUBLIC, MY_CIRCLE, FOLLOWERS_ONLY
}

// Global State
data class StoryRepositoryState(
    val userStoryGroups: List<UserStoryGroup> = emptyList(),
    val activePublishProgress: Float? = null, // null if idle, 0.0f..1.0f if publishing
    val mutedUserIds: Set<String> = emptySet()
)

object StoryRepository {
    private val _state = MutableStateFlow(
        StoryRepositoryState(
            userStoryGroups = seedInitialStories()
        )
    )
    val state: StateFlow<StoryRepositoryState> = _state.asStateFlow()

    // --- Mark Story Segment Viewed ---
    fun markSegmentViewed(userId: String, segmentId: String) {
        _state.update { current ->
            current.copy(
                userStoryGroups = current.userStoryGroups.map { group ->
                    if (group.userId == userId) {
                        val updatedSegments = group.segments.map { seg ->
                            if (seg.id == segmentId) seg.copy(isViewed = true) else seg
                        }
                        group.copy(segments = updatedSegments)
                    } else group
                }
            )
        }
    }

    // --- Story Reaction (Hearts, Fire, Emoji Pulse) ---
    fun addReactionToSegment(storyUserId: String, segmentId: String, emoji: String) {
        _state.update { current ->
            current.copy(
                userStoryGroups = current.userStoryGroups.map { group ->
                    if (group.userId == storyUserId) {
                        val updatedViewers = group.viewersList.toMutableList()
                        val myIdx = updatedViewers.indexOfFirst { it.userId == "me" }
                        val myViewer = StoryViewerItem("me", "Me", "https://i.pravatar.cc/150?img=12", "Just now", emoji)
                        if (myIdx != -1) {
                            updatedViewers[myIdx] = myViewer
                        } else {
                            updatedViewers.add(0, myViewer)
                        }
                        group.copy(
                            viewersList = updatedViewers,
                            totalViewsCount = group.totalViewsCount + (if (myIdx == -1) 1 else 0)
                        )
                    } else group
                }
            )
        }
    }

    // --- Story Reply -> Bridging seamlessly into Direct Messages ---
    fun replyToStory(storyUserId: String, storyUserName: String, segment: StoryMediaSegment, replyText: String) {
        // Record reaction
        addReactionToSegment(storyUserId, segment.id, "💬")

        // Dispatch DM in ChatRepository
        val conversationId = when (storyUserId) {
            "u_sarah" -> "conv_1"
            "u_mike" -> "conv_5"
            else -> "conv_1"
        }

        ChatRepository.sendMessage(
            conversationId = conversationId,
            type = RichMessageType.IMAGE,
            content = "Replying to story: $replyText",
            metadata = mapOf(
                "storyMediaUrl" to segment.mediaUrl,
                "storyOwner" to storyUserName,
                "timestamp" to segment.timestampFormatted
            )
        )
    }

    // --- Toggle Mute User Stories ---
    fun toggleMuteUserStories(userId: String) {
        _state.update { current ->
            val updatedMuted = current.mutedUserIds.toMutableSet()
            if (updatedMuted.contains(userId)) {
                updatedMuted.remove(userId)
            } else {
                updatedMuted.add(userId)
            }
            val updatedGroups = current.userStoryGroups.map { group ->
                if (group.userId == userId) group.copy(isMuted = updatedMuted.contains(userId)) else group
            }
            current.copy(userStoryGroups = updatedGroups, mutedUserIds = updatedMuted)
        }
    }

    // --- Publish New Story Segment ---
    fun publishNewStorySegment(
        mediaUrl: String,
        locationName: String?,
        filterName: String?,
        stickers: List<StorySticker>,
        privacy: StoryPrivacy
    ) {
        val newSegment = StoryMediaSegment(
            id = UUID.randomUUID().toString(),
            mediaUrl = mediaUrl,
            timestampFormatted = "Just now",
            isViewed = true,
            stickers = stickers,
            locationName = locationName,
            filterName = filterName
        )

        _state.update { current ->
            val updatedGroups = current.userStoryGroups.toMutableList()
            val ownIndex = updatedGroups.indexOfFirst { it.isOwnStory }

            if (ownIndex != -1) {
                val ownGroup = updatedGroups[ownIndex]
                updatedGroups[ownIndex] = ownGroup.copy(
                    segments = ownGroup.segments + newSegment
                )
            } else {
                val newOwnGroup = UserStoryGroup(
                    userId = "me",
                    userName = "My Story",
                    userAvatarUrl = "https://i.pravatar.cc/150?img=12",
                    isOwnStory = true,
                    segments = listOf(newSegment),
                    totalViewsCount = 0,
                    viewersList = emptyList()
                )
                updatedGroups.add(0, newOwnGroup)
            }

            current.copy(userStoryGroups = updatedGroups)
        }
    }

    // --- Delete Segment from Own Story ---
    fun deleteOwnStorySegment(segmentId: String) {
        _state.update { current ->
            val updatedGroups = current.userStoryGroups.map { group ->
                if (group.isOwnStory) {
                    val updatedSegments = group.segments.filter { it.id != segmentId }
                    group.copy(segments = updatedSegments)
                } else group
            }
            current.copy(userStoryGroups = updatedGroups)
        }
    }

    // --- Seed Initial Stories ---
    private fun seedInitialStories(): List<UserStoryGroup> {
        return listOf(
            UserStoryGroup(
                userId = "me",
                userName = "My Story",
                userAvatarUrl = "https://i.pravatar.cc/150?img=12",
                isOwnStory = true,
                segments = listOf(
                    StoryMediaSegment(
                        id = "seg_my_1",
                        mediaUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=400",
                        timestampFormatted = "3h ago",
                        isViewed = true,
                        locationName = "Rosebank Rooftop Lounge",
                        filterName = "Golden Hour",
                        stickers = listOf(
                            StorySticker("st_1", StoryStickerType.LOCATION, "Rosebank Rooftop Lounge"),
                            StorySticker("st_2", StoryStickerType.MUSIC, "Amapiano Vibes - Kabza De Small")
                        )
                    )
                ),
                totalViewsCount = 84,
                viewersList = listOf(
                    StoryViewerItem("u_sarah", "Sarah Jenkins", "https://i.pravatar.cc/150?img=5", "10m ago", "🔥"),
                    StoryViewerItem("u_mike", "Mike Ross", "https://i.pravatar.cc/150?img=11", "25m ago", "❤️"),
                    StoryViewerItem("u_emma", "Emma Stone", "https://i.pravatar.cc/150?img=9", "1h ago")
                )
            ),
            UserStoryGroup(
                userId = "u_sarah",
                userName = "Sarah Jenkins",
                userAvatarUrl = "https://i.pravatar.cc/150?img=5",
                isVerified = true,
                segments = listOf(
                    StoryMediaSegment(
                        id = "seg_sarah_1",
                        mediaUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=400",
                        timestampFormatted = "1h ago",
                        isViewed = false,
                        locationName = "Cocoon Club Main Stage",
                        stickers = listOf(
                            StorySticker("st_s1", StoryStickerType.VENUE, "Cocoon Club Rosebank", "Guest List Open"),
                            StorySticker("st_s2", StoryStickerType.FLASH_DROP, "Free Tequila Voucher", "Tap to Claim")
                        )
                    ),
                    StoryMediaSegment(
                        id = "seg_sarah_2",
                        mediaUrl = "https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=400",
                        timestampFormatted = "45m ago",
                        isViewed = false,
                        stickers = listOf(
                            StorySticker("st_s3", StoryStickerType.POLL, "Are you coming tonight?", "Yes 🔥 / Skipping 😴")
                        )
                    )
                )
            ),
            UserStoryGroup(
                userId = "u_cocoon",
                userName = "Cocoon Official",
                userAvatarUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=200",
                isVerified = true,
                segments = listOf(
                    StoryMediaSegment(
                        id = "seg_cocoon_1",
                        mediaUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=400",
                        timestampFormatted = "2h ago",
                        isViewed = false,
                        stickers = listOf(
                            StorySticker("st_c1", StoryStickerType.EVENT, "Friday Fever Night", "Doors open 10 PM")
                        )
                    )
                )
            ),
            UserStoryGroup(
                userId = "u_mike",
                userName = "Mike Ross",
                userAvatarUrl = "https://i.pravatar.cc/150?img=11",
                segments = listOf(
                    StoryMediaSegment(
                        id = "seg_mike_1",
                        mediaUrl = "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?q=80&w=400",
                        timestampFormatted = "5h ago",
                        isViewed = true
                    )
                )
            ),
            UserStoryGroup(
                userId = "u_emma",
                userName = "Emma Stone",
                userAvatarUrl = "https://i.pravatar.cc/150?img=9",
                isMuted = true,
                segments = listOf(
                    StoryMediaSegment(
                        id = "seg_emma_1",
                        mediaUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=400",
                        timestampFormatted = "6h ago",
                        isViewed = true
                    )
                )
            )
        )
    }
}
