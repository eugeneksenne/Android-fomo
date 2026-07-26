# Production Firestore Database Schema Specification

This document provides a comprehensive, enterprise-ready architecture and schema definition for Google Cloud Firestore powering the app. It details data structures, collection/subcollection hierarchies, indexing strategies, access control policies, and deployment rules.

---

## Architectural Overview & Data Modeling Strategy

- **Database Engine**: Cloud Firestore in Native Mode.
- **Data Modeling Paradigm**: Hybrid Normalized/Denormalized strategy. High-frequency queries (e.g. Chat list, Feed item preview) store denormalized author metadata (`authorName`, `authorAvatarUrl`) to eliminate costly multi-document JOIN lookups while maintaining real-time listeners.
- **Offline Persistence Strategy**: Managed via Android Cloud Firestore SDK local cache (`PersistentCacheSettings` with `MemoryCacheSettings` or LRU disk storage).

---

## Collection & Subcollection Specifications

```
firestore-root
 ├── users/ {userId}
 │    └── emergency_contacts/ {contactId}
 ├── posts/ {postId}
 │    ├── comments/ {commentId}
 │    └── likes/ {userId}
 ├── chats/ {chatId}
 │    └── messages/ {messageId}
 ├── groups/ {groupId}
 │    ├── members/ {memberId}
 │    └── events/ {eventId}
 └── safety_checks/ {checkId}
```

---

### 1. `users` Collection
Stores user account details, preferences, privacy configurations, and activity metadata.

- **Document ID**: `{userId}` (Firebase Auth `uid`)

```json
{
  "uid": "usr_98a7bc20",
  "displayName": "Alex Morgan",
  "email": "alex.morgan@example.com",
  "phoneNumber": "+15550192834",
  "photoUrl": "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
  "bio": "Nightlife enthusiast & safety advocate",
  "isVerified": true,
  "status": "online",
  "lastLoginAt": "Timestamp(2026-07-25T23:50:00Z)",
  "createdAt": "Timestamp(2026-05-10T12:00:00Z)",
  "updatedAt": "Timestamp(2026-07-25T23:50:00Z)",
  "fcmTokens": ["fcm_token_device_android_123"],
  "preferences": {
    "pushNotifications": true,
    "locationSharing": true,
    "safetyAlerts": true,
    "darkMode": true
  }
}
```

#### Subcollection: `users/{userId}/emergency_contacts`
- **Document ID**: Auto-generated `{contactId}`

```json
{
  "id": "cnt_10293",
  "name": "Sarah Connor",
  "phoneNumber": "+15559012345",
  "relationship": "Sister",
  "isPrimary": true,
  "createdAt": "Timestamp(2026-06-01T10:00:00Z)"
}
```

---

### 2. `posts` (Feed / Moments) Collection
Stores public/community social posts, media moments, check-ins, and safety updates.

- **Document ID**: Auto-generated `{postId}`

```json
{
  "id": "post_77a901",
  "authorId": "usr_98a7bc20",
  "authorName": "Alex Morgan",
  "authorAvatarUrl": "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
  "content": "Exploring the city lights tonight! Safe walk home active. 🌆✨",
  "mediaUrls": [
    "https://images.unsplash.com/photo-1514525253161-7a46d19cd819"
  ],
  "location": {
    "name": "Downtown Central Park",
    "latitude": -26.2041,
    "longitude": 28.0473
  },
  "tags": ["NightOut", "SafetyCheck", "Downtown"],
  "visibility": "PUBLIC", // OPTIONS: "PUBLIC", "CIRCLE_ONLY", "PRIVATE"
  "likeCount": 24,
  "commentCount": 5,
  "createdAt": "Timestamp(2026-07-25T22:15:00Z)",
  "updatedAt": "Timestamp(2026-07-25T22:15:00Z)"
}
```

#### Subcollection: `posts/{postId}/comments`
- **Document ID**: Auto-generated `{commentId}`

```json
{
  "id": "cmnt_01923",
  "authorId": "usr_44b12a",
  "authorName": "Marcus Vance",
  "authorAvatarUrl": "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d",
  "text": "Stay safe Alex! Text when home.",
  "createdAt": "Timestamp(2026-07-25T22:20:00Z)"
}
```

#### Subcollection: `posts/{postId}/likes`
- **Document ID**: `{userId}`

```json
{
  "likedAt": "Timestamp(2026-07-25T22:18:00Z)"
}
```

---

### 3. `chats` & `messages` Collections
Handles direct standard messages and group thread messaging.

- **Document ID**: `{chatId}` (e.g. `direct_usr1_usr2` or auto-generated for group threads)

