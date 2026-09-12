package com.example.firebasetestapp.presentation.ui.login

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebasetestapp.R
import com.example.firebasetestapp.repository.auth.AuthRepository
import com.example.firebasetestapp.repository.pref.UserSettingsPrefRepository
import com.example.firebasetestapp.utils.Event
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userSettingsPrefRepository: UserSettingsPrefRepository,
    private val repository: AuthRepository
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _emailError = MutableLiveData<Int?>()
    val emailError: LiveData<Int?> = _emailError

    private val _phoneError = MutableLiveData<Int?>()
    val phoneError: LiveData<Int?> = _phoneError

    private val _passwordError = MutableLiveData<Int?>()
    val passwordError: LiveData<Int?> = _passwordError

    private val _message = MutableLiveData<Event<Int>>()
    val message: LiveData<Event<Int>> = _message

    private val _loginSuccess = MutableLiveData<Event<Unit>>()
    val loginSuccess: LiveData<Event<Unit>> = _loginSuccess

    /** Carries the phone number in E.164 form to the OTP screen. */
    private val _navigateToOtp = MutableLiveData<Event<String>>()
    val navigateToOtp: LiveData<Event<String>> = _navigateToOtp

    fun login(identifier: String, password: String, isEmailMode: Boolean) {
        if (_loading.value == true) return

        if (!isEmailMode) {
            // Phone login is code-based — no password involved.
            val phoneError = validatePhone(identifier)
            _phoneError.value = phoneError
            if (phoneError != null) return

            _navigateToOtp.value = Event(COUNTRY_CODE + identifier.filter(Char::isDigit))
            return
        }

        val emailError = validateEmail(identifier)
        _emailError.value = emailError

        val passwordError = validatePassword(password)
        _passwordError.value = passwordError

        if (emailError != null || passwordError != null) return

        viewModelScope.launch {
            _loading.value = true
            val result = repository.loginWithEmail(identifier.trim(), password)
            _loading.value = false

            result.onSuccess {
                if (!repository.checkIsEmailVerified()) {
                    repository.signOut()
                    _message.value = Event(R.string.login_error_email_not_verified)
                } else {
                    userSettingsPrefRepository.isUserLoggedIn = true
                    _loginSuccess.value = Event(Unit)
                }
            }.onFailure {
                _message.value = Event(errorMessageFor(it))
            }
        }
    }

    fun onForgotPasswordClicked(email: String) {
        if (_loading.value == true) return

        val error = validateEmail(email)
        _emailError.value = error
        if (error != null) {
            _message.value = Event(R.string.login_reset_needs_email)
            return
        }

        viewModelScope.launch {
            _loading.value = true
            repository.sendPasswordReset(email)
                .onSuccess { _message.value = Event(R.string.login_reset_sent) }
                .onFailure { _message.value = Event(errorMessageFor(it)) }
            _loading.value = false
        }
    }

    fun clearEmailError() { _emailError.value = null }
    fun clearPhoneError() { _phoneError.value = null }
    fun clearPasswordError() { _passwordError.value = null }

    @StringRes
    private fun validateEmail(email: String): Int? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> R.string.login_error_email_empty
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> R.string.login_error_email_invalid
            else -> null
        }
    }

    @StringRes
    private fun validatePhone(phone: String): Int? {
        val digits = phone.filter(Char::isDigit)
        return when {
            digits.isEmpty() -> R.string.login_error_phone_empty
            digits.length != PHONE_LENGTH -> R.string.login_error_phone_invalid
            else -> null
        }
    }

    @StringRes
    private fun validatePassword(password: String): Int? = when {
        password.isEmpty() -> R.string.login_error_password_empty
        else -> null
    }

    @StringRes
    private fun errorMessageFor(throwable: Throwable): Int = when (throwable) {
        // With email enumeration protection on (the default), Firebase returns the same
        // error for "no such user" and "wrong password", so both map to one message.
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> R.string.login_error_invalid_credentials

        is FirebaseTooManyRequestsException -> R.string.login_error_too_many_requests
        is FirebaseNetworkException -> R.string.login_error_network
        else -> R.string.login_error_generic
    }

    private companion object {
        const val COUNTRY_CODE = "+994"
        const val PHONE_LENGTH = 9

        /** Flip to true to block sign-in until the user clicks the verification link. */
        const val REQUIRE_VERIFIED_EMAIL = false
    }
}