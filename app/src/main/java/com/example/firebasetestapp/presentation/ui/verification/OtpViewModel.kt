package com.example.firebasetestapp.presentation.ui.verification

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebasetestapp.R
import com.example.firebasetestapp.repository.auth.PhoneAuthRepository
import com.example.firebasetestapp.repository.pref.UserSettingsPrefRepository
import com.example.firebasetestapp.utils.Event
import com.example.firebasetestapp.utils.PhoneVerification
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val repository: PhoneAuthRepository,
    private val userSettingsPrefRepository: UserSettingsPrefRepository
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _codeError = MutableLiveData<Int?>()
    val codeError: LiveData<Int?> = _codeError

    /** Seconds left before "resend" becomes available; 0 means it's enabled. */
    private val _secondsUntilResend = MutableLiveData(0)
    val secondsUntilResend: LiveData<Int> = _secondsUntilResend

    /** Emitted when the SMS is auto-retrieved, so the field can be filled in. */
    private val _autoFilledCode = MutableLiveData<Event<String>>()
    val autoFilledCode: LiveData<Event<String>> = _autoFilledCode

    private val _message = MutableLiveData<Event<Int>>()
    val message: LiveData<Event<Int>> = _message

    private val _verificationSuccess = MutableLiveData<Event<Unit>>()
    val verificationSuccess: LiveData<Event<Unit>> = _verificationSuccess

    private var phoneNumber: String = ""
    private var name: String? = null
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private var started = false
    private var countdownJob: Job? = null

    /** Safe to call from onViewCreated — it only sends one SMS per ViewModel. */
    fun start(activity: Activity, phoneNumber: String, name: String?) {
        if (started) return
        started = true
        this.phoneNumber = phoneNumber
        this.name = name
        sendCode(activity, resend = false)
    }

    fun onResendClicked(activity: Activity) {
        if (_secondsUntilResend.value != 0 || _loading.value == true) return
        sendCode(activity, resend = true)
    }

    fun onVerifyClicked(code: String) {
        if (_loading.value == true) return

        val trimmed = code.trim()
        val error = when {
            trimmed.isEmpty() -> R.string.otp_error_empty
            trimmed.length != CODE_LENGTH -> R.string.otp_error_length
            else -> null
        }
        _codeError.value = error
        if (error != null) return

        val id = verificationId
        if (id == null) {
            _message.value = Event(R.string.otp_error_not_sent_yet)
            return
        }

        viewModelScope.launch {
            _loading.value = true
            repository.signInWithCode(id, trimmed, name)
                .onSuccess {
                    userSettingsPrefRepository.isUserLoggedIn = true
                    _verificationSuccess.value = Event(Unit)
                }
                .onFailure { _message.value = Event(errorMessageFor(it)) }
            _loading.value = false
        }
    }

    fun clearCodeError() {
        _codeError.value = null
    }

    private fun sendCode(activity: Activity, resend: Boolean) {
        viewModelScope.launch {
            _loading.value = true
            startResendCountdown()
            repository.verifyPhoneNumber(
                activity = activity,
                phoneNumber = phoneNumber,
                resendToken = if (resend) resendToken else null
            ).collect { verification ->
                when (verification) {
                    is PhoneVerification.CodeSent -> {
                        verificationId = verification.verificationId
                        resendToken = verification.resendToken
                        _loading.value = false
                        _message.value = Event(R.string.otp_code_sent)
                    }

                    is PhoneVerification.AutoVerified -> {
                        verification.credential.smsCode?.let { _autoFilledCode.value = Event(it) }
                        signInWithCredential(verification.credential)
                    }

                    is PhoneVerification.Failed -> {
                        _loading.value = false
                        stopResendCountdown()
                        _message.value = Event(errorMessageFor(verification.exception))
                    }

                    PhoneVerification.AutoRetrievalTimeout -> _loading.value = false
                }
            }
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _loading.value = true
            repository.signInWithCredential(credential, name)
                .onSuccess {
                    userSettingsPrefRepository.isUserLoggedIn = true
                    _verificationSuccess.value = Event(Unit)
                }
                .onFailure { _message.value = Event(errorMessageFor(it)) }
            _loading.value = false
        }
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (second in RESEND_DELAY_SECONDS downTo 0) {
                _secondsUntilResend.value = second
                if (second > 0) delay(1_000)
            }
        }
    }

    private fun stopResendCountdown() {
        countdownJob?.cancel()
        _secondsUntilResend.value = 0
    }

    @StringRes
    private fun errorMessageFor(throwable: Throwable): Int = when (throwable) {
        is FirebaseAuthInvalidCredentialsException -> R.string.otp_error_invalid_code
        is FirebaseTooManyRequestsException -> R.string.otp_error_too_many_requests
        else -> R.string.register_error_generic
    }

    private companion object {
        const val CODE_LENGTH = 6
        const val RESEND_DELAY_SECONDS = 60
    }
}