package com.example.core.data.realtime

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URISyntaxException

/**
 * FOMO Socket.IO Client Manager for Android app
 * Handles connection, auth with Firebase ID token, and event bindings for:
 * - presence, chat, calls (WebRTC signaling), live streaming, safety, map, club lobby
 *
 * Add dependency: implementation("io.socket:socket.io-client:2.1.0")
 *
 * Usage in ViewModel / DI:
 *  val manager = FomoSocketManager.getInstance()
 *  manager.connect(firebaseIdToken) { Log.d("FOMO", "Connected") }
 */
class FomoSocketManager private constructor() {

    private var socket: Socket? = null
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _onlineUsers = MutableStateFlow<List<OnlineUser>>(emptyList())
    val onlineUsers: StateFlow<List<OnlineUser>> = _onlineUsers

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data class Connected(val socketId: String) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    data class OnlineUser(
        val userId: String,
        val displayName: String?,
        val status: String,
        val photoUrl: String?
    )

    companion object {
        private const val TAG = "FOMO_SOCKET"
        // TODO: Replace with your VPS signaling URL
        const val SIGNALING_URL = "https://signaling.yourdomain.com" // or http://10.0.2.2:3000 for emulator
        // For local dev via emulator: "http://10.0.2.2:3000"
        // For physical device on same network: "http://192.168.x.x:3000"

        @Volatile
        private var INSTANCE: FomoSocketManager? = null

        fun getInstance(): FomoSocketManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FomoSocketManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Connect with Firebase ID Token
     * @param idToken Firebase Auth ID token from FirebaseAuth.getInstance().currentUser.getIdToken()
     */
    fun connect(idToken: String, displayName: String? = null, onConnected: (() -> Unit)? = null) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Already connected")
            onConnected?.invoke()
            return
        }

        _connectionState.value = ConnectionState.Connecting

