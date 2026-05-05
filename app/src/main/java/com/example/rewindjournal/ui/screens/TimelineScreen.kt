package com.example.rewindjournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rewindjournal.data.Folder
import com.example.rewindjournal.ui.components.*
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.viewmodel.TimelineMoment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column


@Composable
fun TimelineScreen(
    folderId: String,
    viewModel: JournalViewModel,
    onEntryClick: (TimelineMoment) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val folder by viewModel.getFolderFlow(folderId).collectAsState(initial = null)
    val sentimentNodes by viewModel.getSentimentNodesByFolder(folderId)
        .collectAsState(initial = emptyList())
    val detailedNodes by viewModel.getDetailedNodesByFolder(folderId)
        .collectAsState(initial = emptyList())

    val allNodes = remember(sentimentNodes, detailedNodes, viewModel) {
        (detailedNodes + sentimentNodes)
            .distinctBy { it.entryId }
            .sortedByDescending { it.timestamp }
            .map { node ->
                val bestLocation = node.location.takeIf { it.isNotBlank() }
                    ?: node.locations.firstOrNull().orEmpty()

                val subtitle = bestLocation.ifBlank { node.subtitle }

                node.copy(subtitle = subtitle)
            }
    }

    val topEvents = folder?.let { viewModel.parseListForUi(it.topEvents) } ?: emptyList()
    val topLocations = folder?.let { viewModel.parseListForUi(it.topLocations) } ?: emptyList()

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
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                SectionHeader(
                    title = folder?.name ?: "Timeline",
                    subtitle = "Patterns, sentiment, and key moments over time",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        folder?.let { f ->
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    TimelineInsightsCard(
                        summaryText = f.summaryText,
                        sentimentTrend = f.sentimentTrend,
                        topLocations = topLocations,
                        startTimestamp = f.startTimestamp,
                        endTimestamp = f.endTimestamp,
                        formatDate = { viewModel.formatAbsoluteDate(it) }
                    )
                }
            }
            if (allNodes.isNotEmpty()){
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
//                        Text(
//                            text = "Mood Analytics",
//                            style = MaterialTheme.typography.titleLarge,
//                            modifier = Modifier.padding(bottom = 16.dp)
//                        )

                        MoodTrendCard(nodes = allNodes)

                        Spacer(modifier = Modifier.height(16.dp))

//                        Text(
//                            text = "Sentiment Over Time",
//                            style = MaterialTheme.typography.titleLarge,
//                            modifier = Modifier.padding(bottom = 16.dp)
//                        )

                        MoodGraphCard(nodes = allNodes)
                    }
                }

            if (allNodes.isNotEmpty()) {
                item {
                    Text(
                        text = "Timeline",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 8.dp
                        )
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
                        formatDate = { viewModel.formatAbsoluteDateWithRelativeHint(it) },
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
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

fun folderHasStats(folder: Folder?): Boolean {
    if (folder == null) return false
    return folder.veryPositiveCount > 0 ||
        folder.positiveCount > 0 ||
        folder.neutralCount > 0 ||
        folder.negativeCount > 0 ||
        folder.veryNegativeCount > 0
}