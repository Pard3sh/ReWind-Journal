package com.example.rewindjournal.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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