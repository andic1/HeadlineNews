package com.demo.toutiao.service

import com.demo.toutiao.client.WhytaApiException
import com.demo.toutiao.client.WhytaClient
import com.demo.toutiao.client.WhytaNewsItem
import com.demo.toutiao.model.LayoutType
import com.demo.toutiao.model.NewsDto
import com.demo.toutiao.model.NewsListResponse
import com.demo.toutiao.repo.NewsRepository
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

class NewsService(
    private val repo: NewsRepository,
    private val whyta: WhytaClient,
    private val ttlMinutes: Long,
) {
    private val log = LoggerFactory.getLogger(NewsService::class.java)

    suspend fun loadNews(
        category: String,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean,
    ): NewsListResponse {
        // 小说/视频：直接空列表
        if (whyta.pathForCategory(category) == null) {
            return NewsListResponse(
                category = category,
                page = page,
                pageSize = pageSize,
                hasMore = false,
                fromCache = false,
                list = emptyList(),
            )
        }

        if (!forceRefresh && isFresh(category, page)) {
            val cached = repo.loadPage(category, page, pageSize)
            if (cached.isNotEmpty()) {
                log.info("cache hit category=$category page=$page size=${cached.size}")
                return NewsListResponse(category, page, pageSize, hasMore = true, fromCache = true, list = cached)
            }
        }

        // 调 whyta（带 1 次重试）
        val fetched = try {
            fetchWithRetry(category, page, pageSize)
        } catch (e: Exception) {
            log.warn("whyta fetch failed: ${e.message}, fallback to stale cache")
            val stale = repo.loadPage(category, page, pageSize)
            if (stale.isNotEmpty()) {
                return NewsListResponse(category, page, pageSize, hasMore = true, fromCache = true, list = stale)
            }
            throw e
        }

        val dtos = fetched.mapIndexed { idx, item -> item.toDto(layoutFor(idx, item)) }
        repo.upsertPage(category, page, dtos, hasMore = dtos.size >= pageSize)
        return NewsListResponse(
            category = category,
            page = page,
            pageSize = pageSize,
            hasMore = dtos.size >= pageSize,
            fromCache = false,
            list = dtos,
        )
    }

    private suspend fun isFresh(category: String, page: Int): Boolean {
        val last = repo.lastFetchedAt(category, page) ?: return false
        return Duration.between(last, LocalDateTime.now()).toMinutes() < ttlMinutes
    }

    private suspend fun fetchWithRetry(category: String, page: Int, pageSize: Int): List<WhytaNewsItem> {
        return try {
            whyta.fetch(category, page, pageSize)
        } catch (e: Exception) {
            if (e is WhytaApiException && e.code in 400..499) throw e
            delay(500)
            whyta.fetch(category, page, pageSize)
        }
    }

    private fun layoutFor(index: Int, item: WhytaNewsItem): String = when {
        item.imageUrl.isNullOrBlank() -> LayoutType.TEXT_ONLY
        index % 5 == 4 -> LayoutType.BIG_IMAGE
        else -> LayoutType.TEXT_WITH_THUMB
    }

    private fun WhytaNewsItem.toDto(layout: String) = NewsDto(
        id = id,
        title = title,
        description = description,
        source = source,
        publishTime = publishTime,
        imageUrl = imageUrl,
        originalUrl = originalUrl,
        layoutType = layout,
    )
}
