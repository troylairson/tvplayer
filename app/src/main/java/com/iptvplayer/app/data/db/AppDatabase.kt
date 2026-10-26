package com.iptvplayer.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iptvplayer.app.data.db.dao.FavoriteDao
import com.iptvplayer.app.data.db.dao.HiddenCategoryDao
import com.iptvplayer.app.data.db.dao.RecentDao
import com.iptvplayer.app.data.db.entity.FavoriteEntity
import com.iptvplayer.app.data.db.entity.HiddenCategoryEntity
import com.iptvplayer.app.data.db.entity.RecentEntity

@Database(
    entities = [FavoriteEntity::class, RecentEntity::class, HiddenCategoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun hiddenCategoryDao(): HiddenCategoryDao
}
