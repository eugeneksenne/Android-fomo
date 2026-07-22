-- Supabase PostgreSQL Schema for FOMO Feed Engine
-- This powers the Billion-Dollar Scale Ripple Engine, Moment Distribution, and Venue Invitations.

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==========================================
-- TABLE: moments
-- Represents the core content unit in the feed (Photo, Video, Live, Replay)
-- ==========================================
CREATE TABLE IF NOT EXISTS public.moments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    moment_type VARCHAR(20) NOT NULL CHECK (moment_type IN ('PHOTO', 'VIDEO', 'LIVE', 'REPLAY', 'SPONSORED')),
    media_url TEXT NOT NULL,
    caption TEXT,
    location_name VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    is_live BOOLEAN DEFAULT false,
    live_viewers INTEGER DEFAULT 0,
    ripples_count INTEGER DEFAULT 0,
    likes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,
    current_velocity NUMERIC(8, 2) DEFAULT 0.00, -- Represents momentum/ripples per minute
    momentum_state VARCHAR(20) DEFAULT 'Quiet' CHECK (momentum_state IN ('Quiet', 'Active', 'Heating', 'Hot', 'Viral')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for geographical queries and recency sorting
CREATE INDEX idx_moments_location ON public.moments USING gist (
  ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
);
CREATE INDEX idx_moments_created_at ON public.moments(created_at DESC);

-- Row Level Security for moments
ALTER TABLE public.moments ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public can view moments" ON public.moments
    FOR SELECT USING (true);
CREATE POLICY "Users can create their own moments" ON public.moments
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own moments" ON public.moments
    FOR UPDATE USING (auth.uid() = user_id);

-- ==========================================
-- TABLE: moment_invitations
-- Represents the temporary "Join Me" context card attached to a moment
-- ==========================================
CREATE TABLE IF NOT EXISTS public.moment_invitations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    moment_id UUID NOT NULL REFERENCES public.moments(id) ON DELETE CASCADE,
    venue_name VARCHAR(255) NOT NULL,
    is_venue_verified BOOLEAN DEFAULT false,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ENDED', 'CLOSED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Row Level Security for moment_invitations
ALTER TABLE public.moment_invitations ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public can view invitations" ON public.moment_invitations
    FOR SELECT USING (true);
CREATE POLICY "Users can manage invitations for their moments" ON public.moment_invitations
    FOR ALL USING (
        moment_id IN (SELECT id FROM public.moments WHERE user_id = auth.uid())
    );

-- ==========================================
-- TABLE: user_engagements
-- Tracks likes, ripples, and saves per moment per user
-- ==========================================
CREATE TABLE IF NOT EXISTS public.user_engagements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    moment_id UUID NOT NULL REFERENCES public.moments(id) ON DELETE CASCADE,
    interaction_type VARCHAR(20) NOT NULL CHECK (interaction_type IN ('LIKE', 'RIPPLE', 'SAVE')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, moment_id, interaction_type)
);

-- Row Level Security for user_engagements
ALTER TABLE public.user_engagements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view engagements" ON public.user_engagements
    FOR SELECT USING (true);
CREATE POLICY "Users can manage their own engagements" ON public.user_engagements
    FOR ALL USING (auth.uid() = user_id);

-- ==========================================
-- TABLE: moment_comments
-- Stores replies and comments on moments
-- ==========================================
CREATE TABLE IF NOT EXISTS public.moment_comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    moment_id UUID NOT NULL REFERENCES public.moments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Row Level Security for moment_comments
ALTER TABLE public.moment_comments ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public can view comments" ON public.moment_comments
    FOR SELECT USING (true);
CREATE POLICY "Users can post comments" ON public.moment_comments
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- ==========================================
-- REALTIME SUBSCRIPTIONS
-- Enable live velocity metrics and viewer count updates
-- ==========================================
BEGIN;
  ALTER PUBLICATION supabase_realtime ADD TABLE public.moments;
  ALTER PUBLICATION supabase_realtime ADD TABLE public.moment_invitations;
  ALTER PUBLICATION supabase_realtime ADD TABLE public.user_engagements;
COMMIT;
