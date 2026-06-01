package com.headline.news.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * apiserver.alcex.cn/daily-hot/{platform} 的响应格式
 */
@Serializable
data class HotNewsResponse(
    val code: Int,
    val name: String = "",
    val title: String = "",
    val type: String = "",
    val total: Int = 0,
    val fromCache: Boolean = false,
    val updateTime: String = "",
    val data: List<HotNewsDto> = emptyList(),
)

@Serializable
data class HotNewsDto(
    val id: JsonPrimitive? = null,
    val title: String = "",
    val desc: String? = null,
    val cover: String? = null,
    val hot: Long? = null,
    val timestamp: Long? = null,
    val url: String? = null,
    val mobileUrl: String? = null,
)
