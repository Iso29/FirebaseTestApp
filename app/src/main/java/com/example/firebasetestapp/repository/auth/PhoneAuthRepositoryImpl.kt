package com.example.firebasetestapp.repository.auth

import android.app.Activity
import com.example.firebasetestapp.data.model.auth.UserProfile
import com.example.firebasetestapp.utils.PhoneVerification
import com.example.firebasetestapp.utils.safeCall
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class PhoneAuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : PhoneAuthRepository {

    /**
     * Starts verification. Firebase needs an Activity here (reCAPTCHA / Play Integrity),
     * so the fragment passes requireActivity() through the ViewModel.
     *
     * @param phoneNumber must be E.164, e.g. +994501234567
     */
    override fun verifyPhoneNumber(
        activity: Activity,
        phoneNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken?
    ): Flow<PhoneVerification> = callbackFlow {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                trySend(PhoneVerification.AutoVerified(credential))
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                trySend(PhoneVerification.Failed(exception))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                trySend(PhoneVerification.CodeSent(verificationId, token))
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                trySend(PhoneVerification.AutoRetrievalTimeout)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(AUTO_RETRIEVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .also { builder -> resendToken?.let(builder::setForceResendingToken) }
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

        // Firebase has no way to cancel a request; the callbacks just stop being collected.
        awaitClose { }
    }

    override suspend fun signInWithCode(
        verificationId: String,
        code: String,
        name: String?
    ): Result<UserProfile> =
        signInWithCredential(PhoneAuthProvider.getCredential(verificationId, code), name)

    /**
     * Signs in and, for a brand new account, writes users/{uid}.
     * Existing users keep whatever profile they already have.
     */

    override suspend fun signInWithCredential(
        credential: PhoneAuthCredential,
        name: String?
    ): Result<UserProfile> = safeCall {
        val result = auth.signInWithCredential(credential).await()
        val user = requireNotNull(result.user) { "Firebase returned no user" }
        val isNewUser = result.additionalUserInfo?.isNewUser == true

        val profile = UserProfile(
            uid = user.uid,
            name = name.orEmpty(),
            email = user.email.orEmpty(),
            phone = user.phoneNumber
        )

        if (isNewUser) {
            try {
                firestore.collection(USERS).document(user.uid).set(profile).await()
                if (!name.isNullOrBlank()) {
                    user.updateProfile(userProfileChangeRequest { displayName = name }).await()
                }
            } catch (e: Exception) {
                runCatching { user.delete().await() }
                throw e
            }
        }

        profile
    }

    private companion object {
        const val USERS = "users"
        const val AUTO_RETRIEVAL_TIMEOUT_SECONDS = 60L
    }
}
