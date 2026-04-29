package com.example.rewindjournal.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.rewindjournal.ui.components.AffirmationUiState
import com.example.rewindjournal.data.AffirmationRepository
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

class AffirmationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AffirmationRepository()

    private val _uiState = MutableStateFlow<AffirmationUiState>(AffirmationUiState.Loading)
    val uiState: StateFlow<AffirmationUiState> = _uiState

    private val prefs = application.getSharedPreferences("affirmation_prefs", Context.MODE_PRIVATE)

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // testing:
        //return "2026-01-01"
        //testing:
        //return "2026-01-02"
        return sdf.format(Date())
    }

    fun fetchAffirmation() {
        viewModelScope.launch {

            val savedDate = prefs.getString("date", null)
            val savedAffirmation = prefs.getString("affirmation", null)

            val today = getTodayDate()

            // same day so it reuses
            if (savedDate == today && savedAffirmation != null) {
                _uiState.value = AffirmationUiState.Success(savedAffirmation)
                return@launch
            }

            _uiState.value = AffirmationUiState.Loading

            try {
                val response = repository.getAffirmation()
                val newAffirmation = response.affirmation

                _uiState.value = AffirmationUiState.Success(newAffirmation)

                prefs.edit {
                    putString("affirmation", newAffirmation)
                        .putString("date", today)
                }

            } catch (e: Exception) {
                _uiState.value = AffirmationUiState.Error(
                    e.message ?: "Something went wrong"
                )
            }
        }
    }
}