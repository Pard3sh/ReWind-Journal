package com.example.rewindjournal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//@Database(entities = [Folder::class, JournalEntry::class], version = 1, exportSchema = false)
@Database(entities = [Folder::class, JournalEntry::class], version = 2)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    //not sure about this part below because Prof didn't discuss what to add to Database file
    //need to work on getting migration logic in too
    companion object {
        @Volatile
        private var Instance: JournalDatabase? = null

        fun getDatabase(context: Context): JournalDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, JournalDatabase::class.java, "journal_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}

// For our final version, we should be saving information using EncryptedRoom or using SQLCipher
// Need to get on Cloud Sync asap
// Ideally, I think we should have the insights being generated not locally but using the cloud data
// so the user does not have to wait locally for the computations!
