package com.iptvplayer.app.domain.model

data class Category(
    val id: String,
    val name: String,
    val isHidden: Boolean = false,
)
