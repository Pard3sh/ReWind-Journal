package com.example.rewindjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.rewindjournal.ui.components.FolderCard
import com.example.rewindjournal.ui.components.SectionHeader
import com.example.rewindjournal.ui.screens.*
import com.example.rewindjournal.ui.theme.RewindJournalTheme
import com.example.rewindjournal.ui.viewmodel.AuthViewModel
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RewindJournalTheme {
                val context = LocalContext.current
                val authViewModel: AuthViewModel = viewModel()

                // Initialize ViewModel using the provided Factory
                val journalViewModel: JournalViewModel = viewModel(factory = JournalViewModel.Factory)

                RootScreen(
                    authViewModel = authViewModel,
                    journalViewModel = journalViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewindJournalApp(viewModel: JournalViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var editingEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTimelineFolderId by rememberSaveable { mutableStateOf<String?>(null) }

    val entries by viewModel.entries.collectAsState()
    val editingEntry = entries.find { it.id == editingEntryId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ReWind Journal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Capture now. Revisit later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    val user = FirebaseAuth.getInstance().currentUser
                    val photoUrl = user?.photoUrl?.toString()

                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val items = listOf("Home", "New Entry", "Folders", "Timeline")
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.Default.Add,
                    Icons.Default.BookmarkBorder,
                    Icons.Default.Schedule
                )

                items.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = (selectedTab == index) && (editingEntryId == null),
                        onClick = {
                            selectedTab = index
                            editingEntryId = null
                            // Reset selection when switching away from Timeline tab
                            if (index != 3) selectedTimelineFolderId = null
                        },
                        icon = {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (editingEntryId != null && editingEntry != null) {
                NewEntryScreen(
                    viewModel = viewModel,
                    editingEntry = editingEntry,
                    onSaveComplete = {
                        editingEntryId = null
                    },
                    onCancel = { editingEntryId = null }
                )
            } else {
                when (selectedTab) {
                    0 -> HomeScreen(viewModel, onEntryClick = { editingEntryId = it.id })
                    1 -> NewEntryScreen(viewModel, onSaveComplete = { selectedTab = 0 })
                    2 -> FoldersScreen(viewModel, onEntryClick = { editingEntryId = it.id })
                    3 -> {
                        if (selectedTimelineFolderId != null) {
                            TimelineScreen(
                                folderId = selectedTimelineFolderId!!,
                                viewModel = viewModel,
                                onEntryClick = { editingEntryId = it.id }
                            )
                            BackHandler { selectedTimelineFolderId = null }
                        } else {
                            TimelineFolderPicker(
                                viewModel = viewModel,
                                onFolderSelected = { selectedTimelineFolderId = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineFolderPicker(
    viewModel: JournalViewModel,
    onFolderSelected: (String) -> Unit
) {
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
                title = "Timeline",
                subtitle = "Select a folder to view patterns and key moments."
            )
        }

        // Filter out "General" folder (id = "general") as requested
        items(folders.filter { it.id != "general" }) { folder ->
            FolderCard(
                folder = folder,
                onClick = { onFolderSelected(folder.id) }
            )
        }

        if (folders.none { it.id != "general" }) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxHeight(0.7f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create a custom folder to see timeline patterns.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RewindJournalPreview() {
    RewindJournalTheme {
        // Standard preview implementation
    }
}