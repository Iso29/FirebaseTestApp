package com.example.firebasetestapp.utils

import kotlin.coroutines.cancellation.CancellationException

/** Like runCatching, but lets coroutine cancellation through instead of turning it into a failure. */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}