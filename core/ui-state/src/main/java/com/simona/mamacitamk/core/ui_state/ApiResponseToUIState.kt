@file:OptIn(ExperimentalContracts::class)

package com.simona.mamacitamk.core.ui_state

import com.simona.mamacitamk.core.network.ktor.ApiResponse
import com.simona.mamacitamk.core.network.ktor.getOrNull
import com.simona.mamacitamk.core.network.ktor.messageOrNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private fun <T> ApiResponse<T>.asUIState(): UIState = when (this) {
    is ApiResponse.Success -> handleSuccessResponse()
    is ApiResponse.Failure.Error -> handleErrorResponse()
    is ApiResponse.Failure.Exception -> handleExceptionResponse()
}

private fun <T> ApiResponse.Success<T>.handleSuccessResponse(): UIState = if (data == null) {
    UIState.EmptyData
} else {
    UIState.Success
}

private fun ApiResponse.Failure.Error.handleErrorResponse(): UIState = UIState.Error(messageOrNull)

private fun ApiResponse.Failure.Exception.handleExceptionResponse(): UIState =
    UIState.Error(throwable.message)

fun <T> ApiResponse<T>.asUIStateHolder(): UIStateHolder<T> =
    UIStateHolder(
        uiState = asUIState(),
        payload = getOrNull(),
    )

inline fun <T> UIStateHolder<T>.onSuccess(crossinline onResult: (T) -> Unit): UIStateHolder<T> {
    contract { callsInPlace(onResult, InvocationKind.AT_MOST_ONCE) }
    if (isSuccess) {
        payload?.let(onResult)
    }
    return this
}
