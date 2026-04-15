package com.example.rewindjournal.data

// Data Model
import com.google.gson.annotations.SerializedName

data class Affirmation(
    @SerializedName("affirmation")
    val affirmation: String
)