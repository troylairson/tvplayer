package com.iptvplayer.app.ui.screen.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.app.data.repository.ChannelRepository
import com.iptvplayer.app.data.repository.EpgRepository
import com.iptvplayer.app.domain.model.Channel
import com.iptvplayer.app.domain.model.EpgEvent
import com.iptvplayer.app.domain.model.nowPlaying
import com.iptvplayer.app.player.PlayerManager
import com.iptvplayer.app.util.minuteTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

enum class GuideTab(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
    NEWS("News"),
    SPORTS("Sports"),
}

data class GuideRow(
    val channel: Channel,
    val nowPlaying: EpgEvent?,
    /** Sports channel whose current program looks like a live game. */
    val isLiveGame: Boolean,
)

sealed class LoadState {
    object Loading : LoadState()
    data class Error(val message: String) : LoadState()
    object Ready : LoadState()
}

data class GuideUiState(
    val loadState: LoadState = LoadState.Loading,
    val tab: GuideTab = GuideTab.ALL,
    /** null = search inactive. */
    val query: String? = null,
    val rows: List<GuideRow> = emptyList(),
    /** Channel in the preview cell: what the player has loaded, else the last-watched one. */
    val previewChannel: Channel? = null,
)

private data class Filter(val tab: GuideTab, val query: String?, val now: Instant)

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    val playerManager: PlayerManager,
) : ViewModel() {

    private val loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    private val tab = MutableStateFlow(GuideTab.ALL)
    private val query = MutableStateFlow<String?>(null)

    private val criteria = combine(tab, query, minuteTicker()) { tab, query, now -> Filter(tab, query, now) }

    val uiState: StateFlow<GuideUiState> = combine(
        loadState,
        channelRepository.channels,
        combine(channelRepository.recentIds, playerManager.currentUrl) { ids, url -> ids to url },
        epgRepository.events,
        criteria,
    ) { load, channels, (recentIds, playingUrl), epg, criteria ->
        val byId = channels.associateBy { it.id }
        val base = when (criteria.tab) {
            GuideTab.ALL       -> channels
            GuideTab.FAVORITES -> channels.filter { it.isFavorite }
            GuideTab.RECENT    -> recentIds.mapNotNull { byId[it] }
            GuideTab.NEWS      -> channels.filter { it.isNews }
            GuideTab.SPORTS    -> channels.filter { it.isSports }
        }
        val matching = if (criteria.query.isNullOrEmpty()) base else base.filter { it.matchesQuery(criteria.query) }
        GuideUiState(
            loadState = load,
            tab = criteria.tab,
            query = criteria.query,
            rows = matching.map { channel ->
                val now = epg[channel.id]?.nowPlaying(criteria.now)
                GuideRow(channel, now, isLiveGame = channel.isSports && now?.looksLikeLiveGame == true)
            },
            previewChannel = playingUrl?.let { url -> channels.firstOrNull { it.streamUrl == url } }
                ?: recentIds.firstNotNullOfOrNull { byId[it] },
        )
    }
        .flowOn(Dispatchers.Default)   // can be tens of thousands of channels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GuideUiState())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            loadState.value = LoadState.Loading
            channelRepository.fetchChannels()
                .onSuccess {
                    loadState.value = LoadState.Ready
                    startPreviewIfIdle()
                }
                .onFailure { t -> loadState.value = LoadState.Error(t.message ?: "Unknown error") }
        }
    }

    /** Called each time the guide is shown: the preview cell plays muted. */
    fun onShown() {
        playerManager.setMuted(true)
    }

    // Returning from the Player the stream is already running; on a cold start, resume the last-watched channel
    private suspend fun startPreviewIfIdle() {
        if (playerManager.hasMedia) return
        val lastId = channelRepository.recentIds.first().firstOrNull() ?: return
        val channel = channelRepository.channels.first().firstOrNull { it.id == lastId } ?: return
        playerManager.setMuted(true)
        playerManager.play(channel.streamUrl)
    }

    fun selectTab(newTab: GuideTab) {
        tab.value = newTab
    }

    fun appendToQuery(char: Char) {
        query.update { (it ?: "") + char }
    }

    fun deleteFromQuery() {
        query.update { it?.dropLast(1) }
    }

    fun clearQuery() {
        query.value = null
    }

    fun toggleFavorite(streamId: Int) {
        viewModelScope.launch { channelRepository.toggleFavorite(streamId) }
    }

    /** Rows request EPG as they come on screen; the repository de-dupes and caches. */
    fun requestEpg(streamId: Int) {
        viewModelScope.launch { epgRepository.fetchShortEpg(streamId) }
    }
}
