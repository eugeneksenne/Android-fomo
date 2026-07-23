package com.example.feature.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onCreatorStudioClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // -------------------------------------------------------------------------
    // THEME & ACCENT GLOBAL STATES (DYNAMICS COLORATION)
    // -------------------------------------------------------------------------
    var selectedTheme by remember { mutableStateOf("Dark") } // "Dark", "Light", "System"
    var selectedAccentColor by remember { mutableStateOf("Electric Teal") } // "Electric Teal", "Magenta", "Amber", "Purple", "Blue"

    val dynamicBgColor = if (selectedTheme == "Light") Color(0xFFF3F5FA) else Color(0xFF070B13)
    val dynamicSurfaceColor = if (selectedTheme == "Light") Color(0xFFFFFFFF) else Color(0xFF0F1524)
    val dynamicSurfaceVariantColor = if (selectedTheme == "Light") Color(0xFFE4E9F2) else Color(0xFF1B2338)
    val dynamicTextColor = if (selectedTheme == "Light") Color(0xFF1A1D26) else Color(0xFFFFFFFF)
    val dynamicTextSecondaryColor = if (selectedTheme == "Light") Color(0xFF656F7D) else Color(0xFF909CB2)

    val dynamicPrimaryColor = when (selectedAccentColor) {
        "Electric Teal" -> Color(0xFF00E5FF)
        "Magenta" -> Color(0xFFFF2D55)
        "Amber" -> Color(0xFFFFB300)
        "Purple" -> Color(0xFFB026FF)
        "Blue" -> Color(0xFF2979FF)
        else -> Color(0xFF00E5FF)
    }

    val dynamicPrimaryContainerColor = dynamicPrimaryColor.copy(alpha = 0.15f)

    // -------------------------------------------------------------------------
    // CORE PERSISTENT SETTINGS STATES
    // -------------------------------------------------------------------------
    // Profile Edit States
    var profileName by remember { mutableStateOf("Alfred") }
    var profileUsername by remember { mutableStateOf("alfred") }
    var profileBio by remember { mutableStateOf("Verified Explorer. Nightlife enthusiast & cocktail connoisseur.") }
    var profileEmail by remember { mutableStateOf("alfred@fomo.discovery") }
    var profilePhone by remember { mutableStateOf("+27 82 123 4567") }
    var profileBirthday by remember { mutableStateOf("1998-10-14") }
    var profileCity by remember { mutableStateOf("Johannesburg") }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Active User Role selection inside settings
    var currentUserRole by remember { mutableStateOf("Regular User") } 
    // "Regular User", "Nightclub Owner", "Bar Owner", "Lounge Owner", "Event Organizer", "DJ", "Artist"

    // Smart Recommendations (dismissible)
    var recommendationsList by remember {
        mutableStateOf(
            listOf(
                "Enable NightGuard for safer nights out",
                "Download Johannesburg Offline Map",
                "Activate Two-Factor Authentication",
                "Clear 1.2 GB Cached Media"
            )
        )
    }

    // Developer Unlocking States
    var developerModeUnlocked by remember { mutableStateOf(false) }
    var versionClickCount by remember { mutableStateOf(0) }

    // Navigation and Panels Route States
    var currentSubSection by remember { mutableStateOf<String?>(null) } // e.g. "Account", "Privacy & Safety", etc.
    var searchQuery by remember { mutableStateOf("") }

    // NightGuard Controls
    var emergencyContacts by remember { mutableStateOf(listOf("Sizwe (Brother)", "Naledi (Best Friend)")) }
    var newContactName by remember { mutableStateOf("") }
    var walkMeHomeEnabled by remember { mutableStateOf(false) }
    var walkMeHomeDuration by remember { mutableStateOf(30) }
    var walkMeHomeNotifications by remember { mutableStateOf(true) }
    var safetyCheckTimer by remember { mutableStateOf(15) }
    var safetyCheckEscalation by remember { mutableStateOf("Notify Emergency Contacts") }
    var safetyCheckEmergencyActions by remember { mutableStateOf("Trigger SOS Alarm & Share Location") }
    var buddyPairRequests by remember { mutableStateOf(true) }
    var buddyPairAutoAccept by remember { mutableStateOf(false) }
    var buddyLocationSharing by remember { mutableStateOf(true) }

    // Account Security
    var accountPasskeysEnabled by remember { mutableStateOf(true) }
    var account2FAEnabled by remember { mutableStateOf(false) }
    var connectedGoogle by remember { mutableStateOf(true) }
    var connectedApple by remember { mutableStateOf(false) }
    var connectedFacebook by remember { mutableStateOf(false) }
    var connectedTikTok by remember { mutableStateOf(true) }
    var connectedInstagram by remember { mutableStateOf(true) }

    // Privacy & Safety
    var privacyViewProfile by remember { mutableStateOf("Everyone") }
    var privacyFollowMe by remember { mutableStateOf("Everyone") }
    var privacyMessageMe by remember { mutableStateOf("Friends") }
    var privacyMentionMe by remember { mutableStateOf("Followers") }
    var privacyTagMe by remember { mutableStateOf("Friends") }
    var privacyMomentsVisibility by remember { mutableStateOf("Public") }
    var locationLiveSharing by remember { mutableStateOf(false) }
    var locationBackgroundByApp by remember { mutableStateOf(true) }
    var locationPrecisionHigh by remember { mutableStateOf(true) }
    var locationAutoStopSharingTime by remember { mutableStateOf(4) }
    var locationVisibilityScope by remember { mutableStateOf("Friends") }
    var blockedUsersList by remember { mutableStateOf(listOf("BadVibe101", "SpammySteve", "CreepyCam")) }
    var showBlockedUsersDialog by remember { mutableStateOf(false) }

    // Notifications Toggles
    var notifLikes by remember { mutableStateOf(true) }
    var notifComments by remember { mutableStateOf(true) }
    var notifMentions by remember { mutableStateOf(true) }
    var notifReplies by remember { mutableStateOf(true) }
    var notifFollowers by remember { mutableStateOf(true) }
    var notifNearbyEvents by remember { mutableStateOf(true) }
    var notifOpenings by remember { mutableStateOf(false) }
    var notifClosingSoon by remember { mutableStateOf(true) }
    var notifWeekendGuide by remember { mutableStateOf(true) }
    var notifCityHighlights by remember { mutableStateOf(true) }
    var notifMessages by remember { mutableStateOf(true) }
    var notifCalls by remember { mutableStateOf(true) }
    var notifGroups by remember { mutableStateOf(true) }
    var notifChatMentions by remember { mutableStateOf(true) }
    var notifCreatorVenueUpdates by remember { mutableStateOf(true) }
    var notifCreatorBookings by remember { mutableStateOf(true) }
    var notifCreatorPerformanceRequests by remember { mutableStateOf(true) }
    var notifCreatorTicketSales by remember { mutableStateOf(true) }
    var notifCreatorVerificationUpdates by remember { mutableStateOf(true) }
    var notifSafetyNightGuard by remember { mutableStateOf(true) }
    var notifSafetyBuddyAlerts by remember { mutableStateOf(true) }
    var notifSafetySOSAlerts by remember { mutableStateOf(true) }
    var notifSafetyCheckAlerts by remember { mutableStateOf(true) }
    var notifMarketingNews by remember { mutableStateOf(false) }
    var notifMarketingOffers by remember { mutableStateOf(true) }
    var notifMarketingProductUpdates by remember { mutableStateOf(false) }

    // Location & Maps Settings
    var mapPermissionGranted by remember { mutableStateOf(true) }
    var mapBackgroundLocationPermission by remember { mutableStateOf(true) }
    var mapGpsAccuracy by remember { mutableStateOf("High") }
    var defaultRouteEngine by remember { mutableStateOf("FOMO Routing") }
    var mapStyleSelection by remember { mutableStateOf("Dark") }
    var mapTrafficLayer by remember { mutableStateOf(true) }
    var mapHeatmapLayer by remember { mutableStateOf(true) }
    var map3DBuildings by remember { mutableStateOf(true) }
    var mapCompass by remember { mutableStateOf(true) }
    var downloadedCities by remember { mutableStateOf(listOf("Johannesburg (342 MB)", "Cape Town (210 MB)")) }
    var downloadNewCityName by remember { mutableStateOf("") }
    var isDownloadingCity by remember { mutableStateOf(false) }
    var downloadProgressCity by remember { mutableStateOf(0f) }

    // Appearance / Animations Settings
    var animsEnabled by remember { mutableStateOf(true) }
    var reduceMotion by remember { mutableStateOf(false) }
    var blurEffectsEnabled by remember { mutableStateOf(true) }
    var hapticsEnabled by remember { mutableStateOf(true) }

    // Experience Preferences
    var expAutoRefresh by remember { mutableStateOf(true) }
    var expHeroAnims by remember { mutableStateOf(true) }
    var expHeatmapAnims by remember { mutableStateOf(true) }
    var expLiveCounters by remember { mutableStateOf(true) }
    var expCameraDefaultMode by remember { mutableStateOf("Photo") }
    var expCameraVideoQuality by remember { mutableStateOf("1080p @ 60fps") }
    var expCameraSaveOriginals by remember { mutableStateOf(true) }
    var expCameraAutoHDR by remember { mutableStateOf(true) }
    var expCameraGridLines by remember { mutableStateOf(false) }
    var expCameraFlashPreference by remember { mutableStateOf("Off") }
    var expChatsMediaAutoDownload by remember { mutableStateOf(true) }
    var expChatsReadReceipts by remember { mutableStateOf(true) }
    var expChatsTypingIndicators by remember { mutableStateOf(true) }
    var expChatsLinkPreviews by remember { mutableStateOf(true) }
    var expDiscoverDistanceUnit by remember { mutableStateOf("Kilometers") }
    var expDiscoverDefaultRadius by remember { mutableStateOf(10f) }
    var expDiscoverContentPref by remember { mutableStateOf("Trending Nightlife") }

    // Storage and Actions
    var storageUsedState by remember { mutableStateOf("1.8 GB") }
    var cacheSizeState by remember { mutableStateOf("1.2 GB") }
    var mediaSizeState by remember { mutableStateOf("450 MB") }
    var offlineMapsSizeState by remember { mutableStateOf("552 MB") }
    var downloadsSizeState by remember { mutableStateOf("80 MB") }
    var storageWiFiOnlyUploads by remember { mutableStateOf(true) }
    var storageLowerImageQuality by remember { mutableStateOf(false) }
    var storageLowerVideoQuality by remember { mutableStateOf(false) }
    var storageDisableAutoPlay by remember { mutableStateOf(false) }

    // Accessibility
    var accessLargeText by remember { mutableStateOf(false) }
    var accessHighContrast by remember { mutableStateOf(false) }
    var accessColorBlindMode by remember { mutableStateOf("None") }
    var accessReduceMotion by remember { mutableStateOf(false) }
    var accessReduceTransparency by remember { mutableStateOf(false) }
    var accessLargeButtons by remember { mutableStateOf(false) }
    var accessVoiceFeedback by remember { mutableStateOf(false) }
    var accessHapticFeedback by remember { mutableStateOf(true) }

    // Support Panel States
    var faqExpandedIndex by remember { mutableStateOf<Int?>(null) }
    var contactSupportText by remember { mutableStateOf("") }
    var reportBugText by remember { mutableStateOf("") }
    var suggestFeatureText by remember { mutableStateOf("") }

    // Developer Performance Metrics (dynamic updates)
    var devCpuUsage by remember { mutableStateOf(12) }
    var devRamUsage by remember { mutableStateOf(218) }
    var devFps by remember { mutableStateOf(60) }
    var devFeatureFlag3DHeatmap by remember { mutableStateOf(true) }
    var devFeatureFlagBetaVideo by remember { mutableStateOf(false) }
    var devFeatureFlagExperimentalRoute by remember { mutableStateOf(true) }
    var devFeatureFlagP2PSafety by remember { mutableStateOf(true) }
    var devFeatureFlagAmapianoAI by remember { mutableStateOf(true) }
    var showDevCrashSimulatedDialog by remember { mutableStateOf(false) }

    // Live background rendering loop when developer metrics panel is open
    LaunchedEffect(key1 = developerModeUnlocked) {
        while (true) {
            delay(1500)
            devCpuUsage = (8..24).random()
            devRamUsage = (210..245).random()
            devFps = (58..61).random()
        }
    }

    // -------------------------------------------------------------------------
    // HELPER INTERNAL NAVIGATION HANDLER
    // -------------------------------------------------------------------------
    fun navigateTo(subSection: String) {
        currentSubSection = subSection
        searchQuery = "" // Reset search when entering sub section
    }

    // -------------------------------------------------------------------------
    // MAIN APP LAYOUT
    // -------------------------------------------------------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (currentSubSection != null) currentSubSection!! else "Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = dynamicTextColor
                        )
                        Text(
                            text = if (currentSubSection != null) "Control Center > ${currentSubSection}" else "FOMO Control Center",
                            fontSize = 12.sp,
                            color = dynamicTextSecondaryColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSubSection != null) {
                            currentSubSection = null
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = dynamicTextColor
                        )
                    }
                },
                actions = {
                    // Quick User Role Selector so anyone can test different Creator roles immediately
                    var showRoleMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        Button(
                            onClick = { showRoleMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = dynamicPrimaryContainerColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = currentUserRole,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = dynamicPrimaryColor
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = dynamicPrimaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showRoleMenu,
                            onDismissRequest = { showRoleMenu = false },
                            modifier = Modifier.background(dynamicSurfaceColor)
                        ) {
                            val roles = listOf("Regular User", "Nightclub Owner", "Bar Owner", "Lounge Owner", "Event Organizer", "DJ", "Artist")
                            roles.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role, color = dynamicTextColor, fontSize = 13.sp) },
                                    onClick = {
                                        currentUserRole = role
                                        showRoleMenu = false
                                        Toast.makeText(context, "Switched role to: $role", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = dynamicBgColor)
            )
        },
        containerColor = dynamicBgColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (currentSubSection != null) {
                // -------------------------------------------------------------------------
                // DETAILED SUBSECTION PANEL CONTAINER
                // -------------------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    when (currentSubSection) {
                        "Account" -> AccountSubSection(
                            profileName = profileName,
                            profileUsername = profileUsername,
                            profileBio = profileBio,
                            profileEmail = profileEmail,
                            profilePhone = profilePhone,
                            profileBirthday = profileBirthday,
                            profileCity = profileCity,
                            passkeysEnabled = accountPasskeysEnabled,
                            onPasskeysToggle = { accountPasskeysEnabled = it },
                            twoFactorEnabled = account2FAEnabled,
                            on2FAToggle = { account2FAEnabled = it },
                            connectedGoogle = connectedGoogle,
                            onGoogleToggle = { connectedGoogle = it },
                            connectedApple = connectedApple,
                            onAppleToggle = { connectedApple = it },
                            connectedFacebook = connectedFacebook,
                            onFacebookToggle = { connectedFacebook = it },
                            connectedTikTok = connectedTikTok,
                            onTikTokToggle = { connectedTikTok = it },
                            connectedInstagram = connectedInstagram,
                            onInstagramToggle = { connectedInstagram = it },
                            surfaceColor = dynamicSurfaceColor,
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor,
                            onEditProfileClick = { showEditProfileDialog = true }
                        )

                        "Privacy & Safety" -> PrivacySubSection(
                            viewProfile = privacyViewProfile,
                            onViewProfileChange = { privacyViewProfile = it },
                            followMe = privacyFollowMe,
                            onFollowMeChange = { privacyFollowMe = it },
                            messageMe = privacyMessageMe,
                            onMessageMeChange = { privacyMessageMe = it },
                            mentionMe = privacyMentionMe,
                            onMentionMeChange = { privacyMentionMe = it },
                            tagMe = privacyTagMe,
                            onTagMeChange = { privacyTagMe = it },
                            momentsVisibility = privacyMomentsVisibility,
                            onMomentsChange = { privacyMomentsVisibility = it },
                            liveSharing = locationLiveSharing,
                            onLiveSharingToggle = { locationLiveSharing = it },
                            backgroundLocation = locationBackgroundByApp,
                            onBgLocationToggle = { locationBackgroundByApp = it },
                            highPrecision = locationPrecisionHigh,
                            onPrecisionToggle = { locationPrecisionHigh = it },
                            autoStopHours = locationAutoStopSharingTime,
                            onAutoStopChange = { locationAutoStopSharingTime = it },
                            locationVisibility = locationVisibilityScope,
                            onLocVisChange = { locationVisibilityScope = it },
                            contacts = emergencyContacts,
                            newContactName = newContactName,
                            onContactNameChange = { newContactName = it },
                            onAddContact = {
                                if (newContactName.isNotBlank()) {
                                    emergencyContacts = emergencyContacts + newContactName
                                    newContactName = ""
                                    Toast.makeText(context, "Added trusted contact", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRemoveContact = { contact ->
                                emergencyContacts = emergencyContacts.filter { it != contact }
                                Toast.makeText(context, "Removed trusted contact", Toast.LENGTH_SHORT).show()
                            },
                            walkMeHomeEnabled = walkMeHomeEnabled,
                            onWalkMeHomeToggle = { walkMeHomeEnabled = it },
                            walkMeHomeDur = walkMeHomeDuration,
                            onWalkMeHomeDurChange = { walkMeHomeDuration = it },
                            walkMeHomeNotif = walkMeHomeNotifications,
                            onWalkMeHomeNotifToggle = { walkMeHomeNotifications = it },
                            safetyTimer = safetyCheckTimer,
                            onSafetyTimerChange = { safetyCheckTimer = it },
                            safetyEscalation = safetyCheckEscalation,
                            onSafetyEscalationChange = { safetyCheckEscalation = it },
                            safetyActions = safetyCheckEmergencyActions,
                            onSafetyActionsChange = { safetyCheckEmergencyActions = it },
                            buddyRequests = buddyPairRequests,
                            onBuddyReqToggle = { buddyPairRequests = it },
                            buddyAutoAccept = buddyPairAutoAccept,
                            onBuddyAutoAcceptToggle = { buddyPairAutoAccept = it },
                            buddyLocShare = buddyLocationSharing,
                            onBuddyLocShareToggle = { buddyLocationSharing = it },
                            onOpenBlockedUsers = { showBlockedUsersDialog = true },
                            surfaceColor = dynamicSurfaceColor,
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )

                        "Notifications" -> NotificationsSubSection(
                            notifLikes = notifLikes, onLikesToggle = { notifLikes = it },
                            notifComments = notifComments, onCommentsToggle = { notifComments = it },
                            notifMentions = notifMentions, onMentionsToggle = { notifMentions = it },
                            notifReplies = notifReplies, onRepliesToggle = { notifReplies = it },
                            notifFollowers = notifFollowers, onFollowersToggle = { notifFollowers = it },
                            notifNearbyEvents = notifNearbyEvents, onNearbyEventsToggle = { notifNearbyEvents = it },
                            notifOpenings = notifOpenings, onOpeningsToggle = { notifOpenings = it },
                            notifClosingSoon = notifClosingSoon, onClosingSoonToggle = { notifClosingSoon = it },
                            notifWeekendGuide = notifWeekendGuide, onWeekendGuideToggle = { notifWeekendGuide = it },
                            notifCityHighlights = notifCityHighlights, onCityHighlightsToggle = { notifCityHighlights = it },
                            notifMessages = notifMessages, onMessagesToggle = { notifMessages = it },
                            notifCalls = notifCalls, onCallsToggle = { notifCalls = it },
                            notifGroups = notifGroups, onGroupsToggle = { notifGroups = it },
                            notifChatMentions = notifChatMentions, onChatMentionsToggle = { notifChatMentions = it },
                            notifCreatorVenue = notifCreatorVenueUpdates, onCreatorVenueToggle = { notifCreatorVenueUpdates = it },
                            notifCreatorBookings = notifCreatorBookings, onCreatorBookingsToggle = { notifCreatorBookings = it },
                            notifCreatorPerf = notifCreatorPerformanceRequests, onCreatorPerfToggle = { notifCreatorPerformanceRequests = it },
                            notifCreatorSales = notifCreatorTicketSales, onCreatorSalesToggle = { notifCreatorTicketSales = it },
                            notifCreatorVerif = notifCreatorVerificationUpdates, onCreatorVerifToggle = { notifCreatorVerificationUpdates = it },
                            notifSafetyNightGuard = notifSafetyNightGuard, onSafetyNightGuardToggle = { notifSafetyNightGuard = it },
                            notifSafetyBuddy = notifSafetyBuddyAlerts, onSafetyBuddyToggle = { notifSafetyBuddyAlerts = it },
                            notifSafetySOS = notifSafetySOSAlerts, onSafetySOSToggle = { notifSafetySOSAlerts = it },
                            notifSafetyCheck = notifSafetyCheckAlerts, onSafetyCheckToggle = { notifSafetyCheckAlerts = it },
                            notifMarketingNews = notifMarketingNews, onMarketingNewsToggle = { notifMarketingNews = it },
                            notifMarketingOffers = notifMarketingOffers, onMarketingOffersToggle = { notifMarketingOffers = it },
                            notifMarketingProduct = notifMarketingProductUpdates, onMarketingProductToggle = { notifMarketingProductUpdates = it },
                            surfaceColor = dynamicSurfaceColor,
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )

                        "Location & Maps" -> LocationMapsSubSection(
                            gpsGranted = mapPermissionGranted, onGpsToggle = { mapPermissionGranted = it },
                            bgLocGranted = mapBackgroundLocationPermission, onBgLocToggle = { mapBackgroundLocationPermission = it },
                            gpsAccuracy = mapGpsAccuracy, onGpsAccuracyChange = { mapGpsAccuracy = it },
                            currentCity = profileCity, onCityChange = { profileCity = it },
                            routeEngine = defaultRouteEngine, onRouteEngineChange = { defaultRouteEngine = it },
                            mapStyle = mapStyleSelection, onMapStyleChange = { mapStyleSelection = it },
                            trafficLayer = mapTrafficLayer, onTrafficToggle = { mapTrafficLayer = it },
                            heatmapLayer = mapHeatmapLayer, onHeatmapToggle = { mapHeatmapLayer = it },
                            buildings3D = map3DBuildings, onBuildings3DToggle = { map3DBuildings = it },
                            compassEnabled = mapCompass, onCompassToggle = { mapCompass = it },
                            downloadedCities = downloadedCities,
                            downloadInput = downloadNewCityName,
                            onDownloadInputChange = { downloadNewCityName = it },
                            isDownloading = isDownloadingCity,
                            progress = downloadProgressCity,
                            onDownloadClick = {
                                if (downloadNewCityName.isNotBlank() && !isDownloadingCity) {
                                    scope.launch {
                                        isDownloadingCity = true
                                        downloadProgressCity = 0f
                                        while (downloadProgressCity < 1.0f) {
                                            delay(150)
                                            downloadProgressCity += 0.1f
                                        }
                                        downloadedCities = downloadedCities + "${downloadNewCityName} (${(100..400).random()} MB)"
                                        downloadNewCityName = ""
                                        isDownloadingCity = false
                                        Toast.makeText(context, "Map Download Complete!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDeleteMap = { city ->
                                downloadedCities = downloadedCities.filter { it != city }
                                Toast.makeText(context, "Deleted Map: $city", Toast.LENGTH_SHORT).show()
                            },
                            onUpdateAll = {
                                Toast.makeText(context, "Updating all offline maps...", Toast.LENGTH_SHORT).show()
                            },
                            surfaceColor = dynamicSurfaceColor,
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )

                        "Appearance" -> AppearanceSubSection(
                            selectedTheme = selectedTheme,
                            onThemeChange = { selectedTheme = it },
                            selectedAccent = selectedAccentColor,
                            onAccentChange = { selectedAccentColor = it },
                            mapTheme = mapStyleSelection,
                            onMapThemeChange = { mapStyleSelection = it },
                            animsEnabled = animsEnabled,
                            onAnimsToggle = { animsEnabled = it },
                            reduceMotion = reduceMotion,
                            onReduceMotionToggle = { reduceMotion = it },
                            blurEnabled = blurEffectsEnabled,
                            onBlurToggle = { blurEffectsEnabled = it },
                            hapticsEnabled = hapticsEnabled,
                            onHapticsToggle = { hapticsEnabled = it },
                            surfaceColor = dynamicSurfaceColor,
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )

                        "Experience" -> ExperienceSubSection(
                            autoRefresh = expAutoRefresh, onAutoRefreshToggle = { expAutoRefresh = it },
                            heroAnims = expHeroAnims, onHeroAnimsToggle = { expHeroAnims = it },
                            heatmapAnims = expHeatmapAnims, onHeatmapAnimsToggle = { expHeatmapAnims = it },
                            liveCounters = expLiveCounters, onLiveCountersToggle = { expLiveCounters = it },
                            cameraMode = expCameraDefaultMode, onCameraModeChange = { expCameraDefaultMode = it },
                            videoQuality = expCameraVideoQuality, onVideoQualityChange = { expCameraVideoQuality = it },
                            saveOriginals = expCameraSaveOriginals, onSaveOriginalsToggle = { expCameraSaveOriginals = it },
                            autoHDR = expCameraAutoHDR, onAutoHDRToggle = { expCameraAutoHDR = it },
                            gridLines = expCameraGridLines, onGridLinesToggle = { expCameraGridLines = it },
                            flashPref = expCameraFlashPreference, onFlashPrefChange = { expCameraFlashPreference = it },
                            mediaAutoDownload = expChatsMediaAutoDownload, onMediaAutoDownloadToggle = { expChatsMediaAutoDownload = it },
                            readReceipts = expChatsReadReceipts, onReadReceiptsToggle = { expChatsReadReceipts = it },
                            typingIndicators = expChatsTypingIndicators, onTypingIndicatorsToggle = { expChatsTypingIndicators = it },
                            linkPreviews = expChatsLinkPreviews, onLinkPreviewsToggle = { expChatsLinkPreviews = it },
                            distanceUnit = expDiscoverDistanceUnit, onDistanceUnitChange = { expDiscoverDistanceUnit = it },
                            radiusValue = expDiscoverDefaultRadius, onRadiusValueChange = { expDiscoverDefaultRadius = it },
                            contentPref = expDiscoverContentPref, onContentPrefChange = { expDiscoverContentPref = it },
                            surfaceColor = dynamicSurfaceColor,
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )

                        "Storage & Data" -> StorageSubSection(
                            storageUsed = storageUsedState,
                            cacheSize = cacheSizeState,
                            mediaSize = mediaSizeState,
                            offlineMapsSize = offlineMapsSizeState,
                            downloadsSize = downloadsSizeState,
                            onClearCache = {
                                cacheSizeState = "0.0 KB"
                                storageUsedState = "0.6 GB"
                                Toast.makeText(context, "Cached media files fully cleared!", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteDownloads = {
                                downloadsSizeState = "0.0 KB"
                                Toast.makeText(context, "Downloads deleted!", Toast.LENGTH_SHORT).show()
                            },
                            onOptimizeStorage = {
                                cacheSizeState = "112 KB"
                                mediaSizeState = "120 MB"
                                storageUsedState = "0.3 GB"
                                Toast.makeText(context, "Storage fully optimized!", Toast.LENGTH_SHORT).show()
                            },
                            wifiOnlyUploads = storageWiFiOnlyUploads,
                            onWifiOnlyToggle = { storageWiFiOnlyUploads = it },
                            lowerImageQuality = storageLowerImageQuality,
                            onLowerImageToggle = { storageLowerImageQuality = it },
                            lowerVideoQuality = storageLowerVideoQuality,
                            onLowerVideoToggle = { storageLowerVideoQuality = it },
                            disableAutoPlay = storageDisableAutoPlay,
                            onDisableAutoPlayToggle = { storageDisableAutoPlay = it },
                            surfaceColor = dynamicSurfaceColor,
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )

                        "Accessibility" -> AccessibilitySubSection(
                            largeText = accessLargeText, onLargeTextToggle = { accessLargeText = it },
                            highContrast = accessHighContrast, onHighContrastToggle = { accessHighContrast = it },
                            colorBlindMode = accessColorBlindMode, onColorBlindChange = { accessColorBlindMode = it },
                            reduceMotion = accessReduceMotion, onReduceMotionToggle = { accessReduceMotion = it },
                            reduceTransparency = accessReduceTransparency, onReduceTransToggle = { accessReduceTransparency = it },
                            largeButtons = accessLargeButtons, onLargeButtonsToggle = { accessLargeButtons = it },
                            voiceFeedback = accessVoiceFeedback, onVoiceToggle = { accessVoiceFeedback = it },
                            hapticFeedback = accessHapticFeedback, onHapticToggle = { accessHapticFeedback = it },
                            surfaceColor = dynamicSurfaceColor,
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )

                        "Support" -> SupportSubSection(
                            faqExpandedIndex = faqExpandedIndex,
                            onFaqExpand = { index ->
                                faqExpandedIndex = if (faqExpandedIndex == index) null else index
                            },
                            supportText = contactSupportText,
                            onSupportTextChange = { contactSupportText = it },
                            onSupportSubmit = {
                                if (contactSupportText.isNotBlank()) {
                                    Toast.makeText(context, "Support ticket submitted successfully!", Toast.LENGTH_SHORT).show()
                                    contactSupportText = ""
                                }
                            },
                            bugText = reportBugText,
                            onBugTextChange = { reportBugText = it },
                            onBugSubmit = {
                                if (reportBugText.isNotBlank()) {
                                    Toast.makeText(context, "Bug report transmitted! Tech support will inspect.", Toast.LENGTH_SHORT).show()
                                    reportBugText = ""
                                }
                            },
                            suggestionText = suggestFeatureText,
                            onSuggestionTextChange = { suggestFeatureText = it },
                            onSuggestionSubmit = {
                                if (suggestFeatureText.isNotBlank()) {
                                    Toast.makeText(context, "Feature suggestion dispatched! Thanks for refining FOMO.", Toast.LENGTH_SHORT).show()
                                    suggestFeatureText = ""
                                }
                            },
                            surfaceColor = dynamicSurfaceColor,
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )

                        "Legal" -> LegalSubSection(
                            surfaceColor = dynamicSurfaceColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )

                        "About" -> AboutSubSection(
                            versionClickCount = versionClickCount,
                            onVersionClick = {
                                if (!developerModeUnlocked) {
                                    val newCount = versionClickCount + 1
                                    versionClickCount = newCount
                                    if (newCount >= 7) {
                                        developerModeUnlocked = true
                                        Toast.makeText(context, "🎉 Developer Options Unlocked!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "You are now ${7 - newCount} steps away from being a developer.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Developer Mode is already active!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            developerModeUnlocked = developerModeUnlocked,
                            onOpenDeveloperOptions = { navigateTo("Developer Options") },
                            surfaceColor = dynamicSurfaceColor,
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )

                        "Developer Options" -> if (developerModeUnlocked) {
                            DeveloperSubSection(
                                cpuUsage = devCpuUsage,
                                ramUsage = devRamUsage,
                                fps = devFps,
                                flag3DHeatmap = devFeatureFlag3DHeatmap,
                                onFlag3DToggle = { devFeatureFlag3DHeatmap = it },
                                flagBetaVideo = devFeatureFlagBetaVideo,
                                onFlagBetaVideoToggle = { devFeatureFlagBetaVideo = it },
                                flagExperimentalRoute = devFeatureFlagExperimentalRoute,
                                onFlagExperimentalRouteToggle = { devFeatureFlagExperimentalRoute = it },
                                flagP2PSafety = devFeatureFlagP2PSafety,
                                onFlagP2PSafetyToggle = { devFeatureFlagP2PSafety = it },
                                flagAmapianoAI = devFeatureFlagAmapianoAI,
                                onFlagAmapianoAIToggle = { devFeatureFlagAmapianoAI = it },
                                onCrashClick = { showDevCrashSimulatedDialog = true },
                                onResetTutorials = {
                                    recommendationsList = listOf(
                                        "Enable NightGuard for safer nights out",
                                        "Download Johannesburg Offline Map",
                                        "Activate Two-Factor Authentication",
                                        "Clear 1.2 GB Cached Media"
                                    )
                                    Toast.makeText(context, "Tutorials & smart cards successfully reset!", Toast.LENGTH_SHORT).show()
                                },
                                surfaceColor = dynamicSurfaceColor,
                                surfaceVariantColor = dynamicSurfaceVariantColor,
                                textColor = dynamicTextColor,
                                textSecColor = dynamicTextSecondaryColor,
                                primaryColor = dynamicPrimaryColor
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Access Denied", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // -------------------------------------------------------------------------
                // MAIN SETTINGS CONTROL CENTER VIEW
                // -------------------------------------------------------------------------
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Global Pinned Search Bar
                    item {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            surfaceVariantColor = dynamicSurfaceVariantColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor
                        )
                    }

                    // Interactive Search Results (Instant filtered list)
                    if (searchQuery.isNotBlank()) {
                        val results = filterSettingsItems(searchQuery, developerModeUnlocked, currentUserRole)
                        if (results.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No settings match your query", color = dynamicTextSecondaryColor)
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = "Instant Search Results",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = dynamicPrimaryColor,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(results) { result ->
                                SettingsItemRow(
                                    icon = result.icon,
                                    title = result.title,
                                    subtitle = result.subtitle,
                                    onClick = {
                                        if (result.targetSection == "Clear Cache") {
                                            cacheSizeState = "0.0 KB"
                                            storageUsedState = "0.6 GB"
                                            Toast.makeText(context, "Cached media files fully cleared!", Toast.LENGTH_SHORT).show()
                                            searchQuery = ""
                                        } else {
                                            navigateTo(result.targetSection)
                                        }
                                    },
                                    textColor = dynamicTextColor,
                                    textSecColor = dynamicTextSecondaryColor
                                )
                            }
                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = dynamicSurfaceVariantColor) }
                        }
                    }

                    // Smart Recommendations (Dynamic and dismissible)
                    if (recommendationsList.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    "SMART RECOMMENDATIONS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = dynamicPrimaryColor,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    letterSpacing = 1.sp
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recommendationsList) { rec ->
                                        Surface(
                                            color = dynamicSurfaceColor,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .width(260.dp)
                                                .clickable {
                                                    when (rec) {
                                                        "Enable NightGuard for safer nights out" -> navigateTo("Privacy & Safety")
                                                        "Download Johannesburg Offline Map" -> navigateTo("Location & Maps")
                                                        "Activate Two-Factor Authentication" -> navigateTo("Account")
                                                        "Clear 1.2 GB Cached Media" -> navigateTo("Storage & Data")
                                                    }
                                                },
                                            border = BorderStroke(1.dp, dynamicPrimaryColor.copy(alpha = 0.25f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = dynamicPrimaryColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = rec,
                                                        fontSize = 12.sp,
                                                        color = dynamicTextColor,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        recommendationsList = recommendationsList.filter { it != rec }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Dismiss",
                                                        tint = dynamicTextSecondaryColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Premium Profile Card
                    item {
                        ProfileCard(
                            name = profileName,
                            username = profileUsername,
                            city = profileCity,
                            surfaceColor = dynamicSurfaceColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor,
                            onEditClick = { showEditProfileDialog = true }
                        )
                    }

                    // Quick Actions Controls
                    item {
                        QuickActions(
                            surfaceColor = dynamicSurfaceColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            onActionClick = { action ->
                                when (action) {
                                    "NightGuard" -> navigateTo("Privacy & Safety")
                                    "Saved Places" -> Toast.makeText(context, "Opening Saved Places catalog", Toast.LENGTH_SHORT).show()
                                    "Downloads" -> navigateTo("Storage & Data")
                                    "Blocked Users" -> showBlockedUsersDialog = true
                                    "Privacy Checkup" -> navigateTo("Privacy & Safety")
                                    "Connected Accounts" -> navigateTo("Account")
                                }
                            }
                        )
                    }

                    // Role-Based Creator Studio Dashboard / Become Creator Card
                    item {
                        CreatorStudioSection(
                            role = currentUserRole,
                            onCreatorStudioClick = onCreatorStudioClick,
                            onBecomeCreatorClick = {
                                currentUserRole = "Nightclub Owner"
                                Toast.makeText(context, "Applied & approved! Role set to Nightclub Owner.", Toast.LENGTH_LONG).show()
                            },
                            onNavigateToCreatorSub = { sub ->
                                // Custom navigators for creator sections
                                Toast.makeText(context, "Opening Creator Dashboard > $sub", Toast.LENGTH_SHORT).show()
                                onCreatorStudioClick()
                            },
                            surfaceColor = dynamicSurfaceColor,
                            textColor = dynamicTextColor,
                            textSecColor = dynamicTextSecondaryColor,
                            primaryColor = dynamicPrimaryColor
                        )
                    }

                    // Primary Settings Groups
                    item {
                        Column {
                            SettingsGroup(
                                title = "ACCOUNT",
                                items = listOf(
                                    SettingsItem(Icons.Default.Person, "Account", "Personal Info, Login, Passkeys & 2FA"),
                                    SettingsItem(Icons.Default.Security, "Privacy & Safety", "NightGuard, Location privacy, Visibility"),
                                    SettingsItem(Icons.Default.Notifications, "Notifications", "Customize alerts, chat & safety pings")
                                ),
                                onRowClick = { title -> navigateTo(title) },
                                surfaceColor = dynamicSurfaceColor,
                                surfaceVariantColor = dynamicSurfaceVariantColor,
                                textColor = dynamicTextColor,
                                textSecColor = dynamicTextSecondaryColor,
                                primaryColor = dynamicPrimaryColor
                            )

                            SettingsGroup(
                                title = "APP EXPERIENCE",
                                items = listOf(
                                    SettingsItem(Icons.Default.LocationOn, "Location & Maps", "Permission, routing engine, map styles, downloads"),
                                    SettingsItem(Icons.Default.Palette, "Appearance", "Theme (Light/Dark/System), color accents, motion effects"),
                                    SettingsItem(Icons.Default.Tune, "Experience", "Camera auto-HDR, chat previews, radius settings"),
                                    SettingsItem(Icons.Default.Storage, "Storage & Data", "Cache sizing, Lower Media quality, data usage controls")
                                ),
                                onRowClick = { title -> navigateTo(title) },
                                surfaceColor = dynamicSurfaceColor,
                                surfaceVariantColor = dynamicSurfaceVariantColor,
                                textColor = dynamicTextColor,
                                textSecColor = dynamicTextSecondaryColor,
                                primaryColor = dynamicPrimaryColor
                            )

                            SettingsGroup(
                                title = "MORE",
                                items = buildList {
                                    add(SettingsItem(Icons.Default.Accessibility, "Accessibility", "Vision enhancements, larger texts, touch target assists"))
                                    add(SettingsItem(Icons.AutoMirrored.Filled.Help, "Support", "FAQS, troubleshooting, feature request forms"))
                                    add(SettingsItem(Icons.Default.Description, "Legal", "Terms of service, Cookie settings, platform licensing"))
                                    add(SettingsItem(Icons.Default.Info, "About", "Check builds notes, app API details"))
                                    if (developerModeUnlocked) {
                                        add(SettingsItem(Icons.Default.DeveloperMode, "Developer Options", "Real-time graphs, console, and debug utilities"))
                                    }
                                },
                                onRowClick = { title -> navigateTo(title) },
                                surfaceColor = dynamicSurfaceColor,
                                surfaceVariantColor = dynamicSurfaceVariantColor,
                                textColor = dynamicTextColor,
                                textSecColor = dynamicTextSecondaryColor,
                                primaryColor = dynamicPrimaryColor
                            )

                            SettingsGroup(
                                title = "LOGOUT",
                                items = listOf(
                                    SettingsItem(Icons.AutoMirrored.Filled.Logout, "Log Out", "Gracefully terminate sessions on this unit")
                                ),
                                onRowClick = { onLogoutClick() },
                                surfaceColor = dynamicSurfaceColor,
                                surfaceVariantColor = dynamicSurfaceVariantColor,
                                textColor = dynamicTextColor,
                                textSecColor = dynamicTextSecondaryColor,
                                primaryColor = dynamicPrimaryColor
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------
            // BOTTOM / INTERACTIVE FLOATING POPUPS & DIALOGS
            // -------------------------------------------------------------------------
            // Edit Profile Modal
            if (showEditProfileDialog) {
                AlertDialog(
                    onDismissRequest = { showEditProfileDialog = false },
                    title = { Text("Edit Profile Details", color = dynamicTextColor, fontWeight = FontWeight.Bold) },
                    containerColor = dynamicSurfaceColor,
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(
                                value = profileName,
                                onValueChange = { profileName = it },
                                label = { Text("Full Name") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = dynamicTextColor, unfocusedTextColor = dynamicTextColor)
                            )
                            OutlinedTextField(
                                value = profileUsername,
                                onValueChange = { profileUsername = it },
                                label = { Text("Username") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = dynamicTextColor, unfocusedTextColor = dynamicTextColor)
                            )
                            OutlinedTextField(
                                value = profileBio,
                                onValueChange = { profileBio = it },
                                label = { Text("Bio") },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = dynamicTextColor, unfocusedTextColor = dynamicTextColor)
                            )
                            OutlinedTextField(
                                value = profileCity,
                                onValueChange = { profileCity = it },
                                label = { Text("Home City") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = dynamicTextColor, unfocusedTextColor = dynamicTextColor)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showEditProfileDialog = false
                                Toast.makeText(context, "Profile details updated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = dynamicPrimaryColor)
                        ) {
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditProfileDialog = false }) {
                            Text("Cancel", color = dynamicTextSecondaryColor)
                        }
                    }
                )
            }

            // Blocked Users Management dialog
            if (showBlockedUsersDialog) {
                AlertDialog(
                    onDismissRequest = { showBlockedUsersDialog = false },
                    title = { Text("Blocked Users", color = dynamicTextColor, fontWeight = FontWeight.Bold) },
                    containerColor = dynamicSurfaceColor,
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("The following users cannot message you or find your live presence pins:", fontSize = 12.sp, color = dynamicTextSecondaryColor)
                            if (blockedUsersList.isEmpty()) {
                                Text("No blocked users.", color = dynamicTextColor, fontSize = 14.sp)
                            } else {
                                blockedUsersList.forEach { user ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("@$user", color = dynamicTextColor, fontWeight = FontWeight.SemiBold)
                                        TextButton(onClick = {
                                            blockedUsersList = blockedUsersList.filter { it != user }
                                            Toast.makeText(context, "Unblocked @$user", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Text("Unblock", color = dynamicPrimaryColor)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showBlockedUsersDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = dynamicPrimaryColor)
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Simulated App Crash Dialog
            if (showDevCrashSimulatedDialog) {
                AlertDialog(
                    onDismissRequest = { showDevCrashSimulatedDialog = false },
                    icon = { Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp)) },
                    title = { Text("SIMULATED EXCEPTION DEPLOYED", color = Color.Red, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
                    containerColor = Color(0xFF1E1E1E),
                    text = {
                        Column {
                            Text(
                                "Runtime Exception Stack Trace:\ncom.example.fomo.FatalNightlifeCrash: amapiano beat drop mismatch on track (UncleWaffles_Yahyuppiyah_STEM.ogg)\n\tat com.example.FomoAppKt.PlayBeat(FomoApp.kt:412)\n\tat com.example.FomoAppKt.access\$PlayBeat(FomoApp.kt:32)",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black)
                                    .padding(8.dp)
                                    .border(1.dp, Color.Red)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDevCrashSimulatedDialog = false
                                Toast.makeText(context, "Restarting background systems...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Reboot Engine", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT MODULES
// -------------------------------------------------------------------------

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = surfaceVariantColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = textSecColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search settings (e.g. NightGuard, dark mode...)", fontSize = 13.sp, color = textSecColor) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ProfileCard(
    name: String,
    username: String,
    city: String,
    surfaceColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color,
    onEditClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = surfaceColor,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = "https://i.pravatar.cc/150?img=11",
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text("@$username", color = textSecColor, fontSize = 14.sp)
                    Text("Verified Explorer • $city", color = textSecColor, fontSize = 12.sp)
                }
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = primaryColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Saved Places", "14", textColor, textSecColor)
                StatItem("Moments", "86", textColor, textSecColor)
                StatItem("Followers", "1.2K", textColor, textSecColor)
                StatItem("Following", "840", textColor, textSecColor)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, textColor: Color, textSecColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
        Text(label, fontSize = 11.sp, color = textSecColor)
    }
}

@Composable
fun QuickActions(
    surfaceColor: Color,
    textColor: Color,
    textSecColor: Color,
    onActionClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            "QUICK CONTROLS",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = textSecColor,
            letterSpacing = 1.sp
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val actions = listOf(
                Pair(Icons.Default.Security, "NightGuard"),
                Pair(Icons.Default.Bookmark, "Saved Places"),
                Pair(Icons.Default.Download, "Downloads"),
                Pair(Icons.Default.Block, "Blocked Users"),
                Pair(Icons.Default.PrivacyTip, "Privacy Checkup"),
                Pair(Icons.Default.Link, "Connected Accounts")
            )
            items(actions) { action ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onActionClick(action.second) }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(surfaceColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(action.first, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(action.second, fontSize = 11.sp, color = textColor)
                }
            }
        }
    }
}

@Composable
fun CreatorStudioSection(
    role: String,
    onCreatorStudioClick: () -> Unit,
    onBecomeCreatorClick: () -> Unit,
    onNavigateToCreatorSub: (String) -> Unit,
    surfaceColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    val isEligibleCreator = role != "Regular User"

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "CREATOR CENTER",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = textSecColor,
            modifier = Modifier.padding(vertical = 4.dp),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        if (!isEligibleCreator) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBecomeCreatorClick() },
                color = surfaceColor,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = primaryColor)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Become a Creator", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textColor)
                        Text("Apply for nightlife creator access", fontSize = 12.sp, color = textSecColor)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = textSecColor)
                }
            }
        } else {
            // Dashboard with Creator Studio controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = surfaceColor,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCreatorStudioClick() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Creator Studio Active", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textColor)
                                Text("Role: $role", fontSize = 12.sp, color = textSecColor)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = primaryColor)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = textSecColor.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("QUICK WORKSPACE COMMANDS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    when (role) {
                        "Nightclub Owner", "Bar Owner", "Lounge Owner" -> {
                            CreatorQuickGrid(
                                list = listOf("My Venues", "Club Lobby Manager", "Events", "Promotions", "Ticket Center", "Analytics", "Reviews", "Team Management"),
                                onCommandClick = onNavigateToCreatorSub,
                                textColor = textColor,
                                surfaceVariant = textSecColor.copy(alpha = 0.1f)
                            )
                        }
                        "Event Organizer" -> {
                            CreatorQuickGrid(
                                list = listOf("Events", "Tickets", "Promotions", "Analytics"),
                                onCommandClick = onNavigateToCreatorSub,
                                textColor = textColor,
                                surfaceVariant = textSecColor.copy(alpha = 0.1f)
                            )
                        }
                        "DJ" -> {
                            CreatorQuickGrid(
                                list = listOf("Performance Calendar", "Bookings", "Venue Invitations", "Analytics"),
                                onCommandClick = onNavigateToCreatorSub,
                                textColor = textColor,
                                surfaceVariant = textSecColor.copy(alpha = 0.1f)
                            )
                        }
                        "Artist" -> {
                            CreatorQuickGrid(
                                list = listOf("Performances", "Invitations", "Analytics"),
                                onCommandClick = onNavigateToCreatorSub,
                                textColor = textColor,
                                surfaceVariant = textSecColor.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorQuickGrid(
    list: List<String>,
    onCommandClick: (String) -> Unit,
    textColor: Color,
    surfaceVariant: Color
) {
    // Dynamic grid layout using rows
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val chunked = list.chunked(2)
        chunked.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCommandClick(item) },
                        color = surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item,
                            fontSize = 12.sp,
                            color = textColor,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // Handle odd count alignment
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

@Composable
fun SettingsGroup(
    title: String,
    items: List<SettingsItem>,
    onRowClick: (String) -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            letterSpacing = 1.sp
        )
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = surfaceColor,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsItemRow(
                        icon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = { onRowClick(item.title) },
                        textColor = textColor,
                        textSecColor = textSecColor
                    )
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = surfaceVariantColor.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    textColor: Color,
    textSecColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = textSecColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = textColor)
            Text(subtitle, fontSize = 11.sp, color = textSecColor)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = textSecColor)
    }
}

// -------------------------------------------------------------------------
// DETAILED SUBSECTION LAYOUTS
// -------------------------------------------------------------------------

@Composable
fun AccountSubSection(
    profileName: String,
    profileUsername: String,
    profileBio: String,
    profileEmail: String,
    profilePhone: String,
    profileBirthday: String,
    profileCity: String,
    passkeysEnabled: Boolean,
    onPasskeysToggle: (Boolean) -> Unit,
    twoFactorEnabled: Boolean,
    on2FAToggle: (Boolean) -> Unit,
    connectedGoogle: Boolean, onGoogleToggle: (Boolean) -> Unit,
    connectedApple: Boolean, onAppleToggle: (Boolean) -> Unit,
    connectedFacebook: Boolean, onFacebookToggle: (Boolean) -> Unit,
    connectedTikTok: Boolean, onTikTokToggle: (Boolean) -> Unit,
    connectedInstagram: Boolean, onInstagramToggle: (Boolean) -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color,
    onEditProfileClick: () -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Personal Info Card
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PERSONAL INFORMATION", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    IconButton(onClick = onEditProfileClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = primaryColor, modifier = Modifier.size(16.dp))
                    }
                }
                
                InfoRow("Name", profileName, textColor, textSecColor)
                InfoRow("Username", "@$profileUsername", textColor, textSecColor)
                InfoRow("Bio", profileBio, textColor, textSecColor)
                InfoRow("Email", profileEmail, textColor, textSecColor)
                InfoRow("Phone Number", profilePhone, textColor, textSecColor)
                InfoRow("Birthday", profileBirthday, textColor, textSecColor)
                InfoRow("City", profileCity, textColor, textSecColor)
            }
        }

        // Login & Security Settings
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("LOGIN & SECURITY", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                var showPasswordFields by remember { mutableStateOf(false) }
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPasswordFields = !showPasswordFields }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Change Password", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Update account password security", color = textSecColor, fontSize = 11.sp)
                        }
                        Icon(if (showPasswordFields) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = textSecColor)
                    }
                    if (showPasswordFields) {
                        Spacer(modifier = Modifier.height(10.dp))
                        var curPass by remember { mutableStateOf("") }
                        var newPass by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = curPass,
                            onValueChange = { curPass = it },
                            label = { Text("Current Password") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                                curPass = ""
                                newPass = ""
                                showPasswordFields = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Update Password", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))

                // Passkeys Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Passkeys", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Use biometrics for fast and secure access", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = passkeysEnabled, onCheckedChange = onPasskeysToggle)
                }

                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))

                // Two-Factor Auth Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Two-Factor Authentication", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Inject an extra shield of security on login", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = twoFactorEnabled, onCheckedChange = on2FAToggle)
                }
            }
        }

        // Active Devices list
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TRUSTED DEVICES & SESSIONS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Samsung Galaxy S24 Ultra", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Johannesburg, ZA • Active now", color = textSecColor, fontSize = 11.sp)
                    }
                    Text("Current Unit", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Apple iPad Pro", color = textColor, fontSize = 13.sp)
                        Text("Cape Town, ZA • Active 3 days ago", color = textSecColor, fontSize = 11.sp)
                    }
                    TextButton(onClick = { Toast.makeText(context, "Revoked session", Toast.LENGTH_SHORT).show() }) {
                        Text("Revoke", color = Color.Red, fontSize = 11.sp)
                    }
                }
            }
        }

        // Connected Social Accounts Toggles
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("CONNECTED ACCOUNTS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                SocialConnectRow("Google", connectedGoogle, onGoogleToggle, textColor, textSecColor)
                SocialConnectRow("Apple ID", connectedApple, onAppleToggle, textColor, textSecColor)
                SocialConnectRow("Facebook", connectedFacebook, onFacebookToggle, textColor, textSecColor)
                SocialConnectRow("TikTok", connectedTikTok, onTikTokToggle, textColor, textSecColor)
                SocialConnectRow("Instagram", connectedInstagram, onInstagramToggle, textColor, textSecColor)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, textColor: Color, textSecColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = textSecColor, fontSize = 13.sp, modifier = Modifier.weight(0.35f))
        Text(value, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.65f), textAlign = TextAlign.End)
    }
}

