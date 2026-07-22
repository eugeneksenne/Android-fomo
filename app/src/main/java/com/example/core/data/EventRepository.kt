package com.example.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

data class EventLineupItem(
    val time: String,
    val artistName: String,
    val role: String,
    val imageUrl: String
)

data class Event(
    val id: String,
    val title: String,
    val venueId: String,
    val venueName: String,
    val venueRating: Float = 4.7f,
    val venueStatus: String = "Open Now",
    val isVenueVerified: Boolean = true,
    val posterUrl: String,
    val artists: List<String>,
    val genres: List<String>,
    val dateText: String,
    val timeText: String,
    val countdownText: String,
    val ticketPrice: String,
    val ticketAvailability: String, // "Available", "Selling Fast", "Sold Out", "Free Entry"
    val distance: String,
    val driveTime: String,
    val walkTime: String,
    val rippleHeat: String, // "🔥 Heating Up", "🌊 Peak Night", "⚡ Trending Nearby", "🍾 VIP Filling Fast"
    val description: String,
    val lineup: List<EventLineupItem> = emptyList(),
    val gallery: List<String> = emptyList(),
    val isInterested: Boolean = false,
    val isPlanned: Boolean = false,
    val reminderMinutes: Int? = null,
    val isSponsored: Boolean = false,
    val isNightlife: Boolean = true,
    val hasFlashDrop: Boolean = false,
    val flashDropText: String? = null,
    val dateInMillis: Long = System.currentTimeMillis() // for calendar grouping
)

