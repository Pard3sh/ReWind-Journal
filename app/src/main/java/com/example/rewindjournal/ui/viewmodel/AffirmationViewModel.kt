package com.example.rewindjournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.rewindjournal.ui.components.AffirmationUiState
import com.example.rewindjournal.data.AffirmationRepository

class AffirmationViewModel : ViewModel() {

    private val repository = AffirmationRepository()

    private val _uiState = MutableStateFlow<AffirmationUiState>(AffirmationUiState.Loading)
    val uiState: StateFlow<AffirmationUiState> = _uiState

    fun fetchAffirmation() {
        viewModelScope.launch {
            _uiState.value = AffirmationUiState.Loading

            try {
                val response = repository.getAffirmation()
                _uiState.value = AffirmationUiState.Success(response.affirmation)
            } catch (e: Exception) {
                _uiState.value = AffirmationUiState.Error(
                    e.message ?: "Something went wrong"
                )
            }
        }
    }
}