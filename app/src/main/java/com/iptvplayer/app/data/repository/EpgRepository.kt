package com.iptvplayer.app.data.repository

import com.iptvplayer.app.data.api.XtreamApi
import com.iptvplayer.app.data.datastore.CredentialsStore
import com.iptvplayer.app.domain.model.EpgEvent
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepository @Inject constructor(
    private val api: XtreamApi,
    private val credentialsStore: CredentialsStore,
) {
    // streamId -> cached events
    private val cache = mutableMapOf<Int, List<EpgEvent>>()

    suspend fun fetchShortEpg(streamId: Int, limit: Int = 5): List<EpgEvent> {
        cache[streamId]?.let { return it }
        return runCatching {
            val creds = credentialsStore.credentials.first() ?: return emptyList()
            val response = api.getShortEpg(
                url = creds.playerApiUrl(),
                username = creds.username,
                password = creds.password,
                streamId = streamId,
                limit = limit,
            )
            response.listings.map { listing ->
                EpgEvent(
                    channelId   = listing.channelId,
                    title       = decodeHtml(listing.title),
                    description = decodeHtml(listing.description),
                    start       = Instant.ofEpochSecond(listing.startTimestamp),
                    end         = Instant.ofEpochSecond(listing.stopTimestamp),
                )
            }.also { cache[streamId] = it }
        }.getOrDefault(emptyList())
    }

    fun currentEvent(streamId: Int): EpgEvent? {
        val now = Instant.now()
        return cache[streamId]?.firstOrNull { it.start <= now && it.end > now }
    }

    fun upcomingEvents(streamId: Int, count: Int = 4): List<EpgEvent> {
        val now = Instant.now()
        return cache[streamId]
            ?.filter { it.start >= now }
            ?.take(count)
            ?: emptyList()
    }

    fun clearCache() = cache.clear()

    private fun decodeHtml(input: String): String =
        input.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
}
