package com.demo.toutiao.data.repo

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.demo.toutiao.data.api.NewsApi
import com.demo.toutiao.data.db.AppDatabase
import com.demo.toutiao.data.db.toDomain
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.data.paging.NewsRemoteMediator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val api: NewsApi,
    private val db: AppDatabase,
) {
    @OptIn(ExperimentalPagingApi::class)
    fun pagingFlow(category: String): Flow<PagingData<NewsItem>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            prefetchDistance = 5,
            enablePlaceholders = false,
        ),
        remoteMediator = NewsRemoteMediator(category, api, db),
        pagingSourceFactory = { db.newsDao().pagingSource(category) },
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
}
