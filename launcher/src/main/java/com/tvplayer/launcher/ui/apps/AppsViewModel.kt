package com.tvplayer.launcher.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.launcher.data.apps.AppInfo
import com.tvplayer.launcher.data.apps.AppRepository
import com.tvplayer.launcher.data.settings.AppTile
import com.tvplayer.launcher.data.settings.LauncherSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val apps: AppRepository,
    private val settings: LauncherSettingsStore,
) : ViewModel() {

    /** null while loading. */
    private val _apps = MutableStateFlow<List<AppInfo>?>(null)
    val installedApps: StateFlow<List<AppInfo>?> = _apps

    val tileApps: StateFlow<Map<AppTile, String?>> = settings.config
        .map { it.tileApps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch { _apps.value = apps.tvApps() }
    }

    fun launch(app: AppInfo) {
        apps.launch(app.packageName)
    }

    fun assign(tile: AppTile, packageName: String?) {
        viewModelScope.launch { settings.setTileApp(tile, packageName) }
    }
}
