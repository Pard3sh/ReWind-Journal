# ReWind Journal (CS501 Project)

ReWind Journal is a mobile journaling app built with Jetpack Compose (Material 3) for CS501. It focuses on making it easy to quickly capture daily reflections, organize them into folders, and later review recent moments.

## Current Features

- Home screen with a daily affirmation!
- Basic journal entry composer
- Sample folders 
- Recent moments section 
- Bottom navigation 


## How to Run

1. Open the project in Android Studio
2. Sync Gradle and build the project.
3. Run the app on an Android emulator or physical device (API 24+).

## Screenshots

1. Home Screen
<img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/b31ccef4-1615-4701-9821-81dba63ce5e4" />

2. Folders Screen
<img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/4843ed48-3807-4fdf-adca-1524b1179092" />

3. New Entry Screen
<img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/3b23a48f-7b8f-4c82-ab3a-e1fff064d36f" />

4. Timeline Screen
<img width="497" height="905" alt="image" src="https://github.com/user-attachments/assets/84496b26-43e1-4db7-84c7-f926dbcf9ef1" />

## Figma for work-in-progress screens
<img width="410" height="770" alt="Screenshot 2026-04-07 at 3 05 45 PM" src="https://github.com/user-attachments/assets/0dac987f-1a4a-48da-a89e-dc8259a49d78" />
<img width="390" height="753" alt="Screenshot 2026-04-07 at 3 05 51 PM" src="https://github.com/user-attachments/assets/6e9e9a61-2317-4a89-be99-a878a9965a25" />
<img width="349" height="755" alt="Screenshot 2026-04-07 at 4 29 58 PM" src="https://github.com/user-attachments/assets/1209070b-917d-4c56-9e0b-cab075097911" />

## Notes

This version intentionally focuses on the core journaling experience. Planned next steps include actual data persistence and AI-powered insights.

## View Models

Act as a layer between the data and the UI. 
```
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
```

## AI Reflection

We used AI to aid in development, providing template starter code, assistance with working with Room, organizing and modularizing the code, and coming up with theme advice. The AI generated code came with fluff and features that fell outside of our scope, so we rejected those suggestions. For example, it tried to suggest a journaling streak which we were not planning to implement. Otherwise, it was a valuable tool to suggest other tools and create boilerplate.
