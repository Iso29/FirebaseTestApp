package com.example.firebasetestapp.repository.auth

import com.example.firebasetestapp.data.model.auth.UserProfile
import com.example.firebasetestapp.utils.safeCall
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firestore document at users/{uid}.
 * Default values on every field are required — Firestore needs a no-arg constructor to deserialize.
 */

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override fun checkIsEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    private val currentUserId: String? get() = auth.currentUser?.uid

    /**
     * Creates the Auth account, then the users/{uid} document.
     * If the Firestore write fails the Auth account is deleted again, so you never end up with
     * an account that has no profile.
     */
    override suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<UserProfile> = safeCall {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = requireNotNull(authResult.user) { "Firebase returned no user" }
        val profile = UserProfile(uid = user.uid, name = name, email = email)

        try {
            firestore.collection(USERS)
                .document(user.uid)
                .set(profile)
                .await()
            user.updateProfile(userProfileChangeRequest { displayName = name }).await()
            user.sendEmailVerification().await()
        } catch (e: Exception) {
            runCatching { user.delete().await() }
            throw e
        }

        profile
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<String> =
        safeCall {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            requireNotNull(result.user).uid
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = safeCall {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun getProfile(uid: String): Result<UserProfile?> = safeCall {
        firestore.collection(USERS)
            .document(uid)
            .get()
            .await()
            .toObject(UserProfile::class.java)
    }

    override fun signOut() = auth.signOut()


    private companion object {
        const val USERS = "users"
    }
}