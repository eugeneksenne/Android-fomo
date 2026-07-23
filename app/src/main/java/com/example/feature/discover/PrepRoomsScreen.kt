package com.example.feature.discover

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -------------------------------------------------------------------------
// DATA MODEL DEFINITIONS FOR PREP ROOMS
// -------------------------------------------------------------------------
data class PrepLookPoll(
    val id: String,
    val authorName: String,
    val authorAvatar: String,
    val caption: String,
    val lookAName: String,
    val lookAImage: String,
    val lookBName: String,
    val lookBImage: String,
    var votesA: Int,
    var votesB: Int,
    var userVoted: String?, // "A", "B", or null
    val venueName: String,
    val comments: MutableList<PrepComment> = mutableListOf()
)

data class PrepComment(
    val id: String,
    val author: String,
    val text: String,
    val timestamp: String
)

data class PrepTutorial(
    val id: String,
    val title: String,
    val durationText: String,
    val creatorName: String,
    val creatorAvatar: String,
    val coverImage: String,
    val category: String, // "Makeup", "Hair", "Nails", "Grooming", "Styling"
    var likes: Int,
    var isLiked: Boolean = false,
    val description: String,
    val steps: List<String> = emptyList()
)

data class PrepTip(
    val title: String,
    val content: String,
    val category: String, // "Weather", "Security", "Transport", "Styling"
    val icon: ImageVector
)

data class NearbyPrepPlace(
    val id: String,
    val name: String,
    val type: String, // "Nails", "Barber", "Salon", "Makeup", "Boutique"
    val address: String,
    val distance: String,
    val matchRate: Int,
    val image: String,
    val rating: Float
)

data class VenuePrepGuide(
    val venueName: String,
    val dressCode: String,
    val theme: String,
    val popularColors: List<String>,
    val popularShoes: String,
    val popularMakeup: String,
    val popularHairstyles: String,
    val weatherNote: String,
    val parkingInfo: String,
    val ridePickup: String,
    val openingHours: String,
    val entryRules: String
)

data class CircleActivity(
    val friendName: String,
    val friendAvatar: String,
    val statusText: String,
    val relativeTime: String,
    val itemType: String // "poll", "tutorial", "checking", "active"
)

data class SavedCollection(
    val name: String,
    val count: Int,
    val coverImage: String
)

data class ChecklistItem(
    val id: String,
    val name: String,
    var isChecked: Boolean
)

