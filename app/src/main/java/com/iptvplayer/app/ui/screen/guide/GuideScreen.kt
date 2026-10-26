package com.iptvplayer.app.ui.screen.guide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvplayer.app.domain.model.Channel
import com.iptvplayer.app.player.PlayerManager
import com.iptvplayer.app.ui.theme.Cyan400
import com.iptvplayer.app.ui.theme.Gold400
import com.iptvplayer.app.ui.theme.Gray300
import com.iptvplayer.app.ui.theme.Navy700
import com.iptvplayer.app.ui.theme.Navy800
import com.iptvplayer.app.ui.theme.Navy950
import com.iptvplayer.app.ui.theme.Red400
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())

/** Row index to focus; if [awaitRemovalOf] is set, wait until that channel has left the list first. */
private data class FocusTarget(val index: Int, val awaitRemovalOf: Int? = null)

@Composable
fun GuideScreen(
    onChannelSelected: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: GuideViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Runs on every entry, including Back from the Player: the preview cell is muted
    LaunchedEffect(Unit) { viewModel.onShown() }

    BackHandler(enabled = state.query != null) { viewModel.clearQuery() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .onPreviewKeyEvent { event ->
                handleGuideKey(event, searching = state.query != null, viewModel, onOpenSettings)
            },
        contentAlignment = Alignment.Center,
    ) {
        when (val load = state.loadState) {
            is LoadState.Loading -> CircularProgressIndicator(color = Color.White)
            is LoadState.Error   -> LoadError(load.message, onRetry = viewModel::load, onOpenSettings)
            is LoadState.Ready   -> GuideContent(state, viewModel, onChannelSelected, onOpenSettings)
        }
    }
}

