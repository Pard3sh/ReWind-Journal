package com.example.rewindjournal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rewindjournal.ui.components.AffirmationCard
import com.example.rewindjournal.ui.components.AffirmationScreen
import com.example.rewindjournal.ui.components.StatPill
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.components.TimelineCard
import com.example.rewindjournal.ui.viewmodel.TimelineMoment

@Composable
fun HomeScreen(viewModel: JournalViewModel, onEntryClick: (TimelineMoment) -> Unit = {}) {
    val entries by viewModel.entries.collectAsState()
    val folders by viewModel.folders.collectAsState()

    val totalEntries = entries.size
    val totalFolders = folders.size - 1 // Exclude virtual General folder

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "Welcome back, ready to reflect?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item { AffirmationScreen() }


        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Entries",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Your latest reflections at a glance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(entries.take(5)) { moment ->
            TimelineCard(
                moment = moment, 
                onClick = { onEntryClick(moment) },
                overrideColor = moment.folderColor
            )
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}
