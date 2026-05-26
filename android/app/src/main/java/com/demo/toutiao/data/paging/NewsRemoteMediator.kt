package com.demo.toutiao.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.demo.toutiao.data.api.BackendException
import com.demo.toutiao.data.api.NewsApi
import com.demo.toutiao.data.db.AppDatabase
import com.demo.toutiao.data.db.NewsEntity
import com.demo.toutiao.data.db.RemoteKeysEntity
import com.demo.toutiao.data.db.toEntity
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediator(
    private val category: String,
    private val api: NewsApi,
    private val db: AppDatabase,
) : RemoteMediator<Int, NewsEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, NewsEntity>,
    ): MediatorResult {
        val (page, forceRefresh) = when (loadType) {
            LoadType.REFRESH -> 1 to true
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val key = db.remoteKeysDao().get(category)
                val next = key?.nextPage
                    ?: return MediatorResult.Success(endOfPaginationReached = key != null)
                next to false
            }
        }

        return try {
            val resp = api.getNews(category, page, pageSize = state.config.pageSize, forceRefresh = forceRefresh)
            if (resp.code != 0 || resp.data == null) {
                throw BackendException(resp.code, resp.msg)
            }
            val data = resp.data
            val entities = data.list.mapIndexed { idx, dto -> dto.toEntity(category, idx) }

            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    db.newsDao().clearCategory(category)
                    db.remoteKeysDao().delete(category)
                }
                db.newsDao().upsertAll(entities)
                db.remoteKeysDao().upsert(
                    RemoteKeysEntity(
                        category = category,
                        nextPage = if (data.hasMore) page + 1 else null,
                        lastUpdated = System.currentTimeMillis(),
                    )
                )
            }
            MediatorResult.Success(endOfPaginationReached = !data.hasMore)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        } catch (e: BackendException) {
            MediatorResult.Error(e)
        }
    }
}