        try {
            val options = IO.Options().apply {
                // Send token in auth payload - server will validate via Firebase Admin
                auth = mapOf(
                    "token" to idToken,
                    "displayName" to (displayName ?: ""),
                    "appVersion" to "1.0.0"
                )
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
            }

            socket = IO.socket(SIGNALING_URL, options)

            setupCoreListeners(onConnected)
            setupChatListeners()
            setupCallListeners()
            setupLiveListeners()
            setupSafetyListeners()
            setupMapListeners()
            setupLobbyListeners()

            socket?.connect()
            Log.i(TAG, "Connecting to $SIGNALING_URL")

        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid signaling URL", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Invalid URL")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = ConnectionState.Disconnected
        Log.i(TAG, "Disconnected")
    }

    fun isConnected(): Boolean = socket?.connected() == true

    fun getSocket(): Socket? = socket

    //region Core listeners

    private fun setupCoreListeners(onConnected: (() -> Unit)?) {
        val s = socket ?: return

        s.on(Socket.EVENT_CONNECT) { args ->
            Log.i(TAG, "✅ Connected id=${s.id()}")
            _connectionState.value = ConnectionState.Connected(s.id())
            // Heartbeat
            heartbeat()
            onConnected?.invoke()
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val err = args.getOrNull(0)?.toString() ?: "Unknown"
            Log.e(TAG, "❌ Connect error: $err")
            _connectionState.value = ConnectionState.Error(err)
        }

        s.on(Socket.EVENT_DISCONNECT) { args ->
            Log.w(TAG, "Disconnected: ${args.getOrNull(0)}")
            _connectionState.value = ConnectionState.Disconnected
        }

        s.on("connected") { args ->
            val data = args.getOrNull(0) as? JSONObject
            Log.d(TAG, "Server ack connected: $data")
        }

        s.on("user:online") { args ->
            Log.d(TAG, "User online: ${args.getOrNull(0)}")
        }

        s.on("user:offline") { args ->
            Log.d(TAG, "User offline: ${args.getOrNull(0)}")
        }

        s.on("user:presence") { args ->
            Log.d(TAG, "Presence: ${args.getOrNull(0)}")
        }

        s.on("error") { args ->
            Log.e(TAG, "Server error: ${args.getOrNull(0)}")
        }
    }

    //region Chat
    private fun setupChatListeners() {
        val s = socket ?: return
        s.on("chat:new_message") { args ->
            Log.d(TAG, "Chat new message: ${args.getOrNull(0)}")
            // TODO: Parse and insert into your ChatRepository / Room DB / Firestore cache
        }
        s.on("chat:typing") { args ->
            Log.d(TAG, "Chat typing: ${args.getOrNull(0)}")
        }
        s.on("chat:presence") { args ->
            Log.d(TAG, "Chat presence: ${args.getOrNull(0)}")
        }
    }

    //region Call (WebRTC)
    private fun setupCallListeners() {
        val s = socket ?: return

        s.on("call:incoming") { args ->
            val data = args.getOrNull(0) as? JSONObject
            Log.i(TAG, "📞 Incoming call: $data")
            // TODO: Show CallOverlay with callerId, roomId
            // data?.getString("callerId") etc
        }

        s.on("call:accepted") { args ->
            Log.d(TAG, "Call accepted: ${args.getOrNull(0)}")
        }

        s.on("call:rejected") { args ->
            Log.d(TAG, "Call rejected: ${args.getOrNull(0)}")
        }

        s.on("call:ended") { args ->
            Log.d(TAG, "Call ended: ${args.getOrNull(0)}")
        }

        s.on("webrtc:offer") { args ->
            val offer = args.getOrNull(0) as? JSONObject
            Log.d(TAG, "WebRTC offer: $offer")
            // TODO: Feed into peerConnection.setRemoteDescription and createAnswer
        }

        s.on("webrtc:answer") { args ->
            Log.d(TAG, "WebRTC answer: ${args.getOrNull(0)}")
        }

        s.on("webrtc:ice-candidate") { args ->
            Log.d(TAG, "ICE candidate: ${args.getOrNull(0)}")
            // TODO: peerConnection.addIceCandidate
        }
    }

    //region Live
    private fun setupLiveListeners() {
        val s = socket ?: return
        s.on("live:new") { args -> Log.d(TAG, "New live: ${args.getOrNull(0)}") }
        s.on("live:viewer_count") { args -> Log.d(TAG, "Viewer count: ${args.getOrNull(0)}") }
        s.on("live:comment") { args -> Log.d(TAG, "Live comment: ${args.getOrNull(0)}") }
        s.on("live:reaction") { args -> Log.d(TAG, "Live reaction: ${args.getOrNull(0)}") }
        s.on("live:started") { args -> Log.d(TAG, "Live started: ${args.getOrNull(0)}") }
        s.on("live:ended_global") { args -> Log.d(TAG, "Live ended: ${args.getOrNull(0)}") }
        s.on("live:camera_switched") { args -> Log.d(TAG, "Camera switched: ${args.getOrNull(0)}") }
    }

    //region Safety
    private fun setupSafetyListeners() {
        val s = socket ?: return
        s.on("safety:invited") { args -> Log.i(TAG, "Safety invited: ${args.getOrNull(0)}") }
        s.on("safety:location_update") { args -> Log.d(TAG, "Safety location: ${args.getOrNull(0)}") }
        s.on("safety:sos_alert") { args -> Log.e(TAG, "🚨 SOS ALERT: ${args.getOrNull(0)}") }
        s.on("safety:status_update") { args -> Log.d(TAG, "Safety status: ${args.getOrNull(0)}") }
    }

    //region Map
    private fun setupMapListeners() {
        val s = socket ?: return
        s.on("map:friend_location") { args -> Log.d(TAG, "Friend map location: ${args.getOrNull(0)}") }
        s.on("map:presence") { args -> Log.d(TAG, "Map presence: ${args.getOrNull(0)}") }
        s.on("map:venue_activity") { args -> Log.d(TAG, "Venue activity: ${args.getOrNull(0)}") }
    }

    //region Lobby
    private fun setupLobbyListeners() {
        val s = socket ?: return
        s.on("lobby:new_message") { args -> Log.d(TAG, "Lobby message: ${args.getOrNull(0)}") }
        s.on("lobby:user_joined") { args -> Log.d(TAG, "Lobby joined: ${args.getOrNull(0)}") }
        s.on("lobby:typing") { args -> Log.d(TAG, "Lobby typing: ${args.getOrNull(0)}") }
    }

    //endregion

    //region Emitter helpers (public API for UI ViewModels)

    fun emitHeartbeat(location: Map<String, Double>? = null, status: String = "online") {
        val payload = JSONObject().apply {
            put("status", status)
            put("timestamp", System.currentTimeMillis())
            if (location != null) {
                put("location", JSONObject(location))
            }
        }
        socket?.emit("heartbeat", payload)
    }

    private fun heartbeat() {
        // Schedule periodic heartbeat every 25s
        // In production use coroutines Timer
    }

    // Chat
    fun joinChat(chatId: String, callback: ((JSONObject?) -> Unit)? = null) {
        socket?.emit("chat:join", JSONObject(mapOf("chatId" to chatId))) { args ->
            callback?.invoke(args.getOrNull(0) as? JSONObject)
        }
    }

    fun sendChatMessage(chatId: String, text: String, type: String = "TEXT") {
        val payload = JSONObject().apply {
            put("chatId", chatId)
            put("message", JSONObject().apply {
                put("text", text)
                put("type", type)
            })
            put("tempId", "temp_${System.currentTimeMillis()}")
        }
        socket?.emit("chat:message", payload)
    }

    fun sendTyping(chatId: String, isTyping: Boolean) {
        socket?.emit("chat:typing", JSONObject().apply {
            put("chatId", chatId)
            put("isTyping", isTyping)
        })
    }

    // Calls
    fun initiateCall(targetUserId: String, callType: String = "VIDEO", chatId: String? = null) {
        socket?.emit("call:initiate", JSONObject().apply {
            put("targetUserId", targetUserId)
            put("callType", callType)
            if (chatId != null) put("chatId", chatId)
        })
    }

    fun acceptCall(roomId: String) {
        socket?.emit("call:accept", JSONObject().apply { put("roomId", roomId) })
    }

    fun rejectCall(roomId: String, reason: String = "REJECTED") {
        socket?.emit("call:reject", JSONObject().apply {
            put("roomId", roomId)
            put("reason", reason)
        })
    }

    fun endCall(roomId: String) {
        socket?.emit("call:end", JSONObject().apply { put("roomId", roomId) })
    }

    fun sendWebRTCOffer(roomId: String, targetUserId: String?, sdp: JSONObject) {
        socket?.emit("webrtc:offer", JSONObject().apply {
            put("roomId", roomId)
            if (targetUserId != null) put("targetUserId", targetUserId)
            put("sdp", sdp)
        })
    }

    fun sendWebRTCAnswer(roomId: String, targetUserId: String?, sdp: JSONObject) {
        socket?.emit("webrtc:answer", JSONObject().apply {
            put("roomId", roomId)
            if (targetUserId != null) put("targetUserId", targetUserId)
            put("sdp", sdp)
        })
    }

    fun sendICECandidate(roomId: String, targetUserId: String?, candidate: JSONObject) {
        socket?.emit("webrtc:ice-candidate", JSONObject().apply {
            put("roomId", roomId)
            if (targetUserId != null) put("targetUserId", targetUserId)
            put("candidate", candidate)
        })
    }

    // Live
    fun createLive(venueId: String?, venueName: String?, title: String, callback: ((JSONObject?) -> Unit)? = null) {
        socket?.emit("live:create", JSONObject().apply {
            put("venueId", venueId)
            put("venueName", venueName)
            put("title", title)
        }) { args ->
            callback?.invoke(args.getOrNull(0) as? JSONObject)
        }
    }

    fun joinLive(roomId: String? = null, venueId: String? = null, callback: ((JSONObject?) -> Unit)? = null) {
        socket?.emit("live:join", JSONObject().apply {
            if (roomId != null) put("roomId", roomId)
            if (venueId != null) put("venueId", venueId)
        }) { args ->
            callback?.invoke(args.getOrNull(0) as? JSONObject)
        }
    }

    fun sendLiveComment(roomId: String, text: String) {
        socket?.emit("live:comment", JSONObject().apply {
            put("roomId", roomId)
            put("text", text)
        })
    }

    fun sendLiveReaction(roomId: String, emoji: String = "🔥") {
        socket?.emit("live:reaction", JSONObject().apply {
            put("roomId", roomId)
            put("emoji", emoji)
        })
    }

    // Safety
    fun createSafetySession(type: String, trustedIds: List<String>, destination: String? = null) {
        socket?.emit("safety:create_session", JSONObject().apply {
            put("type", type)
            put("trustedContactIds", trustedIds)
            put("durationMinutes", 45)
            if (destination != null) put("destination", destination)
        })
    }

    fun updateSafetyLocation(roomId: String, lat: Double, lng: Double) {
        socket?.emit("safety:location_update", JSONObject().apply {
            put("roomId", roomId)
            put("location", JSONObject().apply {
                put("latitude", lat)
                put("longitude", lng)
            })
        })
    }

    // Map
    fun joinMap(venueId: String? = null, city: String? = null) {
        socket?.emit("map:join", JSONObject().apply {
            if (venueId != null) put("venueId", venueId)
            if (city != null) put("city", city)
        })
    }

    fun updateMapLocation(lat: Double, lng: Double, venueId: String? = null, isLive: Boolean = false) {
        socket?.emit("map:location_update", JSONObject().apply {
            put("latitude", lat)
            put("longitude", lng)
            if (venueId != null) put("venueId", venueId)
            put("isLive", isLive)
        })
    }

    // Lobby
    fun joinLobby(venueId: String, channel: String = "GENERAL") {
        socket?.emit("lobby:join", JSONObject().apply {
            put("venueId", venueId)
            put("channel", channel)
        })
    }

    fun sendLobbyMessage(venueId: String, text: String) {
        socket?.emit("lobby:message", JSONObject().apply {
            put("venueId", venueId)
            put("text", text)
        })
    }
}
