package com.example.rewindjournal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rewindjournal.data.Folder
import com.example.rewindjournal.ui.viewmodel.AffirmationViewModel
import com.example.rewindjournal.ui.viewmodel.FolderSummary
import com.example.rewindjournal.ui.viewmodel.FolderTimelineNode
import com.example.rewindjournal.ui.viewmodel.TimelineMoment

@Composable
fun AffirmationCard(affirmation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Today’s affirmation",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = affirmation,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun AffirmationScreen(viewModel: AffirmationViewModel = viewModel()) {
    val uiState = viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchAffirmation()
    }

    when (val state = uiState.value) {
        is AffirmationUiState.Loading -> {
            Text("Loading...")
        }
        is AffirmationUiState.Success -> {
            AffirmationCard(affirmation = state.text)
        }
        is AffirmationUiState.Error -> {
            Text(state.message)
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { Text("Search...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(28.dp),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryComposerCard(
    title: String,
    body: String,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    folders: List<FolderSummary> = emptyList(),
    selectedFolderId: String? = null,
    onFolderSelected: (String?) -> Unit = {},
    isEditing: Boolean = false,
    onCancelEdit: () -> Unit = {},
    onDeleteClick: (() -> Unit)? = null
) {
    var showFolderPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val generalFolderId = "general"

    if (showFolderPicker) {
        ModalBottomSheet(
            onDismissRequest = { showFolderPicker = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Select Folder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders) { folder ->
                        val isSelected =
                            (selectedFolderId == folder.id) ||
                                (selectedFolderId == null && folder.id == generalFolderId)

                        Surface(
                            onClick = {
                                onFolderSelected(if (folder.id == generalFolderId) null else folder.id)
                                showFolderPicker = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(folder.color), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEditing) "Edit Entry" else "Journal Entry",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEditing) "Update Reflection" else "New Entry",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isEditing && onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Entry",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text("Enter Title Here", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = body,
                onValueChange = onBodyChange,
                placeholder = { Text("Enter Entry Here", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSaveClick,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditing) "Save Changes" else "Save Entry")
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = { showFolderPicker = true },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Assign Folder", maxLines = 1)
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun StatPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(16.dp))
            action()
        }
    }
}

@Composable
fun FolderCard(
    folder: FolderSummary,
    onClick: () -> Unit = {},
    onEditClick: (() -> Unit)? = null
) {
    val generalFolderId = "general"

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(folder.color), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${folder.entryCount} entries",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (onEditClick != null && folder.id != generalFolderId) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Folder",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = folder.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TimelineCard(
    moment: TimelineMoment,
    onClick: () -> Unit = {},
    overrideColor: Int? = null
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = overrideColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = moment.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = moment.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TimelineInsightChip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun TimelineInsightsCard(
    folderName: String,
    summaryText: String,
    sentimentTrend: String,
    topEvents: List<String>,
    topLocations: List<String>
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = folderName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (sentimentTrend.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Mood trend: $sentimentTrend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (summaryText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (topEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Top events",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(topEvents.take(5)) { item ->
                        TimelineInsightChip(text = item)
                    }
                }
            }

            if (topLocations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Top locations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(topLocations.take(5)) { item ->
                        TimelineInsightChip(text = item)
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineStatsCard(folder: Folder) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Sentiment Stats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { StatPill(label = "Very Positive", value = folder.veryPositiveCount.toString()) }
                item { StatPill(label = "Positive", value = folder.positiveCount.toString()) }
                item { StatPill(label = "Neutral", value = folder.neutralCount.toString()) }
                item { StatPill(label = "Negative", value = folder.negativeCount.toString()) }
                item { StatPill(label = "Very Negative", value = folder.veryNegativeCount.toString()) }
            }
        }
    }
}

@Composable
fun MoodTrendCard(nodes: List<FolderTimelineNode>) {
    // Count each entry only once for the mood trend statistics
    val uniqueEntries = nodes.distinctBy { it.entryId }
    
    var happyCount = 0
    var neutralCount = 0
    var sadCount = 0
    var angryCount = 0

    uniqueEntries.forEach { node ->
        val label = (if (node.emotionLabel.isNotBlank()) node.emotionLabel else node.sentimentLabel).trim().lowercase()
        when {
            // Happy / Positive category
            label.contains("happy") || label.contains("joy") || label.contains("positiv") || 
            label.contains("excit") || label.contains("grate") || label.contains("love") || 
            label.contains("content") || label.contains("cheer") || label.contains("satisf") ||
            label.contains("wonder") || label.contains("great") || label.contains("awesome") -> happyCount++

            // Angry / Very Negative category
            label.contains("ang") || label.contains("frustrat") || label.contains("annoy") || 
            label.contains("irritat") || label.contains("very negative") || label.contains("disgust") ||
            label.contains("mad") || label.contains("outrage") || label.contains("furi") -> angryCount++

            // Sad / Negative category
            label.contains("sad") || label.contains("negativ") || label.contains("lonely") || 
            label.contains("hurt") || label.contains("fear") || label.contains("anxious") || 
            label.contains("gloomy") || label.contains("depress") || label.contains("sorrow") ||
            label.contains("worr") -> sadCount++

            // Default to Neutral
            else -> neutralCount++
        }
    }

    val total = uniqueEntries.size
    val moods = listOf(
        MoodTrendData(label = "Happy", count = happyCount, color = Color(0xFFFFD54F), emoji = "😁"), // Amber
        MoodTrendData(label = "Neutral", count = neutralCount, color = Color(0xFFB0BEC5), emoji = "😌"), // Blue Gray
        MoodTrendData(label = "Sadness", count = sadCount, color = Color(0xFF64B5F6), emoji = "😔"), // Blue
        MoodTrendData(label = "Anger", count = angryCount, color = Color(0xFFE57373), emoji = "😡") // Red
    )

    val maxEntries = moods.maxOf { it.count }.coerceAtLeast(1)

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            moods.forEach { mood ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${mood.count} ${if (mood.count == 1) "entry" else "entries"}",
                        modifier = Modifier.width(85.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(Color.White.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (total > 0) mood.count.toFloat() / maxEntries else 0f)
                                .height(8.dp)
                                .background(mood.color, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = mood.emoji, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = mood.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        modifier = Modifier.width(65.dp)
                    )
                }
            }
        }
    }
}

private data class MoodTrendData(val label: String, val count: Int, val color: Color, val emoji: String)

@Composable
fun StraightTimelineMoment(
    node: FolderTimelineNode,
    index: Int,
    isLast: Boolean,
    accentColor: Int,
    onClick: () -> Unit,
    formatDate: (Long) -> String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Vertical Line and Bubble Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Top part of the line (connecting to previous)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(12.dp)
                    .background(if (index == 0) Color.Transparent else Color.Gray.copy(alpha = 0.3f))
            )
            
            // Bubble (The event dot)
            Surface(
                shape = CircleShape,
                color = Color(accentColor),
                modifier = Modifier.size(16.dp),
                border = BorderStroke(3.dp, Color(accentColor).copy(alpha = 0.2f))
            ) {}

            // Bottom part of the line (connecting to next)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (isLast) Color.Transparent else Color.Gray.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content Column
        Column(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .weight(1f)
        ) {
            Text(
                text = node.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF4527A0)
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (node.location.isNotBlank()) {
                    Text(
                        text = node.location,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Text(
                    text = formatDate(node.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            if (node.emotionLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = node.emotionLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
