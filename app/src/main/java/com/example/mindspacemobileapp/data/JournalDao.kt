package com.example.mindspacemobileapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface JournalDao {
    @Insert
    suspend fun insert(entry: JournalEntry): Long

    @Update
    suspend fun update(entry: JournalEntry)

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): JournalEntry?

    @Delete
    suspend fun delete(entry: JournalEntry)
}