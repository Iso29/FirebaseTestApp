package com.example.firebasetestapp

import android.app.Application
import com.example.firebasetestapp.utils.CrashReporter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FirebaseTestApp : Application() {

    @Inject
    lateinit var auth : FirebaseAuth

    override fun onCreate() {
        super.onCreate()

        // Crashlytics starts itself via a ContentProvider — this only decides whether it
        // actually sends anything. Without it, every crash while you're debugging lands
        // in the same dashboard as real user crashes.
        CrashReporter.setUser(auth.currentUser?.uid)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        FirebaseCrashlytics.getInstance().setCustomKey("build_type", BuildConfig.BUILD_TYPE)
    }
}