package com.example.firebasetestapp.repository.pref

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UserSettingsPrefRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : UserSettingsPrefRepository {
    private val prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    override var isUserLoggedIn: Boolean
        get() = prefs.getBoolean(USER_LOGGED_IN, false)
        set(value) {
            prefs.edit { putBoolean(USER_LOGGED_IN, value) }
        }

    private companion object {
        const val USER_LOGGED_IN = "user_logged_in"
    }
}