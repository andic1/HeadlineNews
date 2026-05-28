package com.demo.toutiao.data.model

object Categories {
    const val FOLLOWING = "关注"
    const val RECOMMEND = "推荐"
    const val HOT = "热榜"
    const val TRENDING = "新时代"

    val ALL = listOf(FOLLOWING, RECOMMEND, HOT, TRENDING)
    const val DEFAULT = RECOMMEND

    fun platformsFor(category: String): List<String> = when (category) {
        FOLLOWING -> listOf("thepaper")
        RECOMMEND -> listOf("toutiao")
        HOT -> listOf("zhihu")
        TRENDING -> listOf("toutiao", "thepaper", "zhihu")
        else -> listOf("toutiao")
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
)
