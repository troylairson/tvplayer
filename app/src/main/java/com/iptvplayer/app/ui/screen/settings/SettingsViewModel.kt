package com.iptvplayer.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.app.data.datastore.CredentialsStore
import com.iptvplayer.app.data.repository.ChannelRepository
import com.iptvplayer.app.data.repository.EpgRepository
import com.iptvplayer.app.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ConnectState {
    object Idle : ConnectState()
    object Loading : ConnectState()
    data class Error(val message: String) : ConnectState()
    object Success : ConnectState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val credentialsStore: CredentialsStore,
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val playerManager: PlayerManager,
) : ViewModel() {

    // null = still loading from DataStore; false = nothing saved
    val hasCredentials: StateFlow<Boolean?> = credentialsStore.credentials
        .map { it?.isComplete == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _connectState = MutableStateFlow<ConnectState>(ConnectState.Idle)
    val connectState: StateFlow<ConnectState> = _connectState

    fun connect(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _connectState.value = ConnectState.Loading
            channelRepository.authenticate(serverUrl, username, password)
                .onSuccess {
                    // Anything cached belongs to the previous login (stream URLs embed credentials)
                    playerManager.reset()
                    epgRepository.clearCache()
                    _connectState.value = ConnectState.Success
                }
                .onFailure { t ->
                    _connectState.value = ConnectState.Error(t.message ?: "Connection failed")
                }
        }
    }

    fun resetState() { _connectState.value = ConnectState.Idle }
}
