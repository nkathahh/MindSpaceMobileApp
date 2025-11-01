package com.example.mindspacemobileapp.data

class AppRepository(private val db: AppDatabase) {

    // Mood
    suspend fun insertMood(entry: MoodEntry): Long = db.moodDao().insert(entry)
    suspend fun getAllMoods(): List<MoodEntry> = db.moodDao().getAll()
    suspend fun getMoodsSince(since: Long): List<MoodEntry> = db.moodDao().getSince(since)
    suspend fun deleteMood(entry: MoodEntry) = db.moodDao().delete(entry)

    // Journal
    suspend fun insertJournal(entry: JournalEntry): Long = db.journalDao().insert(entry)
    suspend fun updateJournal(entry: JournalEntry) = db.journalDao().update(entry)
    suspend fun getAllJournals(): List<JournalEntry> = db.journalDao().getAll()
    suspend fun getJournalById(id: Long): JournalEntry? = db.journalDao().getById(id)
    suspend fun deleteJournal(entry: JournalEntry) = db.journalDao().delete(entry)
}
