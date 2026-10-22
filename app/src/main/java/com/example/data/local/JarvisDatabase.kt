package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.CommandHistoryEntity

@Database(entities = [CommandHistoryEntity::class], version = 1, exportSchema = false)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun commandDao(): CommandDao
}
