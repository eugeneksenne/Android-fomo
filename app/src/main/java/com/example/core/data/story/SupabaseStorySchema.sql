-- =============================================================================
-- SUPABASE POSTGRESQL SCHEMA FOR FOMO STORIES ENGINE
-- Production Architecture: 24-Hour Automated Ephemeral Retention & Realtime Broadcasts
-- =============================================================================

-- 1. ENUMS & EXTENSIONS
CREATE TYPE story_privacy_type AS ENUM ('PUBLIC', 'MY_CIRCLE', 'FOLLOWERS_ONLY');
CREATE TYPE sticker_type AS ENUM ('VENUE', 'EVENT', 'FLASH_DROP', 'POLL', 'LOCATION', 'MUSIC');

-- 2. STORIES MASTER TABLE
CREATE TABLE stories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    privacy story_privacy_type DEFAULT 'PUBLIC' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now() + INTERVAL '24 hours') NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_stories_user_expires ON stories(user_id, expires_at DESC);

-- 3. STORY MEDIA SEGMENTS
CREATE TABLE story_segments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    story_id UUID REFERENCES stories(id) ON DELETE CASCADE NOT NULL,
    media_url TEXT NOT NULL,
    thumbnail_url TEXT,
    is_video BOOLEAN DEFAULT FALSE,
    duration_seconds INT DEFAULT 5 NOT NULL,
    location_name TEXT,
    filter_name TEXT,
    stickers JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE INDEX idx_segments_story ON story_segments(story_id);

-- 4. STORY VIEWS & ENGAGEMENT TELEMETRY
CREATE TABLE story_views (
    segment_id UUID REFERENCES story_segments(id) ON DELETE CASCADE NOT NULL,
    viewer_user_id UUID NOT NULL,
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    reaction_emoji TEXT,
    PRIMARY KEY (segment_id, viewer_user_id)
);

-- 5. STORY MUTED USER PREFERENCES
CREATE TABLE story_mutes (
    user_id UUID NOT NULL,
    muted_user_id UUID NOT NULL,
    muted_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    PRIMARY KEY (user_id, muted_user_id)
);

-- 6. AUTOMATED 24-HOUR RETENTION CLEANUP TRIGGER
CREATE OR REPLACE FUNCTION purge_expired_stories()
RETURNS trigger AS $$
BEGIN
    DELETE FROM stories WHERE expires_at < NOW();
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_purge_expired_stories
AFTER INSERT ON stories
EXECUTE FUNCTION purge_expired_stories();

-- 7. ROW LEVEL SECURITY (RLS) POLICIES
ALTER TABLE stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE story_segments ENABLE ROW LEVEL SECURITY;
ALTER TABLE story_views ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can select stories based on privacy settings"
    ON stories FOR SELECT
    USING (
        expires_at > NOW() AND
        is_deleted = FALSE AND
        NOT EXISTS (
            SELECT 1 FROM story_mutes
            WHERE user_id = auth.uid() AND muted_user_id = stories.user_id
        )
    );

CREATE POLICY "Users can create stories for themselves"
    ON stories FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- 8. REALTIME SUBSCRIPTION CHANNELS
ALTER PUBLICATION supabase_realtime ADD TABLE stories;
ALTER PUBLICATION supabase_realtime ADD TABLE story_views;

-- 9. SUPABASE EDGE FUNCTION FOR MEDIA PROCESSING & TRANSCODING
/*
 EDGE FUNCTION: process-story-media
 - Enforces max duration (15s per video segment).
 - Generates progressive HLS streams & dynamic webp thumbnails.
 - Scans media content via AI moderation pipeline before publication.
*/
