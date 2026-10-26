package com.iptvplayer.app.ui.screen.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptvplayer.app.domain.model.EpgEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("h:mma").withZone(ZoneId.systemDefault())
private val WINDOW_SECONDS = 3 * 3600L   // 3-hour window starting 30 min ago

@Composable
fun EpgTimeline(
    currentEvent: EpgEvent?,
    upcomingEvents: List<EpgEvent>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val allEvents = buildList {
        currentEvent?.let { add(it) }
        addAll(upcomingEvents)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val now       = Instant.now()
        val winStart  = now.minusSeconds(1800)          // 30 min before now
        val winEnd    = winStart.plusSeconds(WINDOW_SECONDS)
        val totalSec  = WINDOW_SECONDS.toFloat()
        val barH      = size.height - 12f

        // Background track
        drawRoundRect(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = Offset(0f, (size.height - barH) / 2f),
            size = Size(size.width, barH),
            cornerRadius = CornerRadius(4f),
        )

        // Event blocks
        for (event in allEvents) {
            val evStart = event.start.coerceAtLeast(winStart)
            val evEnd   = event.end.coerceAtMost(winEnd)
            if (evEnd <= winStart || evStart >= winEnd) continue

            val x     = ((evStart.epochSecond - winStart.epochSecond) / totalSec) * size.width
            val w     = ((evEnd.epochSecond - evStart.epochSecond) / totalSec) * size.width
            val isCurrent = event == currentEvent

            drawRoundRect(
                color = if (isCurrent) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.35f),
                topLeft = Offset(x + 1f, (size.height - barH) / 2f),
                size = Size((w - 2f).coerceAtLeast(0f), barH),
                cornerRadius = CornerRadius(4f),
            )

            // Show title if block is wide enough
            if (w > 80f) {
                val measured = textMeasurer.measure(
                    event.title,
                    style = TextStyle(fontSize = 10.sp, color = Color.Black),
                )
                if (measured.size.width < w - 8) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = event.title,
                        topLeft = Offset(x + 4f, (size.height - measured.size.height) / 2f),
                        style = TextStyle(fontSize = 10.sp, color = Color.Black),
                    )
                }
            }
        }

        // Now marker
        val nowX = ((now.epochSecond - winStart.epochSecond) / totalSec) * size.width
        drawLine(
            color = Color.White,
            start = Offset(nowX, 0f),
            end = Offset(nowX, size.height),
            strokeWidth = 2f,
        )

        // Hour tick labels
        var tickTime = winStart.plusSeconds(3600 - (winStart.epochSecond % 3600))
        while (tickTime < winEnd) {
            val tx = ((tickTime.epochSecond - winStart.epochSecond) / totalSec) * size.width
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(tx, size.height - 8f),
                end = Offset(tx, size.height),
                strokeWidth = 1f,
            )
            val label = timeFormatter.format(tickTime).lowercase()
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(tx + 2f, size.height - 16f),
                style = TextStyle(fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f)),
            )
            tickTime = tickTime.plusSeconds(3600)
        }
    }
}

private fun Instant.coerceAtLeast(min: Instant) = if (this < min) min else this
private fun Instant.coerceAtMost(max: Instant) = if (this > max) max else this
private operator fun Instant.compareTo(other: Instant) = this.compareTo(other)
