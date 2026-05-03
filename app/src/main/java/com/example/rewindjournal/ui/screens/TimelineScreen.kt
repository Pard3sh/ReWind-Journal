package com.example.rewindjournal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import com.example.rewindjournal.data.Folder
import com.example.rewindjournal.ui.components.FolderTimelineCard
import com.example.rewindjournal.ui.components.SectionHeader
import com.example.rewindjournal.ui.components.TimelineInsightsCard
import com.example.rewindjournal.ui.components.TimelineStatsCard
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.viewmodel.TimelineMoment

@Composable
fun TimelineScreen(
    folderId: String,
    viewModel: JournalViewModel,
    onEntryClick: (TimelineMoment) -> Unit = {}
) {
    val folder by viewModel.getFolderFlow(folderId).collectAsState(initial = null)
    val sentimentNodes by viewModel.getSentimentNodesByFolder(folderId).collectAsState(initial = emptyList())
    val detailedNodes by viewModel.getDetailedNodesByFolder(folderId).collectAsState(initial = emptyList())

    val topEvents = folder?.let { viewModel.parseListForUi(it.topEvents) } ?: emptyList()
    val topLocations = folder?.let { viewModel.parseListForUi(it.topLocations) } ?: emptyList()

    val hasSentimentTimeline = sentimentNodes.isNotEmpty()
    val hasEventTimeline = detailedNodes.isNotEmpty()

    val hasTimelineData =
        folder?.summaryText?.isNotBlank() == true ||
            folder?.sentimentTrend?.isNotBlank() == true ||
            topEvents.isNotEmpty() ||
            topLocations.isNotEmpty() ||
            hasSentimentTimeline ||
            hasEventTimeline ||
            folderHasStats(folder)

    if (!hasTimelineData) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No timeline data has been generated for this folder yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = folder?.name ?: "Timeline",
                subtitle = "Patterns, sentiment, and key moments over time"
            )
        }

        folder?.let {
            item {
                TimelineInsightsCard(
                    folderName = it.name,
                    summaryText = it.summaryText,
                    sentimentTrend = it.sentimentTrend,
                    topEvents = topEvents,
                    topLocations = topLocations
                )
            }

            if (folderHasStats(it)) {
                item {
                    TimelineStatsCard(folder = it)
                }
            }
        }

        if (hasSentimentTimeline) {
            item {
                Text(
                    text = "Sentiment Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(
                items = sentimentNodes,
                key = { node -> node.id }
            ) { node ->
                FolderTimelineCard(
                    title = node.title,
                    subtitle = node.subtitle,
                    accentColor = folder?.color ?: 0xFF6200EE.toInt(),
                    onClick = {
                        onEntryClick(
                            TimelineMoment(
                                id = node.entryId,
                                title = node.title,
                                subtitle = node.subtitle,
                                folderId = folderId,
                                folderColor = folder?.color
                            )
                        )
                    }
                )
            }
        }

        if (hasEventTimeline) {
            item {
                Text(
                    text = "Event Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(
                items = detailedNodes,
                key = { node -> node.id }
            ) { node ->
                FolderTimelineCard(
                    title = node.title,
                    subtitle = node.subtitle,
                    accentColor = folder?.color ?: 0xFF6200EE.toInt(),
                    onClick = {
                        onEntryClick(
                            TimelineMoment(
                                id = node.entryId,
                                title = node.title,
                                subtitle = node.subtitle,
                                folderId = folderId,
                                folderColor = folder?.color
                            )
                        )
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun folderHasStats(folder: Folder?): Boolean {
    if (folder == null) return false
    return folder.veryPositiveCount > 0 ||
        folder.positiveCount > 0 ||
        folder.neutralCount > 0 ||
        folder.negativeCount > 0 ||
        folder.veryNegativeCount > 0
}
