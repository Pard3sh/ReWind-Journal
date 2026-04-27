package com.example.rewindjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rewindjournal.ui.screens.FoldersScreen
import com.example.rewindjournal.ui.screens.HomeScreen
import com.example.rewindjournal.ui.screens.NewEntryScreen
import com.example.rewindjournal.ui.screens.TimelineScreen
import com.example.rewindjournal.ui.theme.RewindJournalTheme
import com.example.rewindjournal.ui.viewmodel.AuthViewModel
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.screens.RootScreen
import androidx.compose.ui.platform.LocalContext
import com.example.rewindjournal.data.JournalDatabase
import com.example.rewindjournal.data.JournalRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RewindJournalTheme {


                val authViewModel: AuthViewModel = viewModel()

                val context = LocalContext.current

                val journalViewModel = remember {
                    val db = JournalDatabase.getDatabase(context)

                    val repository = JournalRepository(db.journalDao())

                    JournalViewModel(repository)
                }

                RootScreen(
                    authViewModel = authViewModel,
                    journalViewModel = journalViewModel
                )
                // commented out bc testing splash screen/ Oauth
                // RewindJournalApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewindJournalApp(viewModel: JournalViewModel = viewModel(factory = JournalViewModel.Factory)) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ReWind Journal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Capture now. Revisit later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            if (selectedTab != 2) {
                FloatingActionButton(onClick = { selectedTab = 2 }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New journal entry"
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                val items = listOf("Home", "Folders", "New", "Timeline")
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.Default.BookmarkBorder,
                    Icons.Default.Add,
                    Icons.Default.Schedule
                )

                items.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
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
            when (selectedTab) {
                0 -> HomeScreen(viewModel)
                1 -> FoldersScreen(viewModel)
                2 -> NewEntryScreen(viewModel, onSaveComplete = { selectedTab = 0 })
                3 -> TimelineScreen(viewModel)
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
