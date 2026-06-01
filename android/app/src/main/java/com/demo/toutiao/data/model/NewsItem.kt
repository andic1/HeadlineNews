package com.demo.toutiao.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object Categories {
    const val FOLLOWING = "\u5173\u6ce8"
    const val RECOMMEND = "\u63a8\u8350"
    const val HOT = "\u70ed\u699c"
    const val TRENDING = "\u65b0\u65f6\u4ee3"

    val ALL = listOf(FOLLOWING, RECOMMEND, HOT, TRENDING)
    const val DEFAULT = RECOMMEND

    val ALL_PLATFORMS = listOf("v2ex", "thepaper", "zhihu", "geekpark", "tieba")

    fun platformsFor(category: String): List<String> = when (category) {
        FOLLOWING -> listOf("v2ex", "zhihu", "thepaper", "geekpark", "tieba")
        RECOMMEND -> listOf("thepaper", "geekpark", "v2ex", "tieba", "zhihu")
        HOT -> listOf("zhihu", "tieba", "v2ex", "thepaper", "geekpark")
        TRENDING -> listOf("geekpark", "v2ex", "tieba", "zhihu", "thepaper")
        else -> ALL_PLATFORMS
    }
}

object LayoutType {
    const val TEXT_ONLY = "TEXT_ONLY"
    const val TEXT_WITH_THUMB = "TEXT_WITH_THUMB"
    const val BIG_IMAGE = "BIG_IMAGE"
}

data class NewsItem(
    val id: String,
    val category: String,
    val title: String,
    val description: String?,
    val source: String?,
    val imageUrl: String?,
    val originalUrl: String?,
    val publishTime: String?,
    val layoutType: String,
    val cachedAt: Long? = null,
)

private val displayTimeFormats by lazy {
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

private val fallbackTimeFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }
}

fun NewsItem.displayPublishTime(): String? {
    val rawTime = publishTime?.trim().orEmpty()
    if (rawTime.isNotBlank()) {
        val parsed = displayTimeFormats.firstNotNullOfOrNull { format ->
            try {
                format.parse(rawTime)
            } catch (_: Exception) {
                null
            }
        }
        if (parsed != null && isPlausibleDate(parsed.time)) return fallbackTimeFormat.format(parsed)
    }

    val cached = cachedAt ?: return null
    if (cached <= 0L) return null
    return fallbackTimeFormat.format(cached)
}

private fun isPlausibleDate(epochMillis: Long): Boolean {
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
