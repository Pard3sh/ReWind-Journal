# ReWind Journal (CS501 Project)

ReWind Journal is a mobile journaling app built with Jetpack Compose (Material 3) for CS501. It focuses on making it easy to quickly capture daily reflections, organize them into folders, and later review recent moments.

## Note
Check-in slides are under our presentation folder!

## Current Features

- Home screen with a daily affirmation!
- Basic journal entry composer
- Add/Delete/Update Folders
- Recent moments section 
- Bottom navigation
- Journal entries with options to update and delete entries
- Loading screen when app is first opened
- Timeline screen in progress
- Cloud backup of all data; sync is done whenever the user has internet connection.
- Asks user for location permissions and, if granted, saves location data with each entry.
- Authetication through google (User is now able to connect their journal to their account)
- Cloud jobs that run sentiment analysis on all user created folders (needed to descope the scalability and the extent of analysis)

## Sentiment Analysis and Timeline Generator

**A significant portion of the backend is in the following repo:** https://github.com/Pard3sh/ReWind-Timeline-Generator/

Using ML tools to analyze journal entries was deemed to heavy to run on each local device, so we decided to run a python script that analyzes new entries and generates/updates timeline data as a recurring cloud job. The link to the repo, which the TA and instructor have been added as collaborators for, is here: https://github.com/Pard3sh/ReWind-Timeline-Generator/. The repo consists of a containerized python package that is run as a Cloub Job in the Google Cloud platform we use. It adds to the cloud database, which is then synced locally and displayed on the Android timeline screen. More documentation is in the link. 


## How to Run

1. Open the project in Android Studio
2. Sync Gradle and build the project.
3. Log into google on the emulator device---a Google account is needed to access the application
4. Run the app on an Android emulator or physical device (API 24+). Ensure emulator has google playstore. 

## Screenshots

1. Loading Screen
   <img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/7dba3f4f-8031-4675-96a3-89afed6c7f87" />

2. Login Screen
   <img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/a560396d-553f-4edc-8892-101e435d8553" />

3. Home Screen
<!-- <img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/b31ccef4-1615-4701-9821-81dba63ce5e4" /> -->
<img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/81789cf6-5d76-423a-a896-fe1bc100f271" />

4. Folders Screen
<!-- <img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/4843ed48-3807-4fdf-adca-1524b1179092" /> -->
<img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/7989ae5e-24f4-4725-9ff3-56ee5aacbfe6" />

5. New Folder Screen
<img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/cb0f2a45-29ed-47fd-a306-f3971ed8f8c6" />

6. New Entry Screen
<!-- <img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/3b23a48f-7b8f-4c82-ab3a-e1fff064d36f" /> -->
<img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/93e69b5c-21c7-41ea-a118-95ce4894aa24" />

7. Timeline Screen
<!-- <img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/84496b26-43e1-4db7-84c7-f926dbcf9ef1" /> -->
<img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/2d51b7cd-8033-4090-9cb6-c728f2c24118" />


## Figma for work-in-progress screens 
<img width="410" height="770" alt="Screenshot 2026-04-07 at 3 05 45 PM" src="https://github.com/user-attachments/assets/0dac987f-1a4a-48da-a89e-dc8259a49d78" />
<img width="390" height="753" alt="Screenshot 2026-04-07 at 3 05 51 PM" src="https://github.com/user-attachments/assets/6e9e9a61-2317-4a89-be99-a878a9965a25" />
<img width="349" height="755" alt="Screenshot 2026-04-07 at 4 29 58 PM" src="https://github.com/user-attachments/assets/1209070b-917d-4c56-9e0b-cab075097911" />

## Notes

This version has completed the core journalling experience and has made significant progress on implenmenting AI insights and the generation of a timeline. 

## View Models (full code removed to shorten ReadMe)

Act as a layer between the data and the UI. 
```
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
    val folderId: Long? = null,
    val folderColor: Int? = null
)

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val folders: StateFlow<List<FolderSummary>> = combine(
        repository.allFolders,
        repository.allEntries,
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
        
        // Virtual General folder (id = -1)
        val generalCount = entries.count { it.folderId == null }
        val generalFolder = FolderSummary(
            id = -1,
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val entries: StateFlow<List<TimelineMoment>> = combine(
        repository.allEntriesWithFolder,
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addEntry(title: String, body: String = "", folderId: Long? = null) {...}

    fun updateEntry(id: Long, title: String, body: String, folderId: Long?) {...}

    fun deleteEntry(id: Long) {...}

    fun addFolder(name: String, description: String, color: Int) {...}

    fun updateFolder(id: Long, name: String, description: String, color: Int) {...}

    fun deleteFolder(id: Long) {...}

    fun getEntriesByFolder(folderId: Long): Flow<List<TimelineMoment>> {...}

    suspend fun getEntryById(entryId: Long): TimelineMoment? {...}

    private fun formatSubtitle(timestamp: Long, folderName: String?): String {...}

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JournalApplication)
                JournalViewModel(application.repository)
            }
        }
    }
}
```

## AI Reflection

We used AI to aid in development, providing template starter code, assistance with working with Room, organizing and modularizing the code, and coming up with theme advice. The AI generated code came with fluff and features that fell outside of our scope, so we rejected those suggestions. For example, it tried to suggest a journaling streak which we were not planning to implement. Otherwise, it was a valuable tool to suggest other tools and create boilerplate. We also utilize AI throughout our debugging process, especially when we were working on authentication due to the unfamilairitt of the whole process. We had issues with SHA-1 keys which we had to ask ChatGpt for clarification and ideas on how to fix our error. 
