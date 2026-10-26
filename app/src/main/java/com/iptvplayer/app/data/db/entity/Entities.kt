package com.iptvplayer.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val streamId: Int,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val streamId: Int,
    val watchedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "hidden_categories")
data class HiddenCategoryEntity(
    @PrimaryKey val categoryId: String,
)
