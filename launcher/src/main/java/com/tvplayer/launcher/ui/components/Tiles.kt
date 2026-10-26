package com.tvplayer.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tvplayer.launcher.ui.theme.Cream
import com.tvplayer.launcher.ui.theme.CreamMuted
import com.tvplayer.launcher.ui.theme.Forest
import com.tvplayer.launcher.ui.theme.Ink
import com.tvplayer.launcher.ui.theme.InkBorder
import com.tvplayer.launcher.ui.theme.InkRaised
import com.tvplayer.launcher.ui.theme.Mint

private val TileShape = RoundedCornerShape(10.dp)

/** Illustrated home tile: colored card, art behind, title (+ subtitle) bottom-left, cream ring on focus. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ArtTile(
    color: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleSize: TextUnit = 30.sp,
    trailing: String? = null,
    onLongClick: (() -> Unit)? = null,
    art: @Composable BoxScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = TileShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = color,
            contentColor = Cream,
            focusedContainerColor = color,
            focusedContentColor = Cream,
            pressedContainerColor = color,
            pressedContentColor = Cream,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), shape = TileShape),
            focusedBorder = Border(BorderStroke(3.dp, Cream), shape = TileShape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) {
        Box(Modifier.fillMaxSize().clip(TileShape)) {
            art()
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontSize = titleSize, color = Cream)
                    trailing?.let {
                        Spacer(Modifier.width(14.dp))
                        Text(text = it, fontSize = titleSize * 0.8f, color = Color(0xFFE8A34A))
                    }
                }
                subtitle?.let {
                    Text(text = it, fontSize = 15.sp, color = Cream.copy(alpha = 0.85f))
                }
            }
        }
    }
}

/** Small square icon button for the bottom-right tray. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TrayButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(44.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Cream,
            focusedContainerColor = Forest,
            focusedContentColor = Cream,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(1.5.dp, Mint), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * On/off control card (spa jets, lights…). Dark when idle, cream when focused, with a pill switch.
 * Pass key handling (e.g. brightness on Left/Right) through [modifier].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ToggleCard(
    icon: ImageVector,
    label: String,
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = InkRaised,
            contentColor = Cream,
            focusedContainerColor = Cream,
            focusedContentColor = Ink,
            pressedContainerColor = Cream,
            pressedContentColor = Ink,
            disabledContainerColor = InkRaised.copy(alpha = 0.5f),
            disabledContentColor = CreamMuted,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, InkBorder), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, Ink), inset = 3.dp, shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(text = label, fontSize = 21.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(if (isOn) Mint else CreamMuted, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = detail ?: if (isOn) "On" else "Off",
                        fontSize = 15.sp,
                        color = CreamMuted,
                    )
                }
            }
            PillSwitch(isOn)
        }
    }
}

/** Visual-only switch; the whole card is the focus/click target. */
@Composable
private fun PillSwitch(isOn: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (isOn) Mint else Color(0xFF3A4448)),
        contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(4.dp)
                .size(26.dp)
                .background(if (isOn) Cream else Color(0xFFEDE3D0), CircleShape)
        )
    }
}
