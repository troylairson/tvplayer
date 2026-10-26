package com.tvplayer.launcher.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.tvplayer.launcher.ui.theme.*

// Flat vector illustrations for the home tiles, drawn in proportion to the tile so they
// scale with layout. Shapes follow the mockups; exact geometry is free to tweak.

private val ScreenSky   = Color(0xFF1C4A4E)
private val WaterDark   = Color(0xFF1E5A5C)
private val WaterMid    = Color(0xFF26797B)
private val BrassDark   = Color(0xFF977341)
private val TvBody      = Color(0xFFE6D5B5)
private val FilmBrown   = Color(0xFF7A6450)
private val Groove      = Color(0xFF2A2026)

/** Palm fronds + a TV showing a sunset over water. */
@Composable
fun LiveTvArt(modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height

    // Fronds fanning down from above the tile's top edge
    val origin = Offset(w * 0.55f, -h * 0.12f)
    listOf(100f, 122f, 145f, 168f, 190f, 62f, 40f).forEachIndexed { i, angle ->
        palmLeaf(origin, length = h * (0.95f - i * 0.05f), width = h * 0.07f, angle = angle, color = ForestDeep)
    }

    // TV, bleeding off the right edge
    val left = w * 0.69f
    val top = h * 0.17f
    val bottom = h * 0.70f
    val horizon = h * 0.46f
    val frame = h * 0.022f
    clipRect(left, top, w, bottom) {
        drawRect(ScreenSky, Offset(left, top), Size(w - left, bottom - top))
        clipRect(left, top, w, horizon) {
            drawCircle(Sunset, radius = h * 0.15f, center = Offset(w * 0.88f, horizon))
        }
        val band = (bottom - horizon) / 3f
        listOf(Lagoon, WaterMid, WaterDark).forEachIndexed { i, color ->
            drawRect(color, Offset(left, horizon + band * i), Size(w - left, band))
        }
    }
    drawRect(Cream, Offset(left, top), Size(w - left + frame, bottom - top), style = Stroke(frame))
    // Stand
    drawLine(Cream, Offset(w * 0.84f, bottom), Offset(w * 0.84f, h * 0.80f), strokeWidth = frame)
    drawLine(Cream, Offset(w * 0.76f, h * 0.80f), Offset(w * 0.93f, h * 0.80f), strokeWidth = frame, cap = StrokeCap.Round)
}

/** Brass "play" triangle behind a retro TV with a film strip. */
@Composable
fun NostalgiaArt(modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height

    val triangle = Path().apply {
        moveTo(w * 0.50f, 0f)
        lineTo(w * 1.02f, h * 0.36f)
        lineTo(w * 0.50f, h * 0.80f)
        close()
    }
    drawPath(triangle, Brass)
    clipRect(0f, h * 0.36f, w, h) { drawPath(triangle, BrassDark) }

    // TV body
    val tvLeft = w * 0.54f
    val tvTop = h * 0.20f
    val tvW = w * 0.36f
    val tvH = h * 0.28f
    val r = CornerRadius(tvH * 0.18f)
    drawRoundRect(Cocoa, Offset(tvLeft - 4f, tvTop - 4f), Size(tvW + 8f, tvH + 8f), CornerRadius(r.x + 4f))
    drawRoundRect(TvBody, Offset(tvLeft, tvTop), Size(tvW, tvH), r)

    // Screen with a film strip
    val screenLeft = tvLeft + tvW * 0.08f
    val screenTop = tvTop + tvH * 0.18f
    val screenW = tvW * 0.62f
    val screenH = tvH * 0.64f
    drawRoundRect(FilmBrown, Offset(screenLeft, screenTop), Size(screenW, screenH), CornerRadius(6f))
    val frameW = screenW / 3.6f
    repeat(3) { i ->
        drawRect(
            TvBody,
            Offset(screenLeft + screenW * 0.06f + i * (frameW + screenW * 0.04f), screenTop + screenH * 0.28f),
            Size(frameW, screenH * 0.44f),
        )
    }
    repeat(7) { i ->
        val x = screenLeft + screenW * 0.05f + i * screenW * 0.135f
        drawRect(TvBody, Offset(x, screenTop + screenH * 0.08f), Size(screenW * 0.06f, screenH * 0.1f))
        drawRect(TvBody, Offset(x, screenTop + screenH * 0.82f), Size(screenW * 0.06f, screenH * 0.1f))
    }

    // Knobs
    val knobX = tvLeft + tvW * 0.85f
    drawCircle(Cocoa, radius = tvH * 0.11f, center = Offset(knobX, tvTop + tvH * 0.33f))
    drawCircle(Cocoa, radius = tvH * 0.11f, center = Offset(knobX, tvTop + tvH * 0.68f), style = Stroke(tvH * 0.04f))
}

