package com.simona.mamacitamk.core.network.ktor.mappers

import com.simona.mamacitamk.core.network.ktor.ApiResponse

/**
 *
 * A mapper interface for mapping [ApiResponse] response as another type of [ApiResponse] .
 */
fun interface ApiResponseMapper<T, V> {
    /**
     * maps the [T] type of [ApiResponse] to the [V] type of [ApiResponse] using the mapper.
     *
     * @param apiResponse The [ApiResponse] error response from the network request.
     * @return Another type of the [ApiResponse].
     */
    fun map(apiResponse: ApiResponse<T>): ApiResponse<V>
}
