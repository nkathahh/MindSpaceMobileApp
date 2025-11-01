package com.example.mindspacemobileapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mindspacemobileapp.data.AppRepository
import com.example.mindspacemobileapp.ui.mood.MoodViewModel
import com.example.mindspacemobileapp.ui.mood.JournalViewModel

class ViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MoodViewModel::class.java) -> MoodViewModel(repository) as T
            modelClass.isAssignableFrom(JournalViewModel::class.java) -> JournalViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
