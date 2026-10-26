package com.iptvplayer.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GuideLogicTest {

    private fun channel(name: String, category: String) =
        Channel(id = 1, num = 1, name = name, logoUrl = "", categoryId = "c", categoryName = category,
            countryCode = "", epgChannelId = "", streamUrl = "")

    private fun event(title: String, startOffset: Long, endOffset: Long, now: Instant) =
        EpgEvent("x", title, "", now.plusSeconds(startOffset), now.plusSeconds(endOffset))

    @Test
    fun `news and sports classification`() {
        assertTrue(channel("CNN International", "US| General").isNews)
        assertTrue(channel("BBC One", "UK | NEWS").isNews)
        assertTrue(channel("ESPN 2", "US| Entertainment").isSports)
        assertTrue(channel("Sky Sports Main Event", "UK").isSports)
        assertTrue(channel("beIN 1", "Arabic").isSports)
        assertFalse(channel("HBO", "US| Movies").isSports)
        assertFalse(channel("HBO", "US| Movies").isNews)
        assertFalse(channel("Bentley TV", "Cars").isSports)
    }

    @Test
    fun `live game heuristic`() {
        val now = Instant.now()
        listOf("Lakers vs Celtics", "Lakers vs. Celtics", "Arsenal v Chelsea", "Yankees @ Red Sox",
            "LIVE: NFL Sunday", "Fútbol en vivo")
            .forEach { assertTrue(it, event(it, 0, 60, now).looksLikeLiveGame) }
        listOf("NBA Highlights: Lakers vs Celtics", "Classic Match: Arsenal v Chelsea", "Rocky V",
            "SportsCenter", "Delivery Man", "Olive Garden Special")
            .forEach { assertFalse(it, event(it, 0, 60, now).looksLikeLiveGame) }
    }

    @Test
    fun `now playing and upcoming`() {
        val now = Instant.now()
        val events = listOf(
            event("past", -7200, -3600, now),
            event("now", -600, 1200, now),
            event("next", 1200, 3000, now),
            event("later", 3000, 6000, now),
        )
        assertEquals("now", events.nowPlaying(now)?.title)
        assertEquals(listOf("next", "later"), events.upcoming(now).map { it.title })
        assertNull(listOf(event("gap", 600, 1200, now)).nowPlaying(now))
        assertEquals(0f, event("zero", 0, 0, now).progressFraction, 0f)
    }
}
