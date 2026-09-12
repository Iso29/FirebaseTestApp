package com.example.firebasetestapp.utils

import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

/** What Firebase reports back while verifying a number. */
sealed interface PhoneVerification {
    /** SMS is on its way — keep the id and token for verifying and resending. */
    data class CodeSent(
        val verificationId: String,
        val resendToken: PhoneAuthProvider.ForceResendingToken
    ) : PhoneVerification

    /** Instant verification or SMS auto-retrieval: no code entry needed. */
    data class AutoVerified(val credential: PhoneAuthCredential) : PhoneVerification

    data class Failed(val exception: FirebaseException) : PhoneVerification

    /** Auto-retrieval window closed; the user has to type the code. */
    data object AutoRetrievalTimeout : PhoneVerification
}