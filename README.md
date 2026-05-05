# ReWind Journal – Architecture and Team Report

## 1. Architecture Overview

ReWind's total functionality has two major components:

- An **Android app** (Jetpack Compose, Room, Firebase Auth/Firestore) that provides the journaling experience and displays AI-powered timelines.
- A **Python backend** that periodically analyzes journal entries stored in Firestore, generates timelines and sentiment analysis, and writes this data back into Firestore.

The key reason for the split is so that **heavy ML and NLP work is offloaded to the backend**, while the app remains less heady.

---

## 2. Android Architecture

### MVVM

ReWind follows an MVVM-style architecture on the Android side.

**Model**

The Model layer includes the Room database entities and the repository abstraction.

**ViewModel**

The ViewModel layer is defined by `JournalViewModel.kt`. This  holds the state of the UI and coordinates between the repository and Compose screens.

In ReWind, the `JournalViewModel` is responsible for:

- Holding the current search query
- Applying search filtering to folders and entries
- Loading folder and entry data from the repository
- Exposing UI data through `StateFlow`
- Handling user actions such as CRUD on entries and folders
- Preparing data before it reaches the UI with helper functions

**View**

The View is the Jetpack Compose UI. It describes how the application is composed and how the state is rendered.

The UI is split into screens.

Reusable UI elements are separated into shared components, such as the search bar and other common pieces in `Components.kt`.

**Unidirectional Data Flow**

<img width="658" height="628" alt="image" src="https://github.com/user-attachments/assets/c77c7889-551a-431b-b0db-ed11b6e24e05" />


### 2.1 Local–Cloud Data 

All “core” journaling data is persisted locally via **Room**, and then synced with **Firestore**, with **timeline data as the only exception**:

- **Users**, using their Google/FireAuth accounts, must be logged in so the application ensures that all the following data is tied to a user
- **Folders** and **JournalEntries**:
  - Always stored in Room first.
  - Synced to the cloud so that the backend job can analyze them.
- **Timeline data** (sentiment/detailed nodes, folder summary, emotion counts):
  - Generated on the backend, written into Firestore.
  - Then synced into Room and exposed to the UI.
  - Timeline nodes are eventually stored locally as well, but the phone never computes them.

This architecture lets the app:

- Allows for data persistence across devices with the same account and prevents data loss from losing a device
- Avoid doing heavy ML/NLP on device.
- Allow the backend to be improved independently (better models, better summaries) without changing the app’s schema, unless absolutely necessary


### 2.2 Room Entities and Schema

The final Android schema consists of four Room entities:

#### Folder

```kotlin
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val color: Int = 0xFF6200EE.toInt(),
    val timestamp: Long = System.currentTimeMillis(),

    // summary / aggregate fields maintained by backend job
    val entryCount: Int = 0,
    val averageSentiment: Float = 0f,
    val sentimentTrend: String = "",
    val topLocations: String = "[]",
    val topEvents: String = "[]",
    val summaryText: String = "",
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null,

    // sentiment distribution for summary bar
    val veryPositiveCount: Int = 0,
    val positiveCount: Int = 0,
    val neutralCount: Int = 0,
    val negativeCount: Int = 0,
    val veryNegativeCount: Int = 0
)
```

- `Folder` holds the **timeline summary**:
  - The backend job writes `entryCount`, `averageSentiment`, `sentimentTrend`, distribution counts, and `summaryText` back into the folder document in Firestore.
  - The app syncs these summary fields into the local `Folder` row, so they will be null into its entries are analyzed.
  - This data is used for the **summary bar** and “insights” content at the top of the Timeline screen.

#### JournalEntry

```kotlin
@Entity(
    tableName = "journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class JournalEntry(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val folderId: String? = null,
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null
)
```

- Captures journal entry text and related metadata
- `latitude`/`longitude` are stored and the backend converts them into human-readable locations (e.g., “Boston, MA”).
- Foreign key to `Folder` uses `SET_NULL` so entries survive folder deletion gracefully. A future iteration could make this feature the user's choice.

#### SentimentNode

