package com.example.firebasetestapp.repository.auth

import android.app.Activity
import com.example.firebasetestapp.data.model.auth.UserProfile
import com.example.firebasetestapp.utils.PhoneVerification
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.Flow

interface PhoneAuthRepository {
    fun verifyPhoneNumber(
        activity: Activity,
        phoneNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null
    ): Flow<PhoneVerification>

    suspend fun signInWithCode(
        verificationId: String,
        code: String,
        name: String?
    ): Result<UserProfile>

    suspend fun signInWithCredential(
        credential: PhoneAuthCredential,
        name: String?
    ): Result<UserProfile>
}