package com.example.rewindjournal.data
//for now if we need to use coroutines which we most likely will have to
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    val allFolders: Flow<List<Folder>> = journalDao.getAllFolders()
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()
    val allEntriesWithFolder: Flow<List<EntryWithFolder>> = journalDao.getAllEntriesWithFolder()

    fun getEntriesByFolder(folderId: Long): Flow<List<JournalEntry>> = 
        journalDao.getEntriesByFolder(folderId)

    suspend fun insertFolder(folder: Folder) = journalDao.insertFolder(folder)
    
    suspend fun updateFolder(folder: Folder) = journalDao.updateFolder(folder)

    suspend fun deleteFolder(folder: Folder) = journalDao.deleteFolder(folder)

    suspend fun insertEntry(entry: JournalEntry) = journalDao.insertEntry(entry)

    suspend fun updateEntry(entry: JournalEntry) = journalDao.updateEntry(entry)

    suspend fun deleteEntry(entry: JournalEntry) = journalDao.deleteEntry(entry)
    
    suspend fun getFolderById(id: Long) = journalDao.getFolderById(id)
    
    suspend fun getEntryById(id: Long) = journalDao.getEntryById(id)
}
