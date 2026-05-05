package com.example.rewindjournal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.toArgb
import com.example.rewindjournal.ui.viewmodel.AffirmationViewModel
import com.example.rewindjournal.ui.viewmodel.FolderSummary
import com.example.rewindjournal.ui.viewmodel.FolderTimelineNode
import com.example.rewindjournal.ui.viewmodel.TimelineMoment

data class MoodTrendData(
    val label: String,
    val count: Int,
    val color: Color,
    val emoji: String
)

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
    onDeleteClick: (() -> Unit)? = null,
    onCreateFolder: (String, String, Int) -> Unit = { _, _, _ -> }
) {
    var showFolderPicker by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val generalFolderId = "general"
    val currentFolderName = folders.find { it.id == (selectedFolderId ?: generalFolderId) }?.name ?: "Assign Folder"

    if (showNewFolderDialog) {
        FolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name, desc, color ->
                onCreateFolder(name, desc, color)
                showNewFolderDialog = false
            }
        )
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Folder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = {
                        showNewFolderDialog = true
                    }) {
                        Icon(Icons.Default.CreateNewFolder, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Folder")
                    }
                }

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
                    Text(currentFolderName, maxLines = 1)
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
    topLocations: List<String>,
    startTimestamp: Long?,
    endTimestamp: Long?,
    formatDate: (Long) -> String
) {
    val hasDateRange = startTimestamp != null && endTimestamp != null
    val hasEvents = topEvents.isNotEmpty()
    val hasLocations = topLocations.isNotEmpty()
    val hasSummary = summaryText.isNotBlank()
    val hasTrend = sentimentTrend.isNotBlank()

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

            if (hasDateRange) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${formatDate(startTimestamp!!)} → ${formatDate(endTimestamp!!)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (hasTrend) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Mood trend: $sentimentTrend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (hasEvents) {
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

            if (hasLocations) {
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

            if (hasSummary) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
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
    
    var joyCount = 0
    var sadnessCount = 0
    var angerCount = 0
    var fearCount = 0
    var loveCount = 0
    var surpriseCount = 0

    uniqueEntries.forEach { node ->
        val label = (if (node.emotionLabel.isNotBlank()) node.emotionLabel else node.sentimentLabel).trim().lowercase()
        when {
            label.contains("love") || label.contains("affection") || label.contains("ador") || label.contains("cherish") -> loveCount++
            label.contains("joy") || label.contains("happy") || label.contains("content") || label.contains("cheer") || label.contains("grate") || label.contains("excit") || label.contains("wonder") || label.contains("great") || label.contains("awesome") -> joyCount++
            label.contains("surprise") || label.contains("amazed") || label.contains("shock") || label.contains("astonish") || label.contains("stun") -> surpriseCount++
            label.contains("ang") || label.contains("frustrat") || label.contains("annoy") || label.contains("mad") || label.contains("outrage") || label.contains("furi") || label.contains("irritat") || label.contains("disgust") -> angerCount++
            label.contains("fear") || label.contains("anxious") || label.contains("worr") || label.contains("panic") || label.contains("nervous") || label.contains("scared") || label.contains("terrifi") -> fearCount++
            label.contains("sad") || label.contains("depress") || label.contains("lonely") || label.contains("hurt") || label.contains("negativ") || label.contains("gloomy") || label.contains("sorrow") -> sadnessCount++
        }
    }

    val moods = listOf(
        MoodTrendData("Joy", joyCount, Color(0xFFFFD54F), "😊"),
        MoodTrendData("Love", loveCount, Color(0xFFF06292), "😍"),
        MoodTrendData("Surprise", surpriseCount, Color(0xFFBA68C8), "😲"),
        MoodTrendData("Fear", fearCount, Color(0xFF4DB6AC), "😨"),
        MoodTrendData("Sadness", sadnessCount, Color(0xFF64B5F6), "😔"),
        MoodTrendData("Anger", angerCount, Color(0xFFE57373), "😡")
    )

    val maxCount = moods.maxOf { it.count }.coerceAtLeast(1)

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Mood Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            moods.forEach { mood ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = mood.emoji, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = mood.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray,
                        modifier = Modifier.width(60.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .background(Color.White.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (maxCount > 0) mood.count.toFloat() / maxCount else 0.01f)
                                .height(10.dp)
                                .background(mood.color, CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = mood.count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun FolderDialog(
    initialName: String = "",
    initialDescription: String = "",
    initialColor: Color = Color(0xFF4E378B),
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    val maxNameLength = 25

    val colors = listOf(
        Color(0xFF4E378B), // Dark Purple
        Color(0xFFE91E63), // Pink
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFFC107), // Amber
        Color(0xFFFF5722)  // Deep Orange
    )
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) "Edit Folder" else "New Folder",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= maxNameLength) name = it },
                    label = { Text("Folder Name") },
                    supportingText = {
                        Text(
                            text = "${name.length} / $maxNameLength",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                    )
                )

                Column {
                    Text(
                        "Select Color",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 0.dp,
                                        color = if (selectedColor == color) MaterialTheme.colorScheme.outline else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }

                if (isEditing && onDelete != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Folder")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, description, selectedColor.toArgb()) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (isEditing) "Update" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


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

@Composable
fun MoodGraphCard(nodes: List<FolderTimelineNode>) {
    if (nodes.isEmpty()) return

    // oldest → newest for the graph
    val sorted = remember(nodes) {
        nodes.sortedBy { it.timestamp }
    }

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            val lineColor = MaterialTheme.colorScheme.primary
            val gridColor = Color(0xFFD8CDEB)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (sorted.isEmpty()) return@Canvas

                // Three horizontal guide lines (top / middle / bottom)
                val topY = h * 0.2f
                val midY = h * 0.5f
                val botY = h * 0.8f

                drawLine(
                    color = gridColor,
                    start = Offset(0f, topY),
                    end = Offset(w, topY),
                    alpha = 0.5f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    alpha = 0.5f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, botY),
                    end = Offset(w, botY),
                    alpha = 0.5f
                )

                // Map sentimentScore ([-1,1]) to Y between topY and botY
                fun scoreToY(score: Float): Float {
                    val clamped = score.coerceIn(-1f, 1f)
                    val t = (1f - (clamped + 1f) / 2f) // +1→0, -1→1
                    return topY + (botY - topY) * t
                }

                val count = sorted.size
                val stepX = if (count > 1) w / (count - 1) else 0f

                val points = sorted.mapIndexed { index, node ->
                    val x = if (count == 1) w / 2f else stepX * index
                    val y = scoreToY(node.sentimentScore)
                    Offset(x, y)
                }

                // Faint shadow line
                for (i in 0 until points.lastIndex) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    drawLine(
                        color = lineColor.copy(alpha = 0.25f),
                        start = p1,
                        end = p2,
                        strokeWidth = 6f
                    )
                }

                // Main line
                for (i in 0 until points.lastIndex) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    drawLine(
                        color = lineColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 4f
                    )
                }

                // Points
                points.forEach { p ->
                    drawCircle(
                        color = Color.White,
                        radius = 7f,
                        center = p
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 4f,
                        center = p
                    )
                }
            }
        }
    }
}