object EventRepository {
    private val defaultEvents = listOf(
        Event(
            id = "evt_d48_sunday_vip",
            title = "Sunday VIP Special",
            venueId = "d48_midrand",
            venueName = "D48 Midrand",
            venueRating = 4.8f,
            venueStatus = "Open Now • Closes 04:00",
            isVenueVerified = true,
            posterUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=800&auto=format&fit=crop",
            artists = listOf("D48 Resident DJs", "Special Guests"),
            genres = listOf("Amapiano", "Afro House", "Hip Hop"),
            dateText = "This Sunday",
            timeText = "14:00 — 02:00",
            countdownText = "Starts this Sunday",
            ticketPrice = "R950",
            ticketAvailability = "Reservations Open",
            distance = "18 km",
            driveTime = "15 min",
            walkTime = "2 hr+",
            rippleHeat = "🌟 Elite Lounge",
            description = "D48 Midrand presents the Sunday VIP Special. Book an exclusive VIP booth table including 1x Hennessy VS bottle, 4x RedBull energy drinks, 2x premium Hubblys, and high-end VIP seating. Perfect premium vibe to close off your weekend. Call or WhatsApp 082 716 4275 for table reservations.",
            lineup = listOf(
                EventLineupItem("14:00 - 18:00", "Lounge Warmup", "Sunset chill grooves", "https://i.pravatar.cc/150?img=11"),
                EventLineupItem("18:00 - 22:00", "Resident DJs", "Deep Afro House Set", "https://i.pravatar.cc/150?img=12"),
                EventLineupItem("22:00 - Close", "Special Guests", "Amapiano & Hip Hop Peak", "https://i.pravatar.cc/150?img=14")
            ),
            gallery = listOf(
                "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=400&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=400&auto=format&fit=crop"
            ),
            isSponsored = true,
            hasFlashDrop = true,
            flashDropText = "🍾 R950 VIP Deal: 1x Hennessy, 4x Redbull, 2x Hubbly, VIP Seating",
            dateInMillis = System.currentTimeMillis() + 3 * 24 * 3600 * 1000 // future date (Sunday)
        ),
        Event(
            id = "evt_fomo_amapiano",
            title = "Amapiano Fridays: Winter Edition",
            venueId = "fomo_club",
            venueName = "FOMO Club",
            venueRating = 4.8f,
            venueStatus = "Open Now • Closes 04:00",
            isVenueVerified = true,
            posterUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=800&auto=format&fit=crop",
            artists = listOf("Kabza De Small", "Uncle Waffles", "DJ Maphorisa"),
            genres = listOf("Amapiano", "Afro House"),
            dateText = "Tonight",
            timeText = "21:00 — 04:00",
            countdownText = "Starts in 2h 15m",
            ticketPrice = "R150",
            ticketAvailability = "Selling Fast",
            distance = "1.2 km",
            driveTime = "4 min",
            walkTime = "15 min",
            rippleHeat = "🌊 Peak Night",
            description = "The ultimate Amapiano celebration in South Africa. Experience immersive 3D light projection and pristine acoustics as the pioneers of the sound take over FOMO Club. Arrive early to avoid the queue.",
            lineup = listOf(
                EventLineupItem("21:00 - 23:00", "DJ Maphorisa", "Opening Amapiano Set", "https://i.pravatar.cc/150?img=33"),
                EventLineupItem("23:00 - 01:00", "Kabza De Small", "Headlining Set", "https://i.pravatar.cc/150?img=12"),
                EventLineupItem("01:00 - 03:00", "Uncle Waffles", "Special Performance", "https://i.pravatar.cc/150?img=9")
            ),
            gallery = listOf(
                "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=400&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=400&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=400&auto=format&fit=crop"
            ),
            isSponsored = true,
            hasFlashDrop = true,
            flashDropText = "🎁 Free Welcome Tequila Shot (First 50 guests)",
            dateInMillis = System.currentTimeMillis()
        ),
        Event(
            id = "evt_and_techno",
            title = "Sub-Terranean Techno Ritual",
            venueId = "and_club",
            venueName = "And Club",
            venueRating = 4.6f,
            venueStatus = "Opens 21:00 • Closes 04:00",
            isVenueVerified = true,
            posterUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=800&auto=format&fit=crop",
            artists = listOf("Crom", "Deep Roots", "Void Decay"),
            genres = listOf("Techno", "Minimal", "Acid"),
            dateText = "Tonight",
            timeText = "21:00 — 04:00",
            countdownText = "Starts in 3h 30m",
            ticketPrice = "R100",
            ticketAvailability = "Available",
            distance = "2.8 km",
            driveTime = "7 min",
            walkTime = "35 min",
            rippleHeat = "🔥 Heating Up",
            description = "Descend into the underground. And Club brings a raw, authentic concrete basement experience with custom-tuned minimal and hypnotic techno sounds. No photos allowed on the dance floor.",
            lineup = listOf(
                EventLineupItem("21:00 - 23:30", "Void Decay", "Warmup Loop", "https://i.pravatar.cc/150?img=11"),
                EventLineupItem("23:30 - 02:00", "Crom", "Peak Techno Voyage", "https://i.pravatar.cc/150?img=14"),
                EventLineupItem("02:00 - Close", "Deep Roots", "Acid Sunrise", "https://i.pravatar.cc/150?img=15")
            ),
            gallery = listOf(
                "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=400&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=400&auto=format&fit=crop"
            ),
            isSponsored = false,
            hasFlashDrop = false,
            dateInMillis = System.currentTimeMillis()
        ),
        Event(
            id = "evt_konka_sundays",
            title = "Konka Sunday Live Experience",
            venueId = "konka",
            venueName = "Konka Soweto",
            venueRating = 4.9f,
            venueStatus = "Closed Today • Opens Sunday",
            isVenueVerified = true,
            posterUrl = "https://images.unsplash.com/photo-1574391884720-bbc3740c59d1?q=80&w=800&auto=format&fit=crop",
            artists = listOf("Oscar Mbo", "Kelvin Momo", "Young Stunna"),
            genres = listOf("Amapiano", "Soulful House"),
            dateText = "Sunday, Jul 26",
            timeText = "14:00 — 23:00",
            countdownText = "6 days left",
            ticketPrice = "R300",
            ticketAvailability = "Selling Fast",
            distance = "15.4 km",
            driveTime = "22 min",
            walkTime = "3 hrs",
            rippleHeat = "⚡ Trending Nearby",
            description = "The global capital of premium South African groove. Join us this Sunday at Konka Soweto for luxury bottles, elite fashion, and an unmatched line-up showcasing soulful Amapiano from Kelvin Momo.",
            lineup = listOf(
                EventLineupItem("14:00 - 17:00", "Oscar Mbo", "Deep Soul Sunset", "https://i.pravatar.cc/150?img=21"),
                EventLineupItem("17:00 - 20:00", "Kelvin Momo", "Private School Piano", "https://i.pravatar.cc/150?img=22"),
                EventLineupItem("20:00 - Close", "Young Stunna", "Live Vocal Set", "https://i.pravatar.cc/150?img=23")
            ),
            gallery = listOf(
                "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=400&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=400&auto=format&fit=crop"
            ),
            isSponsored = true,
            hasFlashDrop = true,
            flashDropText = "🍾 VIP Bottle Upgrades Available",
            dateInMillis = System.currentTimeMillis() + 6 * 24 * 60 * 60 * 1000L
        ),
        Event(
            id = "evt_fomo_rooftop",
            title = "VIP Rooftop Sunset Chill",
            venueId = "fomo_club",
            venueName = "FOMO Club",
            venueRating = 4.8f,
            venueStatus = "Open Now • Closes 04:00",
            isVenueVerified = true,
            posterUrl = "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?q=80&w=800&auto=format&fit=crop",
            artists = listOf("DJ Kent", "Sino Msolo"),
            genres = listOf("House", "R&B"),
            dateText = "Tonight",
            timeText = "18:00 — 22:00",
            countdownText = "Live Now",
            ticketPrice = "Free Entry",
            ticketAvailability = "Available",
            distance = "1.2 km",
            driveTime = "4 min",
            walkTime = "15 min",
            rippleHeat = "🍾 VIP Filling Fast",
            description = "Enjoy beautiful cocktails with panoramic views of the Rosebank skyline as Sino Msolo delivers live R&B fusion vocals over smooth resident house sets. Entrance is free, table bookings are highly recommended.",
            lineup = listOf(
                EventLineupItem("18:00 - 20:00", "DJ Kent", "Sunset Groove", "https://i.pravatar.cc/150?img=11"),
                EventLineupItem("20:00 - 22:00", "Sino Msolo", "Live Vocals", "https://i.pravatar.cc/150?img=16")
            ),
            gallery = listOf(
                "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=400&auto=format&fit=crop"
            ),
            isSponsored = false,
            hasFlashDrop = false,
            dateInMillis = System.currentTimeMillis()
        ),
        Event(
            id = "evt_truth_hiphop",
            title = "Urban Trap & Hip Hop Showdown",
            venueId = "truth",
            venueName = "Truth Midrand",
            venueRating = 4.5f,
            venueStatus = "Opens Tomorrow 20:00",
            isVenueVerified = false,
            posterUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=800&auto=format&fit=crop",
            artists = listOf("Nasty C", "Blxckie", "Speedsta"),
            genres = listOf("Hip Hop", "Trap"),
            dateText = "Tomorrow",
            timeText = "20:00 — 03:00",
            countdownText = "Starts in 27h",
            ticketPrice = "R200",
            ticketAvailability = "Available",
            distance = "18.2 km",
            driveTime = "25 min",
            walkTime = "4 hrs",
            rippleHeat = "⚡ Trending Nearby",
            description = "Truth Nightclub turns into a high-octane hip-hop colosseum. Catch Nasty C performing tracks live alongside Durban trap pioneer Blxckie. Massive bass drops guaranteed.",
            lineup = listOf(
                EventLineupItem("20:00 - 22:30", "Speedsta", "Old School Anthems", "https://i.pravatar.cc/150?img=31"),
                EventLineupItem("22:30 - 00:30", "Blxckie", "New-Wave Trap set", "https://i.pravatar.cc/150?img=32"),
                EventLineupItem("00:30 - Close", "Nasty C", "Headlining Rap Live", "https://i.pravatar.cc/150?img=35")
            ),
            gallery = emptyList(),
            isSponsored = false,
            hasFlashDrop = false,
            dateInMillis = System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000L
        )
    )

