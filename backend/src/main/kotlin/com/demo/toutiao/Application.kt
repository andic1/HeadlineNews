package com.demo.toutiao

import com.demo.toutiao.client.WhytaApiException
import com.demo.toutiao.client.WhytaClient
import com.demo.toutiao.db.DbConfig
import com.demo.toutiao.db.initDatabase
import com.demo.toutiao.model.ApiResponse
import com.demo.toutiao.model.ErrorCode
import com.demo.toutiao.repo.NewsRepository
import com.demo.toutiao.routes.newsRoutes
import com.demo.toutiao.service.NewsService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main(args: Array<String>) = EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    val dbCfg = DbConfig(
        url = environment.config.property("db.url").getString(),
        user = environment.config.property("db.user").getString(),
        password = environment.config.property("db.password").getString(),
        maxPoolSize = environment.config.property("db.maxPoolSize").getString().toInt(),
    )
    initDatabase(dbCfg)

    val whyta = WhytaClient(
        baseUrl = environment.config.property("whyta.baseUrl").getString(),
        apiKey = environment.config.property("whyta.apiKey").getString(),
        timeoutMs = environment.config.property("whyta.timeoutMs").getString().toLong(),
    )
    val ttlMinutes = environment.config.property("cache.ttlMinutes").getString().toLong()
    val service = NewsService(NewsRepository(), whyta, ttlMinutes)

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    install(CallLogging) { level = Level.INFO }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }
    install(StatusPages) {
        exception<WhytaApiException> { call, cause ->
            call.respond(
                HttpStatusCode.OK,
                ApiResponse<Unit>(code = ErrorCode.WHYTA_FAIL, msg = cause.message ?: "whyta error")
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(code = ErrorCode.BAD_PARAM, msg = cause.message ?: "bad param")
            )
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("unhandled", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(code = ErrorCode.INTERNAL, msg = cause.message ?: "internal error")
            )
        }
    }

    routing {
        newsRoutes(service)
    }
}
