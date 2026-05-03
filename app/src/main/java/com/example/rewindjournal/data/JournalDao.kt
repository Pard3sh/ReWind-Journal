package com.example.rewindjournal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class EntryWithFolder(
    @Embedded val entry: JournalEntry,
    @Relation(
        parentColumn = "folderId",
        entityColumn = "id"
    )
    val folder: Folder?
)

@Dao
interface JournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("SELECT * FROM folders WHERE userId = :currentUserId ORDER BY name ASC")
    fun getAllFolders(currentUserId: String): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: String): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    @Query("SELECT * FROM journal_entries WHERE userId = :currentUserId ORDER BY timestamp DESC")
    fun getAllEntries(currentUserId: String): Flow<List<JournalEntry>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE userId = :currentUserId ORDER BY timestamp DESC")
    fun getAllEntriesWithFolder(currentUserId: String): Flow<List<EntryWithFolder>>

    @Query("SELECT * FROM journal_entries WHERE folderId = :folderId AND userId = :currentUserId ORDER BY timestamp DESC")
    fun getEntriesByFolder(folderId: String, currentUserId: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: String): JournalEntry?

    @Query("SELECT * FROM sentiment_nodes WHERE folderId = :folderId ORDER BY orderIndex ASC, timestamp ASC")
    fun getSentimentNodesByFolder(folderId: String): Flow<List<SentimentNode>>

    @Query("SELECT * FROM detailed_nodes WHERE folderId = :folderId ORDER BY timestamp ASC")
    fun getDetailedNodesByFolder(folderId: String): Flow<List<DetailedNode>>

    @Query("SELECT * FROM sentiment_nodes WHERE entryId = :entryId")
    suspend fun getSentimentNodesForEntry(entryId: String): List<SentimentNode>

    @Query("SELECT * FROM detailed_nodes WHERE entryId = :entryId")
    suspend fun getDetailedNodesForEntry(entryId: String): List<DetailedNode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentimentNode(node: SentimentNode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailedNode(node: DetailedNode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentimentNodes(nodes: List<SentimentNode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailedNodes(nodes: List<DetailedNode>)

    @Query("DELETE FROM sentiment_nodes WHERE folderId = :folderId")
    suspend fun deleteSentimentNodesByFolder(folderId: String)

    @Query("DELETE FROM detailed_nodes WHERE folderId = :folderId")
    suspend fun deleteDetailedNodesByFolder(folderId: String)

    @Query("DELETE FROM sentiment_nodes")
    suspend fun clearSentimentNodes()

    @Query("DELETE FROM detailed_nodes")
    suspend fun clearDetailedNodes()

    // for cloud sync
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<JournalEntry>)

    @Query("DELETE FROM journal_entries")
    suspend fun clearEntries()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<Folder>)

    @Query("DELETE FROM folders")
    suspend fun clearFolders()
}
