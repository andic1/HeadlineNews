package com.headline.news.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface NewsDao {
    @Query("SELECT * FROM news WHERE category = :cat ORDER BY position ASC")
    fun pagingSource(cat: String): PagingSource<Int, NewsEntity>

    @Query("SELECT * FROM news WHERE category = :cat ORDER BY position ASC LIMIT :limit OFFSET :offset")
    suspend fun loadPage(
        cat: String,
        limit: Int,
        offset: Int,
    ): List<NewsEntity>

    @Query("SELECT * FROM news WHERE category = :cat ORDER BY position ASC")
    suspend fun loadCategory(cat: String): List<NewsEntity>

    @Query("SELECT COUNT(*) FROM news WHERE category = :cat")
    suspend fun count(cat: String): Int

    @Query("DELETE FROM news WHERE category = :cat")
    suspend fun clearCategory(cat: String)

    @Upsert
    suspend fun upsertAll(items: List<NewsEntity>)
}

@Dao
interface RemoteKeysDao {
    @Upsert
    suspend fun upsert(key: RemoteKeysEntity)

    @Query("SELECT * FROM remote_keys WHERE category = :cat")
    suspend fun get(cat: String): RemoteKeysEntity?

    @Query("DELETE FROM remote_keys WHERE category = :cat")
    suspend fun delete(cat: String)
}
