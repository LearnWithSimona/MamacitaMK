package com.simona.mamacitamk.core.network.ktor

import kotlinx.coroutines.CancellationException

/**
 *
 * ApiResponse is an interface for constructing standard responses from the ktor call.
 */
sealed interface ApiResponse<out T> {
    /**
     *
     * API Success response class from OkHttp request call.
     * The [data] is a nullable generic type. (A response without data)

     * @property data The de-serialized response body of a successful data.
     * @property tag An additional value that can be held to distinguish the origin of the [data] or to facilitate post-processing of successful data.
     */
    data class Success<T>(val data: T, val tag: Any? = null) : ApiResponse<T>

    /**
     *
     * API Failure response class from OkHttp request call.
     * There are two subtypes: [ApiResponse.Failure.Error] and [ApiResponse.Failure.Exception].
     */
    sealed interface Failure<T> : ApiResponse<T> {
        /**
         * API response error case.
         * API communication conventions do not match or applications need to handle errors.
         * e.g., internal server error.
         *
         * @property payload An error payload that can contain detailed error information.
         */
        open class Error(val payload: Any?) : Failure<Nothing> {
            override fun equals(other: Any?): Boolean =
                other is Error &&
                    payload == other.payload

            override fun hashCode(): Int {
                var result = 17
                result = 31 * result + payload.hashCode()
                return result
            }

            override fun toString(): String = payload.toString()
        }

        /**
         *
         * API request Exception case.
         * An unexpected exception occurs while creating requests or processing a response in the client side.
         * e.g., network connection error, timeout.
         *
         * @param throwable A throwable exception.
         *
         * @property message The localized message from the exception.
         */
        open class Exception(val throwable: Throwable) : Failure<Nothing> {
            val message: String? = throwable.message

            override fun equals(other: Any?): Boolean =
                other is Exception &&
                    throwable == other.throwable

            override fun hashCode(): Int {
                var result = 17
                result = 31 * result + throwable.hashCode()
                return result
            }

            override fun toString(): String = message.orEmpty()
        }
    }

    companion object {
        /**
         *
         * [Failure] factory function. Only receives [Throwable] as an argument.
         *
         * @param ex A throwable.
         *
         * @return A [ApiResponse.Failure.Exception] based on the throwable.
         */
        fun exception(ex: Throwable): Failure.Exception = Failure.Exception(ex)

        /**
         *
         * ApiResponse Factory.
         *
         * Create an [ApiResponse] from the given executable [f].
         *
         * If the [f] doesn't throw any exceptions, it creates [ApiResponse.Success].
         * If the [f] throws an exception, it creates [ApiResponse.Failure.Exception].
         */
        inline fun <reified T> of(tag: Any? = null, crossinline f: () -> T): ApiResponse<T> =
            try {
                val result = f()
                Success(
                    data = result,
                    tag = tag,
                )
            } catch (e: Exception) {
                Failure.Exception(e)
            }

        /**
         *
         * ApiResponse Factory.
         *
         * Create an [ApiResponse] from the given executable [f].
         *
         * If the [f] doesn't throw any exceptions, it creates [ApiResponse.Success].
         * If the [f] throws an exception, it creates [ApiResponse.Failure.Exception].
         */
        @Throws(CancellationException::class)
        suspend inline fun <reified T> suspendOf(
            tag: Any? = null,
            crossinline f: suspend () -> T,
        ): ApiResponse<T> =
            try {
                val result = f()
                Success(
                    data = result,
                    tag = tag,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Failure.Exception(e)
            }
    }
}
