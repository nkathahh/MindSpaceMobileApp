package com.example.mindspacemobileapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,               // 0..10 or 1..10 depending on your UI
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
