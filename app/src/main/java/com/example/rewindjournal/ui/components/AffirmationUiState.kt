package com.example.rewindjournal.ui.components

sealed class AffirmationUiState {
    object Loading : AffirmationUiState()
    data class Success(val text: String) : AffirmationUiState()
    data class Error(val message: String) : AffirmationUiState()
}