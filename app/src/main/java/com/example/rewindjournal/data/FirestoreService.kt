package com.example.rewindjournal.data

import com.google.firebase.firestore.FirebaseFirestore

class FirestoreService {

    private val db = FirebaseFirestore.getInstance()

    fun getEntriesRef(userId: String) =
        db.collection("users")
            .document(userId)
            .collection("entries")

    fun getFoldersRef(userId: String) =
        db.collection("users")
            .document(userId)
            .collection("folders")
}