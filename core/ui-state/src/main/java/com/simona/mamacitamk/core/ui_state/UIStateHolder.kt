package com.simona.mamacitamk.core.ui_state

import androidx.compose.runtime.Stable

@Stable
data class UIStateHolder<T>(internal val uiState: UIState = UIState.Idle, val payload: T? = null) {
    val isDataLoaded: Boolean get() = payload != null
    val isDataNotLoaded: Boolean get() = payload == null

    val isError: Boolean get() = uiState is UIState.Error
    val isSuccess: Boolean get() = uiState is UIState.Success && payload != null

    val isEmptyData: Boolean get() = uiState is UIState.EmptyData || payload == null
    val isNotEmpty: Boolean get() = !isEmptyData

    val isInitialLoad: Boolean get() = uiState is UIState.Idle

    val isLoading: Boolean get() = uiState is UIState.Loading
    val isNotLoading: Boolean get() = !isLoading

    fun asSuccess(): T? = if (isSuccess) payload else null
}
