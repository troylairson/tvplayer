package com.iptvplayer.app.di

import android.content.Context
import androidx.room.Room
import com.iptvplayer.app.data.db.AppDatabase
import com.iptvplayer.app.data.db.dao.FavoriteDao
import com.iptvplayer.app.data.db.dao.HiddenCategoryDao
import com.iptvplayer.app.data.db.dao.RecentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "iptv_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideRecentDao(db: AppDatabase): RecentDao = db.recentDao()
    @Provides fun provideHiddenCategoryDao(db: AppDatabase): HiddenCategoryDao = db.hiddenCategoryDao()
}
