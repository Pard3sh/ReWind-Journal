package com.example.rewindjournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rewindjournal.JournalApplication
import com.example.rewindjournal.data.Folder
import com.example.rewindjournal.data.JournalEntry
import com.example.rewindjournal.data.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    val description: String,
    val color: Int
)

data class TimelineMoment(
    val id: Long,
    val title: String,
    val subtitle: String,
    val body: String = "",
    val folderId: Long? = null
)

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val folders: StateFlow<List<FolderSummary>> = combine(
        repository.allFolders,
        repository.allEntries
    ) { folders, entries ->
        val folderSummaries = folders.map { folder ->
            FolderSummary(
                id = folder.id,
                name = folder.name,
                entryCount = entries.count { it.folderId == folder.id },
                description = folder.description,
                color = folder.color
            )
        }
        
        // Virtual General folder (id = -1)
        val generalCount = entries.count { it.folderId == null }
        val generalFolder = FolderSummary(
            id = -1,
            name = "General",
            entryCount = generalCount,
            description = "Unorganized reflections and quick notes.",
            color = 0xFF9E9E9E.toInt()
        )
        
        listOf(generalFolder) + folderSummaries
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val entries: StateFlow<List<TimelineMoment>> = combine(
        repository.allEntriesWithFolder,
        _searchQuery
    ) { entriesWithFolder, query ->
        entriesWithFolder
            .filter { 
                it.entry.title.contains(query, ignoreCase = true) || 
                it.entry.body.contains(query, ignoreCase = true) 
            }
            .map { item ->
                TimelineMoment(
                    id = item.entry.id,
                    title = item.entry.title,
                    subtitle = formatSubtitle(item.entry.timestamp, item.folder?.name),
                    body = item.entry.body,
                    folderId = item.entry.folderId
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addEntry(title: String, body: String = "", folderId: Long? = null) {
        viewModelScope.launch {
            repository.insertEntry(
                JournalEntry(
                    title = title.ifBlank { "Untitled entry" },
                    body = body,
                    folderId = if (folderId == -1L) null else folderId,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateEntry(id: Long, title: String, body: String, folderId: Long?) {
        viewModelScope.launch {
            repository.getEntryById(id)?.let { existingEntry ->
                repository.updateEntry(
                    existingEntry.copy(
                        title = title,
                        body = body,
                        folderId = if (folderId == -1L) null else folderId,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun addFolder(name: String, description: String, color: Int) {
        viewModelScope.launch {
            repository.insertFolder(
                Folder(
                    name = name,
                    description = description,
                    color = color
                )
            )
        }
    }

    fun getEntriesByFolder(folderId: Long): Flow<List<TimelineMoment>> {
        val targetEntries = if (folderId == -1L) {
            repository.allEntries.map { it.filter { entry -> entry.folderId == null } }
        } else {
            repository.getEntriesByFolder(folderId)
        }

        return combine(targetEntries, _searchQuery) { entries, query ->
            entries
                .filter { 
                    it.title.contains(query, ignoreCase = true) || 
                    it.body.contains(query, ignoreCase = true) 
                }
                .map { entry ->
                    TimelineMoment(
                        id = entry.id,
                        title = entry.title,
                        subtitle = formatSubtitle(entry.timestamp, null),
                        body = entry.body,
                        folderId = entry.folderId
                    )
                }
        }
    }

    suspend fun getEntryById(entryId: Long): TimelineMoment? {
        return repository.getEntryById(entryId)?.let { entry ->
            val folder = entry.folderId?.let { repository.getFolderById(it) }
            TimelineMoment(
                id = entry.id,
                title = entry.title,
                subtitle = formatSubtitle(entry.timestamp, folder?.name),
                body = entry.body,
                folderId = entry.folderId
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
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
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
