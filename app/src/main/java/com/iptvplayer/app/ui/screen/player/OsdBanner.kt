package com.iptvplayer.app.ui.screen.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptvplayer.app.domain.model.Channel
import com.iptvplayer.app.domain.model.EpgEvent
import com.iptvplayer.app.ui.theme.Cyan400
import com.iptvplayer.app.ui.theme.Navy800

@Composable
fun OsdBanner(
    channel: Channel,
    epgNow: EpgEvent?,
    epgUpcoming: List<EpgEvent>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.92f),
                        Color.Black.copy(alpha = 0.0f),
                    )
                )
            )
            .padding(horizontal = 48.dp, vertical = 20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Top row: logo + number + name + flag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Channel logo
                if (channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(64.dp, 40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Navy800),
                    )
                }

                // Channel number
                Text(
                    text = "${channel.num}",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                )

                // Channel name
                Text(
                    text = channel.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Country flag (2-char code)
                if (channel.countryCode.isNotBlank()) {
                    Text(
                        text = channel.countryCode.uppercase().countryFlag(),
                        fontSize = 28.sp,
                    )
                }
            }

            // Now playing row
            if (epgNow != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "NOW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Cyan400,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = epgNow.title,
                        fontSize = 18.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${epgNow.minutesRemaining}m left",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }

                LinearProgressIndicator(
                    progress = { epgNow.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Cyan400,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
            } else {
                Text(
                    text = "No program info available",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }

            // 3-hour EPG timeline
            EpgTimeline(
                currentEvent = epgNow,
                upcomingEvents = epgUpcoming,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Convert 2-letter ISO country code to flag emoji. */
private fun String.countryFlag(): String {
    if (length < 2) return this
    val code = this.uppercase().take(2)
    return code.map { char ->
        Character.toCodePoint('\uD83C', (char.code - 'A'.code + 0xDDE6).toChar())
    }.joinToString("") { String(Character.toChars(it)) }
}
