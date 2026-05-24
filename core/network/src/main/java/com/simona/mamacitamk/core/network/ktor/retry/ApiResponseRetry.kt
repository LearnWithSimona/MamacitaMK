package com.simona.mamacitamk.core.network.ktor.retry

import com.simona.mamacitamk.core.network.ktor.ApiResponse
import com.simona.mamacitamk.core.network.ktor.messageOrNull
import kotlinx.coroutines.delay

/**
 *
 * Run the [task] and retry if the result of [task] is failure following the [retryPolicy].
 *
 * @param retryPolicy A policy that determines whether retry the [task] or not.
 * @param task A task that you should run and retry. The default 'attempt' parameter starts from 1,
 * and the 'reason' parameter represents the error message if the [task] is failed. If the [task]
 * succeeds, it will be null.
 */
suspend fun <T : Any> runAndRetry(
    retryPolicy: RetryPolicy,
    task: suspend (attempt: Int, reason: String?) -> ApiResponse<T>,
): ApiResponse<T> {
    var attempt = 1
    var reason: String? = null
    var apiResponse: ApiResponse<T>
    while (true) {
        apiResponse = task(attempt, reason)
        when (apiResponse) {
            is ApiResponse.Success -> {
                break
            }

            is ApiResponse.Failure -> {
                reason = apiResponse.messageOrNull
                val shouldRetry = retryPolicy.shouldRetry(attempt, reason)
                val timeout = retryPolicy.retryTimeout(attempt, reason)

                if (shouldRetry) {
                    delay(timeout.toLong())
                    attempt += 1
                } else {
                    break
                }
            }
        }
    }

    return apiResponse
}
