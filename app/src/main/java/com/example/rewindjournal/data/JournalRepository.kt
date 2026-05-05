package com.example.rewindjournal.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class JournalRepository(private val journalDao: JournalDao) {

    private val db = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()
    private var syncedUserId: String? = null

    private var entriesListener: ListenerRegistration? = null
    private var foldersListener: ListenerRegistration? = null
    private val sentimentNodeListeners = mutableMapOf<String, ListenerRegistration>()
    private val detailedNodeListeners = mutableMapOf<String, ListenerRegistration>()

    fun getAllFolders(userId: String): Flow<List<Folder>> =
        journalDao.getAllFolders(userId)

    fun getFolderFlow(folderId: String): Flow<Folder?> =
        journalDao.getFolderFlow(folderId)

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

    suspend fun insertFolder(folder: Folder): String {
        val uid = auth.currentUser?.uid ?: return ""
        val docRef = db.collection("users")
            .document(uid)
            .collection("folders")
            .document()

        val folderWithId = folder.copy(id = docRef.id, userId = uid)

        journalDao.insertFolder(folderWithId)
        docRef.set(folderWithId)
        return docRef.id
    }

    suspend fun insertEntry(entry: JournalEntry) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users")
            .document(uid)
            .collection("entries")
            .document()

        val entryWithId = entry.copy(id = docRef.id, userId = uid)

        journalDao.insertEntry(entryWithId)
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

    suspend fun updateFolder(folder: Folder) {
        val uid = auth.currentUser?.uid ?: return
        val folderToUpdate = folder.copy(userId = uid)

        journalDao.updateFolder(folderToUpdate)

        db.collection("users")
            .document(uid)
            .collection("folders")
            .document(folderToUpdate.id)
            .set(folderToUpdate)
    }

    suspend fun updateEntry(entry: JournalEntry) {
        val uid = auth.currentUser?.uid ?: return
        val entryToUpdate = entry.copy(userId = uid)

        journalDao.updateEntry(entryToUpdate)

        db.collection("users")
            .document(uid)
            .collection("entries")
            .document(entryToUpdate.id)
            .set(entryToUpdate)
    }

    suspend fun deleteFolder(folder: Folder) {
        val uid = auth.currentUser?.uid ?: return

        // Get entries that belong to this folder before deleting
        val entriesToMove = journalDao.getEntriesByFolderSync(folder.id)

        // Delete from local Room DB (FK with SET_NULL)
        journalDao.deleteFolder(folder)

        // Update Firestore entries (folderId = null)
        val batch = db.batch()
        entriesToMove.forEach { entry ->
            val entryRef = db.collection("users")
                .document(uid)
                .collection("entries")
                .document(entry.id)
            batch.update(entryRef, "folderId", null)
        }

        // 4. Delete the folder from Firestore
        val folderRef = db.collection("users")
            .document(uid)
            .collection("folders")
            .document(folder.id)
        batch.delete(folderRef)

        batch.commit()
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

    suspend fun getFolderById(id: String) =
        journalDao.getFolderById(id)

    suspend fun getEntryById(id: String) =
        journalDao.getEntryById(id)

    fun startSync() {
        val uid = auth.currentUser?.uid ?: return
        if (syncedUserId == uid) return

        stopSync()
        syncedUserId = uid

        entriesListener = db.collection("users")
            .document(uid)
            .collection("entries")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(JournalEntry::class.java)?.copy(
                            id = doc.id,
                            userId = uid
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } ?: emptyList()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        journalDao.clearEntries()
                        journalDao.insertEntries(entries)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

        foldersListener = db.collection("users")
            .document(uid)
            .collection("folders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                val folders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toFolder(uid)
                } ?: emptyList()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        journalDao.clearFolders()
                        journalDao.insertFolders(folders)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val activeFolderIds = folders.map { it.id }.toSet()

                val removedSentimentFolderIds = sentimentNodeListeners.keys - activeFolderIds
                removedSentimentFolderIds.forEach { folderId ->
                    sentimentNodeListeners.remove(folderId)?.remove()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            journalDao.deleteSentimentNodesByFolder(folderId)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val removedDetailedFolderIds = detailedNodeListeners.keys - activeFolderIds
                removedDetailedFolderIds.forEach { folderId ->
                    detailedNodeListeners.remove(folderId)?.remove()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            journalDao.deleteDetailedNodesByFolder(folderId)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                folders.forEach { folder ->
                    val folderId = folder.id

                    if (!sentimentNodeListeners.containsKey(folderId)) {
                        val listener = db.collection("users")
                            .document(uid)
                            .collection("folders")
                            .document(folderId)
                            .collection("sentiment_nodes")
                            .addSnapshotListener { nodeSnapshot, nodeError ->
                                if (nodeError != null) {
                                    nodeError.printStackTrace()
                                    return@addSnapshotListener
                                }

                                val nodes = nodeSnapshot?.documents?.mapNotNull { doc ->
                                    doc.toSentimentNode(folderId)
                                } ?: emptyList()

                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        journalDao.deleteSentimentNodesByFolder(folderId)
                                        journalDao.insertSentimentNodes(nodes)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                        sentimentNodeListeners[folderId] = listener
                    }

                    if (!detailedNodeListeners.containsKey(folderId)) {
                        val listener = db.collection("users")
                            .document(uid)
                            .collection("folders")
                            .document(folderId)
                            .collection("detailed_nodes")
                            .addSnapshotListener { nodeSnapshot, nodeError ->
                                if (nodeError != null) {
                                    nodeError.printStackTrace()
                                    return@addSnapshotListener
                                }

                                val nodes = nodeSnapshot?.documents?.mapNotNull { doc ->
                                    doc.toDetailedNode(folderId)
                                } ?: emptyList()

                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        journalDao.deleteDetailedNodesByFolder(folderId)
                                        journalDao.insertDetailedNodes(nodes)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                        detailedNodeListeners[folderId] = listener
                    }
                }
            }
    }

    fun stopSync() {
        entriesListener?.remove()
        entriesListener = null

        foldersListener?.remove()
        foldersListener = null

        sentimentNodeListeners.values.forEach { it.remove() }
        sentimentNodeListeners.clear()

        detailedNodeListeners.values.forEach { it.remove() }
        detailedNodeListeners.clear()

        syncedUserId = null
    }
}

fun DocumentSnapshot.toFolder(userId: String): Folder? {
    return try {
        //try standard deserialization
        this.toObject(Folder::class.java)?.copy(
            id = this.id,
            userId = userId,
            startTimestamp = getSafeLong("startTimestamp"),
            endTimestamp = getSafeLong("endTimestamp")
        )
    } catch (e: Exception) {
        //manual mapping if automatic deserialization fails
        try {
            Folder(
                id = this.id,
                userId = userId,
                name = getString("name") ?: "",
                description = getString("description") ?: "",
                color = getLong("color")?.toInt() ?: 0xFF6200EE.toInt(),
                timestamp = getSafeLong("timestamp") ?: System.currentTimeMillis(),
                entryCount = getLong("entryCount")?.toInt() ?: 0,
                averageSentiment = getDouble("averageSentiment")?.toFloat() ?: 0f,
                sentimentTrend = getString("sentimentTrend") ?: "",
                topLocations = getString("topLocations") ?: "[]",
                topEvents = getString("topEvents") ?: "[]",
                summaryText = getString("summaryText") ?: "",
                startTimestamp = getSafeLong("startTimestamp"),
                endTimestamp = getSafeLong("endTimestamp"),
                veryPositiveCount = getLong("veryPositiveCount")?.toInt() ?: 0,
                positiveCount = getLong("positiveCount")?.toInt() ?: 0,
                neutralCount = getLong("neutralCount")?.toInt() ?: 0,
                negativeCount = getLong("negativeCount")?.toInt() ?: 0,
                veryNegativeCount = getLong("veryNegativeCount")?.toInt() ?: 0
            )
        } catch (e2: Exception) {
            e2.printStackTrace()
            null
        }
    }
}

fun DocumentSnapshot.getSafeLong(field: String): Long? {
    return try {
        when (val value = get(field)) {
            is Long -> value
            is String -> value.toLongOrNull()
            is Number -> value.toLong()
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

fun DocumentSnapshot.toSentimentNode(folderId: String): SentimentNode? {
    return try {
        this.toObject(SentimentNode::class.java)?.copy(
            id = this.id,
            folderId = folderId
        )
    } catch (e: Exception) {
        try {
            SentimentNode(
                id = this.id,
                folderId = folderId,
                entryId = getString("entryId") ?: getString("entry_id") ?: "",
                entryTitle = getString("entryTitle") ?: getString("entry_title") ?: "",
                generatedTitle = getString("generatedTitle") ?: getString("generated_title") ?: "",
                timestamp = getSafeLong("timestamp") ?: System.currentTimeMillis(),
                savedLocation = getString("savedLocation") ?: getString("saved_location") ?: "",
                sentimentScore = getDouble("sentimentScore")?.toFloat() ?: getDouble("sentiment_score")?.toFloat() ?: 0f,
                sentimentMagnitude = getDouble("sentimentMagnitude")?.toFloat() ?: getDouble("sentiment_magnitude")?.toFloat() ?: 0f,
                emotionLabel = getString("emotionLabel") ?: getString("emotion_label") ?: "",
                extractedLocations = getString("extractedLocations") ?: getString("extracted_locations") ?: "[]",
                extractedEvents = getString("extractedEvents") ?: getString("extracted_events") ?: "[]",
                orderIndex = getLong("orderIndex")?.toInt() ?: getLong("order_index")?.toInt() ?: 0
            )
        } catch (e2: Exception) {
            null
        }
    }
}

fun DocumentSnapshot.toDetailedNode(folderId: String): DetailedNode? {
    return try {
        this.toObject(DetailedNode::class.java)?.copy(
            id = this.id,
            folderId = folderId
        )
    } catch (e: Exception) {
        try {
            DetailedNode(
                id = this.id,
                folderId = folderId,
                entryId = getString("entryId") ?: getString("entry_id") ?: "",
                entryTitle = getString("entryTitle") ?: getString("entry_title") ?: "",
                generatedTitle = getString("generatedTitle") ?: getString("generated_title") ?: "",
                timestamp = getSafeLong("timestamp") ?: System.currentTimeMillis(),
                savedLocation = getString("savedLocation") ?: getString("saved_location") ?: "",
                emotionLabel = getString("emotionLabel") ?: getString("emotion_label") ?: "",
                sentimentLabel = getString("sentimentLabel") ?: getString("sentiment_label") ?: "",
                sentimentScore = getDouble("sentimentScore")?.toFloat() ?: getDouble("sentiment_score")?.toFloat() ?: 0f,
                sentimentMagnitude = getDouble("sentimentMagnitude")?.toFloat() ?: getDouble("sentiment_magnitude")?.toFloat() ?: 0f,
                extractedLocations = getString("extractedLocations") ?: getString("extracted_locations") ?: "[]",
                extractedEvents = getString("extractedEvents") ?: getString("extracted_events") ?: "[]",
                entityRecords = getString("entityRecords") ?: getString("entity_records") ?: "[]"
            )
        } catch (e2: Exception) {
            null
        }
    }
}
