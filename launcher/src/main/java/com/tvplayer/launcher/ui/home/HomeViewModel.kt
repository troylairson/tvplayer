package com.tvplayer.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.launcher.data.apps.AppRepository
import com.tvplayer.launcher.data.ha.HaStatus
import com.tvplayer.launcher.data.ha.HomeAssistantRepository
import com.tvplayer.launcher.data.settings.AppTile
import com.tvplayer.launcher.data.settings.LauncherSettingsStore
import com.tvplayer.launcher.ui.spa.spaReading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeState(
    /** "104° · Ready", or a setup hint when Home Assistant isn't configured/reachable. */
    val spaSubtitle: String = "",
    /** Installed app chosen for each tile, null if none/uninstalled. */
    val tileApps: Map<AppTile, String?> = emptyMap(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apps: AppRepository,
    private val settings: LauncherSettingsStore,
    ha: HomeAssistantRepository,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(ha.snapshot, settings.config) { snapshot, config ->
        HomeState(
            spaSubtitle = when (snapshot.status) {
                HaStatus.NotConfigured -> "Set up Home Assistant"
                else -> snapshot.spaReading(config.entities).summary
            },
            tileApps = config.tileApps,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    /** @return false when the tile has no (installed) app — the caller opens the app picker. */
    suspend fun launch(tile: AppTile): Boolean {
        val pkg = settings.config.first().tileApps[tile] ?: return false
        return apps.launch(pkg)
    }

    fun openSystemSettings() = apps.openSystemSettings()
}
