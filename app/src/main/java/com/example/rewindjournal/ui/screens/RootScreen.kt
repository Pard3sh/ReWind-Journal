package com.example.rewindjournal.ui.screens


import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

import android.content.Context

import com.example.rewindjournal.ui.viewmodel.AuthViewModel
import com.example.rewindjournal.ui.viewmodel.JournalViewModel


// google sign in helper
suspend fun signInWithGoogle(context: Context): String? {

    val credentialManager = CredentialManager.create(context)

    val request = GetCredentialRequest(
        listOf(
            GetGoogleIdOption.Builder()
                .setServerClientId("478996599952-vahb2l965rit35da01nd10nblpfb2559.apps.googleusercontent.com")
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
        )
    )

    return try {
        val result = credentialManager.getCredential(
            request = request,
            context = context
        )

        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            googleCredential.idToken
        } else {
            null
        }

    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// root screen
@Composable
fun RootScreen(
    authViewModel: AuthViewModel,
    journalViewModel: JournalViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isLoggedIn by authViewModel.isLoggedIn
    val isLoading by authViewModel.isLoading

    // testing
    val forceLoading = false
    val forceLogin = true

    when {
        forceLoading -> LoadingScreen()

        forceLogin -> SplashLoginScreen(
            onSignInClick = {
                scope.launch {
                    val idToken = signInWithGoogle(context)
                    if (idToken != null) {
                        authViewModel.firebaseAuthWithGoogle(idToken) {}
                    }
                }
            }
        )

        isLoading -> LoadingScreen()

        isLoggedIn -> HomeScreen(journalViewModel)

        else -> SplashLoginScreen(
            onSignInClick = {
                scope.launch {
                    println("START SIGN IN")

                    val idToken = signInWithGoogle(context)

                    println("TOKEN: $idToken")

                    if (idToken != null) {
                        authViewModel.firebaseAuthWithGoogle(idToken) {}
                    } else {
                        println("TOKEN IS NULL ")
                    }
                }
            }
        )
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "Loading...",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            CircularProgressIndicator()
        }
    }
}

// splash login screen

@Composable
fun SplashLoginScreen(
    onSignInClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    println("CLICKED")
                    onSignInClick()
                }
            ) {
                Text("Sign in with Google")
            }
        }
    }
}