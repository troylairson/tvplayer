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
    val progressFraction: Float get() =
        if (durationMinutes <= 0) 0f
        else (elapsedMinutes.toFloat() / durationMinutes.toFloat()).coerceIn(0f, 1f)
    val minutesRemaining: Long get() = (durationMinutes - elapsedMinutes).coerceAtLeast(0)

    fun isAiring(now: Instant): Boolean = start <= now && end > now

    /** Title heuristic for a live sporting event: "A vs B", "A @ B", "LIVE: …" — but not replays/highlights. */
    val looksLikeLiveGame: Boolean get() =
        LIVE_GAME_PATTERN.containsMatchIn(title) && !NOT_LIVE_PATTERN.containsMatchIn(title)

    private companion object {
        val LIVE_GAME_PATTERN = Regex("(?i)\\S\\s+(vs?\\.?|@)\\s+\\S|\\blive\\b|\\ben (vivo|directo)\\b")
        val NOT_LIVE_PATTERN = Regex("(?i)highlights?|replay|classic|rewind|recap|preview|best of|re-?air")
    }
}

fun List<EpgEvent>.nowPlaying(now: Instant = Instant.now()): EpgEvent? = firstOrNull { it.isAiring(now) }

fun List<EpgEvent>.upcoming(now: Instant = Instant.now(), count: Int = 4): List<EpgEvent> =
    filter { it.start >= now }.take(count)
