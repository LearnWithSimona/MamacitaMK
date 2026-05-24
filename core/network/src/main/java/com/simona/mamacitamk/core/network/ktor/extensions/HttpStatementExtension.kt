package com.simona.mamacitamk.core.network.ktor.extensions

import com.simona.mamacitamk.core.network.ktor.ApiResponse
import io.ktor.client.statement.HttpStatement

/**
 * Executes this statement and download the response. After the method execution finishes,
 * the client downloads the response body in memory and release the connection.
 *
 * @return [ApiResponse]
 */
suspend inline fun <reified T> HttpStatement.executeApiResponse(): ApiResponse<T> {
    val response = execute()
    return apiResponseOf { response }
}
