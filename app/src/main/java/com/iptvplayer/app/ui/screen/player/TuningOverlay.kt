package com.iptvplayer.app.ui.screen.player

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val NOISE_W = 240        // ~8px blocks once scaled to 1080p
private const val NOISE_H = 135
private const val NOISE_FRAMES = 4
private const val FRAME_MS = 83L        // ~12fps is plenty for static

@Composable
fun TuningOverlay(modifier: Modifier = Modifier) {
    // A few small noise frames generated once and scaled up with nearest-neighbour filtering:
    // one drawImage per frame instead of tens of thousands of drawRect calls.
    val frames = remember { List(NOISE_FRAMES) { noiseFrame(Random(it)) } }
    var frameIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(FRAME_MS)
            frameIndex = (frameIndex + 1) % NOISE_FRAMES
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = frames[frameIndex],
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(NOISE_W, NOISE_H),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None,
            )
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

private fun noiseFrame(rng: Random): ImageBitmap {
    val pixels = IntArray(NOISE_W * NOISE_H) {
        val grey = rng.nextInt(256)
        (0xFF shl 24) or (grey shl 16) or (grey shl 8) or grey
    }
    return Bitmap.createBitmap(pixels, NOISE_W, NOISE_H, Bitmap.Config.ARGB_8888).asImageBitmap()
}
