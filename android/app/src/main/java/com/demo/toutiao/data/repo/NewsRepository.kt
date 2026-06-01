package com.demo.toutiao.data.repo

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.demo.toutiao.data.api.ApiException
import com.demo.toutiao.data.api.NewsApi
import com.demo.toutiao.data.db.AppDatabase
import com.demo.toutiao.data.db.NewsEntity
import com.demo.toutiao.data.db.RemoteKeysEntity
import com.demo.toutiao.data.db.toEntity
import com.demo.toutiao.data.db.toDomain
import com.demo.toutiao.data.model.Categories
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.data.paging.NewsRemoteMediator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import java.io.IOException

@Singleton
class NewsRepository @Inject constructor(
    private val api: NewsApi,
    private val db: AppDatabase,
) {
    @OptIn(ExperimentalPagingApi::class)
    fun pagingFlow(category: String): Flow<PagingData<NewsItem>> = Pager(
        config = PagingConfig(
            pageSize = 15,
            initialLoadSize = 15,
            prefetchDistance = 3,
            enablePlaceholders = false,
        ),
        remoteMediator = NewsRemoteMediator(category, api, db),
        pagingSourceFactory = { db.newsDao().pagingSource(category) },
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    suspend fun prefetchCategory(category: String) {
        val items = fetchPrefetchItems(category)
        if (items.isEmpty()) return
        db.withTransaction {
            db.newsDao().clearCategory(category)
            db.remoteKeysDao().delete(category)
            db.newsDao().upsertAll(items)
            db.remoteKeysDao().upsert(
                RemoteKeysEntity(
                    category = category,
                    nextPage = null,
                    lastUpdated = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun reshuffleCached(category: String): Boolean {
        return db.withTransaction {
            val cached = db.newsDao().loadCategory(category)
            if (cached.size < 2) {
                false
            } else {
                val shuffled = cached
                    .shuffled(Random(System.currentTimeMillis() xor category.hashCode().toLong()))
                    .mapIndexed { index, entity -> entity.copy(position = index) }
                db.newsDao().upsertAll(shuffled)
                true
            }
        }
    }

    private suspend fun fetchPrefetchItems(category: String): List<NewsEntity> {
        val platforms = Categories.platformsFor(category)
        val buckets = coroutineScope {
            platforms.mapIndexed { platformIndex, platform ->
                async {
                    try {
                        val items = withTimeoutOrNull(2_600L) {
                            val resp = api.getHotNews(platform, cache = true, limit = 24)
                            if (resp.code != 200) {
                                throw ApiException(resp.code, "API error: code=${resp.code}")
                            }
                            val source = when (platform) {
                                "v2ex" -> "V2EX"
                                "thepaper" -> "\u6f8e\u6e43\u65b0\u95fb"
                                "zhihu" -> "\u77e5\u4e4e"
                                "geekpark" -> "\u6781\u5ba2\u516c\u56ed"
                                "tieba" -> "\u8d34\u5427"
                                else -> platform
                            }
                            resp.data
                                .filter { it.title.isNotBlank() }
                                .mapIndexed { idx, dto ->
                                    dto.toEntity(
                                        category = category,
                                        source = source,
                                        position = platformIndex * 1000 + idx,
                                        responseUpdateTime = resp.updateTime,
                                    )
                                }
                        }
                        if (items == null) {
                            Result.failure(IOException("Source timeout: $platform"))
                        } else {
                            Result.success(items)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Result.failure(e)
                    }
                }
            }.awaitAll()
        }

        val successfulBuckets = buckets.mapNotNull { it.getOrNull() }
        if (successfulBuckets.isEmpty()) return emptyList()

        return successfulBuckets
            .roundRobin()
            .distinctBy { it.id }
            .balancedShuffle(Random(System.currentTimeMillis() xor category.hashCode().toLong()))
            .mapIndexed { index, entity -> entity.copy(position = index) }
    }
}

private fun <T> List<List<T>>.roundRobin(): List<T> {
    if (isEmpty()) return emptyList()
    val result = mutableListOf<T>()
    var index = 0
    while (true) {
        var added = false
        for (bucket in this) {
            if (index < bucket.size) {
                result.add(bucket[index])
                added = true
            }
        }
        if (!added) break
        index++
    }
    return result
}

private fun List<NewsEntity>.balancedShuffle(random: Random): List<NewsEntity> {
    if (size <= 1) return this
    val buckets = groupBy { it.source.orEmpty() }
        .values
        .map { bucket -> bucket.shuffled(random).toMutableList() }
        .shuffled(random)
    val mixed = mutableListOf<NewsEntity>()
    var cursor = 0
    while (buckets.any { it.isNotEmpty() }) {
        val bucket = buckets[cursor % buckets.size]
        if (bucket.isNotEmpty()) {
            mixed += bucket.removeAt(0)
        }
        cursor++
    }
    return mixed
}
