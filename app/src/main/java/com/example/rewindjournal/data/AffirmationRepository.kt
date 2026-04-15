package com.example.rewindjournal.data

class AffirmationRepository {
    suspend fun getAffirmation(): Affirmation {
        return RetrofitInstance.api.getAffirmation()
    }
}