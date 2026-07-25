package com.example.feature.camera.live

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-first Live session tracking, crash recovery and the replay library.
 *
 * The spec is explicit: *"Every live is recorded locally before being uploaded"*
 * and *"Users never lose their broadcast"*. Previously ending a broadcast just
 * flipped a boolean — if the app was killed mid-stream there was no record that
 * a session had ever started, and the recording was orphaned.
 *
 * This store writes a session marker the moment recording begins, so an
 * unclean shutdown is detectable on next launch and the user is offered
 * Recover / Delete per the Crash Recovery section.
 *
 * Persistence uses SharedPreferences rather than Room because the payload is a
 * handful of small records and it must survive a process kill with no
 * migration risk.
 */
class LiveSessionStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Lifecycle of a locally recorded Live session. */
    enum class Status {
        /** Recording right now. An entry left in this state means we crashed. */
        RECORDING,

        /** Recording finalised, waiting to upload. */
        QUEUED,

        /** Upload in progress. */
        UPLOADING,

        /** Uploaded and live in the feed. */
        PUBLISHED,

        /** User chose to keep it local only. */
        DRAFT,

        /** Upload failed; retryable. */
        FAILED,
    }

    data class Session(
        val id: String,
        val status: Status,
        val venueName: String,
        val startedAtMs: Long,
        val endedAtMs: Long? = null,
        /** Local file Uri as a string; null until recording finalises. */
        val localUri: String? = null,
        /** Remote download URL once uploaded. */
        val remoteUrl: String? = null,
        val peakViewers: Int = 0,
        val failureReason: String? = null,
    ) {
        val durationSeconds: Long
            get() = ((endedAtMs ?: System.currentTimeMillis()) - startedAtMs) / 1000

        /** True when this session was interrupted rather than ended cleanly. */
        val isUnfinished: Boolean get() = status == Status.RECORDING
    }

    private val _sessions = MutableStateFlow(load())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    /**
     * Any session still marked RECORDING at startup means the process died
     * mid-broadcast. The spec calls for a "Recovered Live Found" prompt.
     */
    fun findRecoverable(): Session? = _sessions.value.firstOrNull { it.isUnfinished }

    fun startSession(id: String, venueName: String): Session {
        val session = Session(
            id = id,
            status = Status.RECORDING,
            venueName = venueName,
            startedAtMs = System.currentTimeMillis()
        )
        update { it + session }
        return session
    }

    fun markEnded(id: String, localUri: String?, peakViewers: Int) {
        update { list ->
            list.map { s ->
                if (s.id == id) {
                    s.copy(
                        status = if (localUri != null) Status.QUEUED else Status.FAILED,
                        endedAtMs = System.currentTimeMillis(),
                        localUri = localUri,
                        peakViewers = peakViewers,
                        failureReason = if (localUri == null) "Recording produced no file" else null
                    )
                } else s
            }
        }
    }

    fun markUploading(id: String) = setStatus(id, Status.UPLOADING)

    fun markPublished(id: String, remoteUrl: String) {
        update { list ->
            list.map { s ->
                if (s.id == id) s.copy(status = Status.PUBLISHED, remoteUrl = remoteUrl) else s
            }
        }
    }

    fun markFailed(id: String, reason: String) {
        update { list ->
            list.map { s ->
                if (s.id == id) s.copy(status = Status.FAILED, failureReason = reason) else s
            }
        }
    }

    /** Recovers an interrupted session into the upload queue. */
    fun recover(id: String, localUri: String?) {
        update { list ->
            list.map { s ->
                if (s.id == id) {
                    s.copy(
                        status = if (localUri != null) Status.QUEUED else Status.FAILED,
                        endedAtMs = s.endedAtMs ?: System.currentTimeMillis(),
                        localUri = localUri ?: s.localUri,
                        failureReason = if (localUri == null && s.localUri == null) {
                            "The recording file could not be found"
                        } else null
                    )
                } else s
            }
        }
    }

    fun delete(id: String) = update { list -> list.filterNot { it.id == id } }

    private fun setStatus(id: String, status: Status) =
        update { list -> list.map { if (it.id == id) it.copy(status = status) else it } }

    private fun update(transform: (List<Session>) -> List<Session>) {
        val next = transform(_sessions.value).sortedByDescending { it.startedAtMs }
        _sessions.value = next
        persist(next)
    }

    // ---- persistence -------------------------------------------------------

    private fun persist(sessions: List<Session>) {
        try {
            val array = JSONArray()
            sessions.forEach { s ->
                array.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("status", s.status.name)
                        put("venueName", s.venueName)
                        put("startedAtMs", s.startedAtMs)
                        s.endedAtMs?.let { put("endedAtMs", it) }
                        s.localUri?.let { put("localUri", it) }
                        s.remoteUrl?.let { put("remoteUrl", it) }
                        put("peakViewers", s.peakViewers)
                        s.failureReason?.let { put("failureReason", it) }
                    }
                )
            }
            prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to persist live sessions", e)
        }
    }

    private fun load(): List<Session> = try {
        val raw = prefs.getString(KEY_SESSIONS, null)
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                Session(
                    id = o.optString("id"),
                    status = runCatching { Status.valueOf(o.optString("status")) }
                        .getOrDefault(Status.FAILED),
                    venueName = o.optString("venueName"),
                    startedAtMs = o.optLong("startedAtMs"),
                    endedAtMs = if (o.has("endedAtMs")) o.optLong("endedAtMs") else null,
                    localUri = o.optString("localUri").takeIf { it.isNotBlank() },
                    remoteUrl = o.optString("remoteUrl").takeIf { it.isNotBlank() },
                    peakViewers = o.optInt("peakViewers"),
                    failureReason = o.optString("failureReason").takeIf { it.isNotBlank() }
                )
            }.sortedByDescending { it.startedAtMs }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Unable to load live sessions", e)
        emptyList()
    }

    companion object {
        private const val TAG = "LiveSessionStore"
        private const val PREFS = "fomo_live_sessions"
        private const val KEY_SESSIONS = "sessions"

        @Volatile
        private var instance: LiveSessionStore? = null

        fun getInstance(context: Context): LiveSessionStore =
            instance ?: synchronized(this) {
                instance ?: LiveSessionStore(context).also { instance = it }
            }
    }
}
