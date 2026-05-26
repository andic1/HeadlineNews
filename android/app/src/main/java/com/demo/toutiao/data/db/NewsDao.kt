package com.demo.toutiao.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface NewsDao {
    @Query("SELECT * FROM news WHERE category = :cat ORDER BY position ASC")
    fun pagingSource(cat: String): PagingSource<Int, NewsEntity>

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
