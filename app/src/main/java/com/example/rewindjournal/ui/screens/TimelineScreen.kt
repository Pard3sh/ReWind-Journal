package com.example.rewindjournal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.viewmodel.TimelineMoment

@Composable
fun TimelineScreen(
    viewModel: JournalViewModel,
    onEntryClick: (TimelineMoment) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Timeline feature is currently being worked on.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
