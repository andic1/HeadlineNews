package com.demo.toutiao.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 热点新闻客户端 —— 从 apiserver.alcex.cn/daily-hot 获取数据。
 *
 * 支持平台：zhihu / toutiao / thepaper
 */
class HotNewsClient(private val baseUrl: String, private val timeoutMs: Long = 10000L) {

    private val log = LoggerFactory.getLogger(HotNewsClient::class.java)

    private val http = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
            socketTimeoutMillis = timeoutMs
        }
        expectSuccess = false
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // ===================== Category → Platform =====================

    fun platformForCategory(category: String): String? = when (category) {
        "关注" -> "thepaper"
        "推荐" -> "toutiao"
        "热榜" -> "zhihu"
        "新时代" -> null   // 聚合，不直接映射
        else -> null
    }

    // ===================== Public API =====================

    suspend fun fetchForCategory(category: String, page: Int, pageSize: Int): List<HotNewsItem> {
        return when (category) {
            "新时代" -> fetchAggregate(pageSize)
            else -> {
                val platform = platformForCategory(category) ?: return emptyList()
                fetchPlatform(platform, pageSize)
            }
        }
    }

    // ===================== 单平台获取 =====================

    private suspend fun fetchPlatform(platform: String, limit: Int): List<HotNewsItem> {
        return try {
            val url = "$baseUrl/daily-hot/$platform"
            val resp = http.get(url)
            if (resp.status.value !in 200..299) {
                log.warn("HTTP ${resp.status.value} for $url")
                return emptyList()
            }
            val body = resp.bodyAsText()
            parseResponse(body, limit)
        } catch (e: Exception) {
            log.warn("fetchPlatform($platform) failed: ${e.message}")
            emptyList()
        }
    }

    // ===================== 聚合（新时代：3 个平台混合） =====================

    private suspend fun fetchAggregate(limit: Int): List<HotNewsItem> = coroutineScope {
        val perSource = (limit / 3).coerceAtLeast(5)
        val results = listOf(
            async { fetchPlatform("toutiao", perSource) },
            async { fetchPlatform("thepaper", perSource) },
            async { fetchPlatform("zhihu", perSource) },
        ).awaitAll()

        results.flatten()
            .distinctBy { it.id }
            .shuffled()
            .take(limit)
    }

    // ===================== JSON 解析 =====================

    private fun parseResponse(body: String, limit: Int): List<HotNewsItem> {
        val root = json.parseToJsonElement(body).jsonObject
        val code = root["code"]?.jsonPrimitive?.intOrNull
        if (code != 200) {
            log.warn("API returned code=$code")
            return emptyList()
        }
        val source = root["title"]?.jsonPrimitive?.contentOrNull ?: ""
        val dataArr = root["data"]?.jsonArray ?: return emptyList()

        return dataArr.take(limit).mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["id"]?.let { prim ->
                // id 可能是 number 或 string
                when {
                    prim is JsonPrimitive && prim.isString -> prim.content
                    prim is JsonPrimitive -> prim.content
                    else -> null
                }
            } ?: return@mapNotNull null

            val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            if (title.isBlank()) return@mapNotNull null

            val desc = obj["desc"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val cover = obj["cover"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val url = obj["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val mobileUrl = obj["mobileUrl"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val timestamp = obj["timestamp"]?.jsonPrimitive?.longOrNull

            HotNewsItem(
                id = id,
                title = title,
                description = desc,
                source = source,
                imageUrl = cover,
                originalUrl = mobileUrl ?: url,
                publishTime = timestamp?.let { formatTimestamp(it) } ?: nowStr(),
            )
        }
    }

    // ===================== Utils =====================

    private fun formatTimestamp(ts: Long): String {
        // API 返回的 timestamp 是毫秒级
        return try {
            val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.of("Asia/Shanghai"))
            ldt.format(dtf)
        } catch (_: Exception) {
            nowStr()
        }
    }

    private fun nowStr(): String = LocalDateTime.now().format(dtf)
}

data class HotNewsItem(
    val id: String,
    val title: String,
    val description: String?,
    val source: String?,
    val imageUrl: String?,
    val originalUrl: String?,
    val publishTime: String?,
)
