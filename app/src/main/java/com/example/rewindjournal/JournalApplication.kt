package com.example.rewindjournal

import android.app.Application
import com.example.rewindjournal.data.JournalDatabase
import com.example.rewindjournal.data.JournalRepository

class JournalApplication : Application() {
    val database by lazy { JournalDatabase.getDatabase(this) }
    val repository by lazy { JournalRepository(database.journalDao()) }
}
