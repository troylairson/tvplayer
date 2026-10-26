package com.tvplayer.launcher.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/** Within this many degrees of target counts as "at temperature". */
private const val AT_TEMP_TOLERANCE = 1.0

enum class WaterState(val label: String) {
    AT_TEMPERATURE("At temperature"),
    HEATING("Heating"),
    COOLING("Cooling down"),
    OFF("Heater off"),
    UNKNOWN("No reading"),
}

data class SpaReading(
    val waterTemp: Double?,
    val targetTemp: Double?,
    /** climate entity state: "heat", "off", "auto", … */
    val hvacMode: String?,
    val jetsOn: Boolean,
) {
    val waterState: WaterState get() = when {
        waterTemp == null -> WaterState.UNKNOWN
        hvacMode == "off" -> WaterState.OFF
        targetTemp == null -> WaterState.UNKNOWN
        abs(waterTemp - targetTemp) <= AT_TEMP_TOLERANCE -> WaterState.AT_TEMPERATURE
        waterTemp < targetTemp -> WaterState.HEATING
        else -> WaterState.COOLING
    }

    /** Home tile subtitle, e.g. "104° · Ready". */
    val summary: String get() {
        val temp = waterTemp?.let { "${it.roundToInt()}°" } ?: "--°"
        val status = when {
            jetsOn -> "Jets on"
            waterState == WaterState.AT_TEMPERATURE -> "Ready"
            waterState == WaterState.UNKNOWN -> "Offline"
            else -> waterState.label
        }
        return "$temp · $status"
    }

    /** Target card caption. */
    val targetCaption: String get() = when (waterState) {
        WaterState.AT_TEMPERATURE -> "Holding temperature"
        WaterState.HEATING -> "Heating to target"
        WaterState.COOLING -> "Above target"
        WaterState.OFF -> "Heater off"
        WaterState.UNKNOWN -> "Waiting for Home Assistant"
    }
}

enum class WeatherKind { CLEAR_DAY, CLEAR_NIGHT, PARTLY_CLOUDY, CLOUDY, RAIN, SNOW, STORM, FOG, UNKNOWN }

/** Maps Home Assistant weather entity states (https://www.home-assistant.io/integrations/weather/). */
fun weatherKind(state: String?): WeatherKind = when (state) {
    "sunny" -> WeatherKind.CLEAR_DAY
    "clear-night" -> WeatherKind.CLEAR_NIGHT
    "partlycloudy" -> WeatherKind.PARTLY_CLOUDY
    "cloudy", "windy", "windy-variant" -> WeatherKind.CLOUDY
    "rainy", "pouring" -> WeatherKind.RAIN
    "snowy", "snowy-rainy", "hail" -> WeatherKind.SNOW
    "lightning", "lightning-rainy", "exceptional" -> WeatherKind.STORM
    "fog" -> WeatherKind.FOG
    else -> WeatherKind.UNKNOWN
}
