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
        jetsOn = this[entities.spaJets1]?.isOn == true || this[entities.spaJets2]?.isOn == true,
    )
}

data class SpaControl(
    val entityId: String,
    val isOn: Boolean,
    /** false when the entity is missing from HA or unavailable. */
    val available: Boolean,
)

data class SpaState(
    val status: HaStatus = HaStatus.Connecting,
    val reading: SpaReading = SpaReading(null, null, null, false),
    /** Target shown on screen — includes D-pad adjustments not yet sent to HA. */
    val displayTarget: Double? = null,
    val jets1: SpaControl? = null,
    val jets2: SpaControl? = null,
    val lights: SpaControl? = null,
    val waterfall: SpaControl? = null,
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
        fun control(id: String) = SpaControl(
            entityId = id,
            isOn = snapshot[id]?.isOn == true,
            available = snapshot[id]?.isAvailable == true,
        )
        val reading = snapshot.spaReading(entities)
        SpaState(
            status = snapshot.status,
            reading = reading,
            displayTarget = pending ?: reading.targetTemp,
            jets1 = control(entities.spaJets1),
            jets2 = control(entities.spaJets2),
            lights = control(entities.spaLights),
            waterfall = control(entities.spaWaterfall),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaState())

    fun toggle(control: SpaControl) {
        ha.setOn(listOf(control.entityId), !control.isOn)
    }

    /** "Start jets" / "Stop jets": both jets together. */
    fun toggleAllJets() {
        val s = state.value
        val ids = listOfNotNull(s.jets1?.entityId, s.jets2?.entityId)
        ha.setOn(ids, on = !s.reading.jetsOn)
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
