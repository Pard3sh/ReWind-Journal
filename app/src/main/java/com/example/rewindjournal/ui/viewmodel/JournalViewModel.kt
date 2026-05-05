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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class FolderSummary(
    val id: String,
    val name: String,
    val entryCount: Int,
    val description: String,
    val color: Int
)

data class TimelineMoment(
    val id: String,
    val title: String,
    val subtitle: String,
    val body: String = "",
    val folderId: String? = null,
    val folderColor: Int? = null
)

data class FolderTimelineNode(
    val id: String,
    val entryId: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val location: String,
    val emotionLabel: String,
    val sentimentLabel: String,
    val sentimentScore: Float,
    val events: List<String> = emptyList(),
    val locations: List<String> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val generalFolderId = "general"
    val searchQuery: StateFlow<String> = _searchQuery

    private val _userId = MutableStateFlow(FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous")
    val userId: String get() = _userId.value

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid ?: "anonymous"
        _userId.value = uid
        if (uid != "anonymous") {
            repository.startSync()
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    val folders: StateFlow<List<FolderSummary>> = _userId.flatMapLatest { currentUid ->
        combine(
            repository.getAllFolders(currentUid),
            repository.getAllEntries(currentUid),
            _searchQuery
        ) { folders, entries, query ->
            val folderSummaries = folders.map { folder ->
                FolderSummary(
                    id = folder.id,
                    name = folder.name,
                    entryCount = entries.count { it.folderId == folder.id },
                    description = folder.description,
                    color = folder.color
                )
            }

            val generalCount = entries.count { it.folderId == null }
            val generalFolder = FolderSummary(
                id = generalFolderId,
                name = "General",
                entryCount = generalCount,
                description = "Unorganized reflections and quick notes.",
                color = 0xFF9E9E9E.toInt()
            )

            val allFolders = listOf(generalFolder) + folderSummaries

            if (query.isBlank()) {
                allFolders
            } else {
                allFolders.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val entries: StateFlow<List<TimelineMoment>> = _userId.flatMapLatest { currentUid ->
        combine(
            repository.getAllEntriesWithFolder(currentUid),
            _searchQuery
        ) { entriesWithFolder, query ->
            val mapped = entriesWithFolder.map { item ->
                TimelineMoment(
                    id = item.entry.id,
                    title = item.entry.title,
                    subtitle = formatSubtitle(item.entry.timestamp, item.folder?.name),
                    body = item.entry.body,
                    folderId = item.entry.folderId,
                    folderColor = item.folder?.color ?: 0xFF9E9E9E.toInt()
                )
            }

            if (query.isBlank()) {
                mapped
            } else {
                mapped.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.body.contains(query, ignoreCase = true)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addEntry(
        title: String,
        body: String = "",
        folderId: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            repository.insertEntry(
                JournalEntry(
                    userId = userId,
                    title = title.ifBlank { "Untitled entry" },
                    body = body,
                    folderId = if (folderId == generalFolderId) null else folderId,
                    timestamp = System.currentTimeMillis(),
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }
    }

    fun updateEntry(id: String, title: String, body: String, folderId: String?) {
        viewModelScope.launch {
            repository.getEntryById(id)?.let { existingEntry ->
                repository.updateEntry(
                    existingEntry.copy(
                        title = title,
                        body = body,
                        folderId = if (folderId == generalFolderId) null else folderId,
                        timestamp = System.currentTimeMillis(),
                        latitude = existingEntry.latitude,
                        longitude = existingEntry.longitude
                    )
                )
            }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            repository.getEntryById(id)?.let { entry ->
                repository.deleteEntry(entry)
            }
        }
    }

    suspend fun addFolder(name: String, description: String, color: Int): String {
        return repository.insertFolder(
            Folder(
                userId = userId,
                name = name,
                description = description,
                color = color
            )
        )
    }

    fun updateFolder(id: String, name: String, description: String, color: Int) {
        viewModelScope.launch {
            repository.getFolderById(id)?.let { existingFolder ->
                repository.updateFolder(
                    existingFolder.copy(
                        name = name,
                        description = description,
                        color = color
                    )
                )
            }
        }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch {
            repository.getFolderById(id)?.let { folder ->
                repository.deleteFolder(folder)
            }
        }
    }

    fun getEntriesByFolder(folderId: String): Flow<List<TimelineMoment>> =
        _userId.flatMapLatest { currentUid ->
            val targetEntries = if (folderId == generalFolderId) {
                repository.getAllEntries(currentUid).map { entries ->
                    entries.filter { entry -> entry.folderId == null }
                }
            } else {
                repository.getEntriesByFolder(folderId, currentUid)
            }

            targetEntries.map { entries ->
                entries.map { entry ->
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

    suspend fun getEntryById(entryId: String): TimelineMoment? {
        return repository.getEntryById(entryId)?.let { entry ->
            val folder = entry.folderId?.let { repository.getFolderById(it) }
            TimelineMoment(
                id = entry.id,
                title = entry.title,
                subtitle = formatSubtitle(entry.timestamp, folder?.name),
                body = entry.body,
                folderId = entry.folderId,
                folderColor = folder?.color ?: 0xFF9E9E9E.toInt()
            )
        }
    }

    fun getFolderFlow(folderId: String): Flow<Folder?> =
        repository.getFolderFlow(folderId)

    fun getSentimentNodesByFolder(folderId: String): Flow<List<FolderTimelineNode>> =
        repository.getSentimentNodesByFolder(folderId).map { nodes ->
            nodes.map { node ->
                FolderTimelineNode(
                    id = node.id,
                    entryId = node.entryId,
                    title = node.generatedTitle.ifBlank { node.entryTitle },
                    subtitle = formatTimelineSubtitle(
                        timestamp = node.timestamp,
                        emotion = node.emotionLabel,
                        location = node.savedLocation
                    ),
                    timestamp = node.timestamp,
                    location = node.savedLocation,
                    emotionLabel = node.emotionLabel,
                    sentimentLabel = sentimentLabelFromScore(node.sentimentScore),
                    sentimentScore = node.sentimentScore,
                    events = parseStoredList(node.extractedEvents),
                    locations = parseStoredList(node.extractedLocations)
                )
            }
        }

    fun getDetailedNodesByFolder(folderId: String): Flow<List<FolderTimelineNode>> =
        repository.getDetailedNodesByFolder(folderId).map { nodes ->
            nodes.map { node ->
                FolderTimelineNode(
                    id = node.id,
                    entryId = node.entryId,
                    title = node.generatedTitle.ifBlank { node.entryTitle },
                    subtitle = formatTimelineSubtitle(
                        timestamp = node.timestamp,
                        emotion = node.emotionLabel.ifBlank { node.sentimentLabel },
                        location = node.savedLocation
                    ),
                    timestamp = node.timestamp,
                    location = node.savedLocation,
                    emotionLabel = node.emotionLabel,
                    sentimentLabel = node.sentimentLabel,
                    sentimentScore = node.sentimentScore,
                    events = parseStoredList(node.extractedEvents),
                    locations = parseStoredList(node.extractedLocations)
                )
            }
        }

    private fun formatTimelineSubtitle(
        timestamp: Long,
        emotion: String,
        location: String
    ): String {
        val datePart = formatSubtitle(timestamp, null)
        return when {
            emotion.isNotBlank() && location.isNotBlank() -> "$datePart · $emotion · $location"
            emotion.isNotBlank() -> "$datePart · $emotion"
            location.isNotBlank() -> "$datePart · $location"
            else -> datePart
        }
    }

    private fun sentimentLabelFromScore(score: Float): String {
        return when {
            score >= 0.6f -> "Very Positive"
            score >= 0.2f -> "Positive"
            score > -0.2f -> "Neutral"
            score >= -0.6f -> "Negative"
            else -> "Very Negative"
        }
    }

   private fun parseStoredList(raw: String): List<String> {
        if (raw.isBlank() || raw == "[]") return emptyList()

        return try {
            val jsonArray = JSONArray(raw)
            List(jsonArray.length()) { index ->
                jsonArray.optString(index).trim()
            }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            raw.removePrefix("[")
                .removeSuffix("]")
                .split(",")
                .map { item ->
                    item.trim().removeSurrounding("\"").removeSurrounding("'")
                }
                .filter { it.isNotBlank() }
        }
    }

    fun parseListForUi(raw: String): List<String> =
        parseStoredList(raw)

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

    fun formatAbsoluteDate(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatAbsoluteDateWithRelativeHint(timestamp: Long): String {
        val absolute = formatAbsoluteDate(timestamp)

        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        val relativeHint = when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days in 2..6 -> "$days days ago"
            else -> null
        }

        return if (relativeHint != null) {
            "$absolute ($relativeHint)"
        } else {
            absolute
        }
    }

    fun formatEntryDate(timestamp: Long): String = formatSubtitle(timestamp, null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JournalApplication
                JournalViewModel(application.repository)
            }
        }
    }
}