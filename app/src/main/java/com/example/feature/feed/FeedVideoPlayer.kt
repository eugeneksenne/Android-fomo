package com.example.feature.feed

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Autoplaying, looping video surface for a feed page.
 *
 * The feed rendered every post — including videos and live replays — with
 * `AsyncImage`, so video posts appeared as a single frozen frame that never
 * played. For a vertical, video-first feed that is the core interaction, so
 * this was the screen's most visible functional gap.
 *
 * Playback rules follow the conventions users expect from a short-form feed:
 *  - only the page currently on screen plays ([isActive]),
 *  - video loops,
 *  - audio follows a global mute toggle,
 *  - the player is released as soon as the page leaves composition, so
 *    scrolling a long feed does not accumulate decoders. Android devices have a
 *    hard limit on concurrent codec instances; leaking them causes playback to
 *    fail silently after a dozen or so videos.
 */
@OptIn(UnstableApi::class)
@Composable
fun FeedVideoPlayer(
    mediaUrl: String,
    isActive: Boolean,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val exoPlayer = remember(mediaUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUrl))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            prepare()
        }
    }

    // Only the visible page plays. Without this every page in the pager would
    // play at once, competing for decoders and audio focus.
    LaunchedEffect(isActive) {
        exoPlayer.playWhenReady = isActive
        if (!isActive) exoPlayer.seekTo(0)
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(mediaUrl) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                // Transparent background avoids a black flash between pages.
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
    )
}

/**
 * True when a moment should be rendered with the video player rather than a
 * static image.
 */
fun isPlayableVideo(momentType: String, mediaUrl: String): Boolean {
    if (mediaUrl.isBlank()) return false
    val type = momentType.uppercase()
    if (type == "VIDEO" || type == "LIVE" || type == "REPLAY") return true
    // Fall back to the file extension for content whose type wasn't recorded.
    val path = mediaUrl.substringBefore('?').lowercase()
    return path.endsWith(".mp4") || path.endsWith(".m3u8") || path.endsWith(".webm")
}
