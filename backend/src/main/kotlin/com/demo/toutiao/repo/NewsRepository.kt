package com.demo.toutiao.repo

import com.demo.toutiao.db.CategoryCacheMetaTable
import com.demo.toutiao.db.NewsTable
import com.demo.toutiao.model.NewsDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NewsRepository {

    suspend fun lastFetchedAt(category: String, page: Int): LocalDateTime? =
        newSuspendedTransaction {
            CategoryCacheMetaTable.selectAll()
                .where { (CategoryCacheMetaTable.category eq category) and (CategoryCacheMetaTable.page eq page) }
                .limit(1)
                .firstOrNull()
                ?.get(CategoryCacheMetaTable.lastFetchedAt)
        }

    suspend fun loadPage(category: String, page: Int, pageSize: Int): List<NewsDto> =
        newSuspendedTransaction {
            NewsTable.selectAll()
                .where { (NewsTable.category eq category) and (NewsTable.page eq page) }
                .orderBy(NewsTable.position to SortOrder.ASC)
                .limit(pageSize)
                .map { it.toDto() }
        }

    suspend fun upsertPage(
        category: String,
        page: Int,
        items: List<NewsDto>,
        hasMore: Boolean,
    ) = newSuspendedTransaction {
        val now = LocalDateTime.now()

        // 先清掉这一页旧数据，再插入新数据 → 顺序稳定
        NewsTable.deleteWhere {
            (NewsTable.category eq category) and (NewsTable.page eq page)
        }

        items.forEachIndexed { idx, dto ->
            NewsTable.insertIgnore { row ->
                row[id] = dto.id
                row[NewsTable.category] = category
                row[title] = dto.title
                row[description] = dto.description
                row[sourceName] = dto.source
                row[imageUrl] = dto.imageUrl
                row[originalUrl] = dto.originalUrl
                row[publishTime] = dto.publishTime?.let { parseDateTimeOrNull(it) }
                row[layoutType] = dto.layoutType
                row[fetchedAt] = now
                row[NewsTable.page] = page
                row[position] = idx
            }
        }

        // upsert cache meta
        val existing = CategoryCacheMetaTable.selectAll()
            .where { (CategoryCacheMetaTable.category eq category) and (CategoryCacheMetaTable.page eq page) }
            .firstOrNull()
        if (existing == null) {
            CategoryCacheMetaTable.insert {
                it[CategoryCacheMetaTable.category] = category
                it[CategoryCacheMetaTable.page] = page
                it[lastFetchedAt] = now
                it[CategoryCacheMetaTable.hasMore] = hasMore
                it[totalCount] = items.size
            }
        } else {
            CategoryCacheMetaTable.update({
                (CategoryCacheMetaTable.category eq category) and (CategoryCacheMetaTable.page eq page)
            }) {
                it[lastFetchedAt] = now
                it[CategoryCacheMetaTable.hasMore] = hasMore
                it[totalCount] = items.size
            }
        }
    }

    private fun ResultRow.toDto() = NewsDto(
        id = this[NewsTable.id],
        title = this[NewsTable.title],
        description = this[NewsTable.description],
        source = this[NewsTable.sourceName],
        imageUrl = this[NewsTable.imageUrl],
        originalUrl = this[NewsTable.originalUrl],
        publishTime = this[NewsTable.publishTime]?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        layoutType = this[NewsTable.layoutType],
    )

    private fun parseDateTimeOrNull(s: String): LocalDateTime? = try {
        LocalDateTime.parse(s.replace(' ', 'T'))
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (_: Exception) {
            null
        }
    }
}