```kotlin
@Entity(
    tableName = "sentiment_nodes",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = JournalEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["entryId"]),
        Index(value = ["folderId", "orderIndex"]),
        Index(value = ["folderId", "timestamp"])
    ]
)
data class SentimentNode(
    @PrimaryKey val id: String = "",
    val folderId: String = "",
    val entryId: String = "",
    val entryTitle: String = "",
    val generatedTitle: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val savedLocation: String = "",
    val sentimentScore: Float = 0f,
    val sentimentMagnitude: Float = 0f,
    val emotionLabel: String = "",
    val extractedLocations: String = "[]",
    val extractedEvents: String = "[]",
    val orderIndex: Int = 0
)
```

- Represents a node in the **timeline** used for the visual timeline:
  - `generatedTitle` is the backend’s AI-generated title (from GCNL + OpenAI).
  - `orderIndex` and `timestamp` are both indexed so the app can render nodes chronologically.
  - `extractedLocations`/`extractedEvents` are stored as JSON strings to match Firestore.

#### DetailedNode

```kotlin
@Entity(
    tableName = "detailed_nodes",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = JournalEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["entryId"]),
        Index(value = ["folderId", "timestamp"])
    ]
)
data class DetailedNode(
    @PrimaryKey val id: String = "",
    val folderId: String = "",
    val entryId: String = "",
    val entryTitle: String = "",
    val generatedTitle: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val savedLocation: String = "",
    val emotionLabel: String = "",
    val sentimentLabel: String = "",
    val sentimentScore: Float = 0f,
    val sentimentMagnitude: Float = 0f,
    val extractedLocations: String = "[]",
    val extractedEvents: String = "[]",
    val entityRecords: String = "[]"
)
```

- Mirrors the backend’s **“detailed timeline”** documents.
- Contains full metadata used for:
  - Insights card and mood graph.
- Again uses JSON strings for complex lists (`entityRecords`) to stay aligned with Firestore.
- The two schemas may seem a bit redundant, and they are, because the original vision was two seperate timelines. 

### 2.3 ViewModel and UI Layer

The main `JournalViewModel` (summarized in the Android README) wraps the repository and exposes:

- `folders: StateFlow<List<FolderSummary>>`
- `entries: StateFlow<List<TimelineMoment>>`
- Filtering/search logic over both, key for the folder search bar.
- CRUD operations for entries and folders.
- Helpers like `formatSubtitle(timestamp, folderName)` for human-readable dates.

On top of this state, the UI is built entirely in **Jetpack Compose (Material 3)**:

- **Home screen**: affirmation, recent entries display, navigation.
- **Folders screen**: list of folders (including a default, unanalyzed “General” folder), search, and counts.
- **Entry screen**: create/edit/delete entries.
- **Timeline screen**:
  - Uses `SentimentNode` and `DetailedNode` data from Room to render:
    - A graph of mood over time (Angel extended this to support all six emotion categories with related emojis).
    - A linear timeline of generated titles with emojis and sentiment labels.
    - Summary and insights (trend, distribution, top locations/events).

---

## 3. Backend Architecture

### 3.1 Overview

