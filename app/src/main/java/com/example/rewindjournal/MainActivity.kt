package com.example.rewindjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rewindjournal.ui.screens.FoldersScreen
import com.example.rewindjournal.ui.screens.HomeScreen
import com.example.rewindjournal.ui.screens.NewEntryScreen
import com.example.rewindjournal.ui.screens.TimelineScreen
import com.example.rewindjournal.ui.theme.RewindJournalTheme
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.screens.RootScreen
import androidx.compose.ui.platform.LocalContext

import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.rewindjournal.data.JournalDatabase
import com.example.rewindjournal.data.JournalRepository
import com.example.rewindjournal.ui.screens.RootScreen
import com.example.rewindjournal.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RewindJournalTheme {
                //RewindJournalApp()
                val context = LocalContext.current

                val authViewModel: AuthViewModel = viewModel()

                val journalViewModel = remember {
                    val db = JournalDatabase.getDatabase(context)
                    val repository = JournalRepository(db.journalDao())
                    JournalViewModel(repository)
                }

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
fun RewindJournalApp(viewModel: JournalViewModel = viewModel(factory = JournalViewModel.Factory)) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var editingEntryId by rememberSaveable { mutableStateOf<String?>(null) }
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
//                            style = MaterialTheme.typography.headlineSmall,
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
//                    Icon(
//                        imageVector = Icons.Default.Person,
//                        contentDescription = "Profile",
//                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                        modifier = Modifier.padding(end = 16.dp)
//                    )
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
//        floatingActionButton = {
//            if (selectedTab != 2) {
//                FloatingActionButton(onClick = { selectedTab = 2 }) {
//                    Icon(
//                        imageVector = Icons.Default.Add,
//                        contentDescription = "New journal entry"
//                    )
//                }
//            }
//        },
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
                        selected = selectedTab == index && editingEntryId == null,
                        onClick = { 
                            selectedTab = index
                            editingEntryId = null 
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
                        selectedTab = 0 
                    },
                    onCancel = { editingEntryId = null }
                )
            } else {
                when (selectedTab) {
                    0 -> HomeScreen(viewModel, onEntryClick = { editingEntryId = it.id })
                    1 -> NewEntryScreen(viewModel, onSaveComplete = { selectedTab = 0 })
                    2 -> FoldersScreen(viewModel, onEntryClick = { editingEntryId = it.id })
                    3 -> TimelineScreen(viewModel, onEntryClick = { editingEntryId = it.id })
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RewindJournalPreview() {
    RewindJournalTheme {
        RewindJournalApp()
    }
}
