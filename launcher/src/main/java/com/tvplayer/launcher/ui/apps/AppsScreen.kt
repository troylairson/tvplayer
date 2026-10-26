package com.tvplayer.launcher.ui.apps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tvplayer.launcher.data.apps.AppInfo
import com.tvplayer.launcher.data.settings.AppTile
import com.tvplayer.launcher.ui.components.Header
import com.tvplayer.launcher.ui.theme.*

/** All TV apps; OK launches. */
@Composable
fun AppsScreen(viewModel: AppsViewModel = hiltViewModel()) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    AppGridScaffold(title = "Apps", apps = apps) { app -> viewModel.launch(app) }
}

/** Choose which app a home tile opens. Includes a "None" entry; the current choice is outlined. */
@Composable
fun AppPickerScreen(
    tile: AppTile,
    onDone: () -> Unit,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val tileApps by viewModel.tileApps.collectAsStateWithLifecycle()
    AppGridScaffold(
        title = "${tile.label} opens…",
        apps = apps,
        selectedPackage = tileApps[tile],
        onNone = { viewModel.assign(tile, null); onDone() },
    ) { app ->
        viewModel.assign(tile, app.packageName)
        onDone()
    }
}

@Composable
private fun AppGridScaffold(
    title: String,
    apps: List<AppInfo>?,
    selectedPackage: String? = null,
    onNone: (() -> Unit)? = null,
    onSelect: (AppInfo) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(apps != null) { if (apps != null) runCatching { firstFocus.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 24.dp),
    ) {
        Header(title = title)
        Spacer(Modifier.height(16.dp))

        // null = still loading (usually a few hundred ms)
        if (apps != null) LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (onNone != null) {
                item(key = "none") {
                    AppCell(
                        label = "None",
                        selected = selectedPackage == null,
                        onClick = onNone,
                        modifier = Modifier.focusRequester(firstFocus),
                    ) {
                        Icon(Icons.Outlined.Block, contentDescription = null, tint = CreamMuted, modifier = Modifier.size(36.dp))
                    }
                }
            }
            items(apps, key = { it.packageName }) { app ->
                val isFirst = onNone == null && app == apps.first()
                AppCell(
                    label = app.label,
                    selected = app.packageName == selectedPackage,
                    onClick = { onSelect(app) },
                    modifier = if (isFirst) Modifier.focusRequester(firstFocus) else Modifier,
                ) {
                    Image(
                        bitmap = app.image,
                        contentDescription = null,
                        contentScale = if (app.hasBanner) ContentScale.Crop else ContentScale.Fit,
                        modifier = if (app.hasBanner) Modifier.fillMaxSize() else Modifier.size(64.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppCell(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = ClickableSurfaceDefaults.shape(shape = shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = InkRaised,
                focusedContainerColor = InkRaised,
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Mint else InkBorder), shape = shape),
                focusedBorder = Border(BorderStroke(3.dp, Cream), shape = shape),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
        }
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (selected) Mint else Cream.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
