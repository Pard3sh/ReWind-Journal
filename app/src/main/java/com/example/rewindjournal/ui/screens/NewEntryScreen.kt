package com.example.rewindjournal.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.components.EntryComposerCard
import com.example.rewindjournal.ui.viewmodel.TimelineMoment

@Composable
fun NewEntryScreen(
    viewModel: JournalViewModel,
    editingEntry: TimelineMoment? = null,
    initialFolderId: String? = null,
    onSaveComplete: () -> Unit,
    onCancel: () -> Unit = {}
) {
    var entryTitle by rememberSaveable { mutableStateOf(editingEntry?.title ?: "") }
    var entryBody by rememberSaveable { mutableStateOf(editingEntry?.body ?: "") }
    var selectedFolderId by rememberSaveable { mutableStateOf<String?>(editingEntry?.folderId ?: initialFolderId) }

    val folders by viewModel.folders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        EntryComposerCard(
            title = entryTitle,
            body = entryBody,
            onTitleChange = { entryTitle = it },
            onBodyChange = { entryBody = it },
            folders = folders,
            selectedFolderId = selectedFolderId,
            onFolderSelected = { selectedFolderId = it },
            isEditing = editingEntry != null,
            onCancelEdit = onCancel,
            onDeleteClick = if (editingEntry != null) {

                {
                    viewModel.deleteEntry(editingEntry.id)
                    onSaveComplete()
                }
            } else null,
            onSaveClick = {
                if (entryTitle.isNotBlank() || entryBody.isNotBlank()) {
                    if (editingEntry != null) {
                        viewModel.updateEntry(editingEntry.id, entryTitle, entryBody, selectedFolderId)
                    } else {
                        viewModel.addEntry(entryTitle, entryBody, selectedFolderId)
                    }
                    entryTitle = ""
                    entryBody = ""
                    selectedFolderId = null
                    onSaveComplete()
                }
            }
        )
    }
}
