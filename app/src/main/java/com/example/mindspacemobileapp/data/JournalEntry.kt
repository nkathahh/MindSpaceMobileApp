package com.example.mindspacemobileapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String? = null,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)