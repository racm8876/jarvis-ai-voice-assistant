package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.JarvisDatabase

class JarvisApplication : Application() {

    lateinit var database: JarvisDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(
            applicationContext,
            JarvisDatabase::class.java,
            "jarvis_assistant.db"
        ).fallbackToDestructiveMigration().build()
    }

    companion object {
        lateinit var instance: JarvisApplication
            private set
    }
}
