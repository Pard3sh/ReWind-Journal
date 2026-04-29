package com.example.rewindjournal.ui.viewmodel


import androidx.lifecycle.ViewModel



import androidx.compose.runtime.mutableStateOf


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// handles auth state using firebase
class AuthViewModel : ViewModel() {

    var isLoggedIn = mutableStateOf(false)
    var isLoading = mutableStateOf(true)

    init {
        checkAuthState()
    }

    private fun checkAuthState() {

        viewModelScope.launch {

            isLoading.value = true

            delay(2000)

            val user = FirebaseAuth.getInstance().currentUser

            isLoggedIn.value = user != null
            isLoading.value = false
        }
    }

    fun firebaseAuthWithGoogle(idToken: String, onComplete: () -> Unit) {

        isLoading.value = true

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        FirebaseAuth.getInstance()
            .signInWithCredential(credential)
            .addOnCompleteListener { task ->

                isLoading.value = false

                if (task.isSuccessful) {
                    isLoggedIn.value = true
                    onComplete()
                }
            }
    }
}