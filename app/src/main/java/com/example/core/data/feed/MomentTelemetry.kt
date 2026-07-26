package com.example.core.data.feed

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Watch and engagement telemetry for Moments.
 *
 * The Feed spec ranks by "Ripple velocity, watch completion, likes, comments,
 * shares, saves, friend activity, venue popularity, distance, recency, trust
 * score". Of those, **watch completion was never measured at all** — nothing in
 * the app recorded that a Moment had been seen, let alone for how long. The
 * ranking engine therefore had no inputs, and the Creator Analytics sheet had
 * nothing real to display.
 *
 * This records a view session per Moment: when it became visible, how long it
 * was watched, and how much of it completed. Counters are aggregated locally
 * for instant ranking and mirrored to Firestore for the creator dashboard.
 *
 * Deliberately cheap: ranking reads from the in-memory map, and only completed
 * sessions are written to the backend (batched by the caller's lifecycle), so
 * scrolling a feed does not produce a write per frame.
 */
object MomentTelemetry {

    private const val TAG = "MomentTelemetry"

    /** A view counts once the Moment has been on screen this long. */
    private const val VIEW_THRESHOLD_MS = 1_000L

    /** Watch fraction at or above which a view counts as "completed". */
    private const val COMPLETION_THRESHOLD = 0.9f

    /** Assumed duration for media whose real length we don't know. */
    private const val ASSUMED_DURATION_MS = 8_000L

    data class MomentStats(
        val momentId: String = "",
        val views: Int = 0,
        val totalWatchMs: Long = 0,
        val completedViews: Int = 0,
        val profileVisits: Int = 0,
        val venueClicks: Int = 0,
        val routeClicks: Int = 0,
        val clubLobbyOpens: Int = 0,
        val shares: Int = 0,
    ) {
        /** Mean fraction of the media watched, 0f..1f. */
        val averageWatchCompletion: Float
            get() = if (views == 0) 0f
            else (totalWatchMs.toFloat() / (views * ASSUMED_DURATION_MS)).coerceIn(0f, 1f)

        /** Share of views that reached the completion threshold. */
        val completionRate: Float
            get() = if (views == 0) 0f else completedViews.toFloat() / views

        val averageWatchMs: Long get() = if (views == 0) 0L else totalWatchMs / views
    }

    private val _stats = MutableStateFlow<Map<String, MomentStats>>(emptyMap())
    val stats: StateFlow<Map<String, MomentStats>> = _stats.asStateFlow()

    /** Start time of the currently visible Moment, if any. */
    private var openSessionMomentId: String? = null
    private var openSessionStartMs: Long = 0L

    fun statsFor(momentId: String): MomentStats =
        _stats.value[momentId] ?: MomentStats(momentId = momentId)

    /**
     * Marks a Moment as visible. Any previously open session is closed first,
     * so a fast scroll can't leave two sessions running and double-count watch
     * time.
     */
    fun onMomentVisible(momentId: String, nowMs: Long = System.currentTimeMillis()) {
        if (openSessionMomentId == momentId) return
        openSessionMomentId?.let { endSession(it, nowMs) }
        openSessionMomentId = momentId
        openSessionStartMs = nowMs
    }

    /** Closes the open session, if the given Moment owns it. */
    fun onMomentHidden(momentId: String, nowMs: Long = System.currentTimeMillis()) {
        if (openSessionMomentId != momentId) return
        endSession(momentId, nowMs)
        openSessionMomentId = null
    }

    /** Closes any open session, e.g. when the feed leaves the screen. */
    fun flush(nowMs: Long = System.currentTimeMillis()) {
        openSessionMomentId?.let { endSession(it, nowMs) }
        openSessionMomentId = null
    }

    private fun endSession(momentId: String, nowMs: Long) {
        val watched = (nowMs - openSessionStartMs).coerceAtLeast(0L)
        if (watched < VIEW_THRESHOLD_MS) return

        val fraction = (watched.toFloat() / ASSUMED_DURATION_MS).coerceIn(0f, 1f)
        val completed = fraction >= COMPLETION_THRESHOLD

        update(momentId) { current ->
            current.copy(
                views = current.views + 1,
                totalWatchMs = current.totalWatchMs + watched,
                completedViews = current.completedViews + if (completed) 1 else 0,
            )
        }

        pushToBackend(momentId, watched, completed)
    }

    // ---- discrete interactions --------------------------------------------

    fun recordProfileVisit(momentId: String) =
        update(momentId) { it.copy(profileVisits = it.profileVisits + 1) }

    fun recordVenueClick(momentId: String) =
        update(momentId) { it.copy(venueClicks = it.venueClicks + 1) }

    fun recordRouteClick(momentId: String) =
        update(momentId) { it.copy(routeClicks = it.routeClicks + 1) }

    fun recordClubLobbyOpen(momentId: String) =
        update(momentId) { it.copy(clubLobbyOpens = it.clubLobbyOpens + 1) }

    fun recordShare(momentId: String) =
        update(momentId) { it.copy(shares = it.shares + 1) }

    private inline fun update(momentId: String, transform: (MomentStats) -> MomentStats) {
        val current = _stats.value[momentId] ?: MomentStats(momentId = momentId)
        _stats.value = _stats.value + (momentId to transform(current))
    }

    /**
     * Increments server-side counters. Uses atomic FieldValue increments so
     * concurrent viewers don't clobber each other's writes.
     */
    private fun pushToBackend(momentId: String, watchedMs: Long, completed: Boolean) {
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            ?: return
        try {
            FirebaseFirestore.getInstance()
                .collection("moments").document(momentId)
                .collection("telemetry").document("aggregate")
                .set(
                    mapOf(
                        "views" to FieldValue.increment(1),
                        "totalWatchMs" to FieldValue.increment(watchedMs),
                        "completedViews" to FieldValue.increment(if (completed) 1L else 0L),
                        "lastViewerId" to uid,
                        "updatedAtMs" to System.currentTimeMillis(),
                    ),
                    com.google.firebase.firestore.SetOptions.merge(),
                )
        } catch (e: Exception) {
            // Telemetry must never break playback.
            Log.w(TAG, "Unable to record telemetry for $momentId", e)
        }
    }

    /** Test hook. */
    internal fun reset() {
        _stats.value = emptyMap()
        openSessionMomentId = null
        openSessionStartMs = 0L
    }
}
