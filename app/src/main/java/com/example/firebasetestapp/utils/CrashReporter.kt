package com.example.firebasetestapp.utils


import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Everything the app sends to Crashlytics goes through here, so there's one place to
 * silence it, swap it out, or add filtering later.
 */
object CrashReporter {

    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    /** Call with the uid after sign-in, and with null after sign-out. */
    fun setUser(uid: String?) {
        crashlytics.setUserId(uid.orEmpty())
    }

    /** Breadcrumb attached to whatever crash happens next. Never put personal data here. */
    fun log(message: String) {
        crashlytics.log(message)
    }

    fun setKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    fun setKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Reports a caught exception as a non-fatal. Use it for failures you handled but
     * didn't expect — not for ordinary validation or wrong-password errors, which would
     * just bury the real problems in noise.
     */
    fun recordException(throwable: Throwable, vararg keys: Pair<String, String>) {
        keys.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
        crashlytics.recordException(throwable)
    }
}