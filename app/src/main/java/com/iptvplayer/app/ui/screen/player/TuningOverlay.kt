package com.iptvplayer.app.ui.screen.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun TuningOverlay(modifier: Modifier = Modifier) {
    // Tick value drives recomposition at ~12fps — enough for a static effect
    val transition = rememberInfiniteTransition(label = "static")
    val tick by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(80, easing = LinearEasing)),
        label = "tick",
    )

    val rng = remember { Random(System.currentTimeMillis()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Suppress unused tick — it drives recomposition so the random pixels refresh
            @Suppress("UNUSED_EXPRESSION") tick

            val blockW = 8f
            val blockH = 8f
            val cols = (size.width / blockW).toInt()
            val rows = (size.height / blockH).toInt()

            repeat(rows) { row ->
                repeat(cols) { col ->
                    val grey = rng.nextInt(256)
                    drawRect(
                        color = Color(grey, grey, grey, 255),
                        topLeft = Offset(col * blockW, row * blockH),
                        size = Size(blockW, blockH),
                    )
                }
            }
        }

        Text(
            text = "TUNING",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 28.sp,
            letterSpacing = 6.sp,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 10.dp),
        )
    }
}