data class SquadChatMessage(
    val id: String,
    val senderName: String,
    val senderAvatar: String,
    val text: String,
    val timestamp: String,
    val isMe: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepRoomsOverlay(
    onDismiss: () -> Unit,
    onNavigateToEventDetails: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Luxury palette
    val themeBg = Color(0xFF0B0F19)
    val cardBg = Color(0xFF0F1524)
    val neonCyan = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF2D55)
    val successGreen = Color(0xFF32D74B)
    val luxGold = Color(0xFFD4AF37)

    // Screen navigation tabs: Following, Trending, Nearby, Events
    var selectedTab by remember { mutableStateOf("Following") }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Filter Chips: Toggle state to filter content
    val filterChips = listOf("👗 Looks", "💄 Makeup", "💇 Hair", "💅 Nails", "👞 Shoes", "👜 Accessories", "🎥 Tutorials", "✨ Prep Tips")
    val activeFilters = remember { mutableStateListOf<String>() }

    // Interactive Checklist items (Squad Checklist)
    val checklistItems = remember {
        mutableStateListOf(
            ChecklistItem("chk_ticket", "Download Tickets", true),
            ChecklistItem("chk_friends", "Invite the Squad", true),
            ChecklistItem("chk_outfit", "Outfit Pressed & Fitted", false),
            ChecklistItem("chk_hair", "Hair Groomed/Styled", false),
            ChecklistItem("chk_makeup", "Makeup Application", false),
            ChecklistItem("chk_nails", "Fresh Nails Setup", false),
            ChecklistItem("chk_charge", "Phone Battery 100%", true),
            ChecklistItem("chk_uber", "Pre-book Uber/Ride", false),
            ChecklistItem("chk_wallet", "Secure Cards & Cash", true),
            ChecklistItem("chk_id", "Pack Official ID/Pass", true)
        )
    }

    // Dynamic Ready Meter based on checklist
    val readyPercentage by remember {
        derivedStateOf {
            val checked = checklistItems.count { it.isChecked }
            val total = checklistItems.size
            if (total == 0) 0 else (checked * 100) / total
        }
    }

    // Tonight's Plan State
    var hasTonightPlan by remember { mutableStateOf(true) }
    var countdownSeconds by remember { mutableStateOf(3 * 3600 + 12 * 60 + 18) } // 3h 12m 18s

    // Run a real-time countdown for Tonight's Plan
    LaunchedEffect(hasTonightPlan) {
        if (hasTonightPlan) {
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds--
            }
        }
    }

    // Squad Chat State
    var showSquadChatModal by remember { mutableStateOf(false) }
    var squadChatInputText by remember { mutableStateOf("") }
    val squadChatMessages = remember {
        mutableStateListOf(
            SquadChatMessage("1", "Sarah Ndlovu", "https://i.pravatar.cc/150?img=32", "Who's picking up the Uber at 20:15?", "18:42"),
            SquadChatMessage("2", "Jessica M.", "https://i.pravatar.cc/150?img=49", "I'm finishing my makeup now! Look A won the poll 🎉", "18:45"),
            SquadChatMessage("3", "Mike K.", "https://i.pravatar.cc/150?img=11", "Tickets are saved in my Apple Wallet. Ready!", "18:48")
        )
    }

    // Add Custom Checklist Item State
    var showAddChecklistDialog by remember { mutableStateOf(false) }
    var newChecklistItemName by remember { mutableStateOf("") }

    // Add Custom Collection State
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }

    // Interactive Outfit Poll Database in state
    val outfitPolls = remember {
        mutableStateListOf(
            PrepLookPoll(
                id = "poll_1",
                authorName = "Sarah Ndlovu",
                authorAvatar = "https://i.pravatar.cc/150?img=32",
                caption = "Which aesthetic fits the AfroHaus Sunset deck tonight? Help me choose! Gold accents or Cyber leather?",
                lookAName = "Golden Goddess",
                lookAImage = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?q=80&w=400&auto=format&fit=crop",
                lookBName = "Cyber Minimalist",
                lookBImage = "https://images.unsplash.com/photo-1509631179647-0177331693ae?q=80&w=400&auto=format&fit=crop",
                votesA = 42,
                votesB = 31,
                userVoted = null,
                venueName = "AfroHaus Rooftop",
                comments = mutableListOf(
                    PrepComment("c1", "Jessica M.", "Look A is flawless for sunset lights! The gold will look insane.", "12m ago"),
                    PrepComment("c2", "Mike K.", "Look B is more cyber-chic if the temperature drops later.", "5m ago")
                )
            ),
            PrepLookPoll(
                id = "poll_2",
                authorName = "Brandon S.",
                authorAvatar = "https://i.pravatar.cc/150?img=68",
                caption = "Need styling advice for And Club's dark techno cavern. Minimalist oversized black vs Techwear utility?",
                lookAName = "Oversized Slate",
                lookAImage = "https://images.unsplash.com/photo-1617137968427-85924c800a22?q=80&w=400&auto=format&fit=crop",
                lookBName = "Cyber Utility",
                lookBImage = "https://images.unsplash.com/photo-1552374196-1ab2a1c593e8?q=80&w=400&auto=format&fit=crop",
                votesA = 18,
                votesB = 29,
                userVoted = "B",
                venueName = "And Club",
                comments = mutableListOf(
                    PrepComment("c3", "Sipho D.", "Techwear at And Club is a rite of passage. Go with B!", "1h ago")
                )
            )
        )
    }

    // Tutorials Database
    val tutorialsList = remember {
        mutableStateListOf(
            PrepTutorial(
                id = "tut_1",
                title = "5-Min Nightlife Glow",
                durationText = "05:12",
                creatorName = "Zama H.",
                creatorAvatar = "https://i.pravatar.cc/150?img=41",
                coverImage = "https://images.unsplash.com/photo-1522337660859-02fbefca4702?q=80&w=400&auto=format&fit=crop",
                category = "Makeup",
                likes = 1420,
                isLiked = false,
                description = "Get a high-shine, long-lasting glow perfect for low-light VIP lounges using minimal setting powder.",
                steps = listOf(
                    "Apply radiant primer to high points",
                    "Blend thin layer of water-based foundation",
                    "Dab metallic cream highlighter on cheekbones",
                    "Finish with lock-in setting spray"
                )
            ),
            PrepTutorial(
                id = "tut_2",
                title = "Clean Fade & Beard Prep",
                durationText = "03:45",
                creatorName = "Barber Lee",
                creatorAvatar = "https://i.pravatar.cc/150?img=59",
                coverImage = "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?q=80&w=400&auto=format&fit=crop",
                category = "Grooming",
                likes = 894,
                isLiked = true,
                description = "Quick hair prep and matte styling hacks to keep your fade sharp and flyaways flat in high-humidity dance floors.",
                steps = listOf(
                    "Wash beard with dry cleanser",
                    "Apply matte wax from roots up",
                    "Shape with wide-toothed wooden comb",
                    "Dab non-greasy beard oil for aroma"
                )
            ),
            PrepTutorial(
                id = "tut_3",
                title = "Luxury Acrylic Sculpting",
                durationText = "08:10",
                creatorName = "Nails by Chloe",
                creatorAvatar = "https://i.pravatar.cc/150?img=49",
                coverImage = "https://images.unsplash.com/photo-1604654894610-df4906b18502?q=80&w=400&auto=format&fit=crop",
                category = "Nails",
                likes = 2305,
                isLiked = false,
                description = "Chrome-tipped extensions designed to catch strobe lights and flash photography. Long-lasting bond secrets.",
                steps = listOf(
                    "Buff and clean natural nail beds",
                    "Secure lightweight acrylic tips",
                    "Apply double-coat silver chrome powder",
                    "UV-cure under 45W lamp for 90s"
                )
            )
        )
    }

    // Nearby Places Database
    val prepPlacesList = remember {
        mutableStateListOf(
            NearbyPrepPlace("p_1", "Elite Barber Rosebank", "Barber", "Oxford Rd, JHB", "0.8 km", 98, "https://images.unsplash.com/photo-1585747860715-2ba37e788b70?q=80&w=400&auto=format&fit=crop", 4.9f),
            NearbyPrepPlace("p_2", "Glow Nails Studio", "Nails", "Cradock Ave, JHB", "1.4 km", 94, "https://images.unsplash.com/photo-1604654894610-df4906b18502?q=80&w=400&auto=format&fit=crop", 4.7f),
            NearbyPrepPlace("p_3", "Sandton Glam Lounge", "Makeup", "Rivonia Rd, Sandton", "3.2 km", 89, "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?q=80&w=400&auto=format&fit=crop", 4.8f),
            NearbyPrepPlace("p_4", "Prestige Dry Cleaners", "Dry Cleaners", "Jan Smuts Ave, JHB", "1.1 km", 86, "https://images.unsplash.com/photo-1545173168-9f1947eebd01?q=80&w=400&auto=format&fit=crop", 4.5f)
        )
    }

    // Saved Collections state
    val savedCollections = remember {
        mutableStateListOf(
            SavedCollection("Luxury Looks", 14, "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?q=80&w=200&auto=format&fit=crop"),
            SavedCollection("Date Night Fits", 8, "https://images.unsplash.com/photo-1522337660859-02fbefca4702?q=80&w=200&auto=format&fit=crop"),
            SavedCollection("Weekend Glow", 22, "https://images.unsplash.com/photo-1562322140-8baeececf3df?q=80&w=200&auto=format&fit=crop")
        )
    }

    // Prep Tips database
    val editorialTips = remember {
        listOf(
            PrepTip("🌡 Cold Night Anticipated", "Temperatures will drop to 12°C. Consider pairing layers or styling a leather jacket. Check-in cloakroom is open at Sky Lounge.", "Weather", Icons.Default.Cloud),
            PrepTip("🎫 ID Requirements Strict", "And Club and Marble require original Physical SA ID or Passports for entry. Digital copies are strictly rejected tonight.", "Security", Icons.Default.AssignmentInd),
            PrepTip("🚗 Parking Alert", "Rooftop parking at Rosebank Mall is limited due to road construction. High recommendation to ride-share or pre-book secure Uber spots.", "Transport", Icons.Default.DriveEta),
            PrepTip("💄 Glow Refresh Station", "The Artistry has installed complimentary makeup and spray refresh bars in the main restrooms. Look out for the glowing mirrors.", "Styling", Icons.Default.Face)
        )
    }

    // Friend Prep Activities
    val friendsCircleActivity = remember {
        listOf(
            CircleActivity("Sarah Ndlovu", "https://i.pravatar.cc/150?img=32", "Posted Outfit Poll for tonight", "5 min ago", "poll"),
            CircleActivity("Jessica M.", "https://i.pravatar.cc/150?img=49", "Uploaded 5-Min Glow Makeup Look", "18 min ago", "tutorial"),
            CircleActivity("Marcus Vance", "https://i.pravatar.cc/150?img=11", "Completed 80% of Prep Checklist", "45 min ago", "checking"),
            CircleActivity("Sip Squad", "https://i.pravatar.cc/150?img=12", "4 Members actively prepping together", "Active", "active")
        )
    }

    // Camera Prep Mode overlay states
    var showCameraOverlay by remember { mutableStateOf(false) }
    var cameraSelectedFilter by remember { mutableStateOf("Cyber Neon") }
    val cameraFilters = listOf("None", "Cyber Neon", "Golden Hour", "Vintage Retro", "Mono Dream")
    var cameraCapturingState by remember { mutableStateOf("READY") } // "READY", "CAPTURED"

    // Custom Poll Creation Modal states
    var showCreatePollModal by remember { mutableStateOf(false) }
    var createPollCaption by remember { mutableStateOf("") }
    var createPollLookA by remember { mutableStateOf("") }
    var createPollLookB by remember { mutableStateOf("") }
    var createPollVenue by remember { mutableStateOf("AfroHaus Rooftop") }

    // Tutorial Theater Overlay
    var activeTutorialOverlay by remember { mutableStateOf<PrepTutorial?>(null) }

    // Venue Guide Selector Drawer
    var activeVenueGuide by remember { mutableStateOf<VenuePrepGuide?>(null) }
    val venueGuides = listOf(
        VenuePrepGuide(
            venueName = "AfroHaus Rooftop",
            dressCode = "Bohemian Luxury & Neon accents",
            theme = "Tropical Cyber Sunset",
            popularColors = listOf("Gold", "Emerald", "Coral Sunset"),
            popularShoes = "Breathable sneakers or premium slides",
            popularMakeup = "Dewy skin glow, holographic face gems",
            popularHairstyles = "Afro puffs, braided crowns, sleek high buns",
            weatherNote = "18°C clear skies with gentle evening breeze",
            parkingInfo = "VIP valet parking on Level 4 or Oxford Rd garage",
            ridePickup = "Dedicated Uber zone at North Gate B",
            openingHours = "16:00 - 02:00",
            entryRules = "No cargo shorts, age limit 23+, physical ID required"
        ),
        VenuePrepGuide(
            venueName = "And Club",
            dressCode = "All black, gothic minimal, industrial",
            theme = "Raw Industrial Techno",
            popularColors = listOf("Matte Black", "Asphalt Gray", "Neon Green lines"),
            popularShoes = "Chunky platform boots or flat skate shoes",
            popularMakeup = "Smokey heavy eyeliner, dark matte lips",
            popularHairstyles = "Textured buzz cut, slicked back wet-look",
            weatherNote = "Underground air-conditioned dance vault (19°C)",
            parkingInfo = "Street parking on Newtown Mall loop",
            ridePickup = "Drop-off bay outside main steel gates",
            openingHours = "21:00 - 05:00",
            entryRules = "Strict no-photo policy, original physical ID only"
        ),
        VenuePrepGuide(
            venueName = "The Artistry",
            dressCode = "Smart elegant cocktail attire",
            theme = "Acoustic Excellence Gala",
            popularColors = listOf("Pure White", "Royal Velvet Blue", "Black Onyx"),
            popularShoes = "Heeled mules, structured loafers",
            popularMakeup = "Classic bold red lip, soft sculpted brows",
            popularHairstyles = "Hollywood retro waves, clean tapered fade",
            weatherNote = "Heated indoor lounge with covered patio",
            parkingInfo = "Secure underground basement parkade",
            ridePickup = "Main entrance Porte-Cochère",
            openingHours = "18:00 - 03:00",
            entryRules = "Collar shirts mandatory for gents, advance booking advised"
        )
    )

    // Dynamic Filtered Feeds
    val filteredPolls = remember(outfitPolls, searchQuery, activeFilters, selectedTab) {
        outfitPolls.filter { poll ->
            val matchesQuery = searchQuery.isBlank() ||
                poll.caption.contains(searchQuery, ignoreCase = true) ||
                poll.authorName.contains(searchQuery, ignoreCase = true) ||
                poll.venueName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = activeFilters.isEmpty() || activeFilters.any { filter ->
                val f = filter.lowercase()
                when {
                    f.contains("look") -> true
                    f.contains("makeup") -> poll.caption.contains("makeup", ignoreCase = true)
                    f.contains("hair") -> poll.caption.contains("hair", ignoreCase = true)
                    f.contains("shoe") -> poll.caption.contains("shoe", ignoreCase = true)
                    else -> true
                }
            }
            matchesQuery && matchesFilter
        }
    }

    val filteredTutorials = remember(tutorialsList, searchQuery, activeFilters) {
        tutorialsList.filter { tut ->
            val matchesQuery = searchQuery.isBlank() ||
                tut.title.contains(searchQuery, ignoreCase = true) ||
                tut.creatorName.contains(searchQuery, ignoreCase = true) ||
                tut.category.contains(searchQuery, ignoreCase = true)

            val matchesFilter = activeFilters.isEmpty() || activeFilters.any { filter ->
                val f = filter.lowercase()
                when {
                    f.contains("makeup") -> tut.category.equals("Makeup", ignoreCase = true)
                    f.contains("hair") -> tut.category.equals("Hair", ignoreCase = true) || tut.category.equals("Grooming", ignoreCase = true)
                    f.contains("nail") -> tut.category.equals("Nails", ignoreCase = true)
                    f.contains("tutorial") -> true
                    else -> tut.title.contains(filter, ignoreCase = true)
                }
            }
            matchesQuery && matchesFilter
        }
    }

    val filteredPlaces = remember(prepPlacesList, searchQuery, activeFilters) {
        prepPlacesList.filter { place ->
            val matchesQuery = searchQuery.isBlank() ||
                place.name.contains(searchQuery, ignoreCase = true) ||
                place.type.contains(searchQuery, ignoreCase = true) ||
                place.address.contains(searchQuery, ignoreCase = true)

            val matchesFilter = activeFilters.isEmpty() || activeFilters.any { filter ->
                val f = filter.lowercase()
                when {
                    f.contains("hair") -> place.type.contains("Barber", ignoreCase = true) || place.type.contains("Salon", ignoreCase = true)
                    f.contains("nail") -> place.type.contains("Nails", ignoreCase = true)
                    f.contains("makeup") -> place.type.contains("Makeup", ignoreCase = true)
                    else -> place.type.contains(filter, ignoreCase = true)
                }
            }
            matchesQuery && matchesFilter
        }
    }

    // Layout Scaffold
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(themeBg),
        color = themeBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("prep_rooms_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Prep Rooms", tint = Color.White)
                    }
                    Text(
                        text = "Prep Rooms Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.testTag("prep_rooms_title")
                    )
                    Row {
                        IconButton(
                            onClick = { showSquadChatModal = true },
                            modifier = Modifier.testTag("squad_chat_button")
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Squad Chat", tint = neonCyan)
                        }
                        IconButton(
                            onClick = {
                                showCameraOverlay = true
                                cameraCapturingState = "READY"
                            },
                            modifier = Modifier.testTag("camera_mode_button")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera Prep Mode", tint = neonPink)
                        }
                    }
                }

                // SEARCH BAR
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search looks, tutorials, styling tips...", color = Color.White.copy(alpha = 0.4f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("prep_rooms_search_bar"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = neonCyan.copy(alpha = 0.8f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // DISCOVERY TABS
                ScrollableTabRow(
                    selectedTabIndex = when (selectedTab) {
                        "Following" -> 0
                        "Trending" -> 1
                        "Nearby" -> 2
                        "Events" -> 3
                        else -> 0
                    },
                    containerColor = themeBg,
                    contentColor = Color.White,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[
                                when (selectedTab) {
                                    "Following" -> 0
                                    "Trending" -> 1
                                    "Nearby" -> 2
                                    "Events" -> 3
                                    else -> 0
                                }
                            ]),
                            color = neonCyan
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == "Following",
                        onClick = { selectedTab = "Following" },
                        text = { Text("👥 Following", fontSize = 13.sp) },
                        modifier = Modifier.testTag("tab_following")
                    )
                    Tab(
                        selected = selectedTab == "Trending",
                        onClick = { selectedTab = "Trending" },
                        text = { Text("🔥 Trending", fontSize = 13.sp) },
                        modifier = Modifier.testTag("tab_trending")
                    )
                    Tab(
                        selected = selectedTab == "Nearby",
                        onClick = { selectedTab = "Nearby" },
                        text = { Text("📍 Nearby Places", fontSize = 13.sp) },
                        modifier = Modifier.testTag("tab_nearby")
                    )
                    Tab(
                        selected = selectedTab == "Events",
                        onClick = { selectedTab = "Events" },
                        text = { Text("📅 Event Hubs", fontSize = 13.sp) },
                        modifier = Modifier.testTag("tab_events")
                    )
                }

                // FILTER CHIP ROW
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterChips) { chip ->
                        val isSelected = activeFilters.contains(chip)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) activeFilters.remove(chip)
                                else activeFilters.add(chip)
                            },
                            label = { Text(chip, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = neonCyan,
                                containerColor = cardBg,
                                selectedLabelColor = Color.Black
                            ),
                            border = BorderStroke(1.dp, if (isSelected) neonCyan else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.testTag("filter_chip_${chip.replace(" ", "_")}")
                        )
                    }
                }

                // MAIN CONTENT LIST
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. TONIGHT'S PLAN & COUNTDOWN CARD
                    if (selectedTab == "Following" || selectedTab == "Events") {
                        item {
                            TonightPlanWidget(
                                hasPlan = hasTonightPlan,
                                countdownSeconds = countdownSeconds,
                                readyPercentage = readyPercentage,
                                onTogglePlan = { hasTonightPlan = !hasTonightPlan },
                                onChecklistClick = { showSquadChatModal = true }
                            )
                        }
                    }

                    // 2. SQUAD PREPARATION CHECKLIST SESSION
                    if (selectedTab == "Following" || selectedTab == "Events") {
                        item {
                            SquadChecklistWidget(
                                items = checklistItems,
                                readyPercentage = readyPercentage,
                                onToggleItem = { id ->
                                    val index = checklistItems.indexOfFirst { it.id == id }
                                    if (index != -1) {
                                        val current = checklistItems[index]
                                        checklistItems[index] = current.copy(isChecked = !current.isChecked)
                                    }
                                },
                                onAddItemClick = { showAddChecklistDialog = true }
                            )
                        }
                    }

                    // 3. YOUR CIRCLE ACTIVITY STORIES FEED
                    if (selectedTab == "Following") {
                        item {
                            YourCirclePreppingWidget(
                                activities = friendsCircleActivity,
                                onFriendClick = { name ->
                                    Toast.makeText(context, "$name is currently getting ready for Friday Night!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    // 4. OUTFIT POLLS SECTION
                    if (selectedTab == "Following" || selectedTab == "Trending" || selectedTab == "Events") {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Outfit Polls Hub", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Help friends choose their looks", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { showCreatePollModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = neonPink),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("ask_squad_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ask Squad", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (filteredPolls.isEmpty()) {
                                    Text("No outfit polls match your active filters or search query.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                } else {
                                    filteredPolls.forEach { poll ->
                                        OutfitPollCard(
                                            poll = poll,
                                            onVote = { option ->
                                                val idx = outfitPolls.indexOfFirst { it.id == poll.id }
                                                if (idx != -1) {
                                                    val p = outfitPolls[idx]
                                                    if (p.userVoted == null) {
                                                        if (option == "A") {
                                                            outfitPolls[idx] = p.copy(votesA = p.votesA + 1, userVoted = "A")
                                                        } else {
                                                            outfitPolls[idx] = p.copy(votesB = p.votesB + 1, userVoted = "B")
                                                        }
                                                        Toast.makeText(context, "🎉 Vote counted for ${if (option == "A") p.lookAName else p.lookBName}!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onAddComment = { commentText ->
                                                val idx = outfitPolls.indexOfFirst { it.id == poll.id }
                                                if (idx != -1 && commentText.isNotBlank()) {
                                                    val p = outfitPolls[idx]
                                                    p.comments.add(PrepComment("c_${System.currentTimeMillis()}", "You", commentText, "Just now"))
                                                    // Trigger compose recomposition
                                                    outfitPolls[idx] = p.copy()
                                                    Toast.makeText(context, "Comment posted!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 5. TUTORIALS SECTION
                    if (selectedTab == "Trending" || selectedTab == "Following") {
                        item {
                            Column {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Text("Glam & Grooming Masterclass", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Learn nightlife looks & styles", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                if (filteredTutorials.isEmpty()) {
                                    Text("No tutorials found for current query.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                                } else {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(filteredTutorials) { tutorial ->
                                            TutorialItemCard(
                                                tutorial = tutorial,
                                                onClick = { activeTutorialOverlay = tutorial },
                                                onLikeToggle = {
                                                    val idx = tutorialsList.indexOfFirst { it.id == tutorial.id }
                                                    if (idx != -1) {
                                                        val t = tutorialsList[idx]
                                                        if (t.isLiked) {
                                                            tutorialsList[idx] = t.copy(likes = t.likes - 1, isLiked = false)
                                                        } else {
                                                            tutorialsList[idx] = t.copy(likes = t.likes + 1, isLiked = true)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. VENUE SPECIFIC PREP GUIDES
                    if (selectedTab == "Events" || selectedTab == "Trending") {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text("Official Venue Dress Guides", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Aesthetic codes & community tips", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                venueGuides.forEach { guide ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeVenueGuide = guide }
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Verified, contentDescription = "Verified Guide", tint = luxGold, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(guide.venueName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Dress Code: ${guide.dressCode}", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. EDITORIAL PREPARATION TIPS
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text("Tonight's Prep Broadcast", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Crucial weather, entry guidelines, and safety alerts", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            editorialTips.forEach { tip ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardBg, RoundedCornerShape(12.dp))
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(neonCyan.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(tip.icon, contentDescription = null, tint = neonCyan, modifier = Modifier.size(18.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tip.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(tip.content, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // 8. NEARBY BEAUTY & PREPARATION PLACES (SMART PLACES SYNC)
                    if (selectedTab == "Nearby" || selectedTab == "Trending") {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text("Nearby Preparation Partners", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Salons, barbers, and stores synced with Smart Places", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                if (filteredPlaces.isEmpty()) {
                                    Text("No nearby prep places match current criteria.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                } else {
                                    filteredPlaces.forEach { place ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(cardBg, RoundedCornerShape(16.dp))
                                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(16.dp))
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = place.image,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(place.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${place.type} • ${place.address}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = luxGold, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(place.rating.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text("Match ${place.matchRate}%", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "📍 Launching routing to ${place.name} (${place.distance})", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Route", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 9. SAVED PREP COLLECTIONS
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Your Saved Collections", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Reusable style boards and outfit checklists", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                                IconButton(onClick = { showCreateCollectionDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Collection", tint = neonCyan)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(savedCollections) { col ->
                                    Column(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .clickable {
                                                Toast.makeText(context, "Opened Saved Board: ${col.name}", Toast.LENGTH_SHORT).show()
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AsyncImage(
                                            model = col.coverImage,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(col.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${col.count} items", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------
            // OVERLAY: CAMERA PREP FILTER HUD
            // -------------------------------------------------------------------------
            if (showCameraOverlay) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Live camera lens preview with selected filter
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?q=80&w=600&auto=format&fit=crop",
                            contentDescription = "Camera viewfinder",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Apply filter tint
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    when (cameraSelectedFilter) {
                                        "Cyber Neon" -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                                        "Golden Hour" -> Color(0xFFD4AF37).copy(alpha = 0.2f)
                                        "Vintage Retro" -> Color(0xFF8B4513).copy(alpha = 0.15f)
                                        "Mono Dream" -> Color.Gray.copy(alpha = 0.25f)
                                        else -> Color.Transparent
                                    }
                                )
                        )

                        // Camera guidelines overlay HUD
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = neonPink,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "📸 PREP CAMERA MODE",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showCameraOverlay = false },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }

                            // Middle focus rectangle
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .border(BorderStroke(2.dp, neonCyan.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    "OUTFIT CHECK",
                                    color = neonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(8.dp)
                                )
                            }

                            // Filter selection row and shutter
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(cameraFilters) { filter ->
                                        val filterSelected = cameraSelectedFilter == filter
                                        Surface(
                                            modifier = Modifier.clickable { cameraSelectedFilter = filter },
                                            color = if (filterSelected) neonCyan else Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Text(
                                                text = filter,
                                                color = if (filterSelected) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (cameraCapturingState == "READY") {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .clickable {
                                                    cameraCapturingState = "CAPTURED"
                                                    Toast.makeText(context, "📸 Mirror selfie captured with $cameraSelectedFilter!", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(6.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().border(2.dp, Color.Black, CircleShape))
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                outfitPolls.add(
                                                    0,
                                                    PrepLookPoll(
                                                        id = "poll_custom_${System.currentTimeMillis()}",
                                                        authorName = "You (Me)",
                                                        authorAvatar = "https://i.pravatar.cc/150?img=12",
                                                        caption = "My outfit check using $cameraSelectedFilter filter. Ready for tonight?",
                                                        lookAName = "My Outfit",
                                                        lookAImage = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?q=80&w=400&auto=format&fit=crop",
                                                        lookBName = "Original Concept",
                                                        lookBImage = "https://images.unsplash.com/photo-1509631179647-0177331693ae?q=80&w=400&auto=format&fit=crop",
                                                        votesA = 1,
                                                        votesB = 0,
                                                        userVoted = "A",
                                                        venueName = createPollVenue
                                                    )
                                                )
                                                showCameraOverlay = false
                                                Toast.makeText(context, "🚀 Selfie uploaded to the public Prep Rooms Feed!", Toast.LENGTH_LONG).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = successGreen)
                                        ) {
                                            Text("Post to Prep Rooms Feed", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        TextButton(onClick = { cameraCapturingState = "READY" }) {
                                            Text("Retake", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------
            // OVERLAY: SQUAD GROUP CHAT MODAL
            // -------------------------------------------------------------------------
            if (showSquadChatModal) {
                AlertDialog(
                    onDismissRequest = { showSquadChatModal = false },
                    confirmButton = {
                        Button(
                            onClick = { showSquadChatModal = false },
                            colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = neonCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sip Squad Prep Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(squadChatMessages) { msg ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        if (!msg.isMe) {
                                            AsyncImage(
                                                model = msg.senderAvatar,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Column(
                                            horizontalAlignment = if (msg.isMe) Alignment.End else Alignment.Start
                                        ) {
                                            if (!msg.isMe) {
                                                Text(msg.senderName, color = neonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Surface(
                                                color = if (msg.isMe) neonCyan else cardBg,
                                                shape = RoundedCornerShape(12.dp),
                                                border = if (!msg.isMe) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null
                                            ) {
                                                Text(
                                                    text = msg.text,
                                                    color = if (msg.isMe) Color.Black else Color.White,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                            Text(msg.timestamp, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Message input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = squadChatInputText,
                                    onValueChange = { squadChatInputText = it },
                                    placeholder = { Text("Chat with squad...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (squadChatInputText.isNotBlank()) {
                                            squadChatMessages.add(
                                                SquadChatMessage(
                                                    id = "m_${System.currentTimeMillis()}",
                                                    senderName = "You",
                                                    senderAvatar = "https://i.pravatar.cc/150?img=12",
                                                    text = squadChatInputText,
                                                    timestamp = "Just now",
                                                    isMe = true
                                                )
                                            )
                                            squadChatInputText = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send", tint = neonPink)
                                }
                            }
                        }
                    },
                    containerColor = Color(0xFF0D1220),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // -------------------------------------------------------------------------
            // OVERLAY: ADD CHECKLIST ITEM DIALOG
            // -------------------------------------------------------------------------
            if (showAddChecklistDialog) {
                AlertDialog(
                    onDismissRequest = { showAddChecklistDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newChecklistItemName.isNotBlank()) {
                                    checklistItems.add(
                                        ChecklistItem("chk_custom_${System.currentTimeMillis()}", newChecklistItemName, false)
                                    )
                                    newChecklistItemName = ""
                                    showAddChecklistDialog = false
                                    Toast.makeText(context, "Item added to squad checklist!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
                        ) {
                            Text("Add Item", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddChecklistDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    title = { Text("Add Checklist Item", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = newChecklistItemName,
                            onValueChange = { newChecklistItemName = it },
                            placeholder = { Text("e.g. Bring power bank") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    },
                    containerColor = cardBg,
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // -------------------------------------------------------------------------
            // OVERLAY: CREATE SAVED COLLECTION DIALOG
            // -------------------------------------------------------------------------
            if (showCreateCollectionDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateCollectionDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newCollectionName.isNotBlank()) {
                                    savedCollections.add(
                                        SavedCollection(newCollectionName, 0, "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?q=80&w=200&auto=format&fit=crop")
                                    )
                                    newCollectionName = ""
                                    showCreateCollectionDialog = false
                                    Toast.makeText(context, "New style collection created!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonPink)
                        ) {
                            Text("Create Collection", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateCollectionDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    title = { Text("New Style Collection Board", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = newCollectionName,
                            onValueChange = { newCollectionName = it },
                            placeholder = { Text("e.g. Festival Fits 2026") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    },
                    containerColor = cardBg,
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // -------------------------------------------------------------------------
            // OVERLAY: DETAILED TUTORIAL THEATER & STEPS DRAWER
            // -------------------------------------------------------------------------
            if (activeTutorialOverlay != null) {
                val tutorial = activeTutorialOverlay!!
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = tutorial.coverImage,
                            contentDescription = "Tutorial Video stream",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                                        startY = 100f
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = tutorial.creatorAvatar,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(tutorial.creatorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Verified Styling Creator", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                IconButton(
                                    onClick = { activeTutorialOverlay = null },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Player", tint = Color.White)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = tutorial.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = tutorial.description,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.9f)),
                                    border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("STEP-BY-STEP LOOK PREP", color = neonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        tutorial.steps.forEachIndexed { idx, step ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(neonCyan),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text((idx + 1).toString(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text(step, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "Saved tutorial to 'Weekend Glow' collection!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = neonPink)
                                    ) {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Look", fontWeight = FontWeight.Bold)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Favorite, contentDescription = "Likes", tint = neonPink, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${tutorial.likes} Likes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------
            // OVERLAY: DETAILED VENUE PREP DRAWERS
            // -------------------------------------------------------------------------
            if (activeVenueGuide != null) {
                val guide = activeVenueGuide!!
                AlertDialog(
                    onDismissRequest = { activeVenueGuide = null },
                    confirmButton = {
                        Button(
                            onClick = { activeVenueGuide = null },
                            colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
                        ) {
                            Text("Got it!", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = luxGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(guide.venueName, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Nightlife Dress Guide & Aesthetics",
                                color = neonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                VenueGuideItem("👗 Dress Code", guide.dressCode, neonCyan)
                                VenueGuideItem("🔮 Theme Night", guide.theme, neonPink)
                                VenueGuideItem("🎨 Popular Colors", guide.popularColors.joinToString(", "), luxGold)
                                VenueGuideItem("👞 Trending Footwear", guide.popularShoes, Color.White)
                                VenueGuideItem("💄 Makeup Accents", guide.popularMakeup, Color.White)
                                VenueGuideItem("💇 Hairstyles", guide.popularHairstyles, Color.White)
                                VenueGuideItem("🌤 Weather Note", guide.weatherNote, Color.White)
                                VenueGuideItem("🚗 Parking", guide.parkingInfo, Color.White)
                                VenueGuideItem("🚕 Ride Pickup", guide.ridePickup, Color.White)
                                VenueGuideItem("⏱ Opening Hours", guide.openingHours, Color.White)
                                VenueGuideItem("⚠️ Entry Policy", guide.entryRules, neonPink)
                            }
                        }
                    },
                    containerColor = cardBg,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // -------------------------------------------------------------------------
            // OVERLAY: CUSTOM POLL CREATOR DIALOG
            // -------------------------------------------------------------------------
            if (showCreatePollModal) {
                AlertDialog(
                    onDismissRequest = { showCreatePollModal = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (createPollCaption.isBlank()) {
                                    Toast.makeText(context, "Please enter a question caption!", Toast.LENGTH_SHORT).show()
                                } else {
                                    outfitPolls.add(
                                        0,
                                        PrepLookPoll(
                                            id = "poll_custom_${System.currentTimeMillis()}",
                                            authorName = "You (Me)",
                                            authorAvatar = "https://i.pravatar.cc/150?img=12",
                                            caption = createPollCaption,
                                            lookAName = if (createPollLookA.isBlank()) "Look A" else createPollLookA,
                                            lookAImage = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?q=80&w=400&auto=format&fit=crop",
                                            lookBName = if (createPollLookB.isBlank()) "Look B" else createPollLookB,
                                            lookBImage = "https://images.unsplash.com/photo-1509631179647-0177331693ae?q=80&w=400&auto=format&fit=crop",
                                            votesA = 0,
                                            votesB = 0,
                                            userVoted = null,
                                            venueName = createPollVenue,
                                            comments = mutableListOf()
                                        )
                                    )
                                    showCreatePollModal = false
                                    Toast.makeText(context, "🎉 Outfit Poll posted to followers & prep session!", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonPink)
                        ) {
                            Text("Post Poll", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePollModal = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    title = {
                        Text("Create Outfit Poll", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = createPollCaption,
                                onValueChange = { createPollCaption = it },
                                label = { Text("What is your styling question?") },
                                placeholder = { Text("e.g. Which shoes match the vibe?") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = createPollLookA,
                                onValueChange = { createPollLookA = it },
                                label = { Text("Name of Look A (Optional)") },
                                placeholder = { Text("e.g. Heeled Mules") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = createPollLookB,
                                onValueChange = { createPollLookB = it },
                                label = { Text("Name of Look B (Optional)") },
                                placeholder = { Text("e.g. Chunky Boots") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = createPollVenue,
                                onValueChange = { createPollVenue = it },
                                label = { Text("Target Venue") },
                                placeholder = { Text("e.g. AfroHaus Sunset") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    },
                    containerColor = cardBg,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPOSABLE COMPONENT PLUGINS
// -------------------------------------------------------------------------

@Composable
fun VenueGuideItem(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
fun TonightPlanWidget(
    hasPlan: Boolean,
    countdownSeconds: Int,
    readyPercentage: Int,
    onTogglePlan: () -> Unit,
    onChecklistClick: () -> Unit
) {
    val cardBg = Color(0xFF0F1524)
    val neonCyan = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF2D55)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (hasPlan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF32D74B))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "TONIGHT'S SQUAD PLAN",
                            color = Color(0xFF32D74B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    TextButton(onClick = onTogglePlan, contentPadding = PaddingValues(0.dp)) {
                        Text("Reset Plan", color = neonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("AfroHaus Rooftop Sessions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))

                // Real-time Countdown formatting
                val hours = countdownSeconds / 3600
                val mins = (countdownSeconds % 3600) / 60
                val secs = countdownSeconds % 60
                val formattedTime = String.format("%02d:%02d:%02d", hours, mins, secs)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Starts in", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        Text(
                            text = formattedTime,
                            color = neonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("6 Friends Going", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        Text("Squad Active", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color.White.copy(alpha = 0.1f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx())
                            )
                            drawArc(
                                color = neonCyan,
                                startAngle = -90f,
                                sweepAngle = (readyPercentage / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                        Text("$readyPercentage%", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Squad Readiness Rating", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Checkbox steps coordinate together", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Button(
                        onClick = onChecklistClick,
                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Squad Chat", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

            } else {
                Text("Who's Going Out Tonight?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("You have no registered prep rooms. Build a plan or join a nearby preparation event below to coordinate.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onTogglePlan,
                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("AfroHaus Rooftop Plan", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Browse Events", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SquadChecklistWidget(
    items: List<ChecklistItem>,
    readyPercentage: Int,
    onToggleItem: (String) -> Unit,
    onAddItemClick: () -> Unit
) {
    val cardBg = Color(0xFF0F1524)
    val neonCyan = Color(0xFF00E5FF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tonight's Shared Checklist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Coordinate preparation with your squad", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddItemClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item", tint = neonCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = neonCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "$readyPercentage% READY",
                            color = neonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleItem(item.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (item.isChecked) neonCyan else Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.isChecked) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            }
                        }

                        Text(
                            text = item.name,
                            color = if (item.isChecked) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (item.isChecked) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YourCirclePreppingWidget(
    activities: List<CircleActivity>,
    onFriendClick: (String) -> Unit
) {
    val cardBg = Color(0xFF0F1524)
    val neonCyan = Color(0xFF00E5FF)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Your Circle Prep Rooms", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("See where friends are getting ready", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                activities.forEach { act ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFriendClick(act.friendName) },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = act.friendAvatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(act.friendName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(act.statusText, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                        Text(act.relativeTime, color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun OutfitPollCard(
    poll: PrepLookPoll,
    onVote: (String) -> Unit,
    onAddComment: (String) -> Unit
) {
    val cardBg = Color(0xFF0F1524)
    val neonCyan = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF2D55)

    var commentInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = poll.authorAvatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(poll.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Prepping for ${poll.venueName}", color = neonCyan, fontSize = 11.sp)
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "VOTE",
                        color = neonPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(poll.caption, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // LOOKS COMPARISON ROW (Look A vs Look B)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Look A
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onVote("A") },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = poll.lookAImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                BorderStroke(
                                    2.dp,
                                    if (poll.userVoted == "A") neonCyan else Color.Transparent
                                ), RoundedCornerShape(12.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(poll.lookAName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    if (poll.userVoted != null) {
                        val total = poll.votesA + poll.votesB
                        val pct = if (total == 0) 0 else (poll.votesA * 100) / total
                        Text("$pct% ($total Votes)", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(top = 4.dp),
                            color = neonCyan,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    } else {
                        Text("Tap to Vote", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    }
                }

                // Look B
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onVote("B") },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = poll.lookBImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                BorderStroke(
                                    2.dp,
                                    if (poll.userVoted == "B") neonCyan else Color.Transparent
                                ), RoundedCornerShape(12.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(poll.lookBName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    if (poll.userVoted != null) {
                        val total = poll.votesA + poll.votesB
                        val pct = if (total == 0) 0 else (poll.votesB * 100) / total
                        Text("$pct% ($total Votes)", color = neonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(top = 4.dp),
                            color = neonPink,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    } else {
                        Text("Tap to Vote", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    }
                }
            }

            // Comments section & comment input
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                if (poll.comments.isNotEmpty()) {
                    poll.comments.forEach { comment ->
                        Text(
                            text = "${comment.author}: ${comment.text}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        placeholder = { Text("Add styling advice...", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (commentInput.isNotBlank()) {
                                onAddComment(commentInput)
                                commentInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = neonCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialItemCard(
    tutorial: PrepTutorial,
    onClick: () -> Unit,
    onLikeToggle: () -> Unit
) {
    val cardBg = Color(0xFF0F1524)
    val neonCyan = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF2D55)

    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = tutorial.coverImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    color = neonCyan,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = tutorial.category.uppercase(),
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = tutorial.durationText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = tutorial.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "by ${tutorial.creatorName}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLikeToggle() }
                    ) {
                        Icon(
                            imageVector = if (tutorial.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (tutorial.isLiked) neonPink else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            tutorial.likes.toString(),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    Text("Watch Look", color = neonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
