package com.example.rewindjournal.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

data class FolderSummary(
    val name: String,
    val entryCount: Int,
    val description: String
)

data class TimelineMoment(
    val title: String,
    val subtitle: String
)

class JournalViewModel : ViewModel() {
    private val _folders = mutableStateListOf(
        FolderSummary("Senior Year", 12, "Classes, milestones, and end-of-year reflections."),
        FolderSummary("Italy Trip", 4, "Travel notes and memorable moments."),
        FolderSummary("Internship", 7, "Wins, lessons, and weekly takeaways.")
    )
    val folders: List<FolderSummary> = _folders

    private val _entries = mutableStateListOf(
        TimelineMoment("Proposal submitted", "Today · Senior Year"),
        TimelineMoment("Commute voice memo", "Yesterday · Internship"),
        TimelineMoment("Booked Florence train", "3 days ago · Italy Trip")
    )
    val entries: List<TimelineMoment> = _entries

    fun addEntry(title: String, folder: String = "Daily Journal") {
        _entries.add(
            0,
            TimelineMoment(
                title = title.ifBlank { "Untitled entry" },
                subtitle = "Just now · $folder"
            )
        )
    }
}
