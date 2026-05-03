package com.example.rewindjournal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

// table for folders per table 
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val color: Int = 0xFF6200EE.toInt(), // Default color (Purple)
    val timestamp: Long = System.currentTimeMillis()
)

// table for journal entries per user
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

// what each journal entry will store
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

// table to store sentiment nodes
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
    indices = [Index(value = ["folderId"]), Index(value = ["entryId"])]
)

// what each sentiment node will store
data class SentimentNode(
    @PrimaryKey val id: String = "",
    val folderId: String,
    val entryId: String,
    val entryTitle: String = "",
    val generatedTitle: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val savedLocation: String = "",
    val sentimentScore: Float = 0f,
    val sentimentMagnitude: Float = 0f,
    val emotionLabel: String = "",
    val extractedLocations: String = "",
    val extractedEvents: String = "",
    val orderIndex: Int = 0
)

// table to store detailed nodes for the detailed timeline
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
    indices = [Index(value = ["folderId"]), Index(value = ["entryId"])]
)

// what each detailed node will store
data class DetailedNode(
    @PrimaryKey val id: String = "",
    val folderId: String,
    val entryId: String,
    val entryTitle: String = "",
    val generatedTitle: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val savedLocation: String = "",
    val emotionLabel: String = "",
    val sentimentLabel: String = "",
    val sentimentScore: Float = 0f,
    val sentimentMagnitude: Float = 0f,
    val extractedLocations: String = "",
    val extractedEvents: String = "",
    val entityRecords: String = ""
)
