package com.iptvplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.iptvplayer.app.player.PlayerManager
import com.iptvplayer.app.ui.navigation.AppNavGraph
import com.iptvplayer.app.ui.theme.IptvTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerManager: PlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            IptvTheme {
                AppNavGraph()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playerManager.resume()
    }

    // The player is app-scoped, so it would keep playing audio after Home without this
    override fun onStop() {
        playerManager.stop()
        super.onStop()
    }
}
