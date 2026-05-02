package com.example.rewindjournal.data
//for now if we need to use coroutines which we most likely will have to
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    
    fun getAllFolders(userId: String): Flow<List<Folder>> = journalDao.getAllFolders(userId)
    
    fun getAllEntries(userId: String): Flow<List<JournalEntry>> = journalDao.getAllEntries(userId)
    
    fun getAllEntriesWithFolder(userId: String): Flow<List<EntryWithFolder>> = 
        journalDao.getAllEntriesWithFolder(userId)

    fun getEntriesByFolder(folderId: Long, userId: String): Flow<List<JournalEntry>> = 
        journalDao.getEntriesByFolder(folderId, userId)

    suspend fun insertFolder(folder: Folder) = journalDao.insertFolder(folder)
    
    suspend fun updateFolder(folder: Folder) = journalDao.updateFolder(folder)

    suspend fun deleteFolder(folder: Folder) = journalDao.deleteFolder(folder)

    suspend fun insertEntry(entry: JournalEntry) = journalDao.insertEntry(entry)

    suspend fun updateEntry(entry: JournalEntry) = journalDao.updateEntry(entry)

    suspend fun deleteEntry(entry: JournalEntry) = journalDao.deleteEntry(entry)
    
    suspend fun getFolderById(id: Long) = journalDao.getFolderById(id)
    
    suspend fun getEntryById(id: Long) = journalDao.getEntryById(id)
}
