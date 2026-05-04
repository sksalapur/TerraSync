package com.terrasync.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.terrasync.app.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {

    override fun currentUser(): FirebaseUser? = auth.currentUser

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result     = auth.signInWithCredential(credential).await()
        checkNotNull(result.user) { "Firebase returned null user after Google sign-in" }
    }

    override suspend fun signOut() = auth.signOut()
}
