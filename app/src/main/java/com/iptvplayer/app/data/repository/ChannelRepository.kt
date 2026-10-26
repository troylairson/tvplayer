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
    private var cachedChannels: List<Channel> = emptyList()
    private var cachedCategories: List<Category> = emptyList()

    val favoriteIds: Flow<Set<Int>> = favoriteDao.getAllIds().map { it.toSet() }
    val recentIds: Flow<List<Int>>  = recentDao.getAllIds()
    val hiddenCategoryIds: Flow<Set<String>> = hiddenCategoryDao.getAllIds().map { it.toSet() }

    suspend fun authenticate(serverUrl: String, username: String, password: String): Result<Unit> {
        return runCatching {
            val creds = XtreamCredentials(serverUrl, username, password)
            api.authenticate(creds.playerApiUrl(), username, password)
            credentialsStore.save(serverUrl, username, password)
        }
    }

    suspend fun fetchChannels(): Result<List<Channel>> {
        return runCatching {
            val creds = credentialsStore.credentials.first() ?: error("No credentials saved")
            val categories = api.getLiveCategories(creds.playerApiUrl(), creds.username, creds.password)
            val streams    = api.getLiveStreams(creds.playerApiUrl(), creds.username, creds.password)
            val catMap     = categories.associate { it.categoryId to it.categoryName }
            val favIds     = favoriteDao.getAllIds().first()
            val recentIdList = recentDao.getAllIds().first()
            val recentSet  = recentIdList.toSet()
            val hiddenIds  = hiddenCategoryDao.getAllIds().first()

            cachedCategories = categories.map { Category(it.categoryId, it.categoryName) }

            val channels = streams
                .filter { it.categoryId !in hiddenIds }
                .mapIndexed { idx, stream ->
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
                        isFavorite   = stream.streamId in favIds,
                        isRecent     = stream.streamId in recentSet,
                    )
                }

            cachedChannels = channels
            channels
        }
    }

    fun getChannels(): List<Channel> = cachedChannels
    fun getCategories(): List<Category> = cachedCategories

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
