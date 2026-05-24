package com.simona.mamacitamk.core.network.ktor.exceptions

/**
 *
 * Thrown by various accessor methods to indicate that the response body being requested
 * does not exist. e.g., 204 (NoContent), 205 (ResetContent).
 *
 * The server has successfully fulfilled the request with the 2xx code
 * and that there is no additional content to send in the response payload body.
 *
 */
class NoContentException(
    val code: Int,
    override val message: String? =
        "The server has successfully fulfilled the request " +
            "with the code ($code) and that there is " +
            "no additional content to send in the response payload body.",
) : Throwable(message)
