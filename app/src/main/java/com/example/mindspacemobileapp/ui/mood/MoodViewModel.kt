package com.example.mindspacemobileapp.ui.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log

class MoodViewModel : ViewModel() {

    fun saveMood(mood: String, journal: String) {
        viewModelScope.launch {
            // For now, just log or print it. Later, you'll save to Room.
            Log.d("MoodViewModel", "Mood saved: $mood | Journal: $journal")

            // TODO: replace this with database save logic
        }
    }
}
