package com.simona.mamacitamk.core.network.ktor.mappers

import com.simona.mamacitamk.core.network.ktor.ApiResponse

/**
 *
 * A mapper interface for mapping [ApiResponse.Failure.Error] response as a custom [V] instance model.
 *
 */
fun interface ApiErrorModelMapper<V> {
    /**
     * maps the [ApiResponse.Failure.Error] to the [V] using the mapper.
     *
     * @param apiErrorResponse The [ApiResponse.Failure.Error] error response from the network request.
     * @return A custom [V] error response model.
     */
    fun map(apiErrorResponse: ApiResponse.Failure.Error): V
}
