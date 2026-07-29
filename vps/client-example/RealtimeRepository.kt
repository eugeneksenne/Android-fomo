package com.example.core.data.realtime

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Repository to wrap FomoSocketManager for easy use in ViewModels
 * Inject via Hilt / manual DI
 */
class RealtimeRepository(
    private val socketManager: FomoSocketManager = FomoSocketManager.getInstance()
) {

    companion object {
        private const val TAG = "RealtimeRepository"
    }

    suspend fun connect(): Result<Unit> {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
                ?: return Result.failure(Exception("No Firebase user"))
            val tokenResult = user.getIdToken(true).await()
            val idToken = tokenResult.token
                ?: return Result.failure(Exception("No ID token"))

            socketManager.connect(idToken, user.displayName) {
                Log.i(TAG, "Socket connected via repo")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed", e)
            Result.failure(e)
        }
    }

    fun disconnect() = socketManager.disconnect()

    fun observeConnection() = socketManager.connectionState

    // Delegates
    fun joinChat(chatId: String) = socketManager.joinChat(chatId)
    fun sendMessage(chatId: String, text: String) = socketManager.sendChatMessage(chatId, text)
    fun initiateCall(targetUserId: String, isVideo: Boolean = true) =
        socketManager.initiateCall(targetUserId, if (isVideo) "VIDEO" else "VOICE")
    fun acceptCall(roomId: String) = socketManager.acceptCall(roomId)
    fun endCall(roomId: String) = socketManager.endCall(roomId)

    fun createLive(venueId: String?, venueName: String?, title: String, cb: (String?) -> Unit) {
        socketManager.createLive(venueId, venueName, title) { json ->
            val roomId = json?.optString("roomId")
            cb(roomId)
        }
    }

    fun joinLive(roomId: String? = null, venueId: String? = null) =
        socketManager.joinLive(roomId, venueId)

    fun joinMap(venueId: String? = null, city: String? = null) =
        socketManager.joinMap(venueId, city)

    fun updateLocation(lat: Double, lng: Double, venueId: String? = null) =
        socketManager.updateMapLocation(lat, lng, venueId)

    fun joinClubLobby(venueId: String) = socketManager.joinLobby(venueId)
}
