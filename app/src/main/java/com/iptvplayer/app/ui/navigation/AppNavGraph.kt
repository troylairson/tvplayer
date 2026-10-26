package com.iptvplayer.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iptvplayer.app.ui.screen.guide.GuideScreen
import com.iptvplayer.app.ui.screen.player.PlayerScreen
import com.iptvplayer.app.ui.screen.settings.SettingsScreen
import com.iptvplayer.app.ui.screen.settings.SettingsViewModel

sealed class Screen(val route: String) {
    object Settings : Screen("settings")
    object Guide    : Screen("guide")
    object Player   : Screen("player/{streamId}") {
        fun withId(id: Int) = "player/$id"
    }
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    // SettingsViewModel at graph scope decides start destination
    val settingsVm: SettingsViewModel = hiltViewModel()
    val hasCredentials by settingsVm.hasCredentials.collectAsState()

    val startDestination = if (hasCredentials == true) Screen.Guide.route else Screen.Settings.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Settings.route) {
            SettingsScreen(
                onConnected = {
                    navController.navigate(Screen.Guide.route) {
                        popUpTo(Screen.Settings.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Guide.route) {
            GuideScreen(
                onChannelSelected = { streamId ->
                    navController.navigate(Screen.Player.withId(streamId))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("streamId") { type = NavType.IntType }),
        ) { backStack ->
            val streamId = backStack.arguments?.getInt("streamId") ?: return@composable
            PlayerScreen(
                initialStreamId = streamId,
                onOpenGuide = { navController.popBackStack() },
            )
        }
    }
}
