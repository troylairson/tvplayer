package com.tvplayer.launcher.ui.lights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.launcher.data.ha.HaStatus
import com.tvplayer.launcher.data.ha.HomeAssistantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.roundToInt

data class LightItem(
    val entityId: String,
    val name: String,
    val isOn: Boolean,
    val available: Boolean,
    /** 0–100, null when off or not dimmable. */
    val brightnessPercent: Int?,
)

data class LightsState(
    val status: HaStatus = HaStatus.Connecting,
    val lights: List<LightItem> = emptyList(),
)

@HiltViewModel
class LightsViewModel @Inject constructor(
    private val ha: HomeAssistantRepository,
) : ViewModel() {

    val state: StateFlow<LightsState> = ha.snapshot.map { snapshot ->
        LightsState(
            status = snapshot.status,
            lights = snapshot.states.values
                .filter { it.domain == "light" }
                .map { light ->
                    LightItem(
                        entityId = light.entityId,
                        name = light.friendlyName,
                        isOn = light.isOn,
                        available = light.isAvailable,
                        // HA brightness is 0–255
                        brightnessPercent = light.number("brightness")?.let { (it / 255.0 * 100).roundToInt() },
                    )
                }
                .sortedBy { it.name.lowercase() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LightsState())

    fun toggle(light: LightItem) = ha.setOn(listOf(light.entityId), !light.isOn)

    fun allOff() = ha.setOn(state.value.lights.filter { it.isOn }.map { it.entityId }, on = false)
}
