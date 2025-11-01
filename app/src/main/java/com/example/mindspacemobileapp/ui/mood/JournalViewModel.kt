package com.example.mindspacemobileapp.ui.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindspacemobileapp.data.AppRepository
import com.example.mindspacemobileapp.data.JournalEntry
import kotlinx.coroutines.launch

class JournalViewModel(private val repository: AppRepository) : ViewModel() {

    fun insertJournal(title: String?, content: String) {
        viewModelScope.launch {
            repository.insertJournal(JournalEntry(title = title, content = content))
        }
    }

    // additional functions can be added: load list, update, delete
}