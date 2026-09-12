package com.example.firebasetestapp.repository.auth

import com.example.firebasetestapp.data.model.auth.UserProfile

interface AuthRepository {

    fun checkIsEmailVerified() : Boolean
    suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<UserProfile>

    suspend fun loginWithEmail(email: String, password: String): Result<String>

    suspend fun getProfile(uid: String): Result<UserProfile?>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    fun signOut()
}