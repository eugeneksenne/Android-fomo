package com.example.feature.map

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.core.data.ExploreVenue
import com.example.core.data.VenueRepository
import com.example.core.data.CircleFriend
import com.example.core.data.MyCircleRepository

// Coordinates Mapping
data class MapCoordinates(val latitude: Double, val longitude: Double)

fun getVenueCoordinates(venueId: String): MapCoordinates {
    return when (venueId) {
        "fomo_club" -> MapCoordinates(-26.1452, 28.0472)
        "d48_midrand" -> MapCoordinates(-25.9981, 28.1263)
        "konka_soweto" -> MapCoordinates(-26.2561, 27.8542)
        "taboo_sandton" -> MapCoordinates(-26.1044, 28.0581)
        "marble_rosebank" -> MapCoordinates(-26.1461, 28.0432)
        "proud_mary" -> MapCoordinates(-26.1445, 28.0454)
        "legend_barber" -> MapCoordinates(-26.1456, 28.0421)
        "sorbet_salon" -> MapCoordinates(-26.1072, 28.0524)
        "sanctuary_spa" -> MapCoordinates(-26.1085, 28.0551)
        "four_seasons_westcliff" -> MapCoordinates(-26.1643, 28.0285)
        "shell_select_rosebank" -> MapCoordinates(-26.1481, 28.0376)
        else -> MapCoordinates(-26.1452 + (Math.random() - 0.5) * 0.02, 28.0472 + (Math.random() - 0.5) * 0.02)
    }
}

val defaultFomoClubVenue = ExploreVenue(
    id = "fomo_club",
    name = "FOMO Club",
    category = "Nightlife",
    subcategory = "VIP Lounge & Nightclub",
    imageUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=600&auto=format&fit=crop",
    isVerified = true,
    rating = 4.9f,
    reviewCount = 2431,
    address = "Rosebank, Johannesburg",
    area = "Rosebank",
    distanceText = "0.2 km away",
    attributes = listOf("🔴 LIVE Now", "Amapiano", "3D Light Mapping", "Rooftop Glasshouse"),
    openDays = "Open Now",
    startHour = 21,
    endHour = 4,
    is24Hours = false,
    websiteUrl = "https://fomoapp.live",
    hasClubLobby = true
)

