package com.example.rewindjournal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.rewindjournal.ui.components.FolderCard
import com.example.rewindjournal.ui.components.FolderDialog
import com.example.rewindjournal.ui.components.SearchBar
import com.example.rewindjournal.ui.components.SectionHeader
import com.example.rewindjournal.ui.components.TimelineCard
import com.example.rewindjournal.ui.viewmodel.FolderSummary
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.viewmodel.TimelineMoment

@Composable
fun FoldersScreen(
    viewModel: JournalViewModel,
    onEntryClick: (TimelineMoment) -> Unit = {}
) {
    var selectedFolder by remember { mutableStateOf<FolderSummary?>(null) }
    val folders by viewModel.folders.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val scope = rememberCoroutineScope()
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<FolderSummary?>(null) }

    if (selectedFolder != null) {
        FolderDetailView(
            folder = selectedFolder!!,
            viewModel = viewModel,
            onBack = { selectedFolder = null },
            onEntryClick = onEntryClick
        )
    } else {
        if (showNewFolderDialog) {
            FolderDialog(
                onDismiss = { showNewFolderDialog = false },
                onConfirm = { name, desc, color ->
                    scope.launch {
                        viewModel.addFolder(name, desc, color)
                    }
                    showNewFolderDialog = false
                }
            )
        }

        if (folderToEdit != null) {
            FolderDialog(
                initialName = folderToEdit!!.name,
                initialDescription = folderToEdit!!.description,
                initialColor = Color(folderToEdit!!.color),
                isEditing = true,
                onDismiss = { folderToEdit = null },
                onConfirm = { name, desc, color ->
                    viewModel.updateFolder(folderToEdit!!.id, name, desc, color)
                    folderToEdit = null
                },
                onDelete = {
                    viewModel.deleteFolder(folderToEdit!!.id)
                    folderToEdit = null
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                SectionHeader(
                    title = "Folders",
                    subtitle = "Organize experiences into dedicated spaces.",
                    action = {
                        Button(
                            onClick = { showNewFolderDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.CreateNewFolder, null)
                            Spacer(Modifier.width(4.dp))
                            Text("New Folder")
                        }
                    }
                )
            }

            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) }
                )
            }

            if (searchQuery.isNotEmpty()) {
                if (folders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Matching Folders",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(folders) { folder ->
                        FolderCard(
                            folder = folder,
                            onClick = { selectedFolder = folder },
                            onEditClick = { folderToEdit = folder }
                        )
                    }
                }

                // if the search entry is NOT empty, then show possible entries related to the query
                // Otherwise act as a regular folder page!
                if (entries.isNotEmpty()) {
                    item {
                        Text(
                            text = "Matching Entries",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(entries) { entry ->
                        TimelineCard(
                            moment = entry,
                            onClick = { onEntryClick(entry) },
                            overrideColor = entry.folderColor
                        )
                    }
                }

                if (folders.isEmpty() && entries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No matches found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(folders) { folder ->
                    FolderCard(
                        folder = folder,
                        onClick = { selectedFolder = folder },
                        onEditClick = { folderToEdit = folder }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }
}

@Composable
fun FolderDetailView(
    folder: FolderSummary,
    viewModel: JournalViewModel,
    onBack: () -> Unit,
    onEntryClick: (TimelineMoment) -> Unit
) {
    val entries by viewModel.getEntriesByFolder(folder.id).collectAsState(initial = emptyList())
    var showQuickAdd by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    if (showQuickAdd) {
        NewEntryScreen(
            viewModel = viewModel,
            initialFolderId = folder.id,
            onSaveComplete = { showQuickAdd = false },
            onCancel = { showQuickAdd = false }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Button(
                        onClick = { showQuickAdd = true },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("New Entry")
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color(folder.color), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (folder.description.isNotBlank()) {
                        Text(
                            text = folder.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No entries in this folder yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(entries) { entry ->
                    TimelineCard(moment = entry, onClick = { onEntryClick(entry) })
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }
}
