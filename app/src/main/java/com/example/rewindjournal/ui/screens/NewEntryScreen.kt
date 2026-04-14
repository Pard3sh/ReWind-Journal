package com.example.rewindjournal.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.components.EntryComposerCard

@Composable
fun NewEntryScreen(viewModel: JournalViewModel, onSaveComplete: () -> Unit) {
    var entryTitle by rememberSaveable { mutableStateOf("") }
    var entryBody by rememberSaveable { mutableStateOf("") }

    val wordCount = entryBody
        .trim()
        .split("\\s+".toRegex())
        .filter { it.isNotBlank() }
        .size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        EntryComposerCard(
            title = entryTitle,
            body = entryBody,
            wordCount = wordCount,
            onTitleChange = { entryTitle = it },
            onBodyChange = { entryBody = it },
            onSaveClick = {
                if (entryTitle.isNotBlank() || entryBody.isNotBlank()) {
                    viewModel.addEntry(entryTitle, entryBody)
                    entryTitle = ""
                    entryBody = ""
                    onSaveComplete()
                }
            }
        )
    }
}
