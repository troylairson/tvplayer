package com.iptvplayer.app.ui.screen.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView

@Composable
fun PlayerScreen(
    initialStreamId: Int,
    onOpenGuide: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val playerState   by viewModel.playerManager.state.collectAsStateWithLifecycle()
    val firstFrame    by viewModel.playerManager.firstFrameRendered.collectAsStateWithLifecycle()
    val currentChannel by viewModel.currentChannel.collectAsStateWithLifecycle()
    val epgNow        by viewModel.epgNow.collectAsStateWithLifecycle()
    val epgUpcoming   by viewModel.epgUpcoming.collectAsStateWithLifecycle()
    val isBannerVisible by viewModel.isBannerVisible.collectAsStateWithLifecycle()

    LaunchedEffect(initialStreamId) {
        viewModel.init(initialStreamId)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp   -> { viewModel.channelUp();   true }
                    Key.DirectionDown -> { viewModel.channelDown(); true }
                    Key.Back          -> { onOpenGuide();            true }
                    Key.Menu, Key.DirectionCenter, Key.Enter -> {
                        if (isBannerVisible) viewModel.dismissBanner()
                        else viewModel.showBanner()
                        true
                    }
                    else -> {
                        viewModel.showBanner()
                        false
                    }
                }
            },
    ) {
        // Video surface
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = viewModel.playerManager.player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Tuning overlay — visible until first frame is rendered
        val showTuning = remember(playerState, firstFrame) {
            !firstFrame && playerState is com.iptvplayer.app.player.PlayerState.Buffering
        }
        if (showTuning) {
            TuningOverlay(modifier = Modifier.fillMaxSize())
        }

        // OSD banner — slides in from top
        val channel = currentChannel
        if (channel != null) {
            AnimatedVisibility(
                visible = isBannerVisible,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                OsdBanner(
                    channel = channel,
                    epgNow = epgNow,
                    epgUpcoming = epgUpcoming,
                )
            }
        }

        // Error state
        if (playerState is com.iptvplayer.app.player.PlayerState.Error) {
            val msg = (playerState as com.iptvplayer.app.player.PlayerState.Error).message
            androidx.tv.material3.Text(
                text = "Playback error: $msg",
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
