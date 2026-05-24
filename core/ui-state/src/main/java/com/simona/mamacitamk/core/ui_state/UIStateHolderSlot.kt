package com.simona.mamacitamk.core.ui_state

import androidx.compose.runtime.Composable

@Composable
fun <T> UIStateHolder<T>.RenderContent(
    loadingContent: (@Composable () -> Unit)? = null,
    idleContent: (@Composable () -> Unit)? = null,
    emptyDataContent: (@Composable () -> Unit)? = null,
    onErrorContent: (@Composable UIStateHolder<T>.(UIState.Error) -> Unit)? = null,
    successContent: (@Composable (T) -> Unit)? = null,
) {
    when (uiState) {
        is UIState.EmptyData -> emptyDataContent?.invoke()
        is UIState.Error -> onErrorContent?.invoke(this, uiState)
        UIState.Idle -> idleContent?.invoke()
        UIState.Loading -> loadingContent?.invoke()
        UIState.Success -> payload?.let { successContent?.invoke(it) }
    }
}

fun <T> UIStateHolder<T>.handleState(
    loading: (() -> Unit)? = null,
    idle: (() -> Unit)? = null,
    emptyData: (() -> Unit)? = null,
    onError: (UIStateHolder<T>.(UIState.Error) -> Unit)? = null,
    success: ((T) -> Unit)? = null,
) {
    when (uiState) {
        is UIState.EmptyData -> emptyData?.invoke()
        is UIState.Error -> onError?.invoke(this, uiState)
        UIState.Idle -> idle?.invoke()
        UIState.Loading -> loading?.invoke()
        UIState.Success -> payload?.let { success?.invoke(it) }
    }
}
