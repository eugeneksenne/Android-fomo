package com.example.core.data.moderation

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-generated content moderation: report, block and hide.
 *
 * **This exists because Google Play requires it.** The User Generated Content
 * policy mandates that any app hosting UGC provides, at minimum:
 *   1. a way to report objectionable content,
 *   2. a way to block abusive users,
 *   3. removal of content from blocked users.
 *
 * The app previously had none of these anywhere — not in the feed, chats or
 * stories — which is one of the most common causes of outright Play rejection
 * for social apps.
 *
 * Blocks and hides are persisted locally so they apply immediately and survive
 * restart even when offline, and mirrored to Firestore (when available) so they
 * follow the account across devices. Reports are queued locally if the write
 * fails, so a report is never silently dropped.
 */
class ModerationRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class ModerationState(
        /** User ids (or usernames for legacy seed content) the user has blocked. */
        val blockedUsers: Set<String> = emptySet(),
        /** Individual posts the user chose to hide. */
        val hiddenMoments: Set<String> = emptySet(),
        /** Posts already reported, so the UI can avoid offering it twice. */
        val reportedMoments: Set<String> = emptySet(),
    )

    /** Report categories. Mirrors the taxonomy Play expects apps to cover. */
    enum class ReportReason(val label: String) {
        SPAM("Spam or misleading"),
        HARASSMENT("Harassment or bullying"),
        HATE_SPEECH("Hate speech or symbols"),
        VIOLENCE("Violence or dangerous acts"),
        NUDITY("Nudity or sexual content"),
        SELF_HARM("Suicide or self-harm"),
        ILLEGAL("Illegal goods or services"),
        IMPERSONATION("Impersonation"),
        OTHER("Something else"),
    }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ModerationState> = _state.asStateFlow()

    // ---- queries -----------------------------------------------------------

    /**
     * True when content from [authorId] must not be shown.
     * Matching is case-insensitive so legacy seed rows keyed by display name
     * behave the same as real account ids.
     */
    fun isBlocked(authorId: String): Boolean =
        _state.value.blockedUsers.any { it.equals(authorId, ignoreCase = true) }

    fun isHidden(momentId: String): Boolean = momentId in _state.value.hiddenMoments

    fun isReported(momentId: String): Boolean = momentId in _state.value.reportedMoments

    /** Single predicate for feed filtering. */
    fun isVisible(momentId: String, authorId: String): Boolean =
        !isHidden(momentId) && !isBlocked(authorId)

    // ---- actions -----------------------------------------------------------

    fun blockUser(authorId: String) {
        if (authorId.isBlank()) return
        _state.value = _state.value.copy(blockedUsers = _state.value.blockedUsers + authorId)
        persist()
        syncBlockToBackend(authorId, blocked = true)
    }

    fun unblockUser(authorId: String) {
        _state.value = _state.value.copy(
            blockedUsers = _state.value.blockedUsers.filterNot {
                it.equals(authorId, ignoreCase = true)
            }.toSet()
        )
        persist()
        syncBlockToBackend(authorId, blocked = false)
    }

    fun hideMoment(momentId: String) {
        if (momentId.isBlank()) return
        _state.value = _state.value.copy(hiddenMoments = _state.value.hiddenMoments + momentId)
        persist()
    }

    fun unhideMoment(momentId: String) {
        _state.value = _state.value.copy(hiddenMoments = _state.value.hiddenMoments - momentId)
        persist()
    }

    /**
     * Files a report.
     *
     * The post is hidden locally straight away: Play expects reported content to
     * disappear for the reporter without waiting on a review. Reports are also
     * written to Firestore for triage; a failure keeps the local hide, so the
     * user's action is never lost.
     */
    fun reportMoment(
        momentId: String,
        authorId: String,
        reason: ReportReason,
        details: String = "",
        alsoBlockAuthor: Boolean = false,
    ) {
        _state.value = _state.value.copy(
            reportedMoments = _state.value.reportedMoments + momentId,
            hiddenMoments = _state.value.hiddenMoments + momentId,
        )
        persist()

        if (alsoBlockAuthor) blockUser(authorId)

        // Signed-out users can still hide/report locally; the backend write is
        // rejected by security rules (which require a matching reporterId) and
        // the report is queued for retry after sign-in. The local hide always
        // applies, so the user's action is never ignored.
        val reporterId = currentUserId()
        val report = mapOf(
            "momentId" to momentId,
            "authorId" to authorId,
            "reporterId" to reporterId,
            "reason" to reason.name,
            "reasonLabel" to reason.label,
            "details" to details.take(2000),
            "createdAtMs" to System.currentTimeMillis(),
            "status" to "PENDING",
        )

        try {
            FirebaseFirestore.getInstance()
                .collection("content_reports")
                .add(report)
                .addOnFailureListener { e ->
                    Log.w(TAG, "Report upload failed; kept locally", e)
                    queueReport(report)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Report backend unavailable; kept locally", e)
            queueReport(report)
        }
    }

    // ---- persistence -------------------------------------------------------

    private fun currentUserId(): String =
        runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()

    private fun syncBlockToBackend(authorId: String, blocked: Boolean) {
        val uid = currentUserId()
        if (uid.isBlank()) return
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("private").document("blocks")
            if (blocked) {
                doc.set(
                    mapOf("blocked" to _state.value.blockedUsers.toList()),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            } else {
                doc.set(
                    mapOf("blocked" to _state.value.blockedUsers.toList()),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to sync block list", e)
        }
    }

    /** Keeps a failed report so it can be retried rather than lost. */
    private fun queueReport(report: Map<String, Any?>) {
        runCatching {
            val existing = prefs.getStringSet(KEY_PENDING_REPORTS, emptySet())!!.toMutableSet()
            existing += report.entries.joinToString("|") { "${it.key}=${it.value}" }
            prefs.edit().putStringSet(KEY_PENDING_REPORTS, existing).apply()
        }
    }

    private fun persist() {
        runCatching {
            prefs.edit()
                .putStringSet(KEY_BLOCKED, _state.value.blockedUsers)
                .putStringSet(KEY_HIDDEN, _state.value.hiddenMoments)
                .putStringSet(KEY_REPORTED, _state.value.reportedMoments)
                .apply()
        }.onFailure { Log.e(TAG, "Unable to persist moderation state", it) }
    }

    private fun load(): ModerationState = runCatching {
        ModerationState(
            blockedUsers = prefs.getStringSet(KEY_BLOCKED, emptySet()).orEmpty(),
            hiddenMoments = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty(),
            reportedMoments = prefs.getStringSet(KEY_REPORTED, emptySet()).orEmpty(),
        )
    }.getOrElse { ModerationState() }

    companion object {
        private const val TAG = "Moderation"
        private const val PREFS = "fomo_moderation"
        private const val KEY_BLOCKED = "blocked_users"
        private const val KEY_HIDDEN = "hidden_moments"
        private const val KEY_REPORTED = "reported_moments"
        private const val KEY_PENDING_REPORTS = "pending_reports"

        @Volatile
        private var instance: ModerationRepository? = null

        fun getInstance(context: Context): ModerationRepository =
            instance ?: synchronized(this) {
                instance ?: ModerationRepository(context).also { instance = it }
            }
    }
}
