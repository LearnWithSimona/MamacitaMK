package com.simona.mamacitamk.core.network.ktor

object StatusCodeExtensions {
    /**
     *
     * determines the success code range of network responses.
     *
     * if a network request is successful and the response code is in the [successCodeRange],
     * its response will be a [ApiResponse.Success].
     *
     * if a network request is successful but out of the [successCodeRange] or failure,
     * the response will be a [ApiResponse.Failure.Error].
     * */
    val successCodeRange: IntRange = 200..299
}
