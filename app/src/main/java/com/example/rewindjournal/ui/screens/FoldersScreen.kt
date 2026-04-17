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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.rewindjournal.ui.components.FolderCard
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
    var showNewFolderDialog by remember { mutableStateOf(false) }

    if (selectedFolder != null) {
        FolderDetailView(
            folder = selectedFolder!!,
            viewModel = viewModel,
            onBack = { selectedFolder = null },
            onEntryClick = onEntryClick
        )
    } else {
        if (showNewFolderDialog) {
            NewFolderDialog(
                onDismiss = { showNewFolderDialog = false },
                onConfirm = { name, desc, color ->
                    viewModel.addFolder(name, desc, color)
                    showNewFolderDialog = false
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
                    subtitle = "Organize longer experiences into dedicated spaces.",
                    action = {
                        TextButton(onClick = { showNewFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, null)
                            Spacer(Modifier.width(4.dp))
                            Text("New Folder")
                        }
                    }
                )
            }

            items(folders) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { selectedFolder = folder }
                )
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
    val searchQuery by viewModel.searchQuery.collectAsState()
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
                    Button(onClick = { showQuickAdd = true }) {
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

            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) }
                )
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
                            text = if (searchQuery.isEmpty()) 
                                "No entries in this folder yet." 
                            else "No results for \"$searchQuery\"",
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

@Composable
fun NewFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    val colors = listOf(
        Color(0xFFE91E63), // Pink
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFFC107), // Amber
        Color(0xFFFF5722)  // Deep Orange
    )
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Column {
                    Text(
                        "Select Color",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (selectedColor == color) 2.dp else 0.dp,
                                        color = if (selectedColor == color) MaterialTheme.colorScheme.outline else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, description, selectedColor.toArgb()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