@Composable
fun SocialConnectRow(
    platform: String,
    connected: Boolean,
    onToggle: (Boolean) -> Unit,
    textColor: Color,
    textSecColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(platform, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(if (connected) "Linked to FOMO" else "Not Linked", color = textSecColor, fontSize = 11.sp)
        }
        Switch(checked = connected, onCheckedChange = onToggle)
    }
}

@Composable
fun PrivacySubSection(
    viewProfile: String, onViewProfileChange: (String) -> Unit,
    followMe: String, onFollowMeChange: (String) -> Unit,
    messageMe: String, onMessageMeChange: (String) -> Unit,
    mentionMe: String, onMentionMeChange: (String) -> Unit,
    tagMe: String, onTagMeChange: (String) -> Unit,
    momentsVisibility: String, onMomentsChange: (String) -> Unit,
    liveSharing: Boolean, onLiveSharingToggle: (Boolean) -> Unit,
    backgroundLocation: Boolean, onBgLocationToggle: (Boolean) -> Unit,
    highPrecision: Boolean, onPrecisionToggle: (Boolean) -> Unit,
    autoStopHours: Int, onAutoStopChange: (Int) -> Unit,
    locationVisibility: String, onLocVisChange: (String) -> Unit,
    contacts: List<String>,
    newContactName: String,
    onContactNameChange: (String) -> Unit,
    onAddContact: () -> Unit,
    onRemoveContact: (String) -> Unit,
    walkMeHomeEnabled: Boolean, onWalkMeHomeToggle: (Boolean) -> Unit,
    walkMeHomeDur: Int, onWalkMeHomeDurChange: (Int) -> Unit,
    walkMeHomeNotif: Boolean, onWalkMeHomeNotifToggle: (Boolean) -> Unit,
    safetyTimer: Int, onSafetyTimerChange: (Int) -> Unit,
    safetyEscalation: String, onSafetyEscalationChange: (String) -> Unit,
    safetyActions: String, onSafetyActionsChange: (String) -> Unit,
    buddyRequests: Boolean, onBuddyReqToggle: (Boolean) -> Unit,
    buddyAutoAccept: Boolean, onBuddyAutoAcceptToggle: (Boolean) -> Unit,
    buddyLocShare: Boolean, onBuddyLocShareToggle: (Boolean) -> Unit,
    onOpenBlockedUsers: () -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    val privacyOptions = listOf("Everyone", "Followers", "Friends", "Nobody")
    val momentsOptions = listOf("Public", "Followers", "Friends", "Only Me")

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Profile Privacy Card
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PROFILE PRIVACY SCOPE", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                PrivacyDropdownItem("Who can view profile", viewProfile, privacyOptions, onViewProfileChange, textColor, textSecColor, surfaceColor)
                PrivacyDropdownItem("Who can follow me", followMe, privacyOptions, onFollowMeChange, textColor, textSecColor, surfaceColor)
                PrivacyDropdownItem("Who can message me", messageMe, privacyOptions, onMessageMeChange, textColor, textSecColor, surfaceColor)
                PrivacyDropdownItem("Who can mention me", mentionMe, privacyOptions, onMentionMeChange, textColor, textSecColor, surfaceColor)
                PrivacyDropdownItem("Who can tag me", tagMe, privacyOptions, onTagMeChange, textColor, textSecColor, surfaceColor)
            }
        }

        // Moments Privacy Card
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MOMENTS VISIBILITY", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                PrivacyDropdownItem("Default moments visibility", momentsVisibility, momentsOptions, onMomentsChange, textColor, textSecColor, surfaceColor)
            }
        }

        // Location Privacy Card
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("LOCATION PRIVACY CONTROL", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Location Sharing", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Share active movement inside venues", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = liveSharing, onCheckedChange = onLiveSharingToggle)
                }

                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Location Sentry", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Allow FOMO tracker to check vibe while in background", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = backgroundLocation, onCheckedChange = onBgLocationToggle)
                }

                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Location Precision Mode", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (highPrecision) "Precise GPS Coordinates" else "Approximate District/City radius", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = highPrecision, onCheckedChange = onPrecisionToggle)
                }

                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))

                Column {
                    Text("Auto Stop Share Live Location", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Expires in $autoStopHours hours", color = textSecColor, fontSize = 11.sp)
                    Slider(
                        value = autoStopHours.toFloat(),
                        onValueChange = { onAutoStopChange(it.toInt()) },
                        valueRange = 1f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(activeTrackColor = primaryColor, thumbColor = primaryColor)
                    )
                }

                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))

                PrivacyDropdownItem("Who can see location visibility", locationVisibility, privacyOptions, onLocVisChange, textColor, textSecColor, surfaceColor)
            }
        }

        // NightGuard Safety Control Center
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor), border = BorderStroke(1.dp, Color(0xFFFF2D55).copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NIGHTGUARD SAFETY CONTROLS", color = Color(0xFFFF2D55), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                // Trusted Contacts Section
                Text("TRUSTED CONTACTS", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                contacts.forEach { contact ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = textSecColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(contact, color = textColor, fontSize = 13.sp)
                        }
                        IconButton(onClick = { onRemoveContact(contact) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = onContactNameChange,
                        placeholder = { Text("Add emergency contact...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onAddContact, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55))) {
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))

                // Walk Me Home Settings
                Text("WALK ME HOME BEHAVIOR", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Walk Me Home Sentry", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Alerts contacts if you do not arrive", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = walkMeHomeEnabled, onCheckedChange = onWalkMeHomeToggle)
                }

                if (walkMeHomeEnabled) {
                    Column {
                        Text("Walk Me Home Duration: $walkMeHomeDur minutes", color = textColor, fontSize = 13.sp)
                        Slider(
                            value = walkMeHomeDur.toFloat(),
                            onValueChange = { onWalkMeHomeDurChange(it.toInt()) },
                            valueRange = 10f..120f,
                            steps = 11,
                            colors = SliderDefaults.colors(activeTrackColor = Color(0xFFFF2D55), thumbColor = Color(0xFFFF2D55))
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Push Progress Alerts", color = textColor, fontSize = 14.sp)
                        Text("Notify contacts of milestones", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = walkMeHomeNotif, onCheckedChange = onWalkMeHomeNotifToggle)
                }

                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))

                // Safety Check Sentry
                Text("SAFETY CHECK PREFERENCES", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column {
                    Text("Periodic check-in timer: $safetyTimer minutes", color = textColor, fontSize = 13.sp)
                    Slider(
                        value = safetyTimer.toFloat(),
                        onValueChange = { onSafetyTimerChange(it.toInt()) },
                        valueRange = 5f..60f,
                        steps = 11,
                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFFFF2D55), thumbColor = Color(0xFFFF2D55))
                    )
                }

                Column {
                    Text("Escalation Protocol", color = textColor, fontSize = 13.sp)
                    Text(safetyEscalation, color = Color(0xFFFF2D55), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Column {
                    Text("Primary Emergency Action", color = textColor, fontSize = 13.sp)
                    Text(safetyActions, color = Color(0xFFFF2D55), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))

                // Buddy Pair Settings
                Text("BUDDY PAIR SETTINGS", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Allow Buddy Pair Requests", color = textColor, fontSize = 14.sp)
                        Text("Allow nearby friends to pair", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = buddyRequests, onCheckedChange = onBuddyReqToggle)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Auto Accept Friends", color = textColor, fontSize = 14.sp)
                        Text("Skip confirmations for close circle", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = buddyAutoAccept, onCheckedChange = onBuddyAutoAcceptToggle)
                }
            }
        }

        // Quick button to open Blocked Users
        Button(
            onClick = onOpenBlockedUsers,
            colors = ButtonDefaults.buttonColors(containerColor = surfaceVariantColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Block, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manage Blocked Users List", color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PrivacyDropdownItem(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    textColor: Color,
    textSecColor: Color,
    surfaceColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(selectedValue, color = textSecColor, fontSize = 11.sp)
            }
            Icon(Icons.Default.ArrowDropDown, null, tint = textSecColor)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(surfaceColor)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = textColor) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationsSubSection(
    notifLikes: Boolean, onLikesToggle: (Boolean) -> Unit,
    notifComments: Boolean, onCommentsToggle: (Boolean) -> Unit,
    notifMentions: Boolean, onMentionsToggle: (Boolean) -> Unit,
    notifReplies: Boolean, onRepliesToggle: (Boolean) -> Unit,
    notifFollowers: Boolean, onFollowersToggle: (Boolean) -> Unit,
    notifNearbyEvents: Boolean, onNearbyEventsToggle: (Boolean) -> Unit,
    notifOpenings: Boolean, onOpeningsToggle: (Boolean) -> Unit,
    notifClosingSoon: Boolean, onClosingSoonToggle: (Boolean) -> Unit,
    notifWeekendGuide: Boolean, onWeekendGuideToggle: (Boolean) -> Unit,
    notifCityHighlights: Boolean, onCityHighlightsToggle: (Boolean) -> Unit,
    notifMessages: Boolean, onMessagesToggle: (Boolean) -> Unit,
    notifCalls: Boolean, onCallsToggle: (Boolean) -> Unit,
    notifGroups: Boolean, onGroupsToggle: (Boolean) -> Unit,
    notifChatMentions: Boolean, onChatMentionsToggle: (Boolean) -> Unit,
    notifCreatorVenue: Boolean, onCreatorVenueToggle: (Boolean) -> Unit,
    notifCreatorBookings: Boolean, onCreatorBookingsToggle: (Boolean) -> Unit,
    notifCreatorPerf: Boolean, onCreatorPerfToggle: (Boolean) -> Unit,
    notifCreatorSales: Boolean, onCreatorSalesToggle: (Boolean) -> Unit,
    notifCreatorVerif: Boolean, onCreatorVerifToggle: (Boolean) -> Unit,
    notifSafetyNightGuard: Boolean, onSafetyNightGuardToggle: (Boolean) -> Unit,
    notifSafetyBuddy: Boolean, onSafetyBuddyToggle: (Boolean) -> Unit,
    notifSafetySOS: Boolean, onSafetySOSToggle: (Boolean) -> Unit,
    notifSafetyCheck: Boolean, onSafetyCheckToggle: (Boolean) -> Unit,
    notifMarketingNews: Boolean, onMarketingNewsToggle: (Boolean) -> Unit,
    notifMarketingOffers: Boolean, onMarketingOffersToggle: (Boolean) -> Unit,
    notifMarketingProduct: Boolean, onMarketingProductToggle: (Boolean) -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NotificationCategory("SOCIAL ACTIVITY", primaryColor, surfaceColor, surfaceVariantColor, textColor, textSecColor, listOf(
            NotifToggleData("Likes", "Notify when someone likes my moment", notifLikes, onLikesToggle),
            NotifToggleData("Comments", "Notify of comments on moments", notifComments, onCommentsToggle),
            NotifToggleData("Mentions", "Alert of tags and mentions in chats", notifMentions, onMentionsToggle),
            NotifToggleData("Replies", "Alert for replies on comments", notifReplies, onRepliesToggle),
            NotifToggleData("New Followers", "Alert on new follows", notifFollowers, onFollowersToggle)
        ))

        NotificationCategory("DISCOVER MAP ALERTS", primaryColor, surfaceColor, surfaceVariantColor, textColor, textSecColor, listOf(
            NotifToggleData("Nearby Events", "Notify when hot event starts", notifNearbyEvents, onNearbyEventsToggle),
            NotifToggleData("Lounge Openings", "Opening night notices of local spots", notifOpenings, onOpeningsToggle),
            NotifToggleData("Closing Soon", "Alert me if favorite spot is locking down", notifClosingSoon, onClosingSoonToggle),
            NotifToggleData("Weekend Guide", "Weekly Amapiano JHB lineup digest", notifWeekendGuide, onWeekendGuideToggle),
            NotifToggleData("City Highlights", "Curated trend updates of the city", notifCityHighlights, onCityHighlightsToggle)
        ))

        NotificationCategory("CHATS & GROUPS", primaryColor, surfaceColor, surfaceVariantColor, textColor, textSecColor, listOf(
            NotifToggleData("Direct Messages", "Alert on personal inbox messages", notifMessages, onMessagesToggle),
            NotifToggleData("Voice & Video Calls", "Alert of ringings of voice calls", notifCalls, onCallsToggle),
            NotifToggleData("Group Mentions", "Alert me when tagged in group rooms", notifGroups, onGroupsToggle),
            NotifToggleData("Chat Room Ping", "High priority alerts of prep rooms", notifChatMentions, onChatMentionsToggle)
        ))

        NotificationCategory("CREATOR STUDIO OPS", primaryColor, surfaceColor, surfaceVariantColor, textColor, textSecColor, listOf(
            NotifToggleData("Venue Updates", "Notify of venue verification reviews", notifCreatorVenue, onCreatorVenueToggle),
            NotifToggleData("Live Bookings", "Notify of venue headliner requests", notifCreatorBookings, onCreatorBookingsToggle),
            NotifToggleData("Performance Requests", "Notify when venue invites DJ", notifCreatorPerf, onCreatorPerfToggle),
            NotifToggleData("Ticket Sales Velocity", "Reports ticket purchase volumes", notifCreatorSales, onCreatorSalesToggle),
            NotifToggleData("Creator Verification", "Verification progress milestones", notifCreatorVerif, onCreatorVerifToggle)
        ))

        NotificationCategory("NIGHTGUARD SAFETY SENTRY", Color(0xFFFF2D55), surfaceColor, surfaceVariantColor, textColor, textSecColor, listOf(
            NotifToggleData("NightGuard Alerts", "Critical safety monitoring milestones", notifSafetyNightGuard, onSafetyNightGuardToggle),
            NotifToggleData("Buddy Alerts", "Alert when paired buddies move out of bounds", notifSafetyBuddy, onSafetyBuddyToggle),
            NotifToggleData("SOS Panic Broadcasts", "Triggers high-priority panic alerts", notifSafetySOS, onSafetySOSToggle),
            NotifToggleData("Safety Check-ins", "Notify me of automated checkin timers", notifSafetyCheck, onSafetyCheckToggle)
        ))

        NotificationCategory("MARKETING REWARDS", primaryColor, surfaceColor, surfaceVariantColor, textColor, textSecColor, listOf(
            NotifToggleData("Platform News", "Periodic updates from FOMO HQ", notifMarketingNews, onMarketingNewsToggle),
            NotifToggleData("Exclusive Offers", "Flash Drop beverage vouchers & early access VIP keys", notifMarketingOffers, onMarketingOffersToggle),
            NotifToggleData("Product Updates", "New camera features and maps layers", notifMarketingProduct, onMarketingProductToggle)
        ))
    }
}

data class NotifToggleData(
    val title: String,
    val subtitle: String,
    val state: Boolean,
    val onToggle: (Boolean) -> Unit
)

@Composable
fun NotificationCategory(
    categoryTitle: String,
    accentColor: Color,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    items: List<NotifToggleData>
) {
    Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(categoryTitle, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            items.forEachIndexed { idx, it ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(it.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(it.subtitle, color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = it.state, onCheckedChange = it.onToggle)
                }
                if (idx < items.size - 1) {
                    HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
fun LocationMapsSubSection(
    gpsGranted: Boolean, onGpsToggle: (Boolean) -> Unit,
    bgLocGranted: Boolean, onBgLocToggle: (Boolean) -> Unit,
    gpsAccuracy: String, onGpsAccuracyChange: (String) -> Unit,
    currentCity: String, onCityChange: (String) -> Unit,
    routeEngine: String, onRouteEngineChange: (String) -> Unit,
    mapStyle: String, onMapStyleChange: (String) -> Unit,
    trafficLayer: Boolean, onTrafficToggle: (Boolean) -> Unit,
    heatmapLayer: Boolean, onHeatmapToggle: (Boolean) -> Unit,
    buildings3D: Boolean, onBuildings3DToggle: (Boolean) -> Unit,
    compassEnabled: Boolean, onCompassToggle: (Boolean) -> Unit,
    downloadedCities: List<String>,
    downloadInput: String,
    onDownloadInputChange: (String) -> Unit,
    isDownloading: Boolean,
    progress: Float,
    onDownloadClick: () -> Unit,
    onDeleteMap: (String) -> Unit,
    onUpdateAll: () -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    val accuracyOptions = listOf("High", "Balanced", "Low Power")
    val engines = listOf("FOMO Routing", "Google Maps", "Waze", "Apple Maps")
    val styles = listOf("Classic", "Dark", "Satellite", "Hybrid")

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Location Services
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("LOCATION SERVICES", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Location Access Permission", color = textColor, fontSize = 14.sp)
                        Text("Needed for real-time map presence", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = gpsGranted, onCheckedChange = onGpsToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Location Sentry", color = textColor, fontSize = 14.sp)
                        Text("Check in automatically even in pocket", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = bgLocGranted, onCheckedChange = onBgLocToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                PrivacyDropdownItem("GPS Accuracy Level", gpsAccuracy, accuracyOptions, onGpsAccuracyChange, textColor, textSecColor, surfaceColor)
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                OutlinedTextField(
                    value = currentCity,
                    onValueChange = onCityChange,
                    label = { Text("Active Operational City") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            }
        }

        // Navigation Engine
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("NAVIGATION ROUTING ENGINE", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                PrivacyDropdownItem("Default Route Engine", routeEngine, engines, onRouteEngineChange, textColor, textSecColor, surfaceColor)
            }
        }

        // Map settings
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MAP LAYERS & DISPLAY", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                PrivacyDropdownItem("Map Style theme", mapStyle, styles, onMapStyleChange, textColor, textSecColor, surfaceColor)
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Traffic Density Layer", color = textColor, fontSize = 14.sp)
                    Switch(checked = trafficLayer, onCheckedChange = onTrafficToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Live Heatmap Layer", color = textColor, fontSize = 14.sp)
                    Switch(checked = heatmapLayer, onCheckedChange = onHeatmapToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("3D Buildings wireframes", color = textColor, fontSize = 14.sp)
                    Switch(checked = buildings3D, onCheckedChange = onBuildings3DToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Compass overlay", color = textColor, fontSize = 14.sp)
                    Switch(checked = compassEnabled, onCheckedChange = onCompassToggle)
                }
            }
        }

        // Offline maps
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OFFLINE MAPS SENTRY", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Pre-download city maps to navigate without mobile data coverage.", color = textSecColor, fontSize = 11.sp)

                downloadedCities.forEach { city ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(city, color = textColor, fontSize = 13.sp)
                        IconButton(onClick = { onDeleteMap(city) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = downloadInput,
                        onValueChange = onDownloadInputChange,
                        placeholder = { Text("Download new city...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDownloadClick,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        enabled = !isDownloading
                    ) {
                        Text(if (isDownloading) "Down..." else "Get Map", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onUpdateAll,
                    colors = ButtonDefaults.buttonColors(containerColor = surfaceVariantColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update All Offline Maps", color = textColor, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AppearanceSubSection(
    selectedTheme: String, onThemeChange: (String) -> Unit,
    selectedAccent: String, onAccentChange: (String) -> Unit,
    mapTheme: String, onMapThemeChange: (String) -> Unit,
    animsEnabled: Boolean, onAnimsToggle: (Boolean) -> Unit,
    reduceMotion: Boolean, onReduceMotionToggle: (Boolean) -> Unit,
    blurEnabled: Boolean, onBlurToggle: (Boolean) -> Unit,
    hapticsEnabled: Boolean, onHapticsToggle: (Boolean) -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    val themes = listOf("Dark", "Light", "System")
    val accents = listOf("Electric Teal", "Magenta", "Amber", "Purple", "Blue")
    val mapStyles = listOf("Classic", "Dark", "Satellite", "Hybrid")

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // App theme options
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("THEME OVERRIDES", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    themes.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        Button(
                            onClick = { onThemeChange(theme) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) primaryColor else surfaceVariantColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(theme, color = if (isSelected) Color.Black else textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Color Palette Selector
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PLATFORM ACCENT COLOR", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Select accent highlights across your FOMO discovery experience", color = textSecColor, fontSize = 11.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    accents.forEach { acc ->
                        val color = when (acc) {
                            "Electric Teal" -> Color(0xFF00E5FF)
                            "Magenta" -> Color(0xFFFF2D55)
                            "Amber" -> Color(0xFFFFB300)
                            "Purple" -> Color(0xFFB026FF)
                            "Blue" -> Color(0xFF2979FF)
                            else -> Color(0xFF00E5FF)
                        }
                        val isSelected = selectedAccent == acc
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    2.dp,
                                    if (isSelected) textColor else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { onAccentChange(acc) }
                        )
                    }
                }
            }
        }

        // Map styles
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DEFAULT MAP ENVIRONMENT", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                PrivacyDropdownItem("Map styling", mapTheme, mapStyles, onMapThemeChange, textColor, textSecColor, surfaceColor)
            }
        }

        // Motion & Haptics
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("UI ANIMATIONS & HAPTICS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable transition animations", color = textColor, fontSize = 14.sp)
                    Switch(checked = animsEnabled, onCheckedChange = onAnimsToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Reduce Motion", color = textColor, fontSize = 14.sp)
                    Switch(checked = reduceMotion, onCheckedChange = onReduceMotionToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Blur visual effects", color = textColor, fontSize = 14.sp)
                    Switch(checked = blurEnabled, onCheckedChange = onBlurToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Haptic Feedback ticks", color = textColor, fontSize = 14.sp)
                    Switch(checked = hapticsEnabled, onCheckedChange = onHapticsToggle)
                }
            }
        }
    }
}

@Composable
fun ExperienceSubSection(
    autoRefresh: Boolean, onAutoRefreshToggle: (Boolean) -> Unit,
    heroAnims: Boolean, onHeroAnimsToggle: (Boolean) -> Unit,
    heatmapAnims: Boolean, onHeatmapAnimsToggle: (Boolean) -> Unit,
    liveCounters: Boolean, onLiveCountersToggle: (Boolean) -> Unit,
    cameraMode: String, onCameraModeChange: (String) -> Unit,
    videoQuality: String, onVideoQualityChange: (String) -> Unit,
    saveOriginals: Boolean, onSaveOriginalsToggle: (Boolean) -> Unit,
    autoHDR: Boolean, onAutoHDRToggle: (Boolean) -> Unit,
    gridLines: Boolean, onGridLinesToggle: (Boolean) -> Unit,
    flashPref: String, onFlashPrefChange: (String) -> Unit,
    mediaAutoDownload: Boolean, onMediaAutoDownloadToggle: (Boolean) -> Unit,
    readReceipts: Boolean, onReadReceiptsToggle: (Boolean) -> Unit,
    typingIndicators: Boolean, onTypingIndicatorsToggle: (Boolean) -> Unit,
    linkPreviews: Boolean, onLinkPreviewsToggle: (Boolean) -> Unit,
    distanceUnit: String, onDistanceUnitChange: (String) -> Unit,
    radiusValue: Float, onRadiusValueChange: (Float) -> Unit,
    contentPref: String, onContentPrefChange: (String) -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    val modes = listOf("Photo", "Video", "Vibe Scan")
    val videoOptions = listOf("480p", "720p", "1080p @ 30fps", "1080p @ 60fps", "4K @ 30fps")
    val flashPrefs = listOf("Off", "On", "Auto", "Torch")
    val distUnits = listOf("Kilometers", "Miles")
    val contentPrefs = listOf("Trending Nightlife", "Clubs & Lounges Only", "DJs & Live Performances", "Sparsely Crowded")

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Explore behavior
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("EXPLORE & FEEDS CONTROLS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto Refresh Live Feeds", color = textColor, fontSize = 14.sp)
                    Switch(checked = autoRefresh, onCheckedChange = onAutoRefreshToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Hero Animations", color = textColor, fontSize = 14.sp)
                    Switch(checked = heroAnims, onCheckedChange = onHeroAnimsToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Heatmap Animations", color = textColor, fontSize = 14.sp)
                    Switch(checked = heatmapAnims, onCheckedChange = onHeatmapAnimsToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Live Capacity Counters", color = textColor, fontSize = 14.sp)
                    Switch(checked = liveCounters, onCheckedChange = onLiveCountersToggle)
                }
            }
        }

        // Camera preferences
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CAMERA CAPTURE ENGINE", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                PrivacyDropdownItem("Default Camera Mode", cameraMode, modes, onCameraModeChange, textColor, textSecColor, surfaceColor)
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                PrivacyDropdownItem("Capture Video Quality", videoQuality, videoOptions, onVideoQualityChange, textColor, textSecColor, surfaceColor)
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Save Originals to gallery", color = textColor, fontSize = 14.sp)
                    Switch(checked = saveOriginals, onCheckedChange = onSaveOriginalsToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto HDR processing", color = textColor, fontSize = 14.sp)
                    Switch(checked = autoHDR, onCheckedChange = onAutoHDRToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Grid Lines overlay", color = textColor, fontSize = 14.sp)
                    Switch(checked = gridLines, onCheckedChange = onGridLinesToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                PrivacyDropdownItem("Flash Preference", flashPref, flashPrefs, onFlashPrefChange, textColor, textSecColor, surfaceColor)
            }
        }

        // Chats behavior
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CHAT & MEDIA SETTINGS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Media Auto-Download", color = textColor, fontSize = 14.sp)
                    Switch(checked = mediaAutoDownload, onCheckedChange = onMediaAutoDownloadToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Read Receipts", color = textColor, fontSize = 14.sp)
                    Switch(checked = readReceipts, onCheckedChange = onReadReceiptsToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Typing Indicators", color = textColor, fontSize = 14.sp)
                    Switch(checked = typingIndicators, onCheckedChange = onTypingIndicatorsToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Rich Link Previews", color = textColor, fontSize = 14.sp)
                    Switch(checked = linkPreviews, onCheckedChange = onLinkPreviewsToggle)
                }
            }
        }

        // Discover preferences
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DISCOVER PREFERENCES", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                PrivacyDropdownItem("Distance Unit", distanceUnit, distUnits, onDistanceUnitChange, textColor, textSecColor, surfaceColor)
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Column {
                    Text("Default Search Radius: ${radiusValue.toInt()} km", color = textColor, fontSize = 14.sp)
                    Slider(
                        value = radiusValue,
                        onValueChange = onRadiusValueChange,
                        valueRange = 1f..50f,
                        colors = SliderDefaults.colors(activeTrackColor = primaryColor, thumbColor = primaryColor)
                    )
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                PrivacyDropdownItem("Content Recommendation Bias", contentPref, contentPrefs, onContentPrefChange, textColor, textSecColor, surfaceColor)
            }
        }
    }
}

@Composable
fun StorageSubSection(
    storageUsed: String,
    cacheSize: String,
    mediaSize: String,
    offlineMapsSize: String,
    downloadsSize: String,
    onClearCache: () -> Unit,
    onDeleteDownloads: () -> Unit,
    onOptimizeStorage: () -> Unit,
    wifiOnlyUploads: Boolean, onWifiOnlyToggle: (Boolean) -> Unit,
    lowerImageQuality: Boolean, onLowerImageToggle: (Boolean) -> Unit,
    lowerVideoQuality: Boolean, onLowerVideoToggle: (Boolean) -> Unit,
    disableAutoPlay: Boolean, onDisableAutoPlayToggle: (Boolean) -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Usage Card
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("STORAGE USAGE DECK", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                InfoRow("App storage used", storageUsed, textColor, textSecColor)
                InfoRow("Cached media files size", cacheSize, textColor, textSecColor)
                InfoRow("User captured moments media", mediaSize, textColor, textSecColor)
                InfoRow("Offline map files size", offlineMapsSize, textColor, textSecColor)
                InfoRow("Downloads size", downloadsSize, textColor, textSecColor)
            }
        }

        // Actions
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("MAINTENANCE ACTIONS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Button(
                    onClick = onClearCache,
                    colors = ButtonDefaults.buttonColors(containerColor = surfaceVariantColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Temporary Cache", color = textColor, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDeleteDownloads,
                    colors = ButtonDefaults.buttonColors(containerColor = surfaceVariantColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Saved Offline Content", color = textColor, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOptimizeStorage,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Run Storage Optimizer Sentry", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Data saver
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DATA SAVER SENTRY", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WiFi Only uploads", color = textColor, fontSize = 14.sp)
                        Text("Hold stories upload until connected to Wi-Fi", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = wifiOnlyUploads, onCheckedChange = onWifiOnlyToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Compress uploaded images", color = textColor, fontSize = 14.sp)
                    Switch(checked = lowerImageQuality, onCheckedChange = onLowerImageToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Compress streaming videos", color = textColor, fontSize = 14.sp)
                    Switch(checked = lowerVideoQuality, onCheckedChange = onLowerVideoToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Disable Auto-Play videos", color = textColor, fontSize = 14.sp)
                    Switch(checked = disableAutoPlay, onCheckedChange = onDisableAutoPlayToggle)
                }
            }
        }
    }
}

@Composable
fun AccessibilitySubSection(
    largeText: Boolean, onLargeTextToggle: (Boolean) -> Unit,
    highContrast: Boolean, onHighContrastToggle: (Boolean) -> Unit,
    colorBlindMode: String, onColorBlindChange: (String) -> Unit,
    reduceMotion: Boolean, onReduceMotionToggle: (Boolean) -> Unit,
    reduceTransparency: Boolean, onReduceTransToggle: (Boolean) -> Unit,
    largeButtons: Boolean, onLargeButtonsToggle: (Boolean) -> Unit,
    voiceFeedback: Boolean, onVoiceToggle: (Boolean) -> Unit,
    hapticFeedback: Boolean, onHapticToggle: (Boolean) -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    val cbModes = listOf("None", "Protanopia", "Deuteranopia", "Tritanopia", "Monochromacy")

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Vision
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("VISION ENHANCEMENTS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Large Text scaling", color = textColor, fontSize = 14.sp)
                    Switch(checked = largeText, onCheckedChange = onLargeTextToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("High Contrast colors", color = textColor, fontSize = 14.sp)
                    Switch(checked = highContrast, onCheckedChange = onHighContrastToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                PrivacyDropdownItem("Color Blind Mode Correction", colorBlindMode, cbModes, onColorBlindChange, textColor, textSecColor, surfaceColor)
            }
        }

        // Motion
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("REDUCED MOTION SENTINEL", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Reduce transitions animations", color = textColor, fontSize = 14.sp)
                    Switch(checked = reduceMotion, onCheckedChange = onReduceMotionToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Reduce transparency", color = textColor, fontSize = 14.sp)
                    Switch(checked = reduceTransparency, onCheckedChange = onReduceTransToggle)
                }
            }
        }

        // Interaction
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TOUCH & FEEDBACK CO-PILOT", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Large Touch Targets Buttons", color = textColor, fontSize = 14.sp)
                    Switch(checked = largeButtons, onCheckedChange = onLargeButtonsToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Audio Voice feedback assist", color = textColor, fontSize = 14.sp)
                    Switch(checked = voiceFeedback, onCheckedChange = onVoiceToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Interactive Haptic responses", color = textColor, fontSize = 14.sp)
                    Switch(checked = hapticFeedback, onCheckedChange = onHapticToggle)
                }
            }
        }
    }
}

@Composable
fun SupportSubSection(
    faqExpandedIndex: Int?,
    onFaqExpand: (Int) -> Unit,
    supportText: String,
    onSupportTextChange: (String) -> Unit,
    onSupportSubmit: () -> Unit,
    bugText: String,
    onBugTextChange: (String) -> Unit,
    onBugSubmit: () -> Unit,
    suggestionText: String,
    onSuggestionTextChange: (String) -> Unit,
    onSuggestionSubmit: () -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    val faqs = listOf(
        "How do I activate SOS Emergency Alerts?" to "To trigger a manual SOS, tap the NightGuard Shield floating action button on Map Screen or click SOS inside your Profile. Your live coordinates will immediately transmit to Emergency Contacts.",
        "What is the FOMO Vibe Health Index?" to "The Vibe Health Index measures crowd dynamics, music quality feedback, and traffic velocity to grade regional nightlife. This ensures you never arrive to a dead spot.",
        "How can I withdraw my ticket payout earnings?" to "As verified creator, navigate to Creator Studio settings tab and bind your bank account. Payout is processed weekly on Mondays at 09:00 AM."
    )

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // FAQs
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("FREQUENTLY ASKED QUESTIONS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                faqs.forEachIndexed { index, faq ->
                    val isExpanded = faqExpandedIndex == index
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFaqExpand(index) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(faq.first, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.9f))
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                null,
                                tint = textSecColor
                            )
                        }
                        if (isExpanded) {
                            Text(faq.second, color = textSecColor, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                        if (index < faqs.size - 1) {
                            HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        // Contact Support Form
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CONTACT FOMO SUPPORT DECK", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                OutlinedTextField(
                    value = supportText,
                    onValueChange = onSupportTextChange,
                    placeholder = { Text("What is happening? Describe your challenge...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
                Button(
                    onClick = onSupportSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Submit Ticket", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Report a bug Form
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TRANSMIT BUG REPORT", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                OutlinedTextField(
                    value = bugText,
                    onValueChange = onBugTextChange,
                    placeholder = { Text("Steps to reproduce bug. E.g. Camera black screen on photo capture...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
                Button(
                    onClick = onBugSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Send Report", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Suggest a feature Form
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SUGGEST NEW PLATFORM FEATURES", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                OutlinedTextField(
                    value = suggestionText,
                    onValueChange = onSuggestionTextChange,
                    placeholder = { Text("Describe your idea to refine nightlife discovery...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
                Button(
                    onClick = onSuggestionSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Dispatch Suggestion", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun LegalSubSection(
    surfaceColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LegalCard("PRIVACY POLICY", "Effective Date: July 21, 2026\n\nAt FOMO, we prioritize your nightlife security above all else. This Privacy Policy details the exact cryptographic standards used to safeguard live location footprints. Live coordination data is exclusively stored in device RAM caches and fully deleted 24 hours post departure.", surfaceColor, textColor, textSecColor, primaryColor)
        LegalCard("TERMS OF SERVICE", "Effective Date: July 21, 2026\n\nBy accessing the FOMO platform discovery engines, you explicitly consent to automated safety checks when walk-me-home sends panics alerts. Fake SOS triggers may result in platform bans.", surfaceColor, textColor, textSecColor, primaryColor)
        LegalCard("COOKIE PREFERENCES", "We leverage cookies and localized secure sandboxes on your device to preserve map cache footprints and credentials. You can opt-out in storage configurations.", surfaceColor, textColor, textSecColor, primaryColor)
        LegalCard("SOFTWARE LICENSES", "This platform utilizes open source components, including Google Maps SDKs, Jetpack Compose UI wrappers, Coil Async image wrappers, and Roborazzi visual screen testing tools.", surfaceColor, textColor, textSecColor, primaryColor)
    }
}

@Composable
fun LegalCard(title: String, content: String, surfaceColor: Color, textColor: Color, textSecColor: Color, primaryColor: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(content, color = textSecColor, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun AboutSubSection(
    versionClickCount: Int,
    onVersionClick: () -> Unit,
    developerModeUnlocked: Boolean,
    onOpenDeveloperOptions: () -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // App Identity Info
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(56.dp)
                )
                Text("FOMO DISCOVERY APP", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Billion-Dollar Scale Nightlife Discovery Platform", color = textSecColor, fontSize = 12.sp, textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(12.dp))

                // Interactive version element to trigger Developer mode
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onVersionClick() }
                        .padding(8.dp)
                ) {
                    Text("App Version: 4.8.2-JHB", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Build: #104820", color = textSecColor, fontSize = 11.sp)
                    if (!developerModeUnlocked) {
                        Text("(Tap 7 times to unlock developer menu)", color = textSecColor.copy(alpha = 0.7f), fontSize = 10.sp)
                    } else {
                        Text("✓ Developer Mode Unlocked", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Release notes list
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("RELEASE NOTES - V4.8", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Text("• NightGuard Safety Walk Me Home auto sentry fully implemented.", color = textColor, fontSize = 12.sp)
                Text("• Interactive Club Lobby streams integration with fast video caching.", color = textColor, fontSize = 12.sp)
                Text("• High performance offline map indexing for Johannesburg & Cape Town.", color = textColor, fontSize = 12.sp)
            }
        }

        // System information
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SYSTEM INFORMATION DETAILS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                InfoRow("FOMO API Core Version", "v1.4.12", textColor, textSecColor)
                InfoRow("Local DB Schema version", "42-ROOM", textColor, textSecColor)
                InfoRow("Secure Sandbox coordinates Sentry", "ACTIVE", textColor, textSecColor)
                InfoRow("Platform Engine Integration", "Jetpack Compose Native", textColor, textSecColor)
            }
        }

        // Shortcut to developer option if unlocked
        if (developerModeUnlocked) {
            Button(
                onClick = onOpenDeveloperOptions,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Developer Options Console", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DeveloperSubSection(
    cpuUsage: Int,
    ramUsage: Int,
    fps: Int,
    flag3DHeatmap: Boolean, onFlag3DToggle: (Boolean) -> Unit,
    flagBetaVideo: Boolean, onFlagBetaVideoToggle: (Boolean) -> Unit,
    flagExperimentalRoute: Boolean, onFlagExperimentalRouteToggle: (Boolean) -> Unit,
    flagP2PSafety: Boolean, onFlagP2PSafetyToggle: (Boolean) -> Unit,
    flagAmapianoAI: Boolean, onFlagAmapianoAIToggle: (Boolean) -> Unit,
    onCrashClick: () -> Unit,
    onResetTutorials: () -> Unit,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    textColor: Color,
    textSecColor: Color,
    primaryColor: Color
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Performance Monitors
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("REAL-TIME PERFORMANCE MONITOR", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DevMetricBlock("CPU Sentry", "$cpuUsage %", textColor, textSecColor, primaryColor)
                    DevMetricBlock("JVM Memory", "$ramUsage MB", textColor, textSecColor, primaryColor)
                    DevMetricBlock("Rendering FPS", "$fps fps", textColor, textSecColor, primaryColor)
                }

                // Interactive render trace graph using Box heights
                Spacer(modifier = Modifier.height(10.dp))
                Text("UI Engine Render load trace:", color = textSecColor, fontSize = 10.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(surfaceVariantColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val renderTraceHeights = listOf(10, 18, 32, 14, 28, 42, 38, 22, 12, 16, 26, 40, cpuUsage * 1.5f)
                    renderTraceHeights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(h.toInt().dp)
                                .background(primaryColor, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }

        // Feature flags
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DOCK FEATURE FLAGS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable 3D Heatmaps Sentry", color = textColor, fontSize = 13.sp)
                        Text("Renders structural vector shapes", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = flag3DHeatmap, onCheckedChange = onFlag3DToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Beta Video story upload", color = textColor, fontSize = 13.sp)
                        Text("Enables H.265 compression", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = flagBetaVideo, onCheckedChange = onFlagBetaVideoToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Experimental GPS routing Sentry", color = textColor, fontSize = 13.sp)
                        Text("Overrides native Google APIs", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = flagExperimentalRoute, onCheckedChange = onFlagExperimentalRouteToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("P2P Mesh safety broadcasts", color = textColor, fontSize = 13.sp)
                        Text("Enable mesh networking for SOS", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = flagP2PSafety, onCheckedChange = onFlagP2PSafetyToggle)
                }
                HorizontalDivider(color = surfaceVariantColor.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Amapiano AI Beat Matcher", color = textColor, fontSize = 13.sp)
                        Text("Analyze tempo match in video capture", color = textSecColor, fontSize = 11.sp)
                    }
                    Switch(checked = flagAmapianoAI, onCheckedChange = onFlagAmapianoAIToggle)
                }
            }
        }

        // Network Inspector (Interactive JSON endpoint clicks)
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("LIVE NETWORK INSPECTOR", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Select endpoint to inspect live response payload", color = textSecColor, fontSize = 11.sp)

                var activeInspectorPayload by remember { mutableStateOf<String?>(null) }
                
                NetworkCallRow("GET", "/v1/discover/venues", "200 OK", "45ms", onClick = {
                    activeInspectorPayload = "{\n  \"status\": \"success\",\n  \"venuesCount\": 18,\n  \"heatmapIndex\": 0.94,\n  \"updatedAt\": 1721516002\n}"
                }, textSecColor)
                NetworkCallRow("POST", "/v1/safety/sos", "201 Created", "120ms", onClick = {
                    activeInspectorPayload = "{\n  \"sos_id\": \"sos_4829\",\n  \"broadcasting\": true,\n  \"contactsNotified\": 2,\n  \"sentryCode\": \"SHIELD_ACTIVE\"\n}"
                }, textSecColor)
                NetworkCallRow("GET", "/v1/creator/analytics", "200 OK", "85ms", onClick = {
                    activeInspectorPayload = "{\n  \"ticketSales\": 48200,\n  \"reachImpressions\": 12400,\n  \"conversions\": 86,\n  \"activeCampaigns\": 2\n}"
                }, textSecColor)

                if (activeInspectorPayload != null) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Response Payload:", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Collapse [X]", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { activeInspectorPayload = null })
                        }
                        Text(
                            text = activeInspectorPayload!!,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(8.dp)
                                .border(0.5.dp, primaryColor)
                        )
                    }
                }
            }
        }

        // Debug Console logs Sentry
        Card(colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DEBUG CONSOLE LOGS", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DebugLogText("I/FomoApp: Initializing Map coordinate system...", Color.Green)
                        DebugLogText("D/LocationSentry: Fetching high precision GPS coordinates.", Color.Cyan)
                        DebugLogText("W/BeatDetector: Audio packet dropped count #12", Color.Yellow)
                        DebugLogText("I/CreatorStudio: Loading analytics indexes...", Color.Green)
                        DebugLogText("D/NetworkInspector: POST /v1/safety/sos successfully verified", Color.Cyan)
                        DebugLogText("E/VibeHealth: Error indexing empty venue coordinates list", Color.Red)
                    }
                }
            }
        }

        // Tech Maintenance button controls
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onCrashClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simulate App Crash Exception", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onResetTutorials,
                colors = ButtonDefaults.buttonColors(containerColor = surfaceVariantColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Tutorials & Smart Cards", color = textColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DevMetricBlock(title: String, valStr: String, textColor: Color, textSecColor: Color, primaryColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = textSecColor, fontSize = 11.sp)
        Text(valStr, color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NetworkCallRow(method: String, endpoint: String, status: String, delay: String, onClick: () -> Unit, textSecColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.7f)) {
            Surface(
                color = if (method == "GET") Color.Green.copy(alpha = 0.15f) else Color.Cyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(method, color = if (method == "GET") Color.Green else Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(endpoint, color = Color.White, fontSize = 12.sp, overflow = TextOverflow.Ellipsis, maxLines = 1)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.weight(0.3f)) {
            Text(status, color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Text(delay, color = textSecColor, fontSize = 10.sp)
        }
    }
}

@Composable
fun DebugLogText(txt: String, color: Color) {
    Text(txt, color = color, fontSize = 10.sp)
}

// -------------------------------------------------------------------------
// SEARCH INDEXING LOGIC
// -------------------------------------------------------------------------

data class SearchItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val targetSection: String
)

fun filterSettingsItems(query: String, devUnlocked: Boolean, role: String): List<SearchItem> {
    val searchable = buildList {
        // Account
        add(SearchItem(Icons.Default.Person, "Personal Information", "View or change name, email, phone, city", "Account"))
        add(SearchItem(Icons.Default.Lock, "Password & Security", "Update passkeys, credentials, trusted devices", "Account"))
        add(SearchItem(Icons.Default.Security, "Two-Factor Authentication (2FA)", "Inject shield logins", "Account"))
        add(SearchItem(Icons.Default.Link, "Connected Accounts", "Manage links to Google, Facebook, TikTok", "Account"))
        
        // Privacy & Safety
        add(SearchItem(Icons.Default.Security, "NightGuard Controls", "Emergency contacts, walk me home timers, buddy alert sentry", "Privacy & Safety"))
        add(SearchItem(Icons.Default.Place, "Live Location Sharing", "Configure maps precision & auto-stop parameters", "Privacy & Safety"))
        add(SearchItem(Icons.Default.Block, "Blocked Users", "Manage list of blocked user handles", "Privacy & Safety"))
        add(SearchItem(Icons.Default.RemoveRedEye, "Profile Privacy Settings", "Control who can message or tag my handle", "Privacy & Safety"))
        
        // Notifications
        add(SearchItem(Icons.Default.Notifications, "Notification Configurations", "Toggles for likes, comments, nearby events, marketing", "Notifications"))
        add(SearchItem(Icons.Default.VolumeUp, "Chats & Group Alerts", "Manage DM sound alerts", "Notifications"))
        
        // Location & Maps
        add(SearchItem(Icons.Default.Map, "Offline Maps Sentry", "Download city maps files for Rosebank, Cape Town", "Location & Maps"))
        add(SearchItem(Icons.Default.Navigation, "Navigation Engine", "Switch engine defaults (FOMO routing, Google Maps, Waze)", "Location & Maps"))
        add(SearchItem(Icons.Default.Layers, "Map Layers styles", "Toggle heatmaps, 3D buildings, traffic flows", "Location & Maps"))
        
        // Appearance
        add(SearchItem(Icons.Default.LightMode, "Dark / Light Mode theme", "Override default Elegant dark visual layouts", "Appearance"))
        add(SearchItem(Icons.Default.Palette, "Accent Color Highlight", "Electric teal, magenta, purple, blue highlights", "Appearance"))
        add(SearchItem(Icons.Default.MotionPhotosOn, "Animations & Haptic ticks", "Reduce motion & enable blur layers", "Appearance"))
        
        // Experience
        add(SearchItem(Icons.Default.Camera, "Camera Options", "Capture video quality, auto HDR, grid lines flash options", "Experience"))
        add(SearchItem(Icons.Default.SettingsInputComponent, "Discover Search Radius", "Configure distance radius in kilometers", "Experience"))
        
        // Storage & Data
        add(SearchItem(Icons.Default.Storage, "Clear Cache", "Wipe temporary stored media images to free 1.2 GB space", "Clear Cache"))
        add(SearchItem(Icons.Default.DataSaverOn, "Data Saver configurations", "Lower image & video uploads quality over cellular networks", "Storage & Data"))
        
        // Accessibility
        add(SearchItem(Icons.Default.Accessibility, "Vision & Contrast Scaling", "Color blind corrections, large touch target buttons", "Accessibility"))
        
        // Support
        add(SearchItem(Icons.Default.Help, "Help center FAQS", "Emergency SOS guides & creator withdrawals guidelines", "Support"))
        add(SearchItem(Icons.Default.BugReport, "Report app bugs", "Transmit stack traces & steps to reproduce issues", "Support"))
        
        // Creator Studio
        if (role != "Regular User") {
            add(SearchItem(Icons.Default.Campaign, "Creator Studio Dashboard", "Inspect venues, events, ticket sales analytics", "Creator Studio"))
        }
    }

    return searchable.filter {
        it.title.contains(query, ignoreCase = true) ||
        it.subtitle.contains(query, ignoreCase = true) ||
        it.targetSection.contains(query, ignoreCase = true)
    }
}
