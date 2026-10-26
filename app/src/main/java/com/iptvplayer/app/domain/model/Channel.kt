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
    val isRecent: Boolean = false,
)
