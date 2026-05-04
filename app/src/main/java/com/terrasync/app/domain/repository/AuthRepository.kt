package com.terrasync.app.domain.repository

import com.google.firebase.auth.FirebaseUser

/**
 * Domain-layer contract for authentication operations.
 */
interface AuthRepository {
    fun currentUser(): FirebaseUser?

    /** Exchange a Google ID token for a Firebase session. */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser>

    suspend fun signOut()
}

