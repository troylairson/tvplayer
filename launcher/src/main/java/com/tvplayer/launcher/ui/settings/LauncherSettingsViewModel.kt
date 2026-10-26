package com.tvplayer.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.launcher.data.apps.AppRepository
import com.tvplayer.launcher.data.ha.HaStatus
import com.tvplayer.launcher.data.ha.HomeAssistantRepository
import com.tvplayer.launcher.data.settings.EntityConfig
import com.tvplayer.launcher.data.settings.LauncherConfig
import com.tvplayer.launcher.data.settings.LauncherSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherSettingsViewModel @Inject constructor(
    private val settings: LauncherSettingsStore,
    private val ha: HomeAssistantRepository,
    private val apps: AppRepository,
) : ViewModel() {

    /** null until DataStore has loaded. */
    val config: StateFlow<LauncherConfig?> = settings.config
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val haStatus: StateFlow<HaStatus> = ha.snapshot
        .map { it.status }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HaStatus.Connecting)

    fun saveHomeAssistant(url: String, token: String, entities: EntityConfig) {
        viewModelScope.launch {
            settings.saveHomeAssistant(url, token, entities)
            ha.refresh()   // status line shows the result straight away
        }
    }

    fun appLabel(packageName: String?): String {
        if (packageName == null) return "Not set"
        return apps.label(packageName) ?: "$packageName (not installed)"
    }
}
