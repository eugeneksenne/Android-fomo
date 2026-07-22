-- Supabase PostgreSQL Schema for FOMO Creator Studio
-- This powers the Creator Studio, enabling event organizers, venue owners, and artists 
-- to manage events, staff, marketing campaigns, and review feedback.

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==========================================
-- TABLE: creator_profiles
-- Represents verified creator entities (Venues, Organizers, DJs, Artists)
-- ==========================================
CREATE TABLE IF NOT EXISTS public.creator_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL, -- e.g., 'Venue', 'Organiser', 'DJ', 'Artist'
    business_name VARCHAR(255) NOT NULL,
    verification_status VARCHAR(50) DEFAULT 'Pending', -- 'Pending', 'InReview', 'Approved', 'Rejected'
    wallet_balance NUMERIC(10, 2) DEFAULT 0.00,
    two_factor_enabled BOOLEAN DEFAULT true,
    default_payout_method VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id)
);

-- Row Level Security for creator_profiles
ALTER TABLE public.creator_profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Creators can view their own profile" ON public.creator_profiles
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Creators can update their own profile" ON public.creator_profiles
    FOR UPDATE USING (auth.uid() = user_id);

-- ==========================================
-- TABLE: creator_staff
-- Manage permissions and staff members for a creator profile
-- ==========================================
CREATE TABLE IF NOT EXISTS public.creator_staff (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    creator_id UUID NOT NULL REFERENCES public.creator_profiles(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL, -- the staff member's user id
    name VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL, -- e.g., 'Venue Owner', 'Door Staff / Scanner', 'Promoter'
    is_online BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Row Level Security for creator_staff
ALTER TABLE public.creator_staff ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Creators can view their staff" ON public.creator_staff
    FOR SELECT USING (
        creator_id IN (SELECT id FROM public.creator_profiles WHERE user_id = auth.uid())
    );
CREATE POLICY "Creators can manage their staff" ON public.creator_staff
    FOR ALL USING (
        creator_id IN (SELECT id FROM public.creator_profiles WHERE user_id = auth.uid())
    );

-- ==========================================
-- TABLE: creator_campaigns
-- Tracks marketing/promo campaigns for events or venues
-- ==========================================
CREATE TABLE IF NOT EXISTS public.creator_campaigns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    creator_id UUID NOT NULL REFERENCES public.creator_profiles(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    budget NUMERIC(10, 2) DEFAULT 0.00,
    spent NUMERIC(10, 2) DEFAULT 0.00,
    reach INTEGER DEFAULT 0,
    routes INTEGER DEFAULT 0,
    conversions INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'Draft', -- 'Draft', 'Running', 'Paused', 'Completed'
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Row Level Security for creator_campaigns
ALTER TABLE public.creator_campaigns ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Creators can view their own campaigns" ON public.creator_campaigns
    FOR SELECT USING (
        creator_id IN (SELECT id FROM public.creator_profiles WHERE user_id = auth.uid())
    );
CREATE POLICY "Creators can manage their own campaigns" ON public.creator_campaigns
    FOR ALL USING (
        creator_id IN (SELECT id FROM public.creator_profiles WHERE user_id = auth.uid())
    );

-- ==========================================
-- TABLE: creator_reviews
-- Stores reviews directed at a venue or event organizer, including replies
-- ==========================================
CREATE TABLE IF NOT EXISTS public.creator_reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    creator_id UUID NOT NULL REFERENCES public.creator_profiles(id) ON DELETE CASCADE,
    author_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    author_name VARCHAR(255) NOT NULL,
    rating NUMERIC(2, 1) CHECK (rating >= 1.0 AND rating <= 5.0),
    content TEXT,
    reply TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Row Level Security for creator_reviews
ALTER TABLE public.creator_reviews ENABLE ROW LEVEL SECURITY;
-- Anyone can view reviews
CREATE POLICY "Public can view reviews" ON public.creator_reviews
    FOR SELECT USING (true);
-- Creators can update to add replies
CREATE POLICY "Creators can reply to reviews" ON public.creator_reviews
    FOR UPDATE USING (
        creator_id IN (SELECT id FROM public.creator_profiles WHERE user_id = auth.uid())
    );

-- ==========================================
-- REALTIME SUBSCRIPTIONS
-- Enable real-time capabilities for dashboard updates
-- ==========================================
BEGIN;
  -- Enable realtime on creator tables
  ALTER PUBLICATION supabase_realtime ADD TABLE public.creator_orders; -- Assuming an external orders table exists or is mapped
  ALTER PUBLICATION supabase_realtime ADD TABLE public.creator_staff;
  ALTER PUBLICATION supabase_realtime ADD TABLE public.creator_reviews;
COMMIT;
