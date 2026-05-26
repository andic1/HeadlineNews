package com.demo.toutiao.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object NewsTable : Table("news") {
    val id = varchar("id", 64)
    val category = varchar("category", 16)
    val title = varchar("title", 512)
    val description = text("description").nullable()
    val sourceName = varchar("source", 128).nullable()
    val imageUrl = varchar("image_url", 1024).nullable()
    val originalUrl = varchar("original_url", 1024).nullable()
    val publishTime = datetime("publish_time").nullable()
    val layoutType = varchar("layout_type", 32)
    val fetchedAt = datetime("fetched_at")
    val page = integer("page")
    val position = integer("position")
    override val primaryKey = PrimaryKey(id)
}

object CategoryCacheMetaTable : Table("category_cache_meta") {
    val category = varchar("category", 16)
    val page = integer("page")
    val lastFetchedAt = datetime("last_fetched_at")
    val hasMore = bool("has_more").default(true)
    val totalCount = integer("total_count").nullable()
    override val primaryKey = PrimaryKey(category, page)
}
