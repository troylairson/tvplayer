package com.tvplayer.launcher.ui.spa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tvplayer.launcher.data.ha.HaStatus
import com.tvplayer.launcher.domain.WaterState
import com.tvplayer.launcher.ui.art.SpaSteamArt
import com.tvplayer.launcher.ui.components.Header
import com.tvplayer.launcher.ui.components.ToggleCard
import com.tvplayer.launcher.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SpaScreen(
    onOpenLauncherSettings: () -> Unit,
    viewModel: SpaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }
    // Re-run when the layout swaps between the controls and the setup prompt
    LaunchedEffect(state.status == HaStatus.NotConfigured) { runCatching { firstFocus.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 24.dp),
    ) {
        Header(title = "Spa")
        Spacer(Modifier.height(16.dp))

        if (state.status == HaStatus.NotConfigured) {
            NotConfigured(onOpenLauncherSettings, Modifier.focusRequester(firstFocus))
        } else Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WaterCard(state, Modifier.weight(0.82f).fillMaxHeight())

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TargetCard(
                    target = state.displayTarget,
                    caption = state.reading.targetCaption,
                    onAdjust = viewModel::adjustTarget,
                    modifier = Modifier.fillMaxWidth().weight(1f).focusRequester(firstFocus),
                )
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SpaToggle(Icons.Outlined.BubbleChart, "Jets 1", state.jets1, viewModel::toggle,
                        Modifier.weight(1f).fillMaxHeight())
                    SpaToggle(Icons.Outlined.BubbleChart, "Jets 2", state.jets2, viewModel::toggle,
                        Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SpaToggle(Icons.Outlined.Lightbulb, "Lights", state.lights, viewModel::toggle,
                        Modifier.weight(1f).fillMaxHeight())
                    SpaToggle(Icons.Outlined.Waves, "Waterfall", state.waterfall, viewModel::toggle,
                        Modifier.weight(1f).fillMaxHeight())
                }
                JetsButton(
                    jetsOn = state.reading.jetsOn,
                    onClick = viewModel::toggleAllJets,
                    modifier = Modifier.fillMaxWidth().weight(0.72f),
                )
            }
        }
    }
}

@Composable
private fun WaterCard(state: SpaState, modifier: Modifier = Modifier) {
    val reading = state.reading
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Terracotta),
    ) {
        SpaSteamArt(Modifier.fillMaxSize(), anchorX = 0.62f, withWave = true)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 24.dp),
        ) {
            Text(
                text = reading.waterTemp?.let { "${it.roundToInt()}°" } ?: "--°",
                style = MaterialTheme.typography.displayLarge,
                color = Cream,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Water", fontSize = 18.sp, color = Cream)
                Spacer(Modifier.width(14.dp))
                Box(
                    Modifier
                        .size(9.dp)
                        .background(
                            if (reading.waterState == WaterState.AT_TEMPERATURE) Mint else Color(0xFFE8A34A),
                            CircleShape,
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(text = reading.waterState.label, fontSize = 18.sp, color = Cream)
            }
            if (state.status is HaStatus.Error) {
                Text(
                    text = "Home Assistant: ${state.status.message}",
                    fontSize = 13.sp,
                    color = Cream.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Focusable: Left/Right step the target temperature. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TargetCard(
    target: Double?,
    caption: String,
    onAdjust: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = {},
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft  -> { onAdjust(-1); true }
                    Key.DirectionRight -> { onAdjust(+1); true }
                    else -> false
                }
            },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TargetGreen,
            contentColor = Cream,
            focusedContainerColor = TargetGreen,
            focusedContentColor = Cream,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, InkBorder), shape = shape),
            focusedBorder = Border(BorderStroke(3.dp, Cream), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = "Target", fontSize = 22.sp)
                Text(
                    text = if (focused) "◀ ▶ to adjust" else caption,
                    fontSize = 15.sp,
                    color = CreamMuted,
                )
            }
            Text(
                text = target?.let { "${it.roundToInt()}°" } ?: "--°",
                style = MaterialTheme.typography.displayMedium,
            )
        }
    }
}

@Composable
private fun SpaToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    control: SpaControl?,
    onToggle: (SpaControl) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToggleCard(
        icon = icon,
        label = label,
        isOn = control?.isOn == true,
        onToggle = { control?.let(onToggle) },
        enabled = control?.available == true,
        detail = if (control?.available == true) null else "Unavailable",
        modifier = modifier,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun JetsButton(jetsOn: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = InkRaised,
            contentColor = Cream,
            focusedContainerColor = Cream,
            focusedContentColor = Ink,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, InkBorder), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (jetsOn) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(text = if (jetsOn) "Stop jets" else "Start jets", fontSize = 21.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NotConfigured(onOpenLauncherSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Connect Home Assistant to control the spa", fontSize = 22.sp, color = Cream)
        Spacer(Modifier.height(20.dp))
        androidx.tv.material3.Button(onClick = onOpenLauncherSettings, modifier = modifier) {
            Text("Open launcher settings")
        }
    }
}
