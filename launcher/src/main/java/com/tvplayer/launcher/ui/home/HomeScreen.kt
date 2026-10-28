package com.tvplayer.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvplayer.launcher.data.settings.AppTile
import com.tvplayer.launcher.ui.art.*
import com.tvplayer.launcher.ui.components.ArtTile
import com.tvplayer.launcher.ui.components.Header
import com.tvplayer.launcher.ui.components.TrayButton
import com.tvplayer.launcher.ui.theme.*
import kotlinx.coroutines.launch

private enum class HomeItem { LIVE_TV, NOSTALGIA, MOVIES, MUSIC, APPS, SPA, SETTINGS }

@Composable
fun HomeScreen(
    isDefaultHome: Boolean,
    onOpenSpa: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenLauncherSettings: () -> Unit,
    onPickApp: (AppTile) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // As the device's home screen Back has nowhere to go; as a regular app it exits as usual
    BackHandler(enabled = isDefaultHome) {}

    // Restore focus to the last tile used when coming back from a sub-screen
    var lastFocused by rememberSaveable { mutableStateOf(HomeItem.LIVE_TV) }
    val requesters = remember { HomeItem.entries.associateWith { FocusRequester() } }
    LaunchedEffect(Unit) { runCatching { requesters.getValue(lastFocused).requestFocus() } }

    fun Modifier.homeItem(item: HomeItem) = this
        .focusRequester(requesters.getValue(item))
        .onFocusChanged { if (it.isFocused) lastFocused = item }

    fun launch(tile: AppTile) {
        scope.launch { if (!viewModel.launch(tile)) onPickApp(tile) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 14.dp)
            .onPreviewKeyEvent { event ->
                // Menu anywhere on the home screen opens launcher settings
                if (event.type == KeyEventType.KeyDown && event.key == Key.Menu) {
                    onOpenLauncherSettings(); true
                } else false
            },
    ) {
        Header()

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().weight(0.62f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ArtTile(
                color = Forest,
                title = "Live TV",
                titleSize = 46.sp,
                onClick = { launch(AppTile.LIVE_TV) },
                onLongClick = { onPickApp(AppTile.LIVE_TV) },
                modifier = Modifier.weight(1.45f).fillMaxHeight().homeItem(HomeItem.LIVE_TV),
            ) { LiveTvArt(Modifier.fillMaxSize()) }

            ArtTile(
                color = Cocoa,
                title = "NostalgiaTV",
                subtitle = "Classic channels",
                onClick = { launch(AppTile.NOSTALGIA) },
                onLongClick = { onPickApp(AppTile.NOSTALGIA) },
                modifier = Modifier.weight(1.05f).fillMaxHeight().homeItem(HomeItem.NOSTALGIA),
            ) { NostalgiaArt(Modifier.fillMaxSize()) }

            ArtTile(
                color = Rust,
                title = "Movies",
                trailing = "›",
                onClick = { launch(AppTile.MOVIES) },
                onLongClick = { onPickApp(AppTile.MOVIES) },
                modifier = Modifier.weight(0.95f).fillMaxHeight().homeItem(HomeItem.MOVIES),
            ) { MoviesArt(Modifier.fillMaxSize()) }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth().weight(0.38f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ArtTile(
                color = Plum,
                title = "Music",
                titleSize = 26.sp,
                onClick = { launch(AppTile.MUSIC) },
                onLongClick = { onPickApp(AppTile.MUSIC) },
                modifier = Modifier.weight(1f).fillMaxHeight().homeItem(HomeItem.MUSIC),
            ) { MusicArt(Modifier.fillMaxSize()) }

            ArtTile(
                color = Navy,
                title = "Apps",
                titleSize = 26.sp,
                onClick = onOpenApps,
                modifier = Modifier.weight(1f).fillMaxHeight().homeItem(HomeItem.APPS),
            ) { AppsArt(Modifier.fillMaxSize()) }

            ArtTile(
                color = Terracotta,
                title = "Spa",
                titleSize = 26.sp,
                subtitle = state.spaSubtitle,
                onClick = onOpenSpa,
                modifier = Modifier.weight(1.25f).fillMaxHeight().homeItem(HomeItem.SPA),
            ) { SpaSteamArt(Modifier.fillMaxSize(), anchorX = 0.80f) }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.align(androidx.compose.ui.Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TrayButton(
                icon = Icons.Outlined.Settings,
                contentDescription = "Settings",
                onClick = viewModel::openSystemSettings,
                // Hold for launcher settings (Home Assistant, tile apps)
                onLongClick = onOpenLauncherSettings,
                modifier = Modifier.homeItem(HomeItem.SETTINGS),
            )
        }
    }
}