sealed interface SelectedMapItem {
    data class Venue(val venue: ExploreVenue) : SelectedMapItem
    data class Friend(val friend: CircleFriend) : SelectedMapItem
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(
    onNavigateToLobby: (String) -> Unit = {},
    onNavigateToNightGuard: () -> Unit = {}
) {
    val context = LocalContext.current
    val staticVenues by VenueRepository.exploreVenuesState.collectAsState()
    val friends by MyCircleRepository.friendsState.collectAsState()

    // Custom Session Added Venues to keep state during session
    val customAddedVenues = remember { mutableStateListOf<ExploreVenue>() }
    val allVenues = remember(staticVenues, customAddedVenues) {
        staticVenues + customAddedVenues
    }

    // Modal and Panel States
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedMapItem by remember { mutableStateOf<SelectedMapItem?>(null) }
    var isHeatmapEnabled by remember { mutableStateOf(true) }
    var bottomTabSelection by remember { mutableStateOf("Venues") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var isSearchOpen by remember { mutableStateOf(false) }
    var isNotificationsOpen by remember { mutableStateOf(false) }
    var isProfileOpen by remember { mutableStateOf(false) }
    var isAddPlaceOpen by remember { mutableStateOf(false) }

    // Dynamic Top Bar city status cycle index
    var cityStatusIndex by remember { mutableStateOf(0) }
    val cityStatuses = listOf(
        "🔥 Johannesburg Pulse",
        "⚡ Friday Night Vibes",
        "🌙 Sandton Nightlife",
        "🟢 NightGuard Active"
    )

    // Dynamic filtering for map listings and nearest cards
    val filteredVenues = remember(selectedCategory, allVenues) {
        val clean = selectedCategory.replace(Regex("[^\\w\\s]"), "").trim().lowercase()
        when {
            clean == "all" -> allVenues
            clean == "wellness" -> allVenues.filter { it.category.lowercase() == "recover" || it.category.lowercase() == "wellness" }
            clean == "events" -> allVenues.filter { it.id == "fomo_club" || it.id == "d48_midrand" || it.id == "konka_soweto" || it.id.startsWith("evt_") }
            else -> allVenues.filter { it.category.lowercase() == clean }
        }
    }

    val nearestVenue = remember(selectedCategory, filteredVenues) {
        filteredVenues.maxByOrNull { it.rating } ?: defaultFomoClubVenue
    }

    // Javascript marker generator
    val venuesScriptBuilder = StringBuilder()
    allVenues.forEach { venue ->
        val coords = getVenueCoordinates(venue.id)
        val score = (venue.rating * 20).toInt()
        val hasFlashDrop = venue.id == "fomo_club" || venue.id == "d48_midrand"
        val isLive = venue.id == "fomo_club" || venue.id == "d48_midrand"
        val hasEvent = venue.id == "fomo_club" || venue.id == "d48_midrand" || venue.id == "konka_soweto" || venue.id.startsWith("evt_")
        val friendsCount = if (venue.id == "fomo_club") 4 else if (venue.id == "konka_soweto") 1 else 0
        val isSponsored = venue.id == "fomo_club" || venue.id == "four_seasons_westcliff"
        val isTrending = venue.id == "fomo_club" || venue.id == "d48_midrand" || venue.id == "konka_soweto"
        val isHot = venue.id == "fomo_club" || venue.id == "konka_soweto"
        val isClosingSoon = venue.id == "taboo_sandton"

        venuesScriptBuilder.append("""
            addVenueMarker(
                '${venue.id}', 
                '${venue.name.replace("'", "\\'")}', 
                ${coords.latitude}, 
                ${coords.longitude}, 
                '${venue.category}', 
                '${venue.subcategory}', 
                ${venue.rating}, 
                $score, 
                '${venue.imageUrl}',
                $hasFlashDrop,
                $isLive,
                $hasEvent,
                $friendsCount,
                $isSponsored,
                $isTrending,
                $isHot,
                $isClosingSoon
            );
        """.trimIndent())
    }

    val friendsScriptBuilder = StringBuilder()
    friends.forEach { friend ->
        friendsScriptBuilder.append("""
            addFriendMarker('${friend.id}', '${friend.name.replace("'", "\\'")}', ${friend.latitude}, ${friend.longitude}, '${friend.avatarUrl}', '${friend.status}', '${friend.currentActivity.replace("'", "\\'")}', ${friend.isCloseFriend});
        """.trimIndent())
    }

    val finalHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body, html { margin: 0; padding: 0; background-color: #0b0f19; width: 100%; height: 100%; overflow: hidden; }
                #map { height: 100%; width: 100%; background-color: #0b0f19; }
                
                /* Dark premium luxury OSM layout filters */
                .leaflet-tile {
                    filter: invert(100%) hue-rotate(190deg) brightness(50%) contrast(125%) saturate(130%);
                }
                .leaflet-zoom-animated {
                    transition: transform 0.45s cubic-bezier(0.25, 1, 0.5, 1);
                }
                .leaflet-control-attribution { display: none !important; }
                
                /* Glowing neon ring keyframes */
                @keyframes neon-glow-hot {
                    0% { transform: scale(0.96); box-shadow: 0 0 8px #FF2D55, inset 0 0 4px #FF2D55; }
                    100% { transform: scale(1.04); box-shadow: 0 0 18px #FF2D55, 0 0 25px #FF2D55; }
                }
                @keyframes neon-glow-trending {
                    0% { transform: scale(0.97); box-shadow: 0 0 6px #B026FF, inset 0 0 3px #B026FF; }
                    100% { transform: scale(1.03); box-shadow: 0 0 15px #B026FF, 0 0 22px #B026FF; }
                }
                @keyframes pulse-user {
                    0% { transform: scale(0.6); opacity: 0.9; }
                    100% { transform: scale(1.8); opacity: 0; }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([-26.115, 28.055], 13);

                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19
                }).addTo(map);

                var venueMarkers = {};
                var friendMarkers = {};
                var densityCircles = [];
                var routePolyline = null;

                // User pulsating dot marker
                var userMarker = L.marker([-26.147, 28.043], {
                    icon: L.divIcon({
                        className: 'user-loc',
                        html: "<div style='position:relative; width:34px; height:34px; display:flex; align-items:center; justify-content:center;'><div style='position:absolute; width:14px; height:14px; background-color:#00E5FF; border-radius:50%; border:2.5px solid #fff; box-shadow:0 0 10px #00E5FF; z-index:2;'></div><div style='position:absolute; width:32px; height:32px; background-color:rgba(0,229,255,0.35); border-radius:50%; animation:pulse-user 2s infinite; z-index:1;'></div></div>",
                        iconSize: [34, 34],
                        iconAnchor: [17, 17]
                    })
                }).addTo(map);

                // Professional addVenueMarker layout with badges & glowing rings
                function addVenueMarker(id, name, lat, lon, category, subcategory, rating, score, imageUrl, hasFlashDrop, isLive, hasEvent, friendsCount, isSponsored, isTrending, isHot, isClosingSoon) {
                    var borderStyle = "box-shadow: 0 0 10px #00E5FF, inset 0 0 5px #00E5FF; border: 2.5px solid #00E5FF;";
                    var animClass = "";
                    
                    if (isHot) {
                        borderStyle = "border: 2.5px solid #FF2D55;";
                        animClass = "animation: neon-glow-hot 1.6s infinite alternate;";
                    } else if (isTrending) {
                        borderStyle = "border: 2.5px solid #B026FF;";
                        animClass = "animation: neon-glow-trending 2.2s infinite alternate;";
                    } else if (isClosingSoon) {
                        borderStyle = "border: 2.5px solid #FF9500; box-shadow: 0 0 10px #FF9500;";
                    }

                    var iconHtml = "<div style='position:relative; width:52px; height:52px; display:flex; align-items:center; justify-content:center;'>";
                    iconHtml += "<div style='position:absolute; width:40px; height:40px; border-radius:50%; background:#000; " + borderStyle + " " + animClass + "'></div>";
                    iconHtml += "<div style='position:absolute; width:33px; height:33px; border-radius:50%; overflow:hidden; background:#151d30; display:flex; align-items:center; justify-content:center; z-index:1;'>";
                    if (imageUrl) {
                        iconHtml += "<img src='" + imageUrl + "' style='width:100%; height:100%; object-fit:cover;' />";
                    } else {
                        iconHtml += "<span style='color:white; font-size:10px;'>🎵</span>";
                    }
                    iconHtml += "</div>";

                    // Overlay badge setup
                    if (isLive) {
                        iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#FF2D55; color:white; font-size:6px; font-weight:bold; padding:1.5px 3.5px; border-radius:5px; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:2;'>🔴 LIVE</div>";
                    } else if (hasFlashDrop) {
                        iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#FF9500; font-size:9px; width:15px; height:15px; display:flex; align-items:center; justify-content:center; border-radius:50%; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:2;'>🎁</div>";
                    } else if (hasEvent) {
                        iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#00E5FF; font-size:9px; width:15px; height:15px; display:flex; align-items:center; justify-content:center; border-radius:50%; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:2;'>🎫</div>";
                    } else if (friendsCount > 0) {
                        iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#32D74B; color:black; font-size:7px; font-weight:bold; padding:1.5px 3.5px; border-radius:5px; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:2;'>👥 +" + friendsCount + "</div>";
                    } else if (isSponsored) {
                        iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#FFD700; color:black; font-size:8px; font-weight:bold; width:14px; height:14px; display:flex; align-items:center; justify-content:center; border-radius:50%; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:2;'>⭐</div>";
                    }
                    iconHtml += "</div>";

                    var venueIcon = L.divIcon({
                        className: 'venue-' + id,
                        html: iconHtml,
                        iconSize: [52, 52],
                        iconAnchor: [26, 26]
                    });

                    var marker = L.marker([lat, lon], { icon: venueIcon }).addTo(map);
                    marker.on('click', function() {
                        if (window.AndroidBridge) window.AndroidBridge.onVenueClick(id);
                    });

                    venueMarkers[id] = { marker: marker, category: category, lat: lat, lon: lon, score: score, hasEvent: hasEvent };
                }

                function addFriendMarker(id, name, lat, lon, avatarUrl, status, currentActivity, isCloseFriend) {
                    var color = status === "Online" ? "#32D74B" : "#8E8E93";
                    var iconHtml = "<div style='position:relative; width:44px; height:44px; display:flex; align-items:center; justify-content:center;'>";
                    iconHtml += "<div style='position:absolute; width:36px; height:36px; border-radius:50%; border:2.5px solid " + color + "; background:#111; box-shadow:0 3px 6px rgba(0,0,0,0.4);'></div>";
                    iconHtml += "<div style='position:absolute; width:29px; height:29px; border-radius:50%; overflow:hidden;'>";
                    iconHtml += "<img src='" + avatarUrl + "' style='width:100%; height:100%; object-fit:cover;' />";
                    iconHtml += "</div></div>";

                    var friendIcon = L.divIcon({
                        className: 'friend-' + id,
                        html: iconHtml,
                        iconSize: [44, 44],
                        iconAnchor: [22, 22]
                    });

                    var marker = L.marker([lat, lon], { icon: friendIcon }).addTo(map);
                    marker.on('click', function() {
                        if (window.AndroidBridge) window.AndroidBridge.onFriendClick(id);
                    });

                    friendMarkers[id] = { marker: marker, lat: lat, lon: lon };
                }

                function centerOn(lat, lon, zoom) {
                    map.flyTo([lat, lon], zoom || 15, { animate: true, duration: 0.85 });
                }

                function drawRoute(endLat, endLon) {
                    if (routePolyline) map.removeLayer(routePolyline);
                    var startLat = -26.147;
                    var startLon = 28.043;
                    routePolyline = L.polyline([[startLat, startLon], [endLat, endLon]], {
                        color: '#00E5FF',
                        weight: 4.5,
                        opacity: 0.85,
                        dashArray: '8, 12',
                        lineJoin: 'round'
                    }).addTo(map);
                    
                    var bounds = L.latLngBounds([[startLat, startLon], [endLat, endLon]]);
                    map.fitBounds(bounds, { padding: [60, 60] });
                }

                function clearRoute() {
                    if (routePolyline) { map.removeLayer(routePolyline); routePolyline = null; }
                }

                function toggleHeatmap(show) {
                    densityCircles.forEach(function(c) { map.removeLayer(c); });
                    densityCircles = [];
                    if (show) {
                        for (var id in venueMarkers) {
                            var v = venueMarkers[id];
                            var col = v.score > 85 ? "rgba(255, 45, 85, 0.22)" : "rgba(0, 229, 255, 0.15)";
                            var rad = v.score > 85 ? 420 : 280;
                            var circle = L.circle([v.lat, v.lon], { color: 'transparent', fillColor: col, fillOpacity: 0.45, radius: rad }).addTo(map);
                            densityCircles.push(circle);
                        }
                    }
                }

                function filterCategory(categoryName) {
                    clearRoute();
                    var clean = categoryName.replace(/[^\w\s]/g, '').trim().toLowerCase();
                    if (clean === "wellness") clean = "recover";
                    
                    for (var id in venueMarkers) {
                        var item = venueMarkers[id];
                        var itemCat = item.category.toLowerCase();
                        if (clean === "all" || itemCat === clean || (clean === "events" && item.hasEvent)) {
                            item.marker.addTo(map);
                        } else {
                            map.removeLayer(item.marker);
                        }
                    }
                }

                $venuesScriptBuilder
                $friendsScriptBuilder
                toggleHeatmap(true);
            </script>
        </body>
        </html>
    """.trimIndent()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0F19))) {

        // 1. Interactive OSM Base Layer
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun onVenueClick(venueId: String) {
                            post {
                                val matched = allVenues.find { it.id == venueId } ?: if (venueId == "fomo_club") defaultFomoClubVenue else null
                                if (matched != null) {
                                    selectedMapItem = SelectedMapItem.Venue(matched)
                                    val coords = getVenueCoordinates(matched.id)
                                    webViewRef?.evaluateJavascript("centerOn(${coords.latitude}, ${coords.longitude}, 15);", null)
                                }
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun onFriendClick(friendId: String) {
                            post {
                                val matched = friends.find { it.id == friendId }
                                if (matched != null) {
                                    selectedMapItem = SelectedMapItem.Friend(matched)
                                    webViewRef?.evaluateJavascript("centerOn(${matched.latitude}, ${matched.longitude}, 15);", null)
                                }
                            }
                        }
                    }, "AndroidBridge")
                    loadDataWithBaseURL("https://openstreetmap.org", finalHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. HUD Overlay Elements (Top-Down Flow)
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Top HUD Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0B0F19).copy(alpha = 0.95f), Color.Transparent)
                        )
                    )
            ) {
                // Floating Top Bar with action centers
                MapTopBar(
                    avatarUrl = "https://i.pravatar.cc/150?img=12",
                    cityStatus = cityStatuses[cityStatusIndex],
                    onCityStatusClick = {
                        cityStatusIndex = (cityStatusIndex + 1) % cityStatuses.size
                        Toast.makeText(context, "Vibe Context: ${cityStatuses[cityStatusIndex]}", Toast.LENGTH_SHORT).show()
                    },
                    onSearchClick = { isSearchOpen = true },
                    onNotificationsClick = { isNotificationsOpen = true },
                    onAvatarClick = { isProfileOpen = true }
                )

                // Category Chips Selector
                CategoryChips(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { cat ->
                        selectedCategory = cat
                        webViewRef?.evaluateJavascript("filterCategory('$cat');", null)
                    }
                )

                // Nearest Venue Card (Adaptive overlay based on Category Chip)
                NearestVenueCard(
                    venue = nearestVenue,
                    categoryLabel = selectedCategory,
                    onNavigateToLobby = onNavigateToLobby,
                    onRouteClick = {
                        val coords = getVenueCoordinates(nearestVenue.id)
                        webViewRef?.evaluateJavascript("drawRoute(${coords.latitude}, ${coords.longitude});", null)
                        Toast.makeText(context, "Simulating premium walking route to ${nearestVenue.name}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Selection card if a pin is active on map
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                when (val item = selectedMapItem) {
                    is SelectedMapItem.Venue -> {
                        VenueDetailsPanel(
                            venue = item.venue,
                            onNavigateToLobby = onNavigateToLobby,
                            onClose = {
                                selectedMapItem = null
                                webViewRef?.evaluateJavascript("clearRoute();", null)
                            },
                            onRoute = {
                                val coords = getVenueCoordinates(item.venue.id)
                                webViewRef?.evaluateJavascript("drawRoute(${coords.latitude}, ${coords.longitude});", null)
                            }
                        )
                    }
                    is SelectedMapItem.Friend -> {
                        FriendDetailsPanel(
                            friend = item.friend,
                            onClose = { selectedMapItem = null }
                        )
                    }
                    null -> {}
                }
            }

            // Bottom horizontal swiping selector carousel
            Surface(
                color = Color(0xFF0C1221).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    
                    // Sliding tab bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SegmentedTabButton(
                                title = "🔥 Live Spots",
                                isSelected = bottomTabSelection == "Venues",
                                onClick = { bottomTabSelection = "Venues" }
                            )
                            SegmentedTabButton(
                                title = "👥 Friends Map",
                                isSelected = bottomTabSelection == "Friends",
                                onClick = { bottomTabSelection = "Friends" }
                            )
                        }
                        
                        // Circle presence chip indicator
                        val onlineCount = friends.count { it.status == "Online" }
                        if (onlineCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF32D74B).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFF32D74B), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("$onlineCount LIVE", color = Color(0xFF32D74B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (bottomTabSelection == "Venues") {
                        LazyRow(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredVenues) { venue ->
                                val isCardExpanded = (selectedMapItem as? SelectedMapItem.Venue)?.venue?.id == venue.id
                                HorizontalVenueCard(
                                    venue = venue,
                                    isExpanded = isCardExpanded,
                                    onSelect = {
                                        selectedMapItem = SelectedMapItem.Venue(venue)
                                        val coords = getVenueCoordinates(venue.id)
                                        webViewRef?.evaluateJavascript("centerOn(${coords.latitude}, ${coords.longitude}, 15);", null)
                                    },
                                    onNavigateToLobby = onNavigateToLobby,
                                    onRouteClick = {
                                        val coords = getVenueCoordinates(venue.id)
                                        webViewRef?.evaluateJavascript("drawRoute(${coords.latitude}, ${coords.longitude});", null)
                                        Toast.makeText(context, "Routing to ${venue.name}...", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(friends) { friend ->
                                HorizontalFriendCard(
                                    friend = friend,
                                    onSelect = {
                                        selectedMapItem = SelectedMapItem.Friend(friend)
                                        webViewRef?.evaluateJavascript("centerOn(${friend.latitude}, ${friend.longitude}, 15);", null)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Floating Action HUD Overlays (SOS, Heatmap Vibe, Recenter, Add Event/Place)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 295.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            
            // Add Place or Event (➕) button
            FloatingActionButton(
                onClick = { isAddPlaceOpen = true },
                containerColor = Color(0xFF0F1524),
                contentColor = Color(0xFF00E5FF),
                modifier = Modifier
                    .size(48.dp)
                    .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Place to Map", modifier = Modifier.size(24.dp))
            }

            // SOS Urgency panic button
            FloatingActionButton(
                onClick = onNavigateToNightGuard,
                containerColor = Color(0xFFFF2D55),
                contentColor = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Warning, contentDescription = "SOS Rescue Guard", modifier = Modifier.size(22.dp))
            }

            // Vibe heat indicator switch
            FloatingActionButton(
                onClick = {
                    isHeatmapEnabled = !isHeatmapEnabled
                    webViewRef?.evaluateJavascript("toggleHeatmap($isHeatmapEnabled);", null)
                },
                containerColor = Color(0xFF0F1524),
                contentColor = if (isHeatmapEnabled) Color(0xFFFF2D55) else Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, if (isHeatmapEnabled) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.15f), CircleShape),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isHeatmapEnabled) Icons.Default.LocalFireDepartment else Icons.Outlined.LocalFireDepartment,
                    contentDescription = "Vibe Hotspots Map Heat",
                    modifier = Modifier.size(22.dp)
                )
            }

            // Recenter onto Sandton / Rosebank Location
            FloatingActionButton(
                onClick = {
                    webViewRef?.evaluateJavascript("centerOn(-26.115, 28.055, 13);", null)
                },
                containerColor = Color(0xFF0F1524),
                contentColor = Color(0xFF00E5FF),
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), CircleShape),
                shape = CircleShape
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter user location", modifier = Modifier.size(20.dp))
            }
        }

        // --- SUBPANELS AND MODAL DIALOGS ---

        // A. Universal Search Dialog Overlay
        if (isSearchOpen) {
            SearchOverlayDialog(
                venues = allVenues,
                friends = friends,
                onClose = { isSearchOpen = false },
                onSelectItem = { item ->
                    isSearchOpen = false
                    selectedMapItem = item
                    val lat: Double
                    val lon: Double
                    if (item is SelectedMapItem.Venue) {
                        val coords = getVenueCoordinates(item.venue.id)
                        lat = coords.latitude
                        lon = coords.longitude
                        webViewRef?.evaluateJavascript("centerOn($lat, $lon, 15); drawRoute($lat, $lon);", null)
                    } else if (item is SelectedMapItem.Friend) {
                        lat = item.friend.latitude
                        lon = item.friend.longitude
                        webViewRef?.evaluateJavascript("centerOn($lat, $lon, 15);", null)
                    }
                }
            )
        }

        // B. Custom Dismissable Notifications Panel
        if (isNotificationsOpen) {
            NotificationsOverlayDialog(
                onClose = { isNotificationsOpen = false },
                onLocateVenue = { venueId ->
                    isNotificationsOpen = false
                    val matched = allVenues.find { it.id == venueId }
                    if (matched != null) {
                        selectedMapItem = SelectedMapItem.Venue(matched)
                        val coords = getVenueCoordinates(venueId)
                        webViewRef?.evaluateJavascript("centerOn(${coords.latitude}, ${coords.longitude}, 15); drawRoute(${coords.latitude}, ${coords.longitude});", null)
                    }
                }
            )
        }

        // C. User Profile Sheet Dialog
        if (isProfileOpen) {
            UserProfileOverlayDialog(
                onClose = { isProfileOpen = false }
            )
        }

        // D. Add Place Dialog with 3 segments
        if (isAddPlaceOpen) {
            AddPlaceOverlayDialog(
                onClose = { isAddPlaceOpen = false },
                onSubmitVenue = { newVenue ->
                    customAddedVenues.add(newVenue)
                    isAddPlaceOpen = false
                    
                    // Append marker dynamically to Leaflet webview
                    val coords = getVenueCoordinates(newVenue.id)
                    val jsCall = """
                        addVenueMarker(
                            '${newVenue.id}', 
                            '${newVenue.name.replace("'", "\\'")}', 
                            ${coords.latitude}, 
                            ${coords.longitude}, 
                            '${newVenue.category}', 
                            '${newVenue.subcategory}', 
                            ${newVenue.rating}, 
                            90, 
                            '${newVenue.imageUrl}',
                            false,
                            false,
                            false,
                            0,
                            false,
                            false,
                            true,
                            false
                        );
                        centerOn(${coords.latitude}, ${coords.longitude}, 15);
                    """.trimIndent()
                    webViewRef?.evaluateJavascript(jsCall, null)
                    Toast.makeText(context, "📍 Successfully posted ${newVenue.name} live on FOMO Map!", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

// --- COMPOSE UI SUB-COMPONENTS ---

@Composable
fun MapTopBar(
    avatarUrl: String,
    cityStatus: String,
    onCityStatusClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Profile Avatar with custom border
        AsyncImage(
            model = avatarUrl,
            contentDescription = "User profile options",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF00E5FF), CircleShape)
                .clickable { onAvatarClick() }
        )

        // Center Pulsing dynamic context status badge
        Surface(
            color = Color(0xFF0F1524).copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color(0xFFFF2D55).copy(alpha = 0.45f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.clickable { onCityStatusClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF2D55))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = cityStatus.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Action Toolbar matching accessibility targets (48.dp area via padding)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F1524).copy(alpha = 0.9f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = "Universal Search", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F1524).copy(alpha = 0.9f))
                    .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), CircleShape)
                    .clickable { onNotificationsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Flash Drop Alerts", tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CategoryChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("All", "🌙 Nightlife", "🍔 Food", "✨ Prep", "☕ Wellness", "✈ Travel", "🎫 Events")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            Surface(
                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF0F1524).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable { onCategorySelected(category) }
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

// Top Category-Adaptive Nearest Hotspot Card
@Composable
fun NearestVenueCard(
    venue: ExploreVenue,
    categoryLabel: String,
    onNavigateToLobby: (String) -> Unit,
    onRouteClick: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clean = categoryLabel.replace(Regex("[^\\w\\s]"), "").trim().uppercase()
    val textTag = if (clean == "ALL") "NEAREST HOTSPOT" else "NEAREST $clean"

    val isLobby = venue.hasClubLobby
    val catClean = if (clean == "ALL") venue.category.uppercase().trim() else clean

    val btnTitle = when {
        catClean == "NIGHTLIFE" -> "Club Lobby"
        catClean == "EVENTS" -> "Event Lobby"
        catClean == "TRAVEL" || catClean == "LUXURY" -> "Reserve"
        catClean == "LIVE" -> "Watch"
        else -> "Website"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = Color(0xFF0C0F19).copy(alpha = 0.95f),
        border = BorderStroke(1.5.dp, Color(0xFFC026D3).copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row across the top of the card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Purple location pin + tracking uppercase title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFC026D3),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = textTag,
                        color = Color(0xFFD946EF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }

                // "Trending" + "92%" pill badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF4500),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Trending ",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = Color(0xFFC026D3),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        val vibeScore = (venue.rating * 20).toInt()
                        Text(
                            text = "$vibeScore%",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Two-Column Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Column: Hero Image with badges and tags
                Box(
                    modifier = Modifier
                        .weight(0.38f)
                        .height(165.dp)
                ) {
                    // Full-height venue photo
                    AsyncImage(
                        model = venue.imageUrl,
                        contentDescription = venue.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )

                    // "HOT TONIGHT" badge top-left
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFF4500),
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "HOT TONIGHT",
                                color = Color.White,
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Large glowing neon "V" logo centered in the photo
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .background(Color(0xFF0C0F19).copy(alpha = 0.75f), CircleShape)
                            .border(1.5.dp, Color(0xFFC026D3), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "V",
                            color = Color(0xFFF472B6),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Adaptive Tags bottom-left
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val isNightlife = catClean == "NIGHTLIFE"
                        val tag1 = if (isNightlife) "Techno" else if (catClean == "FOOD") "Gourmet" else "Premium"
                        val tag2 = if (isNightlife) "Cocktails" else if (catClean == "FOOD") "Drinks" else "Booking"
                        val tag3 = if (isNightlife) "21+" else if (catClean == "FOOD") "All Ages" else "Verified"

                        Surface(
                            color = Color.Black.copy(alpha = 0.80f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(8.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(tag1, color = Color.White, fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.80f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Icon(Icons.Default.LocalBar, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(8.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(tag2, color = Color.White, fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.80f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF32D74B), modifier = Modifier.size(8.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(tag3, color = Color.White, fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Right Column: Details, metadata & actions
                Column(
                    modifier = Modifier
                        .weight(0.62f)
                ) {
                    // Title and FOMO box Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = venue.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = venue.subcategory,
                                    color = Color(0xFFD946EF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (venue.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color(0xFFC026D3),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        // FOMO score vertical metric box
                        val vibeScore = (venue.rating * 20).toInt()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(Color(0xFFC026D3).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFC026D3).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFD946EF),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$vibeScore%",
                                color = Color(0xFFD946EF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "FOMO SCORE",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 5.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Address and Distance Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFC026D3),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = venue.address,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "${venue.distanceText} (2 min walk)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Open / hours status row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color(0xFF32D74B), CircleShape)
                        )
                        Text(
                            text = "Open Now",
                            color = Color(0xFF32D74B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 9.sp
                        )
                        Text(
                            text = "Closes 04:00 AM",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Hours Details Pill
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Thu – Sun 18:00 – 04:00",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rating Row + View Reviews Action Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = " ${venue.rating}",
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " (${venue.reviewCount} reviews)",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 8.5.sp
                            )
                        }

                        Text(
                            text = "View Reviews",
                            color = Color(0xFFD946EF),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Opening reviews for ${venue.name}...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons with custom subtitles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action 1: Club Lobby / Website with dual subtitle labels
                        Surface(
                            onClick = {
                                if (catClean == "NIGHTLIFE" || catClean == "EVENTS") {
                                    onNavigateToLobby(venue.id)
                                } else {
                                    try {
                                        uriHandler.openUri(venue.websiteUrl)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Opening Website for ${venue.name}...", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            color = Color(0xFF1E1430),
                            border = BorderStroke(1.dp, Color(0xFFC026D3).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color(0xFFD946EF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = btnTitle,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when {
                                            catClean == "NIGHTLIFE" -> "Photos, Events & More"
                                            catClean == "EVENTS" -> "Get Tickets & Lineup"
                                            catClean == "TRAVEL" || catClean == "LUXURY" -> "Book Tables & Rooms"
                                            catClean == "LIVE" -> "Watch Stream Feed"
                                            else -> "View Menu & Booking"
                                        },
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 6.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Action 2: Route button
                        Surface(
                            onClick = onRouteClick,
                            color = Color(0xFFC026D3),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = "Route",
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Get Directions",
                                        color = Color.Black.copy(alpha = 0.7f),
                                        fontSize = 6.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedTabButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(2.5.dp)
                .background(if (isSelected) Color(0xFF00E5FF) else Color.Transparent)
        )
    }
}

// Horizontal Carousel cards
@Composable
fun HorizontalVenueCard(
    venue: ExploreVenue,
    isExpanded: Boolean,
    onSelect: () -> Unit,
    onNavigateToLobby: (String) -> Unit,
    onRouteClick: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val catClean = venue.category.uppercase().trim()
    val primaryTitle = when {
        catClean == "NIGHTLIFE" -> "Club Lobby"
        catClean == "EVENTS" -> "Event Lobby"
        catClean == "TRAVEL" || catClean == "LUXURY" -> "Reserve"
        catClean == "LIVE" -> "Watch"
        else -> "Website"
    }

    Surface(
        modifier = Modifier
            .width(220.dp)
            .clickable { onSelect() },
        color = Color(0xFF141A29),
        border = BorderStroke(1.dp, if (isExpanded) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Photo with Vibe Score overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                AsyncImage(
                    model = venue.imageUrl,
                    contentDescription = venue.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
                // Vibe Badge
                Surface(
                    color = Color(0xFF0F1524).copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, Color(0xFFFF4500).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF4500),
                            modifier = Modifier.size(11.dp)
                        )
                        val score = (venue.rating * 20).toInt()
                        Text(
                            text = " $score% Vibe",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                // Name Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = venue.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (venue.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier
                                .size(13.dp)
                                .padding(start = 2.dp)
                        )
                    }
                }
                
                // Category/Subcategory
                Text(
                    text = venue.subcategory,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Rating, Distance, and Open Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("⭐ ${venue.rating}", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                    Text("•", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                    val isOpen = venue.openDays.lowercase().contains("now") || venue.openDays.lowercase().contains("mon") || venue.openDays.lowercase().contains("active")
                    Text(
                        text = "${venue.distanceText} • ${if (isOpen) "Open" else "Closed"}",
                        color = if (isOpen) Color(0xFF32D74B) else Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Expanded State Actions
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (catClean == "NIGHTLIFE" || catClean == "EVENTS") {
                                    onNavigateToLobby(venue.id)
                                } else {
                                    try {
                                        uriHandler.openUri(venue.websiteUrl)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Opening Website for ${venue.name}...", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151D30)),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = primaryTitle,
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onRouteClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Route",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalFriendCard(friend: CircleFriend, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .clickable { onSelect() },
        color = Color(0xFF141A29),
        border = BorderStroke(1.dp, if (friend.status == "Online") Color(0xFF32D74B).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = friend.avatarUrl,
                    contentDescription = friend.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (friend.status == "Online") Color(0xFF32D74B) else Color.Gray)
                        .border(1.dp, Color(0xFF141A29), CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(friend.currentActivity, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("📍 ${friend.distanceText}", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// Map selected item full details panel
@Composable
fun VenueDetailsPanel(
    venue: ExploreVenue,
    onNavigateToLobby: (String) -> Unit,
    onClose: () -> Unit,
    onRoute: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = Color(0xFF0F1524).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(venue.subcategory.uppercase(), color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close pin detail", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
            
            Text(venue.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⭐ ${venue.rating}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("📍 ${venue.distanceText}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("🕒 ${venue.openDays}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(venue.attributes) { attr ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00E5FF).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(attr, color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val catClean = venue.category.uppercase().trim()
                val primaryTitle = when {
                    catClean == "NIGHTLIFE" -> "Club Lobby"
                    catClean == "EVENTS" -> "Event Lobby"
                    catClean == "TRAVEL" || catClean == "LUXURY" -> "Reserve"
                    catClean == "LIVE" -> "Watch"
                    else -> "Website"
                }
                val context = LocalContext.current
                val uriHandler = LocalUriHandler.current

                Button(
                    onClick = {
                        if (catClean == "NIGHTLIFE" || catClean == "EVENTS") {
                            onNavigateToLobby(venue.id)
                        } else {
                            try {
                                uriHandler.openUri(venue.websiteUrl)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Opening Website for ${venue.name}...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151D30)),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text(primaryTitle, color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRoute,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text("Draw Directions", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FriendDetailsPanel(friend: CircleFriend, onClose: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = Color(0xFF0F1524).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFF32D74B).copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (friend.status == "Online") "ONLINE" else "OFFLINE", color = Color(0xFF32D74B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close friend details", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = friend.avatarUrl,
                    contentDescription = friend.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(friend.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(friend.username, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text("🎵 ${friend.currentActivity}", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
            }
        }
    }
}

// Dialog search component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOverlayDialog(
    venues: List<ExploreVenue>,
    friends: List<CircleFriend>,
    onClose: () -> Unit,
    onSelectItem: (SelectedMapItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchTab by remember { mutableStateOf("All") } // "All", "Hotspots", "Friends"

    val filteredVenues = remember(query, venues) {
        if (query.isEmpty()) venues else venues.filter { it.name.contains(query, ignoreCase = true) || it.subcategory.contains(query, ignoreCase = true) }
    }
    val filteredFriends = remember(query, friends) {
        if (query.isEmpty()) friends else friends.filter { it.name.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            color = Color(0xFF0F1524),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Universal Discovery", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close search", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search places, events, people...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = Color(0xFF0A0F19),
                        unfocusedContainerColor = Color(0xFF0A0F19)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tab filters
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("All", "Hotspots", "Friends").forEach { tab ->
                        val active = searchTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) Color(0xFF00E5FF) else Color(0xFF151D30))
                                .clickable { searchTab = tab }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(tab, color = if (active) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text("SEARCH RESULTS", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (filteredVenues.isEmpty() && filteredFriends.isEmpty()) {
                            Text(
                                "No matches found. Try typing another venue or friend name.",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Center),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Simple list containing both
                                val listState = remember { mutableStateOf(0) } // list placeholder
                                Column {
                                    if (searchTab == "All" || searchTab == "Hotspots") {
                                        filteredVenues.take(4).forEach { venue ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onSelectItem(SelectedMapItem.Venue(venue)) }
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(venue.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text(venue.subcategory, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                    if (searchTab == "All" || searchTab == "Friends") {
                                        filteredFriends.take(4).forEach { friend ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onSelectItem(SelectedMapItem.Friend(friend)) }
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF32D74B), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(friend.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text(friend.currentActivity, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Notifications Panel Dialog
@Composable
fun NotificationsOverlayDialog(
    onClose: () -> Unit,
    onLocateVenue: (String) -> Unit
) {
    val initialNotifications = remember {
        mutableStateListOf(
            Triple("fd1", "🎁 FLASH DROP: Welcome tequila shot details at FOMO Club. 12 min left!", "fomo_club"),
            Triple("fd2", "👥 FRIEND ACTIVITY: Sarah checked-in at Taboo Lounge Sandton.", "taboo_sandton"),
            Triple("fd3", "🎫 EVENT LIVE: Amapiano Fridays live stream starts in 30 mins!", "fomo_club"),
            Triple("fd4", "🟢 NIGHTGUARD ESCORT: Safe guards active in Rosebank zone.", "fomo_club")
        )
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f),
            color = Color(0xFF0F1524),
            border = BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.35f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Radar Notifications", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close alerts", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (initialNotifications.isEmpty()) {
                        Text(
                            "You have cleared all alerts.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 40.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        initialNotifications.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.second, color = Color.White, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Locate Venue",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { onLocateVenue(item.third) }
                                    )
                                }
                                IconButton(
                                    onClick = { initialNotifications.remove(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear Alert", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// User Profile Overlay Dialog
@Composable
fun UserProfileOverlayDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f),
            color = Color(0xFF0F1524),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close profiles", tint = Color.White)
                    }
                }
                
                AsyncImage(
                    model = "https://i.pravatar.cc/150?img=12",
                    contentDescription = "User avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFF00E5FF), CircleShape)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                Text("MRSEENZ@gmail.com", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                Surface(
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("PLATINUM EXPLORER", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("14", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Check-ins", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("112", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Ripples", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("5 Days", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Active Streak", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// Add Place Overlay with 3 Tabs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceOverlayDialog(
    onClose: () -> Unit,
    onSubmitVenue: (ExploreVenue) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Permanent, 1 = Special, 2 = Temporary

    // Fields
    var name by remember { mutableStateOf("") }
    var subcategory by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var categorySelect by remember { mutableStateOf("Nightlife") }
    
    var eventTitle by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var eventHeadliner by remember { mutableStateOf("") }

    var partyName by remember { mutableStateOf("") }
    var partyType by remember { mutableStateOf("House Party") }
    var durationHrs by remember { mutableStateOf("6 Hours") }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            color = Color(0xFF0F1524),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add to FOMO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close add form", tint = Color.White)
                    }
                }

                // Tab selectors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Venue", "Event", "Temp Party").forEachIndexed { index, title ->
                        val active = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) Color(0xFF00E5FF) else Color(0xFF151D30))
                                .clickable { activeTab = index }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title, color = if (active) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (activeTab) {
                        0 -> {
                            // Permanent Venue Tab
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Venue Name", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = subcategory,
                                onValueChange = { subcategory = it },
                                label = { Text("Subcategory (e.g. VIP Lounge)", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Address Area", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        1 -> {
                            // Event Tab
                            OutlinedTextField(
                                value = eventTitle,
                                onValueChange = { eventTitle = it },
                                label = { Text("Event Title", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00E5FF)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = eventTime,
                                onValueChange = { eventTime = it },
                                label = { Text("Time (e.g. 18:00 - Midnight)", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00E5FF)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = eventHeadliner,
                                onValueChange = { eventHeadliner = it },
                                label = { Text("Headliner / Lineup", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00E5FF)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        2 -> {
                            // Expiring Party Tab
                            OutlinedTextField(
                                value = partyName,
                                onValueChange = { partyName = it },
                                label = { Text("Party / Venue Name", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00E5FF)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Secret Location Address", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00E5FF)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val finalName = when (activeTab) {
                            0 -> name
                            1 -> eventTitle
                            else -> partyName
                        }
                        val finalSub = when (activeTab) {
                            0 -> subcategory
                            1 -> "Special Event Hosted tonight"
                            else -> "Temporary Secret Party"
                        }
                        val finalCat = when (activeTab) {
                            0 -> categorySelect
                            1 -> "Events"
                            else -> "Nightlife"
                        }
                        if (finalName.isNotEmpty()) {
                            val newId = "evt_${System.currentTimeMillis()}"
                            val generatedVenue = ExploreVenue(
                                id = newId,
                                name = finalName,
                                category = finalCat,
                                subcategory = finalSub,
                                imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop",
                                isVerified = true,
                                rating = 4.8f,
                                reviewCount = 10,
                                address = address.ifEmpty { "Johannesburg" },
                                area = "Sandton",
                                distanceText = "0.5 km away",
                                attributes = listOf("🎁 Dynamic Pin", "🟢 Active", "Simulated"),
                                openDays = "Active Now",
                                startHour = 18,
                                endHour = 4,
                                is24Hours = false,
                                websiteUrl = "https://fomoapp.live",
                                hasClubLobby = true
                            )
                            onSubmitVenue(generatedVenue)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Post Live to Map Radar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
