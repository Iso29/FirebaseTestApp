package com.example.firebasetestapp.utils

/**
 * Wrapper for LiveData values that should only be consumed once — Snackbars, navigation,
 * toasts. Without it, rotating the screen re-delivers the last value and the message
 * shows up a second time.
 */
open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? = if (hasBeenHandled) {
        null
    } else {
        hasBeenHandled = true
        content
    }

    fun peekContent(): T = content
}