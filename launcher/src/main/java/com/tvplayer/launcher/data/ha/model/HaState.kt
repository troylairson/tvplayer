package com.tvplayer.launcher.data.ha.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HaState(
    @Json(name = "entity_id") val entityId: String,
    val state: String,
    val attributes: Map<String, Any?> = emptyMap(),
) {
    val domain: String get() = entityId.substringBefore('.')
    val isAvailable: Boolean get() = state != "unavailable" && state != "unknown"

    /** For `climate` entities, whose state is the HVAC mode ("off", "heat", …) rather than on/off. */
    val isClimateOn: Boolean get() = isAvailable && state != "off"

    fun firstActiveHvacMode(): String? =
        (attributes["hvac_modes"] as? List<*>)?.filterIsInstance<String>()?.firstOrNull { it != "off" }
    val friendlyName: String get() = attributes["friendly_name"] as? String ?: entityId

    /** Moshi decodes JSON numbers as Double. */
    fun number(attribute: String): Double? = (attributes[attribute] as? Number)?.toDouble()
}
