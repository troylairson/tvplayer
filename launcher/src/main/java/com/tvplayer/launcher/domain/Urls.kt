package com.tvplayer.launcher.domain

/** "homeassistant.local:8123/" → "http://homeassistant.local:8123" */
fun normalizeBaseUrl(input: String): String {
    val trimmed = input.trim().trimEnd('/')
    if (trimmed.isEmpty()) return ""
    val hasScheme = trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)
    return if (hasScheme) trimmed else "http://$trimmed"
}
