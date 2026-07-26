-- =============================================================================
-- SUPABASE POSTGRESQL SCHEMA FOR UNIFIED FOMO COMMUNICATION ENGINE
-- Production Architecture: High Scalability, Stateless Edge Services, E2E Security
-- =============================================================================

-- 1. ENUMS & EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE conversation_type AS ENUM (
    'PERSONAL', 'GROUP', 'VENUE', 'BUSINESS', 'EVENT', 'NIGHTGUARD', 'BUDDY_PAIR'
);

CREATE TYPE group_role AS ENUM (
    'OWNER', 'ADMIN', 'MODERATOR', 'MEMBER', 'READ_ONLY'
);

CREATE TYPE message_type AS ENUM (
    'TEXT', 'EMOJI', 'GIF', 'STICKER', 'IMAGE', 'VIDEO', 'AUDIO', 'VOICE_NOTE',
    'DOCUMENT', 'CONTACT', 'LOCATION', 'LIVE_LOCATION',
    'VENUE_CARD', 'EVENT_CARD', 'MOMENT_CARD', 'ROUTE_CARD', 'FLASH_DROP_CARD', 'TICKET_QR_CARD', 'POLL_CARD'
);

CREATE TYPE presence_status AS ENUM (
    'ONLINE', 'OFFLINE', 'TYPING', 'RECORDING', 'IN_CALL', 'IN_VIDEO_CALL', 'LIVE_ON_FOMO', 'SHARING_LIVE_LOCATION'
);

-- 2. CONVERSATIONS TABLE
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type conversation_type NOT NULL DEFAULT 'PERSONAL',
    name TEXT,
    avatar_url TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb
);

-- Index for speedy listing queries
CREATE INDEX idx_conversations_type ON conversations(type);
CREATE INDEX idx_conversations_updated ON conversations(updated_at DESC);

-- 3. CONVERSATION MEMBERS & PERMISSIONS
CREATE TABLE conversation_members (
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role group_role NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    is_muted BOOLEAN DEFAULT FALSE,
    is_pinned BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    last_read_message_id UUID,
    permissions JSONB DEFAULT '{
        "canSendMessages": true,
        "canSendMedia": true,
        "canAddMembers": true,
        "canChangeInfo": false,
        "canPinMessages": true,
        "canCreatePolls": true,
        "isAnnouncementOnly": false
    }'::jsonb,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_members_user ON conversation_members(user_id);

-- 4. MESSAGES TABLE WITH E2E ENCRYPTION PAYLOAD SUPPORT
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE NOT NULL,
    sender_id UUID NOT NULL,
    type message_type NOT NULL DEFAULT 'TEXT',
    encrypted_content TEXT NOT NULL, -- Encrypted AES-GCM or E2E Signal payload
    raw_content_preview TEXT,       -- Fallback preview for offline push titles
    reply_to_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    is_edited BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    metadata JSONB DEFAULT '{}'::jsonb,
    poll_id UUID
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at DESC);

-- 5. GROUP POLLS
CREATE TABLE group_polls (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE NOT NULL,
    creator_id UUID NOT NULL,
    question TEXT NOT NULL,
    is_closed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE poll_options (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    poll_id UUID REFERENCES group_polls(id) ON DELETE CASCADE NOT NULL,
    option_text TEXT NOT NULL
);

CREATE TABLE poll_votes (
    poll_id UUID REFERENCES group_polls(id) ON DELETE CASCADE NOT NULL,
    option_id UUID REFERENCES poll_options(id) ON DELETE CASCADE NOT NULL,
    user_id UUID NOT NULL,
    voted_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    PRIMARY KEY (poll_id, user_id)
);

-- 6. REALTIME PRESENCE & DISCOVERY STATE
CREATE TABLE user_presence (
    user_id UUID PRIMARY KEY,
    status presence_status DEFAULT 'OFFLINE',
    last_active_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    current_location GEOGRAPHY(POINT, 4326),
    active_call_session_id UUID
);

-- 7. E2E DEVICE KEYS & SESSION KEYS MANAGEMENT
CREATE TABLE e2e_device_keys (
    user_id UUID NOT NULL,
    device_id TEXT NOT NULL,
    identity_key TEXT NOT NULL,
    signed_prekey TEXT NOT NULL,
    one_time_keys JSONB NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    PRIMARY KEY (user_id, device_id)
);

-- 8. ROW LEVEL SECURITY (RLS) POLICIES
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversation_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view conversations they are member of"
    ON conversations FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM conversation_members
            WHERE conversation_id = conversations.id
            AND user_id = auth.uid()
        )
    );

CREATE POLICY "Users can select messages in their conversations"
    ON messages FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM conversation_members
            WHERE conversation_id = messages.conversation_id
            AND user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert messages in permitted conversations"
    ON messages FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM conversation_members
            WHERE conversation_id = messages.conversation_id
            AND user_id = auth.uid()
            AND (permissions->>'canSendMessages')::boolean = true
        )
    );

-- 9. SUPABASE REALTIME PUBLICATION CHANNELS
ALTER PUBLICATION supabase_realtime ADD TABLE messages;
ALTER PUBLICATION supabase_realtime ADD TABLE user_presence;
ALTER PUBLICATION supabase_realtime ADD TABLE conversation_members;

-- 10. SUPABASE EDGE FUNCTIONS SPECIFICATIONS
/*
 EDGE FUNCTION 1: process-media-upload
 - Endpoint: /functions/v1/process-media-upload
 - Accepts binary multipart files (voice notes, videos, photos).
 - Performs automated thumbnailing, WebP/H.264 compression, and malware hashing.
 - Returns storage URL and metadata payload for insertion into messages table.

 EDGE FUNCTION 2: webrtc-signaling
 - Endpoint: /functions/v1/webrtc-signaling
 - Validates ephemeral TURN/STUN credentials (Xirsys / Twilio TURN API).
 - Relays WebRTC SDP Offers, Answers, and ICE Candidates across WebSockets.

 EDGE FUNCTION 3: send-push-notification
 - Endpoint: /functions/v1/send-push-notification
 - Triggered on message insert.
 - Batches notification triggers, parses priority channels (NightGuard/Calls = Priority High),
   dispatches APNs and Firebase Cloud Messaging (FCM) alerts.
*/
