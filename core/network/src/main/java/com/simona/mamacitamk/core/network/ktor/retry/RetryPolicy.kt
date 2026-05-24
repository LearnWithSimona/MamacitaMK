package com.simona.mamacitamk.core.network.ktor.retry

/**
 *
 * The retry policy is being used to determine if and when the request should be retried if a temporary error occurred.
 */
interface RetryPolicy {
    /**
     * Determines whether the request should be retried.
     *
     * @param attempt Current retry attempt.
     * @param message The error message returned by the previous attempt.
     *
     * @return true if the request should be retried, false otherwise.
     */
    fun shouldRetry(attempt: Int, message: String?): Boolean

    /**
     * Provides a timeout used to delay the next request.
     *
     * @param attempt Current retry attempt.
     * @param message The error message returned by the previous attempt.
     *
     * @return The timeout in milliseconds before making a retry.
     */
    fun retryTimeout(attempt: Int, message: String?): Int
}
