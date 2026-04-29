package com.example.rewindjournal.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthClient(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): GoogleUser? {
        return try {

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId("478996599952-vahb2l965rit35da01nd10nblpfb2559.apps.googleusercontent.com")
                .setFilterByAuthorizedAccounts(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse =
                credentialManager.getCredential(context, request)

            handleResult(result)

        } catch (e: GetCredentialException) {
            null
        }
    }

    private fun handleResult(result: GetCredentialResponse): GoogleUser? {
        val credential = result.credential

        if (credential is GoogleIdTokenCredential) {
            return GoogleUser(
                id = credential.id,
                name = credential.displayName ?: "",
                email = credential.id
            )
        }

        return null
    }
}