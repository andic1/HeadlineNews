package com.demo.toutiao.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.demo.toutiao.data.api.ApiException
import com.demo.toutiao.data.api.NewsApi
import com.demo.toutiao.data.db.AppDatabase
import com.demo.toutiao.data.db.NewsEntity
import com.demo.toutiao.data.db.RemoteKeysEntity
import com.demo.toutiao.data.db.toEntity
import com.demo.toutiao.data.model.Categories
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediator(
    private val category: String,
    private val api: NewsApi,
    private val db: AppDatabase,
) : RemoteMediator<Int, NewsEntity>() {

    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, NewsEntity>,
    ): MediatorResult {
        if (loadType == LoadType.PREPEND) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }
        if (loadType == LoadType.APPEND) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }

        return try {
            val pageItems = prepareRefreshItems(fetchMergedItems())
            if (pageItems.isEmpty()) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            db.withTransaction {
                db.newsDao().clearCategory(category)
                db.remoteKeysDao().delete(category)
                db.newsDao().upsertAll(pageItems)
                db.remoteKeysDao().upsert(
                    RemoteKeysEntity(
                        category = category,
                        nextPage = null,
                        lastUpdated = System.currentTimeMillis(),
                    ),
                )
            }

            MediatorResult.Success(endOfPaginationReached = true)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        } catch (e: ApiException) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun fetchMergedItems(): List<NewsEntity> {
        val platforms = Categories.platformsFor(category)
        return fetchMergedNewsEntities(
            platforms = platforms,
            platformLimit = PLATFORM_LIMIT,
        ) { platform, platformIndex, limit ->
            val resp = api.getHotNews(platform, cache = false, limit = limit)
            if (resp.code != 200) {
                throw ApiException(resp.code, "API error: code=${resp.code}")
            }

            val source = resp.title.ifBlank { platformDisplayName(platform) }
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
    }

    private companion object {
        const val PLATFORM_LIMIT = 50
    }
}

internal fun prepareRefreshItems(items: List<NewsEntity>): List<NewsEntity> =
    items.mapIndexed { index, entity -> entity.copy(position = index) }

internal suspend fun fetchMergedNewsEntities(
    platforms: List<String>,
    platformLimit: Int,
    fetchPlatform: suspend (platform: String, platformIndex: Int, limit: Int) -> List<NewsEntity>,
): List<NewsEntity> = coroutineScope {
    val buckets = platforms.mapIndexed { platformIndex, platform ->
        async {
            try {
                val items = withTimeoutOrNull(SOURCE_TIMEOUT_MS) {
                    fetchPlatform(platform, platformIndex, platformLimit)
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

    val successfulBuckets = buckets.mapNotNull { it.getOrNull() }
    if (successfulBuckets.isEmpty()) {
        buckets.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { throw it }
    }

    successfulBuckets
        .roundRobin()
        .distinctBy { it.id }
}

private const val SOURCE_TIMEOUT_MS = 4_000L

private fun platformDisplayName(platform: String): String = when (platform) {
    "thepaper" -> "澎湃新闻"
    "toutiao" -> "今日头条"
    "zhihu" -> "知乎"
    "geekpark" -> "极客公园"
    "tieba" -> "贴吧"
    else -> platform
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
