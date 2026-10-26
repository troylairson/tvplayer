package com.iptvplayer.app.ui.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.app.data.repository.ChannelRepository
import com.iptvplayer.app.data.repository.EpgRepository
import com.iptvplayer.app.domain.model.Channel
import com.iptvplayer.app.domain.model.EpgEvent
import com.iptvplayer.app.domain.model.nowPlaying
import com.iptvplayer.app.domain.model.upcoming
import com.iptvplayer.app.player.PlayerManager
import com.iptvplayer.app.util.minuteTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TUNE_DEBOUNCE_MS = 300L
private const val RECENT_AFTER_MS  = 5_000L

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    val playerManager: PlayerManager,
) : ViewModel() {

    private var channels: List<Channel> = emptyList()
    private var currentIndex: Int = 0
    private var initialized = false

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel

    // Derived from the shared EPG cache, so a slow response for a channel we've already
    // zapped past can never overwrite the banner — it just lands in the cache.
    private val epgForChannel = combine(_currentChannel, epgRepository.events, minuteTicker()) { channel, events, now ->
        channel?.let { events[it.id] }.orEmpty() to now
    }

    val epgNow: StateFlow<EpgEvent?> = epgForChannel
        .map { (events, now) -> events.nowPlaying(now) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val epgUpcoming: StateFlow<List<EpgEvent>> = epgForChannel
        .map { (events, now) -> events.upcoming(now) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isBannerVisible = MutableStateFlow(false)
    val isBannerVisible: StateFlow<Boolean> = _isBannerVisible

    private var bannerJob: Job? = null
    private var tuneJob: Job? = null
    private var recentJob: Job? = null

    fun init(initialStreamId: Int) {
        if (initialized) return
        initialized = true
        playerManager.setMuted(false)
        viewModelScope.launch {
            // Normally a no-op; fetches when restored after process death or opened via deep link
            channelRepository.ensureLoaded()
            channels = channelRepository.channels.first()
            val idx = channels.indexOfFirst { it.id == initialStreamId }
            currentIndex = if (idx >= 0) idx else 0
            channels.getOrNull(currentIndex)?.let { channel ->
                _currentChannel.value = channel
                revealBanner()
                tune(channel)
            }
        }
    }

    fun channelUp() = step(-1)

    fun channelDown() = step(+1)

    private fun step(delta: Int) {
        if (channels.isEmpty()) return
        currentIndex = (currentIndex + delta + channels.size) % channels.size
        val channel = channels[currentIndex]
        // Banner follows every press; the stream only starts once the key stops repeating
        _currentChannel.value = channel
        revealBanner()
        tuneJob?.cancel()
        tuneJob = viewModelScope.launch {
            delay(TUNE_DEBOUNCE_MS)
            tune(channel)
        }
    }

    /** User asked for the banner — also refresh EPG if the cached programs have run out. */
    fun showBanner() {
        _currentChannel.value?.let { channel ->
            viewModelScope.launch { epgRepository.fetchShortEpg(channel.id) }  // no-op while fresh
        }
        revealBanner()
    }

    private fun revealBanner() {
        _isBannerVisible.value = true
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            delay(5_000)
            _isBannerVisible.value = false
        }
    }

    fun dismissBanner() {
        bannerJob?.cancel()
        _isBannerVisible.value = false
    }

    private fun tune(channel: Channel) {
        playerManager.play(channel.streamUrl)
        viewModelScope.launch { epgRepository.fetchShortEpg(channel.id) }
        // Only count it as watched once the viewer stays — surfing past shouldn't fill Recents
        recentJob?.cancel()
        recentJob = viewModelScope.launch {
            delay(RECENT_AFTER_MS)
            channelRepository.recordRecent(channel.id)
        }
    }
}
