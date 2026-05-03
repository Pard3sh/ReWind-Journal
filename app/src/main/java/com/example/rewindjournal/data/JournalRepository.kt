package com.example.rewindjournal.data
//for now if we need to use coroutines which we most likely will have to

import kotlinx.coroutines.flow.Flow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JournalRepository(private val journalDao: JournalDao) {

    private val db = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    // read data

    fun getAllFolders(userId: String): Flow<List<Folder>> =
        journalDao.getAllFolders(userId)

    fun getAllEntries(userId: String): Flow<List<JournalEntry>> =
        journalDao.getAllEntries(userId)

    fun getAllEntriesWithFolder(userId: String): Flow<List<EntryWithFolder>> =
        journalDao.getAllEntriesWithFolder(userId)

    fun getEntriesByFolder(folderId: String, userId: String): Flow<List<JournalEntry>> =
        journalDao.getEntriesByFolder(folderId, userId)

    fun getSentimentNodesByFolder(folderId: String): Flow<List<SentimentNode>> =
        journalDao.getSentimentNodesByFolder(folderId)

    fun getDetailedNodesByFolder(folderId: String): Flow<List<DetailedNode>> =
        journalDao.getDetailedNodesByFolder(folderId)

    // insert folder/entry

    suspend fun insertFolder(folder: Folder) {

        val uid = auth.currentUser?.uid ?: return

        val docRef = db.collection("users")
            .document(uid)
            .collection("folders")
            .document()

        val folderWithId = folder.copy(id = docRef.id)

        // save locally ONCE
        journalDao.insertFolder(folderWithId)

        // save to firestore
        docRef.set(folderWithId)

    }

    suspend fun insertEntry(entry: JournalEntry) {

        val uid = auth.currentUser?.uid ?: return

        val docRef = db.collection("users")
            .document(uid)
            .collection("entries")
            .document()

        val entryWithId = entry.copy(id = docRef.id)

        println("Saving entry: $entryWithId")
        //  save locally
        journalDao.insertEntry(entryWithId)

        // then save to cloud
        docRef.set(entryWithId)
    }

    suspend fun insertSentimentNode(node: SentimentNode) {
        journalDao.insertSentimentNode(node)
    }

    suspend fun insertDetailedNode(node: DetailedNode) {
        journalDao.insertDetailedNode(node)
    }

    suspend fun insertSentimentNodes(nodes: List<SentimentNode>) {
        journalDao.insertSentimentNodes(nodes)
    }

    suspend fun insertDetailedNodes(nodes: List<DetailedNode>) {
        journalDao.insertDetailedNodes(nodes)
    }

    suspend fun clearSentimentNodes() {
        journalDao.clearSentimentNodes()
    }

    suspend fun clearDetailedNodes() {
        journalDao.clearDetailedNodes()
    }

    // update folder/entry

    suspend fun updateFolder(folder: Folder) {

        journalDao.updateFolder(folder)

        val uid = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(uid)
            .collection("folders")
            .document(folder.id)
            .set(folder)

    }

    suspend fun updateEntry(entry: JournalEntry) {

        journalDao.updateEntry(entry)

        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("entries")
            .document(entry.id)
            .set(entry)
    }

    // delete folder/entry

    suspend fun deleteFolder(folder: Folder) {

        journalDao.deleteFolder(folder)

        val uid = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(uid)
            .collection("folders")
            .document(folder.id)
            .delete()

    }

    suspend fun deleteEntry(entry: JournalEntry) {

        journalDao.deleteEntry(entry)

        val uid = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(uid)
            .collection("entries")
            .document(entry.id)
            .delete()

    }

    // get by id

    suspend fun getFolderById(id: String) =
        journalDao.getFolderById(id)

    suspend fun getEntryById(id: String) =
        journalDao.getEntryById(id)

    // sync

    fun startSync() {

        val uid = auth.currentUser?.uid ?: return

        // sync entries
        db.collection("users")
            .document(uid)
            .collection("entries")
            .addSnapshotListener { snapshot, _ ->

            val entries = snapshot?.toObjects(JournalEntry::class.java) ?: emptyList()

            CoroutineScope(Dispatchers.IO).launch {
                //journalDao.clearEntries()
                journalDao.insertEntries(entries)
            }
        }

        // sync folders
        db.collection("users")
            .document(uid)
            .collection("folders")
            .addSnapshotListener { snapshot, _ ->

                val folders = snapshot?.toObjects(Folder::class.java) ?: emptyList()

                CoroutineScope(Dispatchers.IO).launch {
                    //journalDao.clearFolders()
                    journalDao.insertFolders(folders)
                }
            }
    }
}
