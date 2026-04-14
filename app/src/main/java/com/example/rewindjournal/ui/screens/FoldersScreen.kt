package com.example.rewindjournal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.components.FolderCard
import com.example.rewindjournal.ui.components.SectionHeader

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun FoldersScreen(viewModel: JournalViewModel) {
    val folders by viewModel.folders.collectAsState()

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
                subtitle = "Organize longer experiences into dedicated spaces."
            )
        }

        items(folders) { folder ->
            FolderCard(folder = folder)
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}