/** Letter keys start a search; while searching, digits/space append and Del erases. Menu opens Settings. */
private fun handleGuideKey(
    event: KeyEvent,
    searching: Boolean,
    viewModel: GuideViewModel,
    onOpenSettings: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (event.key == Key.Menu) {
        onOpenSettings()
        return true
    }
    if (searching && event.key == Key.Backspace) {
        viewModel.deleteFromQuery()
        return true
    }
    val char = event.utf16CodePoint.toChar()
    return when {
        char.isLetter() -> { viewModel.appendToQuery(char); true }
        searching && (char.isDigit() || char == ' ') -> { viewModel.appendToQuery(char); true }
        else -> false
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuideContent(
    state: GuideUiState,
    viewModel: GuideViewModel,
    onChannelSelected: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val rows = state.rows
    val listState = rememberLazyListState()
    val rowFocus = remember { FocusRequester() }
    val tabFocus = remember { FocusRequester() }
    var rowFocusIndex by remember { mutableIntStateOf(-1) }   // the row currently carrying rowFocus
    var focusTarget by remember { mutableStateOf<FocusTarget?>(null) }
    var focusedId by remember { mutableStateOf<Int?>(null) }
    var didInitialFocus by remember { mutableStateOf(false) }
    val currentRows by rememberUpdatedState(rows)

    // On entry, focus the channel in the preview cell (i.e. the one just watched)
    LaunchedEffect(rows.isNotEmpty()) {
        if (didInitialFocus || rows.isEmpty()) return@LaunchedEffect
        didInitialFocus = true
        val previewId = state.previewChannel?.id
        focusTarget = FocusTarget(rows.indexOfFirst { it.channel.id == previewId }.coerceAtLeast(0))
    }

    LaunchedEffect(state.tab) { listState.scrollToItem(0) }

    // Typing replaces the list under the focused row, so pull focus to the first match
    LaunchedEffect(state.query) {
        if (state.query != null) focusTarget = FocusTarget(0)
    }

    LaunchedEffect(focusTarget) {
        val target = focusTarget ?: return@LaunchedEffect
        target.awaitRemovalOf?.let { id ->
            withTimeoutOrNull(1_000) {
                snapshotFlow { currentRows }.first { list -> list.none { it.channel.id == id } }
            }
        }
        val list = currentRows
        if (list.isEmpty()) {
            runCatching { tabFocus.requestFocus() }   // nothing left to focus in the list
        } else {
            val index = target.index.coerceIn(0, list.lastIndex)
            rowFocusIndex = index
            listState.scrollToItem(index)
            withFrameNanos { }   // let the row compose and attach rowFocus
            runCatching { rowFocus.requestFocus() }
        }
        focusTarget = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        GuideHeader(query = state.query, onOpenSettings = onOpenSettings)

        Spacer(Modifier.height(12.dp))

        TabRow(selectedTabIndex = state.tab.ordinal) {
            GuideTab.entries.forEach { tab ->
                val selected = tab == state.tab
                Tab(
                    selected = selected,
                    onFocus = { viewModel.selectTab(tab) },
                    modifier = if (selected) Modifier.focusRequester(tabFocus) else Modifier,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (rows.isEmpty()) {
                    EmptyList(state.tab, state.query, Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(rows, key = { _, row -> row.channel.id }) { index, row ->
                            ChannelRow(
                                row = row,
                                onClick = { onChannelSelected(row.channel.id) },
                                onLongClick = {
                                    viewModel.toggleFavorite(row.channel.id)
                                    // Un-favoriting removes the row from this tab — keep focus in place
                                    if (state.tab == GuideTab.FAVORITES) {
                                        focusTarget = FocusTarget(index, awaitRemovalOf = row.channel.id)
                                    }
                                },
                                onFocused = { focusedId = row.channel.id },
                                onVisible = { viewModel.requestEpg(row.channel.id) },
                                modifier = if (index == rowFocusIndex) Modifier.focusRequester(rowFocus) else Modifier,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.width(320.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val focusedRow = rows.firstOrNull { it.channel.id == focusedId }
                if (focusedRow != null) {
                    ChannelDetails(focusedRow, Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                PreviewCell(state.previewChannel, viewModel.playerManager)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuideHeader(query: String?, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Guide",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
        )
        Spacer(Modifier.width(32.dp))
        if (query != null) {
            Text(
                text = "Search: $query▌",
                fontSize = 20.sp,
                color = Cyan400,
            )
            Spacer(Modifier.width(16.dp))
            Text(text = "Back to clear", fontSize = 12.sp, color = Gray300)
        } else {
            Text(
                text = "Type to search  ·  Hold OK to favorite  ·  Menu for settings",
                fontSize = 13.sp,
                color = Gray300,
            )
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onOpenSettings) { Text("Settings") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelRow(
    row: GuideRow,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    onVisible: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val channel = row.channel

    // Fetch EPG only for rows that stay on screen — fast scrolling cancels this before it fires
    LaunchedEffect(channel.id) {
        delay(300)
        onVisible()
    }

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Navy700,
            focusedContentColor = Color.White,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "${channel.num}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Cyan400,
                textAlign = TextAlign.End,
                modifier = Modifier.width(56.dp),
            )
            ChannelLogo(channel, Modifier.size(64.dp, 36.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.nowPlaying?.title ?: channel.categoryName,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (row.isLiveGame) LiveBadge()
            row.nowPlaying?.let {
                Text(
                    text = "${it.minutesRemaining}m left",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
            if (channel.isFavorite) {
                Text(text = "★", fontSize = 20.sp, color = Gold400)
            }
        }
    }
}

@Composable
private fun ChannelLogo(channel: Channel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Navy800),
        contentAlignment = Alignment.Center,
    ) {
        if (channel.logoUrl.isNotBlank()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Pulsing red dot for a live game on a sports channel. */
@Composable
private fun LiveBadge() {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .graphicsLayer { alpha = pulse }
                .background(Red400, CircleShape)
        )
        Text(text = "LIVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red400)
    }
}

@Composable
private fun ChannelDetails(row: GuideRow, modifier: Modifier = Modifier) {
    val channel = row.channel
    val now = row.nowPlaying
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = channel.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (channel.categoryName.isNotBlank()) {
            Text(text = channel.categoryName, fontSize = 13.sp, color = Gray300)
        }
        if (now != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = now.title,
                fontSize = 18.sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${timeFormatter.format(now.start)} – ${timeFormatter.format(now.end)}",
                fontSize = 13.sp,
                color = Gray300,
            )
            LinearProgressIndicator(
                progress = { now.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Cyan400,
                trackColor = Color.White.copy(alpha = 0.2f),
            )
            if (now.description.isNotBlank()) {
                Text(
                    text = now.description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(text = "No program info", fontSize = 13.sp, color = Gray300)
        }
    }
}

/**
 * The singleton player on a small surface — the same decoder and buffer as full screen,
 * so opening the guide costs no extra hardware decode session and selecting it doesn't re-tune.
 */
@Composable
private fun PreviewCell(channel: Channel?, playerManager: PlayerManager) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        player = playerManager.player
                    }
                },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = channel?.let { "${it.num}  ${it.name}" } ?: "Nothing playing",
            fontSize = 13.sp,
            color = Gray300,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyList(tab: GuideTab, query: String?, modifier: Modifier = Modifier) {
    val message = when {
        !query.isNullOrEmpty()      -> "No channels match \"$query\""
        tab == GuideTab.FAVORITES   -> "No favorites yet — hold OK on a channel to add one"
        tab == GuideTab.RECENT      -> "Channels you watch will appear here"
        else                        -> "No channels"
    }
    Text(text = message, fontSize = 16.sp, color = Gray300, modifier = modifier)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LoadError(message: String, onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { retryFocus.requestFocus() } }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Failed to load channels",
            color = Red400,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = message,
            color = Gray300,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onRetry, modifier = Modifier.focusRequester(retryFocus)) { Text("Retry") }
            Button(onClick = onOpenSettings) { Text("Settings") }
        }
    }
}
