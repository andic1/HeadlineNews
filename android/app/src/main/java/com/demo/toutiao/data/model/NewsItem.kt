package com.demo.toutiao.data.model

object Categories {
    val ALL = listOf("关注", "推荐", "热榜", "新时代")
    const val DEFAULT = "推荐"
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
