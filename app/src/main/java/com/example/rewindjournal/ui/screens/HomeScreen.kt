package com.example.rewindjournal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.components.AffirmationCard
import com.example.rewindjournal.ui.components.SectionHeader
import com.example.rewindjournal.ui.components.TimelineCard

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.rewindjournal.ui.components.AffirmationScreen

@Composable
fun HomeScreen(viewModel: JournalViewModel) {
    val entries by viewModel.entries.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text(
                text = "Welcome back, Angel!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            AffirmationScreen()
        }

        item {
            SectionHeader(
                title = "Recent entries",
                subtitle = "Your latest reflections at a glance."
            )
        }

        items(entries.take(5)) { entry ->
            TimelineCard(moment = entry)
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}
