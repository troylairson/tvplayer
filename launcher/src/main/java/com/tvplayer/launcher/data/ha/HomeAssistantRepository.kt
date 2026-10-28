package com.tvplayer.launcher.data.ha

import com.tvplayer.launcher.data.ha.model.HaState
import com.tvplayer.launcher.data.settings.HomeAssistantConfig
import com.tvplayer.launcher.data.settings.LauncherSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class HaStatus {
    object NotConfigured : HaStatus()
    object Connecting : HaStatus()
    object Connected : HaStatus()
    data class Error(val message: String) : HaStatus()
}

data class HaSnapshot(
    val status: HaStatus = HaStatus.Connecting,
    val states: Map<String, HaState> = emptyMap(),
) {
    operator fun get(entityId: String): HaState? = states[entityId]
}

private const val POLL_INTERVAL_MS = 5_000L
private const val SETTLE_MS = 600L

@Singleton
class HomeAssistantRepository @Inject constructor(
    private val api: HomeAssistantApi,
    private val settings: LauncherSettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val latest = MutableStateFlow(HaSnapshot())

    /**
     * Live Home Assistant state. Polls `/api/states` only while something collects it
     * (i.e. a launcher screen is visible), and stops 5s after the last collector leaves.
     */
    val snapshot: SharedFlow<HaSnapshot> = channelFlow {
        launch {
            while (isActive) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
        latest.collect { send(it) }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    suspend fun refresh() {
        val config = settings.config.first().homeAssistant
        if (!config.isComplete) {
            latest.value = HaSnapshot(HaStatus.NotConfigured)
            return
        }
        runCatching { api.getStates(config.url("/api/states"), config.bearer) }
            .onSuccess { states -> latest.value = HaSnapshot(HaStatus.Connected, states.associateBy { it.entityId }) }
            .onFailure { t ->
                // Keep the last known states on screen; just flag the error
                latest.update { it.copy(status = HaStatus.Error(t.message ?: "Can't reach Home Assistant")) }
            }
    }

    /**
     * Turns a `climate` entity on or off via its HVAC mode. "On" is the first non-off mode the
     * entity lists in `hvac_modes` (e.g. "heat", "fan_only"), which works for any thermostat-style
     * entity whether or not it supports climate.turn_on.
     */
    fun setClimateOn(entityId: String, on: Boolean) {
        val current = latest.value.states[entityId] ?: return
        val mode = if (on) current.firstActiveHvacMode() ?: return else "off"
        // Optimistic: flip locally so the toggle responds instantly; the next poll corrects it
        latest.update { snap -> snap.copy(states = snap.states + (entityId to current.copy(state = mode))) }
        call("climate", "set_hvac_mode", mapOf("entity_id" to entityId, "hvac_mode" to mode))
    }

    fun setTemperature(climateEntity: String, temperature: Double) {
        latest.update { snap ->
            val current = snap.states[climateEntity] ?: return@update snap
            snap.copy(states = snap.states + (climateEntity to current.copy(
                attributes = current.attributes + ("temperature" to temperature)
            )))
        }
        call("climate", "set_temperature", mapOf("entity_id" to climateEntity, "temperature" to temperature))
    }

    private fun call(domain: String, service: String, body: Map<String, Any>) {
        scope.launch {
            val config: HomeAssistantConfig = settings.config.first().homeAssistant
            if (!config.isComplete) return@launch
            runCatching { api.callService(config.url("/api/services/$domain/$service"), config.bearer, body) }
                .onFailure { t -> latest.update { it.copy(status = HaStatus.Error(t.message ?: "Service call failed")) } }
            // Give HA a moment to apply the change, or the refresh can briefly undo the optimistic state
            delay(SETTLE_MS)
            refresh()
        }
    }
}
