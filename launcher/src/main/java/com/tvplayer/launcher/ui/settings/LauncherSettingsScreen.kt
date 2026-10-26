package com.tvplayer.launcher.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tvplayer.launcher.data.ha.HaStatus
import com.tvplayer.launcher.data.settings.AppTile
import com.tvplayer.launcher.data.settings.EntityConfig
import com.tvplayer.launcher.data.settings.LauncherConfig
import com.tvplayer.launcher.ui.components.Header
import com.tvplayer.launcher.ui.theme.*

/**
 * Launcher-specific settings (reached by holding the gear, or Menu on the home screen).
 * Long values like the HA token are easiest to paste from the Google TV app on a phone.
 */
@Composable
fun LauncherSettingsScreen(
    onPickApp: (AppTile) -> Unit,
    viewModel: LauncherSettingsViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 36.dp, end = 36.dp, top = 20.dp),
    ) {
        Header(title = "Launcher settings")
        Spacer(Modifier.height(12.dp))
        // Build the form only once stored values are loaded, so fields start with them
        config?.let { SettingsForm(it, viewModel, onPickApp) }
    }
}

@Composable
private fun SettingsForm(
    initial: LauncherConfig,
    viewModel: LauncherSettingsViewModel,
    onPickApp: (AppTile) -> Unit,
) {
    val haStatus by viewModel.haStatus.collectAsStateWithLifecycle()
    // Live, so rows update after returning from the app picker
    val tileApps = viewModel.config.collectAsStateWithLifecycle().value?.tileApps.orEmpty()

    var url by remember { mutableStateOf(initial.homeAssistant.baseUrl) }
    var token by remember { mutableStateOf(initial.homeAssistant.token) }
    var spaClimate by remember { mutableStateOf(initial.entities.spaClimate) }
    var jets1 by remember { mutableStateOf(initial.entities.spaJets1) }
    var jets2 by remember { mutableStateOf(initial.entities.spaJets2) }
    var spaLights by remember { mutableStateOf(initial.entities.spaLights) }
    var waterfall by remember { mutableStateOf(initial.entities.spaWaterfall) }
    var weather by remember { mutableStateOf(initial.entities.weather) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Home Assistant")
            Field("URL", url, { url = it }, placeholder = "http://homeassistant.local:8123", keyboardType = KeyboardType.Uri)
            Field("Long-lived access token", token, { token = it }, isSecret = true)
            Text(
                text = "Create one in Home Assistant under your profile → Security.",
                fontSize = 13.sp,
                color = CreamMuted,
            )

            SectionTitle("Entities")
            Field("Spa thermostat (climate)", spaClimate, { spaClimate = it })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Jets 1", jets1, { jets1 = it }, Modifier.weight(1f))
                Field("Jets 2", jets2, { jets2 = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Spa lights", spaLights, { spaLights = it }, Modifier.weight(1f))
                Field("Waterfall", waterfall, { waterfall = it }, Modifier.weight(1f))
            }
            Field("Weather (header)", weather, { weather = it })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    viewModel.saveHomeAssistant(
                        url, token,
                        EntityConfig(spaClimate, jets1, jets2, spaLights, waterfall, weather),
                    )
                }) { Text("Save & test") }
                Spacer(Modifier.width(18.dp))
                Text(text = haStatus.describe(), fontSize = 15.sp, color = haStatus.color())
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Tiles")
            AppTile.entries.forEach { tile ->
                TileAppRow(tile.label, viewModel.appLabel(tileApps[tile])) { onPickApp(tile) }
            }
            Text(
                text = "Tip: hold OK on a home tile to change its app.",
                fontSize = 13.sp,
                color = CreamMuted,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, fontSize = 20.sp, color = Cream, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isSecret: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { androidx.compose.material3.Text(label) },
        // if/else (not ?.let) so the lambda is typed as @Composable from the parameter
        placeholder = if (placeholder != null) {
            { androidx.compose.material3.Text(placeholder) }
        } else null,
        singleLine = true,
        visualTransformation = if (isSecret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isSecret) KeyboardType.Password else keyboardType,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TileAppRow(tileLabel: String, appLabel: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = InkRaised,
            contentColor = Cream,
            focusedContainerColor = Cream,
            focusedContentColor = Ink,
        ),
        border = ClickableSurfaceDefaults.border(border = Border(BorderStroke(1.dp, InkBorder), shape = shape)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = tileLabel, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Text(text = appLabel, fontSize = 15.sp)
        }
    }
}

private fun HaStatus.describe(): String = when (this) {
    HaStatus.NotConfigured -> "Not configured"
    HaStatus.Connecting    -> "Connecting…"
    HaStatus.Connected     -> "Connected"
    is HaStatus.Error      -> "Error: $message"
}

private fun HaStatus.color() = when (this) {
    HaStatus.Connected -> Mint
    is HaStatus.Error  -> Coral
    else               -> CreamMuted
}
