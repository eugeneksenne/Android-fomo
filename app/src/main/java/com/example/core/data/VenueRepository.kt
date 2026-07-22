package com.example.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LiveUpdate(
    val id: String,
    val author: String,
    val authorRole: String, // "Venue", "DJ", "Artist", "Attendee"
    val authorImage: String,
    val content: String,
    val timeAgo: String,
    val badgeColor: String = "#FF2D55"
)

data class TonightEvent(
    val id: String,
    val title: String,
    val time: String,
    val headliner: String,
    val interestedCount: Int,
    val ticketStatus: String, // "Available", "Selling Fast", "Sold Out"
    val posterUrl: String
)

data class LineupSlot(
    val id: String,
    val time: String,
    val artistName: String,
    val role: String, // "DJ", "MC", "Special Guest", "Host"
    val imageUrl: String
)

data class FlashDrop(
    val id: String,
    val title: String,
    val subtitle: String,
    val expiresMinutes: Int,
    val initialStock: Int,
    val currentStock: Int,
    val imageRes: String,
    val claimed: Boolean = false,
    val venueId: String = "fomo_club",
    val venueName: String = "FOMO Club",
    val price: String? = null,
    val tableReservations: String? = null,
    val posterUrl: String? = null,
    val category: String = "VENUE", // "VENUE", "EVENT", "CREATOR", "BRAND", "MYSTERY"
    val status: String = "LIVE", // "SCHEDULED", "ARMED", "LIVE", "TRENDING", "ENDING_SOON", "EXPIRED"
    val distanceText: String = "350m away",
    val urgencyBadge: String = "⚡ Just Dropped", // "⚡ Just Dropped", "🔥 Trending", "⏳ Ends Soon", "🚨 Final Minutes", "🌙 Tonight Only", "🎉 Limited Availability"
    val isVerifiedVenue: Boolean = true,
    val eventId: String? = null,
    val creatorName: String? = null,
    val creatorAvatar: String? = null,
    val heroImageUrl: String? = null,
    val hintText: String? = null
)

data class VenueNotice(
    val id: String,
    val type: String, // "Warning", "Info", "Alert"
    val title: String,
    val content: String,
    val timeAgo: String
)

data class VenueHighlight(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String
)

data class MomentData(
    val id: String,
    val title: String,
    val date: String,
    val photoCount: Int,
    val videoCount: Int,
    val rating: Float,
    val imageUrl: String
)

data class FriendPlan(
    val name: String,
    val imgUrl: String,
    val status: String // "Going", "Maybe", "Interested"
)

data class LiveBroadcastInfo(
    val isLive: Boolean,
    val title: String,
    val watcherCount: Int,
    val channelName: String
)

data class VibePoll(
    val litCount: Int,
    val goodCount: Int,
    val quietCount: Int,
    val userVote: String? // "Lit", "Good", "Quiet", null
)

