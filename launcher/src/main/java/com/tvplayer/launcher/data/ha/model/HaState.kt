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
    val isOn: Boolean get() = state == "on"
    val isAvailable: Boolean get() = state != "unavailable" && state != "unknown"
    val friendlyName: String get() = attributes["friendly_name"] as? String ?: entityId

    /** Moshi decodes JSON numbers as Double. */
    fun number(attribute: String): Double? = (attributes[attribute] as? Number)?.toDouble()
}
