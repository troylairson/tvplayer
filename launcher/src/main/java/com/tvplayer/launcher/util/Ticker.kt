package com.tvplayer.launcher.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant

/** Emits now, then at the top of every minute — drives the header clock. */
fun minuteTicker(): Flow<Instant> = flow {
    while (true) {
        val now = Instant.now()
        emit(now)
        delay(60_000L - (now.toEpochMilli() % 60_000L))
    }
}
