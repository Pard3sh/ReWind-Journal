package com.example.rewindjournal.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.example.rewindjournal.ui.components.EntryComposerCard
import com.example.rewindjournal.ui.viewmodel.TimelineMoment

data class PendingEntrySave(
    val title: String,
    val body: String,
    val folderId: String?
)

@Composable
fun NewEntryScreen(
    viewModel: JournalViewModel,
    editingEntry: TimelineMoment? = null,
    initialFolderId: String? = null,
    onSaveComplete: () -> Unit,
    onCancel: () -> Unit = {}
) {
    var entryTitle by rememberSaveable { mutableStateOf(editingEntry?.title ?: "") }
    var entryBody by rememberSaveable { mutableStateOf(editingEntry?.body ?: "") }
    var selectedFolderId by rememberSaveable { mutableStateOf<String?>(editingEntry?.folderId ?: initialFolderId) }
    var pendingEntrySave by remember { mutableStateOf<PendingEntrySave?>(null) }
    val context = LocalContext.current

    // request location from user
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingEntrySave?.let { pendingEntry ->
            if (isGranted) {
                val location = getLastKnownLocation(context)
                viewModel.addEntry(
                    pendingEntry.title,
                    pendingEntry.body,
                    pendingEntry.folderId,
                    location?.latitude,
                    location?.longitude
                )
            } else {
                viewModel.addEntry(pendingEntry.title, pendingEntry.body, pendingEntry.folderId)
            }
            pendingEntrySave = null
            onSaveComplete()
        }
    }

    // add location to Entry
    fun saveEntryWithLocation(title: String, body: String, folderId: String?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val location = getLastKnownLocation(context)
            viewModel.addEntry(title, body, folderId, location?.latitude, location?.longitude)
            onSaveComplete()
        } else {
            pendingEntrySave = PendingEntrySave(title, body, folderId)
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val folders by viewModel.folders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        EntryComposerCard(
            title = entryTitle,
            body = entryBody,
            onTitleChange = { entryTitle = it },
            onBodyChange = { entryBody = it },
            folders = folders,
            selectedFolderId = selectedFolderId,
            onFolderSelected = { selectedFolderId = it },
            isEditing = editingEntry != null,
            onCancelEdit = onCancel,
            onDeleteClick = if (editingEntry != null) {

                {
                    viewModel.deleteEntry(editingEntry.id)
                    onSaveComplete()
                }
            } else null,
            onSaveClick = {
                if (entryTitle.isNotBlank() || entryBody.isNotBlank()) {
                    if (editingEntry != null) {
                        viewModel.updateEntry(editingEntry.id, entryTitle, entryBody, selectedFolderId)
                        onSaveComplete()
                    } else {
                        saveEntryWithLocation(entryTitle, entryBody, selectedFolderId)
                    }
                    entryTitle = ""
                    entryBody = ""
                    selectedFolderId = null
                }
            }
        )
    }
}

// more location help 
private fun getLastKnownLocation(context: Context): Location? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    return locationManager?.let {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider ->
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        it.getLastKnownLocation(provider)
                    } else {
                        null
                    }
                } catch (_: SecurityException) {
                    null
                }
            }
            .firstOrNull()
    }
}
