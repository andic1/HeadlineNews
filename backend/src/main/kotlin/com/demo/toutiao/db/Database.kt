package com.demo.toutiao.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

data class DbConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
)

fun initDatabase(cfg: DbConfig): Database {
    val hikari = HikariConfig().apply {
        jdbcUrl = cfg.url
        username = cfg.user
        password = cfg.password
        maximumPoolSize = cfg.maxPoolSize
        driverClassName = "com.mysql.cj.jdbc.Driver"
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    val ds = HikariDataSource(hikari)
    return Database.connect(ds)
}
