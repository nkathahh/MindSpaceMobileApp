package com.example.mindspacemobileapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MoodDao {
    @Insert
    suspend fun insert(entry: MoodEntry): Long

    @Query("SELECT * FROM mood_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<MoodEntry>

    @Query("SELECT * FROM mood_entries WHERE createdAt >= :since ORDER BY createdAt ASC")
    suspend fun getSince(since: Long): List<MoodEntry>

    @Delete
    suspend fun delete(entry: MoodEntry)
}
