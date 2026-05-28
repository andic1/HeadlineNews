package com.demo.toutiao.service

import com.demo.toutiao.client.HotNewsClient
import com.demo.toutiao.client.HotNewsItem
import com.demo.toutiao.model.LayoutType
import com.demo.toutiao.model.NewsDto
import com.demo.toutiao.model.NewsListResponse
import com.demo.toutiao.repo.NewsRepository
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

class NewsService(
    private val repo: NewsRepository,
    private val hotNews: HotNewsClient,
    private val ttlMinutes: Long,
) {
    private val log = LoggerFactory.getLogger(NewsService::class.java)

    suspend fun loadNews(
        category: String,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean,
    ): NewsListResponse {
        if (!forceRefresh && isFresh(category, page)) {
            val cached = repo.loadPage(category, page, pageSize)
            if (cached.isNotEmpty()) {
                log.info("cache hit category=$category page=$page size=${cached.size}")
                return NewsListResponse(category, page, pageSize, hasMore = true, fromCache = true, list = cached)
            }
        }

        val fetched = try {
            val items = hotNews.fetchForCategory(category, page, pageSize)
            if (items.isNotEmpty()) {
                log.info("hotNews success category=$category count=${items.size}")
                items
            } else {
                log.warn("hotNews returned empty for category=$category, fallback to cache")
                val stale = repo.loadPage(category, page, pageSize)
                if (stale.isNotEmpty()) {
                    return NewsListResponse(category, page, pageSize, hasMore = true, fromCache = true, list = stale)
                }
                return NewsListResponse(category, page, pageSize, hasMore = false, fromCache = false, list = emptyList())
            }
        } catch (e: Exception) {
            log.warn("hotNews failed: ${e.message}, fallback to stale cache")
            val stale = repo.loadPage(category, page, pageSize)
            if (stale.isNotEmpty()) {
                return NewsListResponse(category, page, pageSize, hasMore = true, fromCache = true, list = stale)
            }
            log.error("all sources unavailable for category=$category page=$page")
            return NewsListResponse(category, page, pageSize, hasMore = false, fromCache = false, list = emptyList())
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

    private fun layoutFor(index: Int, item: HotNewsItem): String = when {
        item.imageUrl.isNullOrBlank() -> LayoutType.TEXT_ONLY
        index % 5 == 4 -> LayoutType.BIG_IMAGE
        else -> LayoutType.TEXT_WITH_THUMB
    }

    private fun HotNewsItem.toDto(layout: String) = NewsDto(
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