/** Sunburst from the bottom-left with a curving film reel across the top. */
@Composable
fun MoviesArt(modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height

    val radius = maxOf(w, h) * 1.6f
    val center = Offset(0f, h)
    val rays = listOf(RustDeep, Rust, Cream.copy(alpha = 0.85f), RustLight, RustDeep, Rust, RustLight)
    val sweep = 90f / rays.size
    rays.forEachIndexed { i, color ->
        drawArc(
            color = color,
            startAngle = -90f + i * sweep,
            sweepAngle = sweep + 0.5f,   // overlap a hair so no seams show
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
        )
    }

    // Film reel band: an arc around a point off the top-right corner, with sprocket holes
    val reelCenter = Offset(w * 1.02f, -h * 0.02f)
    val reelRadius = h * 0.58f
    val band = h * 0.13f
    drawCircle(Cream, radius = reelRadius, center = reelCenter, style = Stroke(band))
    val hole = band * 0.42f
    for (angle in 95..185 step 11) {
        rotate(angle.toFloat(), pivot = reelCenter) {
            drawRect(
                Rust,
                topLeft = Offset(reelCenter.x + reelRadius - hole / 2, reelCenter.y - hole / 2),
                size = Size(hole, hole),
            )
        }
    }
}

/** A vinyl record peeking in from the right. */
@Composable
fun MusicArt(modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.88f, h * 0.55f)
    val radius = h * 0.78f

    drawCircle(Vinyl, radius = radius, center = center)
    var groove = radius * 0.95f
    while (groove > radius * 0.3f) {
        drawCircle(Groove, radius = groove, center = center, style = Stroke(1.5f))
        groove -= radius * 0.045f
    }
    drawCircle(Coral, radius = radius * 0.25f, center = center)
    drawCircle(Vinyl, radius = radius * 0.035f, center = center)
}

/** A 2×2 grid of app squares. */
@Composable
fun AppsArt(modifier: Modifier = Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val square = h * 0.30f
    val gap = h * 0.05f
    val left = w - square * 2 - gap - w * 0.10f
    val top = h * 0.14f
    listOf(Sage, Cream, Slate, Sky).forEachIndexed { i, color ->
        drawRoundRect(
            color,
            topLeft = Offset(left + (i % 2) * (square + gap), top + (i / 2) * (square + gap)),
            size = Size(square, square),
            cornerRadius = CornerRadius(square * 0.16f),
        )
    }
}

/**
 * Three rising steam curls. On the spa screen [withWave] adds the darker water line along the bottom.
 * [anchorX] is the horizontal centre of the curls as a fraction of the width.
 */
@Composable
fun SpaSteamArt(
    modifier: Modifier = Modifier,
    anchorX: Float = 0.78f,
    withWave: Boolean = false,
) = Canvas(modifier) {
    val w = size.width
    val h = size.height

    if (withWave) {
        val wave = Path().apply {
            moveTo(0f, h * 0.62f)
            cubicTo(w * 0.35f, h * 0.55f, w * 0.65f, h * 0.70f, w, h * 0.58f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(wave, TerracottaDeep)
    }

    val stroke = minOf(w, h) * 0.065f
    val spacing = w * 0.13f
    listOf(-1, 0, 1).forEach { i ->
        val x = w * anchorX + i * spacing
        val topY = h * (if (i == 0) 0.08f else 0.16f)
        val bottomY = h * (if (i == 0) 0.88f else 0.80f)
        val curl = Path().apply {
            moveTo(x - spacing * 0.15f, bottomY)
            cubicTo(
                x - spacing * 0.75f, bottomY - (bottomY - topY) * 0.35f,
                x + spacing * 0.55f, bottomY - (bottomY - topY) * 0.65f,
                x + spacing * 0.05f, topY,
            )
        }
        drawPath(curl, Cream, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

/** Header logo: a small palm. */
@Composable
fun PalmLogo(modifier: Modifier = Modifier, color: Color = Cream) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val crown = Offset(w * 0.5f, h * 0.38f)
    val stroke = w * 0.07f
    drawLine(color, crown, Offset(w * 0.5f, h), strokeWidth = stroke, cap = StrokeCap.Round)
    listOf(-165f, -135f, -105f, -75f, -45f, -15f).forEach { angle ->
        rotate(angle, pivot = crown) {
            drawLine(color, crown, Offset(crown.x + w * 0.42f, crown.y), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

/** A tapered leaf rotated [angle]° (0 = pointing right, clockwise) about [origin]. */
private fun DrawScope.palmLeaf(origin: Offset, length: Float, width: Float, angle: Float, color: Color) {
    val leaf = Path().apply {
        moveTo(0f, 0f)
        quadraticBezierTo(length * 0.5f, -width, length, 0f)
        quadraticBezierTo(length * 0.5f, width, 0f, 0f)
        close()
    }
    translate(origin.x, origin.y) {
        rotate(angle, pivot = Offset.Zero) {
            drawPath(leaf, color)
        }
    }
}
