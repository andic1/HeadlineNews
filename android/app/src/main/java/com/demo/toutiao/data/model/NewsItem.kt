package com.demo.toutiao.data.model

object Categories {
    val ALL = listOf("关注", "推荐", "热榜", "新时代")
    const val DEFAULT = "推荐"

    /** App 分类 → API 平台代码 */
    fun platformsFor(category: String): List<String> = when (category) {
        "关注" -> listOf("thepaper")
        "推荐" -> listOf("toutiao")
        "热榜" -> listOf("zhihu")
        "新时代" -> listOf("toutiao", "thepaper", "zhihu")  // 聚合
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
