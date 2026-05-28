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
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediator(
    private val category: String,
    private val api: NewsApi,
    private val db: AppDatabase,
) : RemoteMediator<Int, NewsEntity>() {

    /** 缓存有效期：10 分钟 */
    private val cacheTimeout = TimeUnit.MINUTES.toMillis(10)

    override suspend fun initialize(): InitializeAction {
        val keys = db.remoteKeysDao().get(category)
        return if (keys != null && System.currentTimeMillis() - keys.lastUpdated < cacheTimeout) {
            // 缓存未过期，跳过刷新
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, NewsEntity>,
    ): MediatorResult {
        // 这个 API 没有分页，一次返回全部数据，所以只处理 REFRESH
        if (loadType == LoadType.PREPEND) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }
        if (loadType == LoadType.APPEND) {
            // 不支持加载更多，数据已全部拉取
            return MediatorResult.Success(endOfPaginationReached = true)
        }

        return try {
            val platforms = Categories.platformsFor(category)
            val allEntities = mutableListOf<NewsEntity>()
            var posOffset = 0

            for (platform in platforms) {
                val resp = api.getHotNews(platform)
                if (resp.code != 200) {
                    throw ApiException(resp.code, "API error: code=${resp.code}")
                }
                val source = resp.title  // e.g. "知乎", "今日头条", "澎湃新闻"
                val entities = resp.data
                    .filter { it.title.isNotBlank() }
                    .mapIndexed { idx, dto ->
                        dto.toEntity(category, source, posOffset + idx)
                    }
                allEntities.addAll(entities)
                posOffset += entities.size
            }

            // 如果是聚合（新时代），打乱顺序
            if (platforms.size > 1) {
                allEntities.shuffle()
                allEntities.forEachIndexed { idx, entity ->
                    allEntities[idx] = entity.copy(position = idx)
                }
            }

            db.withTransaction {
                db.newsDao().clearCategory(category)
                db.remoteKeysDao().delete(category)
                db.newsDao().upsertAll(allEntities)
                db.remoteKeysDao().upsert(
                    RemoteKeysEntity(
                        category = category,
                        nextPage = null,  // 无分页
                        lastUpdated = System.currentTimeMillis(),
                    )
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
}
