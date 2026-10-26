package com.iptvplayer.app.domain.model

import java.time.Instant

data class EpgEvent(
    val channelId: String,
    val title: String,
    val description: String,
    val start: Instant,
    val end: Instant,
) {
    val durationMinutes: Long get() = (end.epochSecond - start.epochSecond) / 60
    val elapsedMinutes: Long get() = (Instant.now().epochSecond - start.epochSecond) / 60
    val progressFraction: Float get() = (elapsedMinutes.toFloat() / durationMinutes.toFloat()).coerceIn(0f, 1f)
    val minutesRemaining: Long get() = (durationMinutes - elapsedMinutes).coerceAtLeast(0)
}
