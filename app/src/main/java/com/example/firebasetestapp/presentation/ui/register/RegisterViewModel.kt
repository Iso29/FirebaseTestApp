package com.example.firebasetestapp.presentation.ui.register

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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userSettingsPrefRepository: UserSettingsPrefRepository,
    private val repository: AuthRepository
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _nameError = MutableLiveData<Int?>()
    val nameError: LiveData<Int?> = _nameError

    private val _emailError = MutableLiveData<Int?>()
    val emailError: LiveData<Int?> = _emailError

    private val _phoneError = MutableLiveData<Int?>()
    val phoneError: LiveData<Int?> = _phoneError

    private val _passwordError = MutableLiveData<Int?>()
    val passwordError: LiveData<Int?> = _passwordError

    private val _confirmPasswordError = MutableLiveData<Int?>()
    val confirmPasswordError: LiveData<Int?> = _confirmPasswordError

    /** Wrapped in Event so a Snackbar isn't shown again after rotation. */
    private val _message = MutableLiveData<Event<Int>>()
    val message: LiveData<Event<Int>> = _message

    private val _registerSuccess = MutableLiveData<Event<Unit>>()
    val registerSuccess: LiveData<Event<Unit>> = _registerSuccess

    private val _navigateToOtp = MutableLiveData<Event<String>>()
    val navigateToOtp: LiveData<Event<String>> = _navigateToOtp

    fun register(
        name: String,
        identifier: String,
        password: String,
        confirmPassword: String,
        termsAccepted: Boolean,
        isEmailMode: Boolean
    ) {
        if (_loading.value == true) return
        if (!isFormValid(name, identifier, password, confirmPassword, isEmailMode)) return

        if (!termsAccepted) {
            _message.value = Event(R.string.register_error_terms)
            return
        }

        if (!isEmailMode) {
            _navigateToOtp.value = Event("+994" + identifier.filter(Char::isDigit))
            return
        }

        viewModelScope.launch {
            _loading.value = true

            repository.registerWithEmail(
                name = name.trim(),
                email = identifier.trim(),
                password = password
            ).onSuccess {
                userSettingsPrefRepository.isUserLoggedIn = true
                _registerSuccess.value = Event(Unit)
            }.onFailure {
                _message.value = Event(errorMessageFor(it))
            }

            _loading.value = false
        }
    }

    // Called while the user types, so the error disappears as soon as they fix it.
    fun clearNameError() { _nameError.value = null }
    fun clearEmailError() { _emailError.value = null }
    fun clearPhoneError() { _phoneError.value = null }

    fun clearPasswordError() {
        _passwordError.value = null
        _confirmPasswordError.value = null
    }

    fun clearConfirmPasswordError() { _confirmPasswordError.value = null }

    private fun isFormValid(
        name: String,
        identifier: String,
        password: String,
        confirmPassword: String,
        isEmailMode: Boolean
    ): Boolean {
        val nameError = validateName(name)
        _nameError.value = nameError

        val identifierError = if (isEmailMode) validateEmail(identifier) else validatePhone(identifier)
        if (isEmailMode) _emailError.value = identifierError else _phoneError.value = identifierError

        val passwordError = validatePassword(password)
        _passwordError.value = passwordError

        val confirmError = validateConfirmPassword(password, confirmPassword)
        _confirmPasswordError.value = confirmError

        return nameError == null && identifierError == null &&
                passwordError == null && confirmError == null
    }

    @StringRes
    private fun validateName(name: String): Int? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> R.string.register_error_name_empty
            trimmed.length < MIN_NAME_LENGTH -> R.string.register_error_name_short
            else -> null
        }
    }

    @StringRes
    private fun validateEmail(email: String): Int? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> R.string.register_error_email_empty
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> R.string.register_error_email_invalid
            else -> null
        }
    }

    @StringRes
    private fun validatePhone(phone: String): Int? {
        val digits = phone.filter(Char::isDigit)
        return when {
            digits.isEmpty() -> R.string.register_error_phone_empty
            digits.length != PHONE_LENGTH -> R.string.register_error_phone_invalid
            else -> null
        }
    }

    @StringRes
    private fun validatePassword(password: String): Int? = when {
        password.isEmpty() -> R.string.register_error_password_empty
        password.length < MIN_PASSWORD_LENGTH -> R.string.register_error_password_short
        else -> null
    }

    @StringRes
    private fun validateConfirmPassword(password: String, confirm: String): Int? = when {
        confirm.isEmpty() -> R.string.register_error_confirm_empty
        confirm != password -> R.string.register_error_password_mismatch
        else -> null
    }

    @StringRes
    private fun errorMessageFor(throwable: Throwable): Int = when (throwable) {
        // WeakPassword extends InvalidCredentials, so it has to be checked first.
        is FirebaseAuthWeakPasswordException -> R.string.register_error_weak_password
        is FirebaseAuthUserCollisionException -> R.string.register_error_email_in_use
        is FirebaseAuthInvalidCredentialsException -> R.string.register_error_email_invalid
        is FirebaseNetworkException -> R.string.register_error_network
        else -> R.string.register_error_generic
    }

    private companion object {
        const val MIN_NAME_LENGTH = 3
        const val MIN_PASSWORD_LENGTH = 6
        const val PHONE_LENGTH = 9
    }
}