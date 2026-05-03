package com.example.rewindjournal.ui.screens


import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.rewindjournal.RewindJournalApp
import com.example.rewindjournal.ui.viewmodel.AuthViewModel
import com.example.rewindjournal.ui.viewmodel.JournalViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RootScreen(
    authViewModel: AuthViewModel,
    journalViewModel: JournalViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isLoggedIn by authViewModel.isLoggedIn
    val isLoading by authViewModel.isLoading

    // decide which screen to show
    val screenState = when {
        isLoading -> "loading"
        isLoggedIn -> "home"
        else -> "login"
    }

    AnimatedContent(
        targetState = screenState,
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith
                    fadeOut(animationSpec = tween(500))
        },
        label = "screen_transition"
    ) { state ->

        when (state) {
            "loading" -> LoadingScreen()

            "home" -> RewindJournalApp(journalViewModel)

            "login" -> SplashLoginScreen(
                onSignInClick = {
                    scope.launch {
                        val idToken = signInWithGoogle(context)
                        if (idToken != null) {
                            authViewModel.firebaseAuthWithGoogle(idToken) {}
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun LoadingScreen() {

    var progress by remember { mutableFloatStateOf(0f) }

    // loading bar
    // speed (lower delay = faster progress)
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(15)
            progress += 0.01f
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "ReWind Journal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
            )
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
                text = "Welcome To ReWind Journal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Capture Now. Revisit later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    println("CLICKED")
                    onSignInClick()
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Sign in with Google")
            }
        }
    }
}