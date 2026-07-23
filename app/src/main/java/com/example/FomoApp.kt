package com.example

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.core.navigation.CameraRoute
import com.example.core.navigation.ChatsRoute
import com.example.core.navigation.CreatorStudioRoute
import com.example.core.navigation.DiscoverRoute
import com.example.core.navigation.FeedRoute
import com.example.core.navigation.MapRoute
import com.example.core.navigation.ProfileRoute
import com.example.core.navigation.SettingsRoute
import com.example.core.navigation.ClubLobbyRoute
import com.example.core.navigation.EventsRoute
import com.example.core.navigation.EventDetailsRoute
import androidx.navigation.toRoute
import com.example.feature.camera.CameraScreen
import com.example.feature.chats.ChatsScreen
import com.example.feature.discover.DiscoverScreen
import com.example.feature.discover.EventsHomeScreen
import com.example.feature.discover.EventDetailsScreen
import com.example.feature.feed.FeedScreen
import com.example.feature.map.MapScreen
import com.example.feature.clublobby.ClubLobbyScreen

data class TopLevelRoute<T : Any>(
    val name: String,
    val route: T,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val topLevelRoutes = listOf(
    TopLevelRoute("Discover", DiscoverRoute, Icons.Filled.Explore, Icons.Outlined.Explore),
    TopLevelRoute("Feed", FeedRoute, Icons.Filled.Home, Icons.Outlined.Home),
    TopLevelRoute("Camera", CameraRoute, Icons.Filled.PhotoCamera, Icons.Outlined.PhotoCamera),
    TopLevelRoute("Map", MapRoute, Icons.Filled.Place, Icons.Outlined.Place),
    TopLevelRoute("Chats", ChatsRoute, Icons.Filled.Forum, Icons.Outlined.Forum)
)

@Composable
fun FomoApp() {
    val navController = rememberNavController()
    val currentUser = androidx.compose.runtime.remember {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }
    }
    val startDestinationRoute: Any = if (currentUser != null) DiscoverRoute else com.example.core.navigation.WelcomeRoute

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.hierarchy?.any {
                it.route?.contains("WelcomeRoute") == true
            } != true

            if (showBottomBar) {
                NavigationBar(
                    containerColor = com.example.ui.theme.SurfaceContainerDark,
                    contentColor = com.example.ui.theme.OnBackgroundDark
                ) {
                    topLevelRoutes.forEach { topLevelRoute ->
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.route?.contains(topLevelRoute.route::class.simpleName ?: "") == true
                        } == true
                        NavigationBarItem(
                            selected = isSelected,
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = com.example.ui.theme.PrimaryDark,
                                selectedTextColor = com.example.ui.theme.PrimaryDark,
                                indicatorColor = com.example.ui.theme.PrimaryContainerDark,
                                unselectedIconColor = com.example.ui.theme.OnSurfaceVariantDark,
                                unselectedTextColor = com.example.ui.theme.OnSurfaceVariantDark
                            ),
                            onClick = {
                                navController.navigate(topLevelRoute.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) topLevelRoute.selectedIcon else topLevelRoute.unselectedIcon,
                                    contentDescription = topLevelRoute.name
                                )
                            },
                            label = { Text(topLevelRoute.name) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestinationRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<com.example.core.navigation.WelcomeRoute> {
                com.example.feature.auth.WelcomeAuthScreen(
                    onAuthComplete = {
                        navController.navigate(com.example.core.navigation.DiscoverRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable<DiscoverRoute> { 
                DiscoverScreen(
                    onProfileClick = { navController.navigate(ProfileRoute) },
                    onNavigateToEvents = { navController.navigate(EventsRoute) },
                    onNavigateToEventDetails = { id -> navController.navigate(EventDetailsRoute(id)) },
                    onNavigateToLobby = { id -> navController.navigate(ClubLobbyRoute(id)) },
                    onNavigateToNightGuard = { navController.navigate(com.example.core.navigation.NightGuardRoute) },
                    onNavigateToCountryPackHub = { navController.navigate(com.example.core.navigation.CountryPackHubRoute) },
                    onNavigateToPlansWorkspace = { navController.navigate(com.example.core.navigation.PlansWorkspaceRoute) }
                ) 
            }
            composable<EventsRoute> {
                EventsHomeScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToEventDetails = { id -> navController.navigate(EventDetailsRoute(id)) },
                    onNavigateToLobby = { id -> navController.navigate(ClubLobbyRoute(id)) }
                )
            }
            composable<EventDetailsRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<EventDetailsRoute>()
                EventDetailsScreen(
                    eventId = route.eventId,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToLobby = { id -> navController.navigate(ClubLobbyRoute(id)) }
                )
            }
            composable<ProfileRoute> {
                com.example.feature.profile.ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(SettingsRoute) }
                )
            }
            composable<SettingsRoute> {
                com.example.feature.settings.SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onCreatorStudioClick = { navController.navigate(CreatorStudioRoute) },
                    onLogoutClick = {
                        try {
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        navController.navigate(com.example.core.navigation.WelcomeRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable<CreatorStudioRoute> {
                com.example.feature.creatorstudio.CreatorStudioScreen(onBackClick = { navController.popBackStack() })
            }
            composable<FeedRoute> { 
                FeedScreen(onNavigateToLobby = { id -> navController.navigate(ClubLobbyRoute(id)) }) 
            }
            composable<CameraRoute> { 
                CameraScreen(
                    onCloseClick = {
                        navController.navigate(DiscoverRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToLobby = { lobbyId ->
                        navController.navigate(ClubLobbyRoute(lobbyId))
                    }
                )
            }
            composable<MapRoute> { 
                MapScreen(
                    onNavigateToLobby = { id -> navController.navigate(ClubLobbyRoute(id)) },
                    onNavigateToNightGuard = { navController.navigate(com.example.core.navigation.NightGuardRoute) }
                ) 
            }
            composable<ChatsRoute> { ChatsScreen() }
            composable<com.example.core.navigation.NightGuardRoute> {
                com.example.feature.nightguard.NightGuardScreen(onBackClick = { navController.popBackStack() })
            }
            composable<com.example.core.navigation.CountryPackHubRoute> {
                com.example.feature.discover.CountryPackHubScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable<com.example.core.navigation.PlansWorkspaceRoute> {
                com.example.feature.discover.PlansWorkspaceScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNightGuard = { navController.navigate(com.example.core.navigation.NightGuardRoute) },
                    onNavigateToMap = { navController.navigate(MapRoute) }
                )
            }
            composable<ClubLobbyRoute> { backStackEntry ->
                val route = backStackEntry.destination.route
                val context = LocalContext.current
                ClubLobbyScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToRoute = {
                        Toast.makeText(context, "Routing to FOMO Club Rosebank Main Entrance...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
