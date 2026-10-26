package com.iptvplayer.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

sealed class PlayerState {
    object Idle      : PlayerState()
    object Buffering : PlayerState()
    object Playing   : PlayerState()
    data class Error(val message: String) : PlayerState()
}

@OptIn(UnstableApi::class)
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    okHttpClient: OkHttpClient,
) {
    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val state: StateFlow<PlayerState> = _state

    /** URL of the loaded stream (playing or stopped in background), null when nothing is loaded. */
    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl

    private val _firstFrameRendered = MutableStateFlow(false)
    val firstFrameRendered: StateFlow<Boolean> = _firstFrameRendered

    // Defined before player so the .addListener() call below can reference it
    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            // On failure ExoPlayer drops to STATE_IDLE right after onPlayerError — don't clobber the error
            if (player.playerError != null) return
            _state.value = when (playbackState) {
                Player.STATE_BUFFERING -> PlayerState.Buffering
                Player.STATE_READY     -> PlayerState.Playing
                Player.STATE_IDLE      -> PlayerState.Idle
                else                   -> PlayerState.Idle
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // Live HLS fell behind the playlist window (e.g. after a stall) — rejoin at the live edge
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition()
                player.prepare()
                return
            }
            _state.value = PlayerState.Error(error.message ?: "Playback error")
        }

        override fun onRenderedFirstFrame() {
            _firstFrameRendered.value = true
        }
    }

    val player: ExoPlayer = ExoPlayer.Builder(context)
        // OkHttp follows Xtream's cross-host / http<->https stream redirects, which
        // DefaultHttpDataSource refuses by default
        .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(okHttpClient)))
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs                    */ 2_000,
                    /* maxBufferMs                    */ 10_000,
                    /* bufferForPlaybackMs            */ 1_500,
                    /* bufferForPlaybackAfterRebufferMs */ 2_000,
                ).build()
        )
        .setRenderersFactory(
            DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        )
        .setTrackSelector(
            DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setSelectUndeterminedTextLanguage(true)
                )
            }
        )
        .build()
        .also { exo ->
            exo.setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
            exo.addListener(listener)
        }

    val hasMedia: Boolean get() = player.currentMediaItem != null

    fun play(streamUrl: String) {
        // Already playing this stream (e.g. the guide preview) — keep the buffer, no re-tune
        val currentUrl = player.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUrl == streamUrl && player.playerError == null && player.playbackState != Player.STATE_IDLE) {
            player.playWhenReady = true
            return
        }
        _currentUrl.value = streamUrl
        _firstFrameRendered.value = false
        _state.value = PlayerState.Buffering
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    /** The guide preview plays the same stream muted. */
    fun setMuted(muted: Boolean) {
        player.volume = if (muted) 0f else 1f
    }

    /** Drop the current stream entirely, e.g. after switching provider credentials. */
    fun reset() {
        player.stop()
        player.clearMediaItems()
        _currentUrl.value = null
        _firstFrameRendered.value = false
        _state.value = PlayerState.Idle
    }

    /** Stop decoding when the app leaves the foreground; keeps the media item for [resume]. */
    fun stop() {
        player.stop()
    }

    /** Rejoin the live edge of whatever was playing before [stop]. */
    fun resume() {
        if (player.currentMediaItem == null || player.isPlaying) return
        _firstFrameRendered.value = false
        _state.value = PlayerState.Buffering
        player.seekToDefaultPosition()
        player.prepare()
        player.playWhenReady = true
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }
}
