package com.example.rewindjournal.data

import retrofit2.http.GET

// API Interface
interface AffirmationApi {

    @GET("/")
    suspend fun getAffirmation(): Affirmation
}
