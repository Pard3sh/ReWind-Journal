package com.example.rewindjournal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

//@Database(entities = [Folder::class, JournalEntry::class], version = 1, exportSchema = false)
@Database(
    entities = [Folder::class, JournalEntry::class, SentimentNode::class, DetailedNode::class],
    version = 3,
    exportSchema = true
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    //not sure about this part below because Prof didn't discuss what to add to Database file
    //need to work on getting migration logic in too
    companion object {
        @Volatile
        private var Instance: JournalDatabase? = null

        /*
         * Manual Migration from 1 -> 2.
         * Required because version 1 didn't export its schema.
         * For version 3 and beyond automigration should be used
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to 'folders' table
                db.execSQL("ALTER TABLE folders ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE folders ADD COLUMN color INTEGER NOT NULL DEFAULT -10354450")

                // Add new columns to 'journal_entries' table
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN longitude REAL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS sentiment_nodes (" +
                            "id TEXT NOT NULL PRIMARY KEY, " +
                            "folderId TEXT NOT NULL, " +
                            "entryId TEXT NOT NULL, " +
                            "entryTitle TEXT NOT NULL, " +
                            "generatedTitle TEXT NOT NULL, " +
                            "timestamp INTEGER NOT NULL, " +
                            "savedLocation TEXT NOT NULL, " +
                            "sentimentScore REAL NOT NULL, " +
                            "sentimentMagnitude REAL NOT NULL, " +
                            "emotionLabel TEXT NOT NULL, " +
                            "extractedLocations TEXT NOT NULL, " +
                            "extractedEvents TEXT NOT NULL, " +
                            "orderIndex INTEGER NOT NULL, " +
                            "FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE CASCADE, " +
                            "FOREIGN KEY(entryId) REFERENCES journal_entries(id) ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sentiment_nodes_folderId ON sentiment_nodes(folderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sentiment_nodes_entryId ON sentiment_nodes(entryId)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS detailed_nodes (" +
                            "id TEXT NOT NULL PRIMARY KEY, " +
                            "folderId TEXT NOT NULL, " +
                            "entryId TEXT NOT NULL, " +
                            "entryTitle TEXT NOT NULL, " +
                            "generatedTitle TEXT NOT NULL, " +
                            "timestamp INTEGER NOT NULL, " +
                            "savedLocation TEXT NOT NULL, " +
                            "emotionLabel TEXT NOT NULL, " +
                            "sentimentLabel TEXT NOT NULL, " +
                            "sentimentScore REAL NOT NULL, " +
                            "sentimentMagnitude REAL NOT NULL, " +
                            "extractedLocations TEXT NOT NULL, " +
                            "extractedEvents TEXT NOT NULL, " +
                            "entityRecords TEXT NOT NULL, " +
                            "FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE CASCADE, " +
                            "FOREIGN KEY(entryId) REFERENCES journal_entries(id) ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_detailed_nodes_folderId ON detailed_nodes(folderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_detailed_nodes_entryId ON detailed_nodes(entryId)")
            }
        }

        fun getDatabase(context: Context): JournalDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, JournalDatabase::class.java, "journal_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = OFF;")
                        }
                    })
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
