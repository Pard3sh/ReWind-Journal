package com.example.rewindjournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rewindjournal.data.Folder
import com.example.rewindjournal.ui.components.*
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

    // Combine and sort nodes for a unified timeline.
    // Prefer detailedNodes if both exist for the same entry to avoid double counting.
    val allNodes = remember(sentimentNodes, detailedNodes) {
        (detailedNodes + sentimentNodes)
            .distinctBy { it.entryId }
            .sortedByDescending { it.timestamp }
    }

    val topEvents = folder?.let { viewModel.parseListForUi(it.topEvents) } ?: emptyList()
    val topLocations = folder?.let { viewModel.parseListForUi(it.topLocations) } ?: emptyList()

//    val hasSentimentTimeline = sentimentNodes.isNotEmpty()
//    val hasEventTimeline = detailedNodes.isNotEmpty()

    val hasTimelineData =
        folder?.summaryText?.isNotBlank() == true ||
            folder?.sentimentTrend?.isNotBlank() == true ||
            topEvents.isNotEmpty() ||
            topLocations.isNotEmpty() ||
            allNodes.isNotEmpty() ||
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
    ) {
        item {
            SectionHeader(
                title = folder?.name ?: "Timeline",
                subtitle = "Patterns, sentiment, and key moments over time",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        folder?.let {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Mood Trend",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    MoodTrendCard(nodes = allNodes)
                }
            }

            item {
                Box(modifier = Modifier.padding(16.dp)) {
                    TimelineInsightsCard(
                        folderName = it.name,
                        summaryText = it.summaryText,
                        sentimentTrend = it.sentimentTrend,
                        topEvents = topEvents,
                        topLocations = topLocations
                    )
                }
            }
        }

        if (allNodes.isNotEmpty()) {
            item {
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(
                items = allNodes,
                key = { _, node -> node.id }
            ) { index, node ->
                StraightTimelineMoment(
                    node = node,
                    index = index,
                    isLast = index == allNodes.size - 1,
                    accentColor = folder?.color ?: 0xFF6200EE.toInt(),
                    formatDate = { viewModel.formatEntryDate(it) },
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
