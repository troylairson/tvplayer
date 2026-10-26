package com.iptvplayer.app.data.repository

import com.iptvplayer.app.data.api.XtreamApi
import com.iptvplayer.app.data.datastore.CredentialsStore
import com.iptvplayer.app.domain.model.EpgEvent
import com.iptvplayer.app.domain.model.nowPlaying
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepository @Inject constructor(
    private val api: XtreamApi,
    private val credentialsStore: CredentialsStore,
) {
    private data class Entry(val events: List<EpgEvent>, val fetchedAt: Instant)

    // streamId -> cached events; a StateFlow so the OSD and guide rows update when fetches land
    private val entries = MutableStateFlow<Map<Int, Entry>>(emptyMap())
    val events: Flow<Map<Int, List<EpgEvent>>> = entries.map { map -> map.mapValues { it.value.events } }

    private val inFlight = ConcurrentHashMap.newKeySet<Int>()

    /** Fetches unless a fresh entry is cached or a fetch for [streamId] is already running. */
    suspend fun fetchShortEpg(streamId: Int, limit: Int = 5) {
        if (entries.value[streamId]?.isFresh(Instant.now()) == true) return
        if (!inFlight.add(streamId)) return
        try {
            val creds = credentialsStore.credentials.first() ?: return
            val response = runCatching {
                api.getShortEpg(
                    url = creds.playerApiUrl(),
                    username = creds.username,
                    password = creds.password,
                    streamId = streamId,
                    limit = limit,
                )
            }.getOrNull() ?: return
            val events = response.listings.map { listing ->
                EpgEvent(
                    channelId   = listing.channelId,
                    title       = decodeText(listing.title),
                    description = decodeText(listing.description),
                    start       = Instant.ofEpochSecond(listing.startTimestamp),
                    end         = Instant.ofEpochSecond(listing.stopTimestamp),
                )
            }.sortedBy { it.start }
            entries.update { it + (streamId to Entry(events, Instant.now())) }
        } finally {
            inFlight.remove(streamId)
        }
    }

    fun clearCache() = entries.update { emptyMap() }

    // Stale once everything cached has ended. Empty results and gaps between programs
    // are retried after RETRY_AFTER rather than on every request.
    private fun Entry.isFresh(now: Instant): Boolean {
        val recent = fetchedAt.plus(RETRY_AFTER) > now
        if (events.isEmpty()) return recent
        return events.last().end > now && (events.nowPlaying(now) != null || recent)
    }

    private fun decodeText(input: String): String = decodeHtml(decodeBase64OrNull(input) ?: input)

    // Xtream returns EPG title/description base64-encoded; some panels send plain text,
    // so only accept the decode if it yields clean UTF-8.
    private fun decodeBase64OrNull(input: String): String? {
        if (!BASE64_PATTERN.matches(input)) return null
        val decoded = try {
            val bytes = Base64.getDecoder().decode(input)
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: IllegalArgumentException) {
            return null
        } catch (e: CharacterCodingException) {
            return null
        }
        return decoded.takeIf { text -> text.none { it.isISOControl() && it !in "\n\r\t" } }
    }

    private fun decodeHtml(input: String): String =
        input.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")

    private companion object {
        val BASE64_PATTERN = Regex("^[A-Za-z0-9+/]+={0,2}$")
        val RETRY_AFTER: Duration = Duration.ofMinutes(30)
    }
}
