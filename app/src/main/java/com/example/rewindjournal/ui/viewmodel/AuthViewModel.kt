package com.example.rewindjournal.ui.viewmodel


import androidx.lifecycle.ViewModel



import androidx.compose.runtime.mutableStateOf


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


// handles auth state using firebase
class AuthViewModel : ViewModel() {

    var isLoggedIn = mutableStateOf(false)
    var isLoading = mutableStateOf(false)

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