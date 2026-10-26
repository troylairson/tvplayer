package com.tvplayer.launcher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvplayer.launcher.domain.WeatherKind
import com.tvplayer.launcher.ui.art.SunsetLogo
import com.tvplayer.launcher.ui.theme.Cream

/**
 * Top bar shared by every screen: sunset logo (home) or back chevron + title on the left,
 * clock and Home Assistant weather on the right.
 */
@Composable
fun Header(
    modifier: Modifier = Modifier,
    title: String? = null,
    viewModel: HeaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (title == null) {
            SunsetLogo(Modifier.size(32.dp))
        } else {
            // Not focusable: Back on the remote does this
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
                contentDescription = null,
                tint = Cream,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = Cream)
        }

        Spacer(Modifier.weight(1f))

        Text(text = state.time, fontSize = 18.sp, color = Cream)
        state.temperature?.let { temp ->
            Spacer(Modifier.width(28.dp))
            Icon(
                imageVector = state.weather.icon(),
                contentDescription = null,
                tint = Cream,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(28.dp))
            Text(text = temp, fontSize = 18.sp, color = Cream)
        }
    }
}

private fun WeatherKind.icon(): ImageVector = when (this) {
    WeatherKind.CLEAR_DAY     -> Icons.Outlined.WbSunny
    WeatherKind.CLEAR_NIGHT   -> Icons.Outlined.NightsStay
    WeatherKind.PARTLY_CLOUDY -> Icons.Outlined.WbCloudy
    WeatherKind.CLOUDY        -> Icons.Outlined.Cloud
    WeatherKind.RAIN          -> Icons.Outlined.Umbrella
    WeatherKind.SNOW          -> Icons.Outlined.AcUnit
    WeatherKind.STORM         -> Icons.Outlined.Thunderstorm
    WeatherKind.FOG           -> Icons.Outlined.FilterDrama
    WeatherKind.UNKNOWN       -> Icons.Outlined.WbSunny
}
