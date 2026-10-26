package com.iptvplayer.app.ui.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.app.data.repository.ChannelRepository
import com.iptvplayer.app.data.repository.EpgRepository
import com.iptvplayer.app.domain.model.Channel
import com.iptvplayer.app.domain.model.EpgEvent
import com.iptvplayer.app.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    val playerManager: PlayerManager,
) : ViewModel() {

    private var channels: List<Channel> = emptyList()
    private var currentIndex: Int = 0

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel

    private val _epgNow = MutableStateFlow<EpgEvent?>(null)
    val epgNow: StateFlow<EpgEvent?> = _epgNow

    private val _epgUpcoming = MutableStateFlow<List<EpgEvent>>(emptyList())
    val epgUpcoming: StateFlow<List<EpgEvent>> = _epgUpcoming

    private val _isBannerVisible = MutableStateFlow(false)
    val isBannerVisible: StateFlow<Boolean> = _isBannerVisible

    private var bannerJob: Job? = null

    fun init(initialStreamId: Int) {
        channels = channelRepository.getChannels()
        val idx = channels.indexOfFirst { it.id == initialStreamId }
        currentIndex = if (idx >= 0) idx else 0
        tuneToCurrentIndex()
    }

    fun channelUp() {
        if (channels.isEmpty()) return
        currentIndex = (currentIndex - 1 + channels.size) % channels.size
        tuneToCurrentIndex()
    }

    fun channelDown() {
        if (channels.isEmpty()) return
        currentIndex = (currentIndex + 1) % channels.size
        tuneToCurrentIndex()
    }

    fun showBanner() {
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

    private fun tuneToCurrentIndex() {
        val channel = channels.getOrNull(currentIndex) ?: return
        _currentChannel.value = channel
        playerManager.play(channel.streamUrl)
        showBanner()
        viewModelScope.launch {
            channelRepository.recordRecent(channel.id)
            epgRepository.fetchShortEpg(channel.id)
            _epgNow.value = epgRepository.currentEvent(channel.id)
            _epgUpcoming.value = epgRepository.upcomingEvents(channel.id)
        }
    }
}
