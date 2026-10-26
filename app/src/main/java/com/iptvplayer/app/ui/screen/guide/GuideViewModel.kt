package com.iptvplayer.app.ui.screen.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.app.data.repository.ChannelRepository
import com.iptvplayer.app.domain.model.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GuideUiState {
    object Loading : GuideUiState()
    data class Error(val message: String) : GuideUiState()
    data class Ready(val channels: List<Channel>) : GuideUiState()
}

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GuideUiState>(GuideUiState.Loading)
    val uiState: StateFlow<GuideUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.value = GuideUiState.Loading
            channelRepository.fetchChannels()
                .onSuccess { channels -> _uiState.value = GuideUiState.Ready(channels) }
                .onFailure { t -> _uiState.value = GuideUiState.Error(t.message ?: "Unknown error") }
        }
    }
}
