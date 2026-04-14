package com.example.rewindjournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rewindjournal.JournalApplication
import com.example.rewindjournal.data.JournalEntry
import com.example.rewindjournal.data.JournalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class FolderSummary(
    val id: Long,
    val name: String,
    val entryCount: Int,
    val description: String
)

data class TimelineMoment(
    val id: Long,
    val title: String,
    val subtitle: String
)

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

    val folders: StateFlow<List<FolderSummary>> = combine(
        repository.allFolders,
        repository.allEntries
    ) { folders, entries ->
        folders.map { folder ->
            FolderSummary(
                id = folder.id,
                name = folder.name,
                entryCount = entries.count { it.folderId == folder.id },
                description = folder.description
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val entries: StateFlow<List<TimelineMoment>> = repository.allEntriesWithFolder
        .map { entriesWithFolder ->
            entriesWithFolder.map { item ->
                TimelineMoment(
                    id = item.entry.id,
                    title = item.entry.title,
                    subtitle = formatSubtitle(item.entry.timestamp, item.folder?.name)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addEntry(title: String, body: String = "", folderId: Long? = null) {
        viewModelScope.launch {
            repository.insertEntry(
                JournalEntry(
                    title = title.ifBlank { "Untitled entry" },
                    body = body,
//                    folderId = folderId?:0L,
                    folderId = folderId,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun formatSubtitle(timestamp: Long, folderName: String?): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        val timeLabel = when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days < 7L -> "$days days ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }

        return if (folderName != null) {
            "$timeLabel · $folderName"
        } else {
            timeLabel
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JournalApplication)
                JournalViewModel(application.repository)
            }
        }
    }
}
