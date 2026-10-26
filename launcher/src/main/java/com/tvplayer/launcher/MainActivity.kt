package com.tvplayer.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tvplayer.launcher.ui.navigation.LauncherNavGraph
import com.tvplayer.launcher.ui.theme.LauncherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Bumped when Home is pressed while we're already in front, so the nav graph can return to the home screen
    private val homePresses = mutableIntStateOf(0)

    // Until Sunset replaces the stock launcher it's a regular app, and Back on Home should exit it
    private val isDefaultHome = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            LauncherTheme {
                LauncherNavGraph(
                    homePresses = homePresses.intValue,
                    isDefaultHome = isDefaultHome.value,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-checked on every resume: the default home can change while we're in the background
        isDefaultHome.value = resolveDefaultHome() == packageName
    }

    private fun resolveDefaultHome(): String? =
        packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.intValue++
    }
}