data class VenueProfileState(
    val venueId: String = "fomo_club",
    val name: String = "FOMO Club",
    val rating: Float = 4.7f,
    val reviewCount: String = "2.4k",
    val neighborhood: String = "Rosebank, Johannesburg",
    val distance: String = "1.2 km away",
    val driveTime: String = "4 min drive",
    val walkTime: String = "15 min walk",
    val openStatus: String = "Open Now",
    val closingTime: String = "Closes 04:00",
    val description: String = "The premier multi-sensory nightlife experience in Johannesburg. Featuring high-definition 3D mapping projection, an immersive L-Acoustics sound system, and multiple themed rooms for an unforgettable premium vibe.",
    val isFollowing: Boolean = false,
    val isSaved: Boolean = false,
    val followerCount: Int = 18452,
    val musicTags: List<String> = listOf("Amapiano", "Afro House", "House", "Hip Hop"),
    val atmosphereTags: List<String> = listOf("Luxury", "High Energy", "Rooftop", "VIP Friendly", "Cocktail Lounge"),
    val amenities: List<String> = listOf("Safe Parking", "VIP Booths", "Bottle Service", "Fine Dining", "Rooftop Deck", "Premium Acoustics", "Smoking Lounge", "Wi-Fi", "Coat Check", "Secure Valet"),
    val notices: List<VenueNotice> = listOf(
        VenueNotice("1", "Alert", "VIP Booths Fully Booked", "All premium tables are sold out for tonight. General admission standard queue is moving fast.", "10 min ago"),
        VenueNotice("2", "Info", "Strict Dress Code Tonight", "Upscale fashionable attire required. No sandals, sportswear, or headwear allowed.", "1 hr ago"),
        VenueNotice("3", "Info", "Cashless Venue Only", "We accept all major credit/debit cards, Apple Pay, and Google Wallet.", "2 hr ago")
    ),
    val liveUpdates: List<LiveUpdate> = listOf(
        LiveUpdate("1", "FOMO Club", "Venue", "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=200&auto=format&fit=crop", "Doors are officially open! Early bird ticket holders can enter through the VIP fast-lane.", "2 min ago", "#FF2D55"),
        LiveUpdate("2", "Kabza De Small", "DJ", "https://i.pravatar.cc/150?img=12", "Just arrived at the venue. Soundcheck sounds incredible! Prepare yourselves for an exclusive Amapiano set at 01:00.", "15 min ago", "#007AFF"),
        LiveUpdate("3", "Uncle Waffles", "DJ", "https://i.pravatar.cc/150?img=9", "Warmup vibes are immaculate. Ready to turn up Rosebank tonight! ✨ See you guys soon.", "45 min ago", "#32C759"),
        LiveUpdate("4", "Sipho_K (Verified Attendee)", "Attendee", "https://i.pravatar.cc/150?img=11", "Queue outside is about 15 mins. Highly recommend arriving before 10 PM!", "1 hr ago", "#FFD60A")
    ),
    val events: List<TonightEvent> = listOf(
        TonightEvent("e1", "Amapiano Fridays: Winter Edition", "21:00 — 04:00", "Kabza De Small & Uncle Waffles", 420, "Selling Fast", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=600&auto=format&fit=crop"),
        TonightEvent("e2", "VIP Rooftop Sunset Chill", "18:00 — 22:00", "DJ Kent", 185, "Available", "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?q=80&w=600&auto=format&fit=crop")
    ),
    val lineup: List<LineupSlot> = listOf(
        LineupSlot("l1", "18:00 - 21:00", "DJ Kent", "Sunset DJ Set", "https://i.pravatar.cc/150?img=11"),
        LineupSlot("l2", "21:00 - 23:00", "DJ Maphorisa", "Opening Amapiano Set", "https://i.pravatar.cc/150?img=33"),
        LineupSlot("l3", "23:00 - 01:00", "Kabza De Small", "Headlining Set", "https://i.pravatar.cc/150?img=12"),
        LineupSlot("l4", "01:00 - 03:00", "Uncle Waffles", "Special Performance", "https://i.pravatar.cc/150?img=9"),
        LineupSlot("l5", "03:00 - Close", "MC Zulu", "Resident Host", "https://i.pravatar.cc/150?img=5")
    ),
    val flashDrops: List<FlashDrop> = listOf(
        FlashDrop("fd1", "Free Welcome Tequila Shot", "First 100 guests before 10 PM. Show this card at the bar.", 34, 100, 48, "shot"),
        FlashDrop("fd2", "VIP Rooftop Access Upgrade", "Claim free entry to our exclusive glasshouse sunset lounge.", 58, 25, 3, "vip"),
        FlashDrop("fd3", "Buy 1 Get 1 Free: Signature Cocktail", "Enjoy FOMO Specials at the main bar inside before midnight.", 118, 150, 92, "cocktail")
    ),
    val highlights: List<VenueHighlight> = listOf(
        VenueHighlight("h1", "The Dome Sanctuary", "An imposing architectural marvel with 360-degree interactive 3D light mapping and custom visual sequences syncing to the music.", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop"),
        VenueHighlight("h2", "Veuve Clicquot VIP Lounge", "Sleek, elevated private booths overlooking the main stage with personal table service and private security details.", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=600&auto=format&fit=crop"),
        VenueHighlight("h3", "Cloud Nine Terrace", "An open-air glasshouse rooftop bar offering incredible scenic views of the Johannesburg skyline and curated craft cocktails.", "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=600&auto=format&fit=crop")
    ),
    val moments: List<MomentData> = listOf(
        MomentData("m1", "Black Coffee Live Takeover", "June 14, 2026", 84, 25, 4.9f, "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop"),
        MomentData("m2", "New Year's Eve Extravaganza", "Jan 1, 2026", 142, 60, 4.8f, "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=600&auto=format&fit=crop"),
        MomentData("m3", "Amapiano Awards Afterparty", "Mar 28, 2026", 96, 45, 4.9f, "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=600&auto=format&fit=crop")
    ),
    val liveBroadcast: LiveBroadcastInfo = LiveBroadcastInfo(
        isLive = true,
        title = "DJ Maphorisa Live Amapiano Launch",
        watcherCount = 384,
        channelName = "dj_booth_stream"
    ),
    val vibePoll: VibePoll = VibePoll(
        litCount = 124,
        goodCount = 42,
        quietCount = 8,
        userVote = null
    ),
    val friendsGoing: List<FriendPlan> = listOf(
        FriendPlan("Sarah Jenkins", "https://i.pravatar.cc/150?img=5", "Going"),
        FriendPlan("Mike Ross", "https://i.pravatar.cc/150?img=11", "Going"),
        FriendPlan("Emma Stone", "https://i.pravatar.cc/150?img=9", "Going"),
        FriendPlan("James Bond", "https://i.pravatar.cc/150?img=12", "Maybe")
    )
)

object VenueRepository {
    private val _venueState = MutableStateFlow(VenueProfileState())
    val venueState: StateFlow<VenueProfileState> = _venueState.asStateFlow()

    fun toggleFollow() {
        _venueState.update { current ->
            val nextFollowing = !current.isFollowing
            current.copy(
                isFollowing = nextFollowing,
                followerCount = if (nextFollowing) current.followerCount + 1 else current.followerCount - 1
            )
        }
    }

    fun toggleSave() {
        _venueState.update { current ->
            current.copy(isSaved = !current.isSaved)
        }
    }

    fun submitVibeVote(option: String) {
        _venueState.update { current ->
            val poll = current.vibePoll
            if (poll.userVote == option) return@update current // Already voted same

            // Undo previous vote if any
            var lit = poll.litCount
            var good = poll.goodCount
            var quiet = poll.quietCount

            when (poll.userVote) {
                "Lit" -> lit--
                "Good" -> good--
                "Quiet" -> quiet--
            }

            // Apply new vote
            when (option) {
                "Lit" -> lit++
                "Good" -> good++
                "Quiet" -> quiet++
            }

            current.copy(
                vibePoll = VibePoll(
                    litCount = lit,
                    goodCount = good,
                    quietCount = quiet,
                    userVote = option
                )
            )
        }
    }

    fun claimFlashDrop(id: String) {
        _venueState.update { current ->
            val updatedDrops = current.flashDrops.map { drop ->
                if (drop.id == id && !drop.claimed && drop.currentStock > 0) {
                    drop.copy(currentStock = drop.currentStock - 1, claimed = true)
                } else {
                    drop
                }
            }
            current.copy(flashDrops = updatedDrops)
        }
    }

    fun addLiveUpdate(author: String, role: String, content: String) {
        _venueState.update { current ->
            val newUpdate = LiveUpdate(
                id = (current.liveUpdates.size + 1).toString(),
                author = author,
                authorRole = role,
                authorImage = when (role) {
                    "Venue" -> "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=200&auto=format&fit=crop"
                    "DJ" -> "https://i.pravatar.cc/150?img=12"
                    else -> "https://i.pravatar.cc/150?img=5"
                },
                content = content,
                timeAgo = "Just now",
                badgeColor = when (role) {
                    "Venue" -> "#FF2D55"
                    "DJ" -> "#007AFF"
                    else -> "#32C759"
                }
            )
            current.copy(liveUpdates = listOf(newUpdate) + current.liveUpdates)
        }
    }

    fun addNotice(type: String, title: String, content: String) {
        _venueState.update { current ->
            val newNotice = VenueNotice(
                id = (current.notices.size + 1).toString(),
                type = type,
                title = title,
                content = content,
                timeAgo = "Just now"
            )
            current.copy(notices = listOf(newNotice) + current.notices)
        }
    }

    fun addFlashDrop(title: String, subtitle: String, expiresMinutes: Int, stock: Int) {
        _venueState.update { current ->
            val newDrop = FlashDrop(
                id = "fd_${current.flashDrops.size + 1}",
                title = title,
                subtitle = subtitle,
                expiresMinutes = expiresMinutes,
                initialStock = stock,
                currentStock = stock,
                imageRes = "gift"
            )
            current.copy(flashDrops = listOf(newDrop) + current.flashDrops)
        }
    }

    fun updateLiveBroadcastStatus(isLive: Boolean, title: String = "") {
        _venueState.update { current ->
            current.copy(
                liveBroadcast = current.liveBroadcast.copy(
                    isLive = isLive,
                    title = if (title.isNotEmpty()) title else current.liveBroadcast.title
                )
            )
        }
    }

    // Explore the City - Billion-Dollar Discover Experience (MVP)
    private val _exploreVenues = MutableStateFlow<List<ExploreVenue>>(
        listOf(
            ExploreVenue(
                id = "d48_midrand",
                name = "D48 Midrand",
                category = "Nightlife",
                subcategory = "VIP Lounge & Nightclub",
                imageUrl = "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=600&auto=format&fit=crop",
                isVerified = true,
                rating = 4.8f,
                reviewCount = 943,
                address = "563 Old Pretoria Road, Midrand",
                area = "Midrand",
                distanceText = "18 km away",
                attributes = listOf("VIP Seating", "Hubbly", "Hennessy", "RedBull", "Bottle Service", "Live DJs"),
                openDays = "Thu–Sun",
                startHour = 18,
                endHour = 4,
                is24Hours = false,
                websiteUrl = "https://d48midrand.co.za",
                hasClubLobby = true
            ),
            ExploreVenue(
                id = "konka_soweto",
                name = "Konka Soweto",
                category = "Nightlife",
                subcategory = "Nightclub",
                imageUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=600&auto=format&fit=crop",
                isVerified = true,
                rating = 4.8f,
                reviewCount = 2431,
                address = "Modjadji Street, Soweto",
                area = "Soweto",
                distanceText = "12 min away",
                attributes = listOf("DJ", "VIP", "Dance Floor", "Cocktails"),
                openDays = "Fri–Sun",
                startHour = 14,
                endHour = 2,
                is24Hours = false,
                websiteUrl = "https://konka.co.za",
                hasClubLobby = true
            ),
            ExploreVenue(
                id = "taboo_sandton",
                name = "Taboo Lounge",
                category = "Nightlife",
                subcategory = "Lounge",
                imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop",
                isVerified = true,
                rating = 4.6f,
                reviewCount = 812,
                address = "24 Gwigwi Mrwebi Street",
                area = "Sandton",
                distanceText = "15 min away",
                attributes = listOf("VIP Friendly", "Cocktails", "DJ", "Live Music"),
                openDays = "Fri–Sat",
                startHour = 18,
                endHour = 4,
                is24Hours = false,
                websiteUrl = "https://taboosandton.co.za",
                hasClubLobby = true
            ),
            ExploreVenue(
                id = "marble_rosebank",
                name = "Marble Restaurant",
                category = "Food",
                subcategory = "Fine Dining",
                imageUrl = "https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=600&auto=format&fit=crop",
                isVerified = true,
                rating = 4.7f,
                reviewCount = 1254,
                address = "19 Keyes Avenue",
                area = "Rosebank",
                distanceText = "8 min away",
                attributes = listOf("Steakhouse", "Fine Dining", "Rooftop", "Reservations"),
                openDays = "Mon–Sun",
                startHour = 12,
                endHour = 22,
                is24Hours = false,
                websiteUrl = "https://marble.restaurant",
                hasClubLobby = false
            ),
            ExploreVenue(
                id = "proud_mary",
                name = "Proud Mary",
                category = "Food",
                subcategory = "Casual Dining",
                imageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=600&auto=format&fit=crop",
                isVerified = false,
                rating = 4.5f,
                reviewCount = 620,
                address = "The Zone, Oxford Road",
                area = "Rosebank",
                distanceText = "5 min away",
                attributes = listOf("Restaurant", "Brunch Spots", "Coffee Shops", "Cocktails"),
                openDays = "Mon–Sun",
                startHour = 7,
                endHour = 22,
                is24Hours = false,
                websiteUrl = "https://proudmary.co.za",
                hasClubLobby = false
            ),
            ExploreVenue(
                id = "legend_barber",
                name = "Legend Barber",
                category = "Prep",
                subcategory = "Barber",
                imageUrl = "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?q=80&w=600&auto=format&fit=crop",
                isVerified = true,
                rating = 4.9f,
                reviewCount = 310,
                address = "Rosebank Mall, 50 Bath Ave",
                area = "Rosebank",
                distanceText = "6 min away",
                attributes = listOf("Barber", "Walk-ins", "Premium", "Card Payments"),
                openDays = "Mon–Sun",
                startHour = 9,
                endHour = 18,
                is24Hours = false,
                websiteUrl = "https://legend-barber.com",
                hasClubLobby = false
            ),
            ExploreVenue(
                id = "sorbet_salon",
                name = "Sorbet Salon",
                category = "Prep",
                subcategory = "Nail Studio",
                imageUrl = "https://images.unsplash.com/photo-1604654894610-df63bc536371?q=80&w=600&auto=format&fit=crop",
                isVerified = true,
                rating = 4.4f,
                reviewCount = 189,
                address = "Sandton City Mall, Rivonia Rd",
                area = "Sandton",
                distanceText = "14 min away",
                attributes = listOf("Nail Studios", "Walk-ins", "Premium", "Card Payments"),
                openDays = "Mon–Sun",
                startHour = 9,
                endHour = 19,
                is24Hours = false,
                websiteUrl = "https://sorbet.co.za",
                hasClubLobby = false
            ),
            ExploreVenue(
                id = "sanctuary_spa",
                name = "Sanctuary Spa",
                category = "Recover",
                subcategory = "Spa",
                imageUrl = "https://images.unsplash.com/photo-1540555700478-4be289fbecef?q=80&w=600&auto=format&fit=crop",
                isVerified = true,
                rating = 4.8f,
                reviewCount = 412,
                address = "The Michelangelo, West Street",
                area = "Sandton",
                distanceText = "16 min away",
                attributes = listOf("Spa", "Massage", "Ice Bath", "Wellness"),
                openDays = "Mon–Sun",
                startHour = 8,
                endHour = 20,
                is24Hours = false,
                websiteUrl = "https://sanctuaryspa.co.za",
                hasClubLobby = false
            ),
            ExploreVenue(
                id = "four_seasons_westcliff",
                name = "Four Seasons Resort",
                category = "Travel",
                subcategory = "Resort",
                imageUrl = "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?q=80&w=600&auto=format&fit=crop",
                isVerified = true,
                rating = 4.9f,
                reviewCount = 928,
                address = "67 Jan Smuts Avenue",
                area = "Westcliff",
                distanceText = "10 min away",
                attributes = listOf("Hotel", "Resort", "Restaurant", "Pool"),
                openDays = "Mon–Sun",
                startHour = 0,
                endHour = 24,
                is24Hours = true,
                websiteUrl = "https://fourseasons.com/westcliff",
                hasClubLobby = false
            ),
            ExploreVenue(
                id = "shell_select_rosebank",
                name = "Shell Select 24/7",
                category = "24/7",
                subcategory = "Convenience",
                imageUrl = "https://images.unsplash.com/photo-1527018601619-a508a2be00cd?q=80&w=600&auto=format&fit=crop",
                isVerified = true,
                rating = 4.2f,
                reviewCount = 145,
                address = "Jan Smuts Ave & Jellicoe Ave",
                area = "Rosebank",
                distanceText = "4 min away",
                attributes = listOf("Filling Station", "Coffee", "Convenience Store", "Fast Food", "Open 24 Hours"),
                openDays = "Mon–Sun",
                startHour = 0,
                endHour = 24,
                is24Hours = true,
                websiteUrl = "https://shell.co.za",
                hasClubLobby = false
            )
        )
    )
    val exploreVenuesState: StateFlow<List<ExploreVenue>> = _exploreVenues.asStateFlow()

    fun toggleLikeVenue(id: String) {
        _exploreVenues.update { list ->
            list.map { venue ->
                if (venue.id == id) {
                    venue.copy(isLiked = !venue.isLiked)
                } else {
                    venue
                }
            }
        }
    }

    // Global Flash Drops (including Sunday VIP Special & multi-category drops)
    private val _globalFlashDrops = MutableStateFlow<List<FlashDrop>>(
        listOf(
            FlashDrop(
                id = "fd_d48_vip_special",
                title = "Sunday VIP Special",
                subtitle = "1x Hennessy VS, 4x RedBull, 2x Hubbly, VIP Seating",
                expiresMinutes = 120,
                initialStock = 15,
                currentStock = 4,
                imageRes = "vip",
                venueId = "d48_midrand",
                venueName = "D48 Midrand",
                price = "R950",
                tableReservations = "082 716 4275",
                posterUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop",
                category = "VENUE",
                status = "TRENDING",
                distanceText = "450m away",
                urgencyBadge = "🔥 Trending",
                isVerifiedVenue = true,
                heroImageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop"
            ),
            FlashDrop(
                id = "fd1",
                title = "Free Welcome Tequila Shot",
                subtitle = "First 100 guests before 10 PM. Arrive early at the main bar.",
                expiresMinutes = 18,
                initialStock = 100,
                currentStock = 12,
                imageRes = "shot",
                venueId = "fomo_club",
                venueName = "FOMO Club",
                category = "VENUE",
                status = "ENDING_SOON",
                distanceText = "250m away",
                urgencyBadge = "🚨 Final Minutes",
                isVerifiedVenue = true,
                heroImageUrl = "https://images.unsplash.com/photo-1551024709-8f23befc6f87?q=80&w=600&auto=format&fit=crop"
            ),
            FlashDrop(
                id = "fd2",
                title = "VIP Rooftop Access Upgrade",
                subtitle = "Claim free entry to our exclusive glasshouse sunset lounge.",
                expiresMinutes = 58,
                initialStock = 25,
                currentStock = 3,
                imageRes = "vip",
                venueId = "fomo_club",
                venueName = "FOMO Club",
                category = "VENUE",
                status = "LIVE",
                distanceText = "250m away",
                urgencyBadge = "⚡ Just Dropped",
                isVerifiedVenue = true,
                heroImageUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=600&auto=format&fit=crop"
            ),
            FlashDrop(
                id = "fd_event_backstage",
                title = "Backstage Access Pass: Amapiano Fest",
                subtitle = "Exclusive stage-side credentials & artist lounge access for the first 10 arrivals.",
                expiresMinutes = 45,
                initialStock = 10,
                currentStock = 2,
                imageRes = "vip",
                venueId = "konka_soweto",
                venueName = "Konka Soweto",
                price = "FREE",
                category = "EVENT",
                status = "ENDING_SOON",
                distanceText = "1.2km away",
                urgencyBadge = "🚨 Final Minutes",
                isVerifiedVenue = true,
                eventId = "evt_amapiano_launch",
                heroImageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=600&auto=format&fit=crop"
            ),
            FlashDrop(
                id = "fd_creator_meetup",
                title = "Uncle Waffles Rooftop Meetup & Drink",
                subtitle = "Live sunset photo shoot & complimentary welcome cocktail with Uncle Waffles.",
                expiresMinutes = 75,
                initialStock = 30,
                currentStock = 18,
                imageRes = "cocktail",
                venueId = "taboo_sandton",
                venueName = "Taboo Sandton",
                price = "R150",
                category = "CREATOR",
                status = "LIVE",
                distanceText = "800m away",
                urgencyBadge = "🎉 Limited Availability",
                isVerifiedVenue = true,
                creatorName = "Uncle Waffles",
                creatorAvatar = "https://i.pravatar.cc/150?img=32",
                heroImageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=600&auto=format&fit=crop"
            ),
            FlashDrop(
                id = "fd_brand_redbull",
                title = "RedBull Night Edition Sampler",
                subtitle = "Free limited-edition energy elixir at the main courtyard lounge entrance.",
                expiresMinutes = 90,
                initialStock = 200,
                currentStock = 114,
                imageRes = "cocktail",
                venueId = "marble_rosebank",
                venueName = "Marble Rosebank",
                price = "FREE",
                category = "BRAND",
                status = "LIVE",
                distanceText = "500m away",
                urgencyBadge = "⚡ Just Dropped",
                isVerifiedVenue = true,
                heroImageUrl = "https://images.unsplash.com/photo-1527529482837-4698179dc6ce?q=80&w=600&auto=format&fit=crop"
            ),
            FlashDrop(
                id = "fd_mystery_dj",
                title = "Secret Guest DJ Set & Hidden Bar",
                subtitle = "A world-renowned surprise DJ takes the decks in a secret underground room.",
                expiresMinutes = 35,
                initialStock = 40,
                currentStock = 8,
                imageRes = "vip",
                venueId = "four_seasons_westcliff",
                venueName = "Secret Westcliff Courtyard",
                price = "FREE",
                category = "MYSTERY",
                status = "TRENDING",
                distanceText = "600m away",
                urgencyBadge = "🔥 Trending",
                isVerifiedVenue = true,
                hintText = "Look for the velvet curtain near the rooftop fountain.",
                heroImageUrl = "https://images.unsplash.com/photo-1571266028243-e4733b0f0bb1?q=80&w=600&auto=format&fit=crop"
            ),
            FlashDrop(
                id = "fd3",
                title = "Buy 1 Get 1 Free: Signature Cocktail",
                subtitle = "Enjoy FOMO Specials at the main bar inside before midnight.",
                expiresMinutes = 118,
                initialStock = 150,
                currentStock = 92,
                imageRes = "cocktail",
                venueId = "fomo_club",
                venueName = "FOMO Club",
                category = "VENUE",
                status = "LIVE",
                distanceText = "250m away",
                urgencyBadge = "🌙 Tonight Only",
                isVerifiedVenue = true,
                heroImageUrl = "https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?q=80&w=600&auto=format&fit=crop"
            )
        )
    )
    val globalFlashDropsState: StateFlow<List<FlashDrop>> = _globalFlashDrops.asStateFlow()

    fun claimGlobalFlashDrop(id: String) {
        _globalFlashDrops.update { list ->
            list.map { drop ->
                if (drop.id == id && !drop.claimed && drop.currentStock > 0) {
                    drop.copy(currentStock = drop.currentStock - 1, claimed = true)
                } else {
                    drop
                }
            }
        }
        // Also update local venue profile state if it matches
        _venueState.update { current ->
            val updatedDrops = current.flashDrops.map { drop ->
                if (drop.id == id && !drop.claimed && drop.currentStock > 0) {
                    drop.copy(currentStock = drop.currentStock - 1, claimed = true)
                } else {
                    drop
                }
            }
            current.copy(flashDrops = updatedDrops)
        }
    }
}

data class ExploreVenue(
    val id: String,
    val name: String,
    val category: String, // "Nightlife", "Food", "Prep", "Recover", "Travel", "24/7"
    val subcategory: String,
    val imageUrl: String,
    val isVerified: Boolean = true,
    val rating: Float,
    val reviewCount: Int,
    val address: String,
    val area: String,
    val distanceText: String,
    val attributes: List<String>,
    val openDays: String,
    val startHour: Int,
    val endHour: Int,
    val is24Hours: Boolean = false,
    val websiteUrl: String = "https://fomoapp.live",
    val hasClubLobby: Boolean = false,
    val isLiked: Boolean = false
)

