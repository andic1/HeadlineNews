package com.headline.news.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.headline.news.data.api.HotNewsDto
import com.headline.news.data.model.LayoutType
import com.headline.news.data.model.NewsItem
import java.text.SimpleDateFormat
import java.util.Calendar
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
        isLenient = false
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }
}

private val inputTimeFormats by lazy {
    listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        },
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        },
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        },
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.CHINA).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.CHINA).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        },
    )
}

fun HotNewsDto.toEntity(
    category: String,
    source: String,
    position: Int,
    responseUpdateTime: String? = null,
): NewsEntity {
    val idStr = id?.content ?: title.hashCode().toString()
    val layout = when {
        cover.isNullOrBlank() -> LayoutType.TEXT_ONLY
        position % 5 == 4 -> LayoutType.BIG_IMAGE
        else -> LayoutType.TEXT_WITH_THUMB
    }
    val time = normalizePublishTime(responseUpdateTime)
        ?: formatPublishTime(timestamp)
        ?: formatNow()
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
    cachedAt = cachedAt,
)

@Entity(tableName = "remote_keys")
data class RemoteKeysEntity(
    @PrimaryKey val category: String,
    val nextPage: Int?,
    val lastUpdated: Long,
)

private fun formatPublishTime(timestamp: Long?): String? {
    if (timestamp == null || timestamp <= 0L) return null

    val candidates = buildList {
        add(timestamp)
        if (timestamp <= Long.MAX_VALUE / 1000L) add(timestamp * 1000L)
        add(timestamp / 1000L)
        add(timestamp / 1_000_000L)
        add(timestamp / 1_000_000_000L)
    }.distinct().filter { it > 0L }

    val validMillis = candidates.firstOrNull(::isPlausibleEpochMillis) ?: return null
    return sdf.format(validMillis)
}

private fun normalizePublishTime(rawTime: String?): String? {
    val value = rawTime?.trim().orEmpty()
    if (value.isBlank()) return null

    val parsed = inputTimeFormats.firstNotNullOfOrNull { format ->
        try {
            format.parse(value)
        } catch (_: Exception) {
            null
        }
    } ?: return null

    return sdf.format(parsed)
}

private fun formatNow(): String = sdf.format(System.currentTimeMillis())

private fun isPlausibleEpochMillis(epochMillis: Long): Boolean {
    return try {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
        calendar.timeInMillis = epochMillis
        val year = calendar.get(Calendar.YEAR)
        val currentYear = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).get(Calendar.YEAR)
        year in 2020..(currentYear + 1)
    } catch (_: Exception) {
        false
    }
}