```json
{
  "id": "chat_direct_881a",
  "type": "DIRECT", // "DIRECT" or "GROUP"
  "participantIds": ["usr_98a7bc20", "usr_44b12a"],
  "participantDetails": {
    "usr_98a7bc20": {
      "displayName": "Alex Morgan",
      "photoUrl": "https://images.unsplash.com/photo-1534528741775-53994a69daeb"
    },
    "usr_44b12a": {
      "displayName": "Marcus Vance",
      "photoUrl": "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d"
    }
  },
  "lastMessage": {
    "text": "Just reached home safely!",
    "senderId": "usr_98a7bc20",
    "timestamp": "Timestamp(2026-07-25T23:45:00Z)"
  },
  "unreadCounts": {
    "usr_98a7bc20": 0,
    "usr_44b12a": 1
  },
  "createdAt": "Timestamp(2026-07-01T08:00:00Z)",
  "updatedAt": "Timestamp(2026-07-25T23:45:00Z)"
}
```

#### Subcollection: `chats/{chatId}/messages`
- **Document ID**: Auto-generated `{messageId}`

```json
{
  "id": "msg_001928",
  "chatId": "chat_direct_881a",
  "senderId": "usr_98a7bc20",
  "text": "Just reached home safely!",
  "mediaUrl": null,
  "type": "TEXT", // "TEXT", "IMAGE", "LOCATION_SHARE", "SAFETY_ALERT"
  "readBy": ["usr_98a7bc20"],
  "timestamp": "Timestamp(2026-07-25T23:45:00Z)"
}
```

---

### 4. `groups` (My Circle) Collection
Provides safety circles, event groups, and guardian monitor teams.

- **Document ID**: Auto-generated `{groupId}`

```json
{
  "id": "grp_night_guard_01",
  "name": "Weekend NightGuard Circle",
  "description": "Friends monitoring late night walk home safety checks",
  "creatorId": "usr_98a7bc20",
  "category": "SAFETY_CIRCLE",
  "avatarUrl": "https://images.unsplash.com/photo-1517048676732-d65bc937f952",
  "memberCount": 4,
  "createdAt": "Timestamp(2026-06-15T14:00:00Z)",
  "updatedAt": "Timestamp(2026-07-25T23:00:00Z)"
}
```

#### Subcollection: `groups/{groupId}/members`
- **Document ID**: `{userId}`

```json
{
  "userId": "usr_98a7bc20",
  "role": "admin", // "admin" or "member"
  "joinedAt": "Timestamp(2026-06-15T14:00:00Z)"
}
```

#### Subcollection: `groups/{groupId}/events`
- **Document ID**: Auto-generated `{eventId}`

```json
{
  "id": "evt_99182",
  "groupId": "grp_night_guard_01",
  "title": "Concert Night Out",
  "location": "Arena Stage",
  "eventDate": "Timestamp(2026-07-26T20:00:00Z)",
  "attendeeUserIds": ["usr_98a7bc20", "usr_44b12a"],
  "createdAt": "Timestamp(2026-07-20T09:00:00Z)"
}
```

---

### 5. `safety_checks` Collection
Manages scheduled safety timers, custom clock check-in triggers (e.g. 20:00, 00:00), and automated SOS escalation.

- **Document ID**: Auto-generated `{checkId}`

```json
{
  "id": "chk_881923",
  "userId": "usr_98a7bc20",
  "userName": "Alex Morgan",
  "targetTimeLabel": "20:00",
  "durationMinutes": 45,
  "scheduledTime": "Timestamp(2026-07-26T20:00:00Z)",
  "status": "ACTIVE", // "ACTIVE", "COMPLETED", "TRIGGERED_ALERT", "CANCELLED"
  "trustedContactIds": ["usr_44b12a", "usr_77192b"],
  "lastKnownLocation": {
    "latitude": -26.2041,
    "longitude": 28.0473,
    "updatedAt": "Timestamp(2026-07-25T23:50:00Z)"
  },
  "createdAt": "Timestamp(2026-07-25T23:50:00Z)"
}
```

---

## Indexing Strategy

Compound indexes required in `firestore.indexes.json` for high-performance querying:

1. **Feed Posts Listing**: Filter by `visibility` + Sort by `createdAt DESC`
2. **User Profile Feed**: Filter by `authorId` + Sort by `createdAt DESC`
3. **Chat Conversations Listing**: Filter array `participantIds` + Sort by `updatedAt DESC`
4. **Group Event Schedules**: Filter by `groupId` + Sort by `eventDate ASC`
5. **Safety Check Monitoring**: Filter by `userId` + `status` + Sort by `scheduledTime ASC`
6. **Trusted Contacts Alert Monitor**: Filter array `trustedContactIds` + `status` + Sort by `scheduledTime DESC`

---

## Deployment & Verification Commands

Deploy schema rules and indexes using Firebase CLI:

```bash
# Verify rules syntax locally
firebase deploy --only firestore:rules

# Deploy composite indexes
firebase deploy --only firestore:indexes
```
