package com.example.rewindjournal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String = "",
    val userId: String,
    val name: String,
    val description: String = "",
    val color: Int = 0xFF6200EE.toInt(), // Default color (Purple)
    val timestamp: Long = System.currentTimeMillis()
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
    val userId: String,
    val folderId: String? = null,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null
)
