package com.demo.toutiao.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WhytaApiException(val code: Int, message: String) : RuntimeException(message)

@Serializable
data class WhytaEnvelope(val code: Int, val msg: String, val result: JsonElement? = null)

data class WhytaNewsItem(
    val id: String,
    val title: String,
    val description: String?,
    val source: String?,
    val imageUrl: String?,
    val originalUrl: String?,
    val publishTime: String?,
)

class WhytaClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val timeoutMs: Long,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
            socketTimeoutMillis = timeoutMs
        }
        expectSuccess = false
    }

    fun pathForCategory(category: String): String? = when (category) {
        "关注" -> "/api/tx/news"
        "推荐" -> "/api/tx/generalnews"
        "热榜" -> "/api/tx/hotnews"
        "新时代" -> "/api/tx/topnews"
        else -> null
    }

    /** 聚合"推荐"时使用的所有端点（从多个频道混合拉取） */
    val aggregatePaths: List<String> = listOf(
        "/api/tx/generalnews",
        "/api/tx/news",
        "/api/tx/hotnews",
        "/api/tx/topnews",
    )

    suspend fun fetch(category: String, page: Int, pageSize: Int): List<WhytaNewsItem> {
        val path = pathForCategory(category) ?: return emptyList()
        return fetchByPath(path, page, pageSize)
    }

    suspend fun fetchByPath(path: String, page: Int, pageSize: Int): List<WhytaNewsItem> {
        val resp = http.get("$baseUrl$path") {
            parameter("key", apiKey)
            parameter("num", pageSize)
            parameter("page", page)
        }

        if (resp.status.value !in 200..299) {
            throw WhytaApiException(resp.status.value, "whyta http ${resp.status.value}")
        }

        val envelope: WhytaEnvelope = resp.body()
        if (envelope.code != 200) {
            throw WhytaApiException(envelope.code, "whyta business err: ${envelope.msg}")
        }
        return parseList(envelope.result)
    }

    private fun parseList(result: JsonElement?): List<WhytaNewsItem> {
        val obj = result?.jsonObject ?: return emptyList()
        // whyta 不同端点用 list / newslist / data 等不同 key
        val listEl = obj["list"] ?: obj["newslist"] ?: obj["data"] ?: return emptyList()
        if (listEl !is JsonArray) return emptyList()
        return listEl.mapNotNull { el ->
            val o = el.jsonObject
            val id = o["id"]?.jsonPrimitive?.contentOrNull() ?: return@mapNotNull null
            WhytaNewsItem(
                id = id,
                title = o["title"]?.jsonPrimitive?.contentOrNull().orEmpty(),
                description = o["description"]?.jsonPrimitive?.contentOrNull(),
                source = o["source"]?.jsonPrimitive?.contentOrNull(),
                imageUrl = o["picUrl"]?.jsonPrimitive?.contentOrNull()?.let(::normalizeUrl),
                originalUrl = o["url"]?.jsonPrimitive?.contentOrNull()?.let(::normalizeUrl),
                publishTime = o["ctime"]?.jsonPrimitive?.contentOrNull(),
            )
        }
    }

    private fun JsonPrimitive.contentOrNull(): String? =
        if (this is JsonNull) null else content

    private fun normalizeUrl(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http") -> url
        url.isBlank() -> url
        else -> "https://$url"
    }
}
