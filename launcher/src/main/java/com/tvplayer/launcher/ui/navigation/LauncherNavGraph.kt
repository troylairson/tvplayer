package com.tvplayer.launcher.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tvplayer.launcher.data.settings.AppTile
import com.tvplayer.launcher.ui.apps.AppPickerScreen
import com.tvplayer.launcher.ui.apps.AppsScreen
import com.tvplayer.launcher.ui.home.HomeScreen
import com.tvplayer.launcher.ui.settings.LauncherSettingsScreen
import com.tvplayer.launcher.ui.spa.SpaScreen
import com.tvplayer.launcher.ui.theme.Ink

private object Routes {
    const val HOME = "home"
    const val SPA = "spa"
    const val APPS = "apps"
    const val SETTINGS = "settings"
    const val PICKER = "picker/{tile}"
    fun picker(tile: AppTile) = "picker/${tile.name}"
}

/**
 * [homePresses] changes each time the Home button is pressed while the launcher is already showing.
 * [isDefaultHome]: Sunset is the device's home screen, so Back on Home must not close it.
 */
@Composable
fun LauncherNavGraph(homePresses: Int, isDefaultHome: Boolean) {
    val navController = rememberNavController()

    LaunchedEffect(homePresses) {
        if (homePresses > 0) navController.popBackStack(Routes.HOME, inclusive = false)
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    isDefaultHome = isDefaultHome,
                    onOpenSpa = { navController.navigate(Routes.SPA) },
                    onOpenApps = { navController.navigate(Routes.APPS) },
                    onOpenLauncherSettings = { navController.navigate(Routes.SETTINGS) },
                    onPickApp = { tile -> navController.navigate(Routes.picker(tile)) },
                )
            }
            composable(Routes.SPA) {
                SpaScreen(onOpenLauncherSettings = { navController.navigate(Routes.SETTINGS) })
            }
            composable(Routes.APPS) {
                AppsScreen()
            }
            composable(Routes.SETTINGS) {
                LauncherSettingsScreen(onPickApp = { tile -> navController.navigate(Routes.picker(tile)) })
            }
            composable(
                route = Routes.PICKER,
                arguments = listOf(navArgument("tile") { type = NavType.StringType }),
            ) { entry ->
                val tile = entry.arguments?.getString("tile")
                    ?.let { name -> AppTile.entries.firstOrNull { it.name == name } }
                    ?: return@composable
                AppPickerScreen(tile = tile, onDone = { navController.popBackStack() })
            }
        }
    }
}
