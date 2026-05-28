package com.demo.toutiao.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NewsEntity::class, RemoteKeysEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun remoteKeysDao(): RemoteKeysDao
}
