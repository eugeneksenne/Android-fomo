package com.example.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object DiscoverRoute


@Serializable
data object FeedRoute

@Serializable
data object CameraRoute

@Serializable
data object MapRoute

@Serializable
data object ChatsRoute

@Serializable
data object ProfileRoute

@Serializable
data object SettingsRoute

@Serializable
data object CreatorStudioRoute

@Serializable
data class ClubLobbyRoute(val venueId: String = "fomo_club")

@Serializable
data object NightGuardRoute

@Serializable
data object EventsRoute

@Serializable
data object CountryPackHubRoute

@Serializable
data object PlansWorkspaceRoute

@Serializable
data class EventDetailsRoute(val eventId: String)
