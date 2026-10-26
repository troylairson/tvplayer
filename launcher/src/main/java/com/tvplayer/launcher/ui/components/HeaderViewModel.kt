package com.tvplayer.launcher.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.launcher.data.ha.HomeAssistantRepository
import com.tvplayer.launcher.data.settings.LauncherSettingsStore
import com.tvplayer.launcher.domain.WeatherKind
import com.tvplayer.launcher.domain.weatherKind
import com.tvplayer.launcher.util.minuteTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToInt

data class HeaderState(
    val time: String = "",
    val weather: WeatherKind = WeatherKind.UNKNOWN,
    /** e.g. "76°", null when no weather entity is available. */
    val temperature: String? = null,
)

private val clockFormat = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())

@HiltViewModel
class HeaderViewModel @Inject constructor(
    ha: HomeAssistantRepository,
    settings: LauncherSettingsStore,
) : ViewModel() {

    val state: StateFlow<HeaderState> = combine(minuteTicker(), ha.snapshot, settings.config) { now, snapshot, config ->
        val weather = snapshot[config.entities.weather]
        HeaderState(
            time = clockFormat.format(now),
            weather = weatherKind(weather?.state),
            temperature = weather?.number("temperature")?.let { "${it.roundToInt()}°" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeaderState())
}
