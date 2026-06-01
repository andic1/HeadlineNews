package com.headline.news.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.headline.news.data.api.ApiException
import com.headline.news.data.api.NewsApi
import com.headline.news.data.db.AppDatabase
import com.headline.news.data.db.NewsEntity
import com.headline.news.data.db.RemoteKeysEntity
import com.headline.news.data.db.toEntity
import com.headline.news.data.model.Categories
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.io.IOException
import kotlin.random.Random

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
            val pageItems = prepareRefreshItems(fetchMergedItems(), category)
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

            val source = platformDisplayName(platform)
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
        const val PLATFORM_LIMIT = 24
    }
}

internal fun prepareRefreshItems(items: List<NewsEntity>, category: String = ""): List<NewsEntity> {
    val seed = System.currentTimeMillis() xor category.hashCode().toLong()
    return items
        .balancedShuffle(Random(seed))
        .mapIndexed { index, entity -> entity.copy(position = index) }
}

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

private const val SOURCE_TIMEOUT_MS = 2_600L

private fun platformDisplayName(platform: String): String = when (platform) {
    "v2ex" -> "V2EX"
    "thepaper" -> "\u6f8e\u6e43\u65b0\u95fb"
    "zhihu" -> "\u77e5\u4e4e"
    "geekpark" -> "\u6781\u5ba2\u516c\u56ed"
    "tieba" -> "\u8d34\u5427"
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
