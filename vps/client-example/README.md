# FOMO Android Client - Socket.IO Example

## Setup

1. Add dependency in `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.socket:socket.io-client:2.1.0")
    implementation("org.json:json:20240303")
}
```

2. Copy `FomoSocketManager.kt` and `RealtimeRepository.kt` to `app/src/main/java/com/example/core/data/realtime/`

3. Update `SIGNALING_URL` in `FomoSocketManager.kt`:
   - Emulator: `http://10.0.2.2:3000`
   - Physical device same WiFi: `http://192.168.1.X:3000`
   - Production: `https://signaling.yourdomain.com`

## Usage in ViewModel

```kotlin
@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val realtimeRepo: RealtimeRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            realtimeRepo.connect()
        }
    }

    fun onSendMessage(chatId: String, text: String) {
        realtimeRepo.sendMessage(chatId, text)
        // Also persist to Firestore/Supabase as you already do
    }

    fun onStartCall(targetUserId: String) {
        realtimeRepo.initiateCall(targetUserId, isVideo = true)
    }

    override fun onCleared() {
        realtimeRepo.disconnect()
    }
}
```

## WebRTC Integration

The server relays SDP offer/answer and ICE. In your `CallOverlay` / `FomoCallSession`:

```kotlin
// When you create offer:
peerConnection.createOffer { sdp ->
    peerConnection.setLocalDescription(sdp) {
        socketManager.sendWebRTCOffer(roomId, targetUserId, JSONObject().apply {
            put("type", sdp.type.canonicalForm())
            put("sdp", sdp.description)
        })
    }
}

// Listen for offer in FomoSocketManager:
socket.on("webrtc:offer") { ... peerConnection.setRemoteDescription(...) + createAnswer ... }
```

## Firebase Token Refresh

Firebase ID tokens expire after ~1h. Listen for token refresh:

```kotlin
FirebaseAuth.getInstance().addIdTokenListener { auth ->
    auth.currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
        // Reconnect with new token
        socketManager.connect(result.token!!)
    }
}
```

## Testing with Node client

```bash
cd vps
npm install socket.io-client jsonwebtoken
SIGNALING_URL=http://localhost:3000 node client-example/test-client.js
```
