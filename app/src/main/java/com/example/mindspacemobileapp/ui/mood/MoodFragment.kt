package com.example.mindspacemobileapp.ui.mood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.mindspacemobileapp.R

class MoodFragment : Fragment() {

    // ViewModel that talks to Room or Repository (we’ll connect later)
    private val viewModel: MoodViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the XML layout
        return inflater.inflate(R.layout.fragment_mood, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Grab references to UI elements
        val radioGroup = view.findViewById<RadioGroup>(R.id.moodOptions)
        val journalEditText = view.findViewById<EditText>(R.id.journalEntry)
        val saveButton = view.findViewById<Button>(R.id.saveMoodButton)

        // Save button click listener
        saveButton.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            val mood = when (selectedId) {
                R.id.happyOption -> "Happy"
                R.id.sadOption -> "Sad"
                R.id.neutralOption -> "Neutral"
                else -> "Unknown"
            }

            val journal = journalEditText.text.toString()
            viewModel.saveMood(mood, journal)

            Toast.makeText(requireContext(), "Mood saved!", Toast.LENGTH_SHORT).show()
            journalEditText.text.clear()
            radioGroup.clearCheck()
        }
    }
}
