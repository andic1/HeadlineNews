package com.demo.toutiao.routes

import com.demo.toutiao.model.ApiResponse
import com.demo.toutiao.model.ErrorCode
import com.demo.toutiao.service.NewsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val VALID_CATEGORIES = setOf("关注", "推荐", "热榜", "新时代", "小说", "视频")

fun Route.newsRoutes(service: NewsService) {
    get("/api/news") {
        val category = call.request.queryParameters["category"]
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
        val forceRefresh = call.request.queryParameters["forceRefresh"]?.toBooleanStrictOrNull() ?: false

        if (category.isNullOrBlank() || category !in VALID_CATEGORIES) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(code = ErrorCode.BAD_PARAM, msg = "invalid category: $category")
            )
            return@get
        }
        if (page < 1 || pageSize !in 1..50) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(code = ErrorCode.BAD_PARAM, msg = "invalid page/pageSize")
            )
            return@get
        }

        val data = service.loadNews(category, page, pageSize, forceRefresh)
        call.respond(ApiResponse(code = ErrorCode.OK, msg = "ok", data = data))
    }

    get("/health") {
        call.respondText("ok")
    }
}
