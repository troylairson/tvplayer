package com.iptvplayer.app.data.repository

import com.iptvplayer.app.data.api.XtreamApi
import com.iptvplayer.app.data.datastore.CredentialsStore
import com.iptvplayer.app.data.datastore.XtreamCredentials
import com.iptvplayer.app.data.db.dao.FavoriteDao
import com.iptvplayer.app.data.db.dao.HiddenCategoryDao
import com.iptvplayer.app.data.db.dao.RecentDao
import com.iptvplayer.app.data.db.entity.FavoriteEntity
import com.iptvplayer.app.data.db.entity.RecentEntity
import com.iptvplayer.app.domain.model.Category
import com.iptvplayer.app.domain.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val api: XtreamApi,
    private val credentialsStore: CredentialsStore,
    private val favoriteDao: FavoriteDao,
    private val recentDao: RecentDao,
    private val hiddenCategoryDao: HiddenCategoryDao,
) {
    // Every stream from the provider, unfiltered; visibility and favorite flags are applied in [channels]
    private val allChannels = MutableStateFlow<List<Channel>>(emptyList())

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    val favoriteIds: Flow<Set<Int>> = favoriteDao.getAllIds().map { it.toSet() }
    /** Most recent first, max 50. */
    val recentIds: Flow<List<Int>>  = recentDao.getAllIds()
    val hiddenCategoryIds: Flow<Set<String>> = hiddenCategoryDao.getAllIds().map { it.toSet() }

    /** Channels outside hidden categories, with favorite flags applied. Updates live with Room. */
    val channels: Flow<List<Channel>> = combine(allChannels, favoriteIds, hiddenCategoryIds) { all, favs, hidden ->
        all.filter { it.categoryId !in hidden }
            .map { if (it.id in favs) it.copy(isFavorite = true) else it }
    }

    suspend fun authenticate(serverUrl: String, username: String, password: String): Result<Unit> {
        return runCatching {
            val creds = XtreamCredentials(serverUrl, username, password)
            api.authenticate(creds.playerApiUrl(), username, password)
            credentialsStore.save(serverUrl, username, password)
        }
    }

    suspend fun fetchChannels(): Result<Unit> {
        return runCatching {
            val creds = credentialsStore.credentials.first() ?: error("No credentials saved")
            val categories = api.getLiveCategories(creds.playerApiUrl(), creds.username, creds.password)
            val streams    = api.getLiveStreams(creds.playerApiUrl(), creds.username, creds.password)
            val catMap     = categories.associate { it.categoryId to it.categoryName }

            _categories.value = categories.map { Category(it.categoryId, it.categoryName) }
            allChannels.value = streams.mapIndexed { idx, stream ->
                Channel(
                    id           = stream.streamId,
                    num          = stream.num ?: (idx + 1),
                    name         = stream.name,
                    logoUrl      = stream.streamIcon ?: "",
                    categoryId   = stream.categoryId ?: "",
                    categoryName = catMap[stream.categoryId] ?: "",
                    countryCode  = extractCountryCode(stream.name),
                    epgChannelId = stream.epgChannelId ?: "",
                    streamUrl    = creds.streamUrl(stream.streamId),
                )
            }
        }
    }

    /** Fetches only if nothing is cached — e.g. Player restored after process death, or a deep link. */
    suspend fun ensureLoaded(): Result<Unit> =
        if (allChannels.value.isNotEmpty()) Result.success(Unit) else fetchChannels()

    suspend fun toggleFavorite(streamId: Int) {
        if (favoriteDao.exists(streamId) > 0) {
            favoriteDao.delete(FavoriteEntity(streamId))
        } else {
            favoriteDao.insert(FavoriteEntity(streamId))
        }
    }

    suspend fun recordRecent(streamId: Int) {
        recentDao.insert(RecentEntity(streamId, System.currentTimeMillis()))
        recentDao.trimToLimit()
    }

    private fun extractCountryCode(name: String): String {
        val match = Regex("\\|([A-Z]{2,3})\\|").find(name)
            ?: Regex("\\b([A-Z]{2}):\\s").find(name)
        return match?.groupValues?.get(1) ?: ""
    }
}
