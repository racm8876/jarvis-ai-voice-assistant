package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CommandHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandDao {

    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllHistory(): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(entry: CommandHistoryEntity): Long

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}
