package com.iptvplayer.app.domain.model

data class Channel(
    val id: Int,
    val num: Int,
    val name: String,
    val logoUrl: String,
    val categoryId: String,
    val categoryName: String,
    val countryCode: String,
    val epgChannelId: String,
    val streamUrl: String,
    val isFavorite: Boolean = false,
) {
    val isNews: Boolean get() = NEWS_PATTERN.containsMatchIn(categoryName) || NEWS_PATTERN.containsMatchIn(name)

    val isSports: Boolean get() = SPORTS_PATTERN.containsMatchIn(categoryName) || SPORTS_PATTERN.containsMatchIn(name)

    fun matchesQuery(query: String): Boolean =
        name.contains(query, ignoreCase = true) || categoryName.contains(query, ignoreCase = true)

    private companion object {
        val NEWS_PATTERN = Regex("(?i)news|noticias|nachrichten|\\bcnn\\b|\\bmsnbc\\b|\\bcnbc\\b")
        val SPORTS_PATTERN = Regex("(?i)sport|deporte|\\bespn|\\bdazn\\b|\\bbein\\b|\\btsn\\b|\\bnfl\\b|\\bnba\\b|\\bnhl\\b|\\bmlb\\b")
    }
}
