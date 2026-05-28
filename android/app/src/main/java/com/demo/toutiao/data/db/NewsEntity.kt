package com.demo.toutiao.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.demo.toutiao.data.api.HotNewsDto
import com.demo.toutiao.data.model.LayoutType
import com.demo.toutiao.data.model.NewsItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

private val sdf by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }
}

fun HotNewsDto.toEntity(category: String, source: String, position: Int): NewsEntity {
    val idStr = id?.content ?: title.hashCode().toString()
    val layout = when {
        cover.isNullOrBlank() -> LayoutType.TEXT_ONLY
        position % 5 == 4 -> LayoutType.BIG_IMAGE
        else -> LayoutType.TEXT_WITH_THUMB
    }
    val time = timestamp?.let {
        try { sdf.format(Date(it)) } catch (_: Exception) { null }
    }
    return NewsEntity(
        id = idStr,
        category = category,
        title = title,
        description = desc,
        source = source,
        imageUrl = cover,
        originalUrl = url ?: mobileUrl,
        publishTime = time,
        layoutType = layout,
        position = position,
        cachedAt = System.currentTimeMillis(),
    )
}

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
