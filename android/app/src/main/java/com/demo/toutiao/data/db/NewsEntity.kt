package com.demo.toutiao.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.demo.toutiao.data.api.NewsDto
import com.demo.toutiao.data.model.NewsItem

@Entity(tableName = "news", primaryKeys = ["id", "category"])
data class NewsEntity(
    val id: String,
    val category: String,
    val title: String,
    val description: String?,
    val source: String?,
    val imageUrl: String?,
    val originalUrl: String?,
    val publishTime: String?,
    val layoutType: String,
    val position: Int,
    val cachedAt: Long,
)

fun NewsDto.toEntity(category: String, position: Int) = NewsEntity(
    id = id,
    category = category,
    title = title,
    description = description,
    source = source,
    imageUrl = imageUrl,
    originalUrl = originalUrl,
    publishTime = publishTime,
    layoutType = layoutType,
    position = position,
    cachedAt = System.currentTimeMillis(),
)

fun NewsEntity.toDomain() = NewsItem(
    id = id,
    category = category,
    title = title,
    description = description,
    source = source,
    imageUrl = imageUrl,
    originalUrl = originalUrl,
    publishTime = publishTime,
    layoutType = layoutType,
)

@Entity(tableName = "remote_keys")
data class RemoteKeysEntity(
    @PrimaryKey val category: String,
    val nextPage: Int?,
    val lastUpdated: Long,
)