    private val _eventsState = MutableStateFlow<List<Event>>(defaultEvents)
    val eventsState: StateFlow<List<Event>> = _eventsState.asStateFlow()

    fun toggleInterested(id: String) {
        _eventsState.update { list ->
            list.map { evt ->
                if (evt.id == id) {
                    evt.copy(isInterested = !evt.isInterested)
                } else {
                    evt
                }
            }
        }
    }

    fun togglePlanned(id: String) {
        _eventsState.update { list ->
            list.map { evt ->
                if (evt.id == id) {
                    evt.copy(isPlanned = !evt.isPlanned)
                } else {
                    evt
                }
            }
        }
    }

    fun setReminder(id: String, minutes: Int?) {
        _eventsState.update { list ->
            list.map { evt ->
                if (evt.id == id) {
                    evt.copy(reminderMinutes = minutes)
                } else {
                    evt
                }
            }
        }
    }

    fun addNewEvent(
        title: String,
        venueId: String,
        venueName: String,
        genres: List<String>,
        artists: List<String>,
        ticketPrice: String,
        timeText: String,
        description: String,
        hasFlashDrop: Boolean,
        flashDropText: String?
    ) {
        _eventsState.update { list ->
            val newEvt = Event(
                id = "evt_custom_${list.size + 1}",
                title = title,
                venueId = venueId,
                venueName = venueName,
                venueRating = 4.7f,
                venueStatus = "Open Now",
                isVenueVerified = true,
                posterUrl = "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=800&auto=format&fit=crop",
                artists = artists,
                genres = genres,
                dateText = "Tonight",
                timeText = timeText,
                countdownText = "Just Added",
                ticketPrice = ticketPrice,
                ticketAvailability = "Available",
                distance = "1.2 km",
                driveTime = "4 min",
                walkTime = "15 min",
                rippleHeat = "🔥 Heating Up",
                description = description,
                lineup = artists.mapIndexed { index, name ->
                    EventLineupItem("22:00 - Close", name, "Special Guest", "https://i.pravatar.cc/150?img=${10 + index}")
                },
                isSponsored = false,
                hasFlashDrop = hasFlashDrop,
                flashDropText = flashDropText,
                dateInMillis = System.currentTimeMillis()
            )
            listOf(newEvt) + list
        }
    }
}
