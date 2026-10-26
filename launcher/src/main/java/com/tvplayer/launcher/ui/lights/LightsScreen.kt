package com.tvplayer.launcher.ui.lights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.tvplayer.launcher.data.ha.HaStatus
import com.tvplayer.launcher.ui.components.Header
import com.tvplayer.launcher.ui.components.ToggleCard
import com.tvplayer.launcher.ui.theme.Cream

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LightsScreen(
    onOpenLauncherSettings: () -> Unit,
    viewModel: LightsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }
    val hasLights = state.lights.isNotEmpty()
    LaunchedEffect(hasLights, state.status == HaStatus.NotConfigured) {
        runCatching { firstFocus.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 24.dp),
    ) {
        Header(title = "Lights")
        Spacer(Modifier.height(16.dp))

        when {
            state.status == HaStatus.NotConfigured -> CenteredMessage("Connect Home Assistant to control lights") {
                Button(onClick = onOpenLauncherSettings, modifier = Modifier.focusRequester(firstFocus)) {
                    Text("Open launcher settings")
                }
            }

            !hasLights -> CenteredMessage(
                if (state.status is HaStatus.Error) "Can't reach Home Assistant" else "No lights found in Home Assistant"
            ) {}

            else -> {
                Button(
                    onClick = viewModel::allOff,
                    modifier = Modifier.focusRequester(firstFocus),
                ) {
                    Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("All off")
                }
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.lights, key = { it.entityId }) { light ->
                        ToggleCard(
                            icon = Icons.Outlined.Lightbulb,
                            label = light.name,
                            isOn = light.isOn,
                            onToggle = { viewModel.toggle(light) },
                            enabled = light.available,
                            detail = when {
                                !light.available -> "Unavailable"
                                light.isOn && light.brightnessPercent != null -> "On · ${light.brightnessPercent}%"
                                else -> null
                            },
                            modifier = Modifier.fillMaxWidth().height(96.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(message: String, action: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, fontSize = 22.sp, color = Cream)
        Spacer(Modifier.height(20.dp))
        action()
    }
}
