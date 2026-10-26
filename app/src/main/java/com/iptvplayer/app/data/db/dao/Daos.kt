package com.iptvplayer.app.data.db.dao

import androidx.room.*
import com.iptvplayer.app.data.db.entity.FavoriteEntity
import com.iptvplayer.app.data.db.entity.HiddenCategoryEntity
import com.iptvplayer.app.data.db.entity.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT streamId FROM favorites")
    fun getAllIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Delete
    suspend fun delete(entity: FavoriteEntity)

    @Query("SELECT COUNT(*) FROM favorites WHERE streamId = :id")
    suspend fun exists(id: Int): Int
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY watchedAt DESC LIMIT 50")
    fun getAll(): Flow<List<RecentEntity>>

    @Query("SELECT streamId FROM recents ORDER BY watchedAt DESC LIMIT 50")
    fun getAllIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentEntity)

    @Query("DELETE FROM recents WHERE streamId NOT IN (SELECT streamId FROM recents ORDER BY watchedAt DESC LIMIT 50)")
    suspend fun trimToLimit()
}

@Dao
interface HiddenCategoryDao {
    @Query("SELECT * FROM hidden_categories")
    fun getAll(): Flow<List<HiddenCategoryEntity>>

    @Query("SELECT categoryId FROM hidden_categories")
    fun getAllIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HiddenCategoryEntity)

    @Delete
    suspend fun delete(entity: HiddenCategoryEntity)
}
