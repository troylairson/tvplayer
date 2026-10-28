package com.tvplayer.launcher.ui.spa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.launcher.data.ha.HaSnapshot
import com.tvplayer.launcher.data.ha.HaStatus
import com.tvplayer.launcher.data.ha.HomeAssistantRepository
import com.tvplayer.launcher.data.settings.EntityConfig
import com.tvplayer.launcher.data.settings.LauncherSettingsStore
import com.tvplayer.launcher.domain.SpaReading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TARGET_COMMIT_DELAY_MS = 800L

fun HaSnapshot.spaReading(entities: EntityConfig): SpaReading {
    val climate = this[entities.spaClimate]
    return SpaReading(
        waterTemp = climate?.number("current_temperature"),
        targetTemp = climate?.number("temperature"),
        hvacMode = climate?.state,
        bubblesOn = this[entities.spaBubbles]?.isClimateOn == true,
    )
}

data class SpaControl(
    val entityId: String,
    val isOn: Boolean,
    /** false when the entity is missing from HA, unavailable, or has no "on" mode to switch to. */
    val available: Boolean,
    /** Current HVAC mode when on, e.g. "Fan only". */
    val modeLabel: String?,
)

data class SpaState(
    val status: HaStatus = HaStatus.Connecting,
    val reading: SpaReading = SpaReading(null, null, null, false),
    /** Target shown on screen — includes D-pad adjustments not yet sent to HA. */
    val displayTarget: Double? = null,
    val bubbles: SpaControl? = null,
)

@HiltViewModel
class SpaViewModel @Inject constructor(
    private val ha: HomeAssistantRepository,
    private val settings: LauncherSettingsStore,
) : ViewModel() {

    // Local target while the user is pressing Left/Right; sent to HA once they pause
    private val pendingTarget = MutableStateFlow<Double?>(null)
    private var commitJob: Job? = null

    val state: StateFlow<SpaState> = combine(ha.snapshot, settings.config, pendingTarget) { snapshot, config, pending ->
        val entities = config.entities
        val bubbles = snapshot[entities.spaBubbles]
        val reading = snapshot.spaReading(entities)
        SpaState(
            status = snapshot.status,
            reading = reading,
            displayTarget = pending ?: reading.targetTemp,
            bubbles = SpaControl(
                entityId = entities.spaBubbles,
                isOn = bubbles?.isClimateOn == true,
                available = bubbles != null && bubbles.isAvailable && bubbles.firstActiveHvacMode() != null,
                modeLabel = bubbles?.takeIf { it.isClimateOn }?.state?.let(::hvacModeLabel),
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaState())

    fun toggleBubbles() {
        val bubbles = state.value.bubbles ?: return
        ha.setClimateOn(bubbles.entityId, on = !bubbles.isOn)
    }

    fun adjustTarget(direction: Int) {
        viewModelScope.launch {
            val climateId = settings.config.first().entities.spaClimate
            val climate = ha.snapshot.first()[climateId] ?: return@launch
            val step = climate.number("target_temp_step") ?: 1.0
            val min = climate.number("min_temp") ?: 80.0
            val max = climate.number("max_temp") ?: 104.0
            val current = pendingTarget.value ?: climate.number("temperature") ?: return@launch
            pendingTarget.value = (current + direction * step).coerceIn(min, max)

            commitJob?.cancel()
            commitJob = viewModelScope.launch {
                delay(TARGET_COMMIT_DELAY_MS)
                pendingTarget.value?.let { ha.setTemperature(climateId, it) }
                pendingTarget.value = null
            }
        }
    }
}

/** "fan_only" → "Fan only" */
private fun hvacModeLabel(mode: String): String =
    mode.replace('_', ' ').replaceFirstChar { it.uppercase() }