The backend is a Python package (`rewind-sentiment-analysis`) (https://github.com/Pard3sh/ReWind-Timeline-Generator) that:

- Connects to Firestore using service account credentials for our Google Cloud project.
- Fetches unanalyzed entries for all users and folders in firebase
- Analyzes each entry using:
  - **Google Cloud Natural Language API** for document sentiment and entity extraction.
  - **j-hartmann/emotion-english-distilroberta-base** transformer model for emotion classification.
  - An **OpenAI-based helper** that improves timeline title generation when GCNL alone does not produce good events. Since this feature costs money,
  we have restricted it to only run if there is no usable data produced by the other models. 
- Builds two per-folder timelines:
  - A  **sentiment timeline** (nodes).
  - A  **detailed timeline**.
    - Their schemas and data stored are reflected in Room schema section
- Writes:
  - `sentiment_nodes` collection documents.
  - `detailed_nodes` collection documents.
  - A **Folder summary object** back into the folder document on firebase.
- Scope:
  - With a larger scope, we could extract more detailed information and generate more statistical information

### 3.2 Project Structure

From the backend README:

- `rewind/`
  - `api.py`: Google Cloud Natural Language client, calls to sentiment/entity endpoints, integration with emotion/title helpers.
  - `config.py`: handles environment variables.
  - `database.py`: Firestore operations (fetch users/folders/entries, write new nodes and summaries, location conversion).
  - `models.py`: data models (`TimelineNode`, `FolderSummary`, etc.) meant to mirror Firebase and Room schemas to avoid friction in data pipeline.
  - `sentiment.py`: sentiment categorization, emojis, emotion inference (transformer-based), timeline title generation.
  - `serializers.py`: converts analyzed entries into Firestore/Room-compatible node documents.
  - `timeline.py`: timeline container classes (`SentimentFolderTimeline`, `DetailedFolderTimeline`) that compute trends, distributions, and summary text.
- `main.py`: local/sample script showing how timelines are generated for sample entries.
- `job.py`: **cloud/batch job entry point**, used by GitHub Actions to process all users and folders.

### 3.3 Data Flow

1. **Entries are written** by the Android app to Firestore (with text, timestamp, lat/long, folderId, userId).
2. **Backend job**:
   - Fetches all users.
   - For each folder, fetches **unanalyzed entries** (entries that don’t yet have sentiment nodes).
   - For each entry:
     - Calls GCNL `analyzeSentiment` and `analyzeEntitySentiment`.
     - Uses the transformer model to assign an `emotionLabel`.
     - Uses a custom function `generate_time_line_title(...)` that incorporates:
       - Original title.
       - Emotion label.
       - Extracted events and locations.
       - And an optional OpenAI helper when events/locations are sparse.
   - Feeds analyzed entries into `SentimentFolderTimeline` and `DetailedFolderTimeline` to compute:
     - Time-ordered nodes.
     - Folder-level summary:
       - `average_sentiment`
       - `sentiment_trend` (e.g., “Declining”)
       - sentiment distribution counts
       - `top_locations`, `top_events`
       - narrative `summary_text`.
3. **Serialization & Write**:
   - `serializers.py` builds Firestore documents that match the Android Room schema:
     - `sentiment_nodes` collection.
     - `detailed_nodes` collection.
   - `database.py` writes those into Firestore and updates the folder summary fields.
4. **App syncs**:
   - Android listens to/syncs the updated collections and folder fields into its Room DB.
   - Timeline screen now sees new nodes and summary.

### 3.4 Job Execution and Deployment

The backend can be run three ways:

- Locally, via `python main.py` for experimentation.
- Locally, in Docker via `docker run --env-file .env …`.
- **Production path:** a **GitHub Actions workflow** that:
  - Builds the Docker image.
  - Runs `python job.py` against the real Firestore database on a schedule (about every 12 hours) or on demand.

During development, there was an attempt to run this on **Cloud Run Jobs + Cloud Scheduler**, but:

- The project quickly ran into **Cloud Run job quotas during testing**.
- For the purposes of this class and grading, the team **descoped Cloud Run** and settled on GitHub Actions as the batch runner.
  - Realized how useful this Github tool is in this process as well

---

## 4. APIs and Sensor Usage

### 4.1 Android (Device APIs & Services)

- **Location Services**:
  - The app requests location permission.
  - If granted, it stores `latitude`/`longitude` with each entry.
- **Firebase**:
  - Firebase Auth with Google sign-in.
  - Firestore as the cloud database for entries, folders, and timeline nodes.
- **Affirmation API**:
  - HTTP REST call (via Retrofit) to fetch daily affirmations, displayed on the home screen.

### 4.2 Python Package Backend (External APIs/Models)

- **Google Cloud Natural Language API**:
  - `analyzeSentiment` for document-level score/magnitude.
  - `analyzeEntitySentiment` for entities with sentiment (locations, events, etc.).
- **Hugging Face Transformers**:
  - `j-hartmann/emotion-english-distilroberta-base` for emotion classification (Joy, Anger, Sadness, Fear, Disgust, Surprise).
- **OpenAI (title improvement)**:
  - Used in the latest iteration to generate activity labels and improved `generatedTitle` strings when GCNL alone is too generic/awkward.
- **Geopy / Nominatim**:
  - Converts lat/long into human-readable locations like “Boston, MA” or “Cambridge, MA”.

---

## 5. Team Responsibilities and Contributions

### 5.1 Pardesh Dhakal

**Android app**

- Created the **base journaling application**:
  - Initial repo and project setup.
  - Core screens and navigation
  - Core CRUD for entries and folders.
  - Quality-of-life improvements (search, editing/deletion, better UX flows).
- Implemented and later refactored the **ViewModel architecture**:
  - Reorganized project structure for separation of data and UI.
- Extended the **folder schema** for AI/ML insights and added the timeline schemas to Room
- Implemented **location capture** for entries:
  - Added location permissions to the app.
- Built the base **timeline UI**:
  - Initial screen displaying all available back-end generated data.
- First draft of the report 

**Backend**

- Designed and implemented the **backend repository**
- Implemented **Firestore integration** for batch job
- Built the **batch processing job** with logging:
- **Dockerization and CI/CD**:
  - Crafted the Dockerfile to run the backend in a container. This was initially done so that it could be run as a Cloud Run job.
  - Configured the GitHub Actions workflow.
- **OpenAI-based title generation**:
  - Added an OpenAI helper to improve generated timeline titles.
- Wrote up **documentation on the package and Cloud-shell/GitHub Actions**:



### 5.2 Cheyenne Mowatt

**Android + UX**

- Designed the **Figma prototypes** for every screens:
  - Home screen, folders screen, entry screen, timeline screen, auth flow, and splash screens.
- Implemented **Firebase Auth**:
  - Google OAuth flows.
  - Login screen, logout behavior, and connecting a journal to a specific user account.
  - Extensively handled the debugging for issues with authentication
  - Ensured that users were logged in before having access to the application
- Set up the **Cloud Firestore database and Google Cloud Project**:
  - Designed and configured the Firestore collections for users, folders, and entries.
  - Implemented sync logic so that local Room data can be backed up to the cloud and later restored.
- Implemented the **affirmation API**:
  - Integrated a REST API (via Retrofit) to display daily affirmations on the home screen.
  - Used the Retrofit package for handling network requests
- Created **UX elements**:
  - Loading splash screen.
  - Affirmation/home screen content.
  - General polish around the user experience.


### 5.3 Angel Manson

**Android data & UI integration**

- Worked heavily on the **database and migration side**:
  - Fixed foreign key errors.
  - Adjusted migration handling to avoid destructive migrations.
  - Pushed changes to entities (adding `userId`, updating schemas to align with auth and backend).
- Implemented **deserialization and wiring** of timeline data:
  - Integrated sentiment/detailed node data into the Timeline screen.
  - Connected Room entities to Compose UI components.
- Extended the **Insights UI** and refined the **Timeline Screen**:
  - Implemented the mood trend logic using all six emotion categories.
  - Added a back button and UX improvements to the Timeline screen.
- Matched UI to **Figma designs**:
  - Updated screens so that the Compose layouts matched the Figma design.
- Added comments and improved README content for the Android repo.

---

## 6. How Graders/Testers Should Use the GitHub Action (Important for accessing full functionality)


### 6.1 Core Necessities

- Android Studio installed.
- Access to the Android project repo and the backend repo:
  - Android: ReWind Journal (CS501 project).
  - Backend: ReWind Sentiment Analysis / ReWind-Timeline-Generator.
- A Google account for signing into the app.

### 6.2 Android App

1. Open the Android project in Android Studio.
2. Build and run the app on an emulator or physical device (API 24+, Google Play-enabled).
3. Sign in with Google (Firebase Auth).
4. On the Folders screen, **create a folder** (any folder that is not the default “General” folder).
5. Create **several detailed entries** in that folder:
   - Rich text (not just “test”).
   - Slightly different timestamps (just creating them sequentially is fine).
   - Ideally with location permission granted so coordinates are captured. Experiement with setting the emulator's location to various different locations when writing entries.

The app will sync entries to Firestore as long as the device has an internet connection.

### 6.3 Running the Timeline Generator GitHub Action

1. Go to the **backend repo** on GitHub (ReWind Sentiment Analysis / listed above)
2. Navigate to the **Actions** tab.
3. Find the workflow for the **timeline generator**.
4. Click **“Run workflow”** (or re-run the latest run):
   - This will build/run the container and run the batch job analyzing new entries in firestore.
5. Wait for the workflow to succeed (logs show users, folders, entries processed) (if there are no new entries, you’ll see the “0 folders / 0 entries” output like in the README example).

**Important:**

- For graders, there is **no need to touch Cloud Run or gcloud**.
  - Cloud Run Jobs were attempted but ultimately **descoped** after quota issues.
  - GitHub Actions is the canonical and supported path for this demo.
- The job is configured to run **automatically on a schedule** (about every 24 hours), but:
  - For grading, a **manual re-run** is strongly recommended so that new entries are analyzed immediately.

If anyone wants to inspect how data is stored in the cloud (Firestore collections, documents), they can reach out to the developers for Firebase console access.

### 6.4 Viewing the Timelines

After the GitHub Action finishes:

1. Return to the Android app.
2. Open the custom folder you created.
3. Go to the **Timeline screen**:
   - You should see:
     - A summary section with average sentiment, trend, counts, and top locations/events.
     - A sentiment graph (mood trend) over time.
     - A list of nodes labeled with **generated titles** and emojis, ordered chronologically.

This verifies the full pipeline: app -> Firestore -> backend job -> Firestore timeline data -> app.

---

## 7. AI Reflection

### 7.1 How AI Was Used

**Android app**

- AI was used to:
  - Provide **template starter code** and boilerplate for Jetpack Compose and Room.
  - Suggest patterns for **ViewModel organization** and modularizing the code.
  - Offer **theme and UI advice**, especially for Material 3.
  - Assist with debugging, particularly during **Firebase Auth** integration:
    - There were issues around SHA‑1 keys and configuration, as the team was not familiar with it, AI was consulted for clarifications and fixes.

The team deliberately rejected AI suggestions that strayed from scope, such as:

- Journaling streaks, gamification, and other non-essential features. Also several unecessary statistical displays such as # of new entries, or # of 'active' folders
- Noisy UI flows or extra screens that would clutter the experience and distract from the core goal (journal -> timeline).

**Backend**

AI played a much stronger role here:

- Helped design the **Python package structure**:
  - Splitting the project into module.
  - Suggested approaches for packaging and imports, which were especially helpful as batch ML jobs in Python were new to the team.
- Assisted with **integration of Hugging Face Transformers**:
  - Suggested `j-hartmann/emotion-english-distilroberta-base`.
  - Setting up lazy-loading (`get_emotion_classifier`) and handling fallback logic.
- Provided boilerplate for **error-handling and logging patterns**:
  - Logging around Firebase initialization and project IDs.
  - Logging for user/folder counts, and handling the “no new entries” case gracefully.
- Helped draft and refine **documentation**, especially docstrings and README sections:
  - The AI-generated skeleton for project structure is explicitly noted in the README.
  - Human-written notes were layered on top to make things clearer and context-specific.
- Assisted in developing the **OpenAI-based timeline title enhancements**:
  - AI code examples helped wire in an OpenAI call for activity inference.
  - The actual behavior (when to call OpenAI, how to combine with GCNL output, and how to keep titles short and meaningful) was then tuned by hand.
- AI generated entry samples for the main.py script used to test the package on sample data

### 7.2 Where AI Influenced Architecture 

AI **did not** come up with the idea of moving all ML into a backend batch job but from the team’s constraints and goals:
- Running GCNL, Hugging Face, and OpenAI on-device would be too heavy and complex.

However, once that choice was made, AI was important in **figuring out how*, with some issues as well:

- It tunnel-visioned on **cloud-heavy solutions** (Cloud Functions, Cloud Run, etc.), which were initially the goal.
- In practice, cost/quotas and time pressure pushed us to **descope Cloud Run** and choose GitHub Actions as a simpler runner.
- It did assist in creating the deploy.yml file. It suggested moving .env files into the secrets section, and explained that json values had to be stored as one lines in the secrets.

On the UX side, AI often suggested:

- Extra screens, complex flows, and noisy layouts that did not align with our team's user-experience focus.


### 7.3 What AI Accelerated

Across the ReWind project, AI accelerated:

- **Boilerplate code**:
  - Room entities, DAOs, and ViewModel scaffolding.
  - Python package/module boilerplate.
- **Integration patterns**:
  - Hugging Face pipeline setup.
  - Firestore and Google Cloud library usage.
- **Debugging and troubleshooting**:
  - Firebase Auth SHA‑1 issues.
  - Python packaging/import issues.
  - Firestore query issues.
- **Documentation**:
  - Sections of READMEs and comments
  - Example outputs


### 7.4 Why Suggestions Were Rejected

Some AI suggestions were explicitly rejected because they:

- **Exceeded scope/time**:
  - Features like journaling streaks, leaderboard-style views, or complex ML enhancements (e.g., selecting most positive and most negative sentences as part of the back-ends sentiment analysis).
- **Complicated UX**:
  - Overly busy screens that would have made the app feel cluttered.
  - Additional flows 
- **Increased operational complexity**:
  - Full Cloud deployment with auto-scaling and services, which would be overkill for a class project and hard to keep under quota limits.
