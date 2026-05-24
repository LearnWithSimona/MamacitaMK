package com.simona.mamacitamk.core.ui_state

import androidx.compose.runtime.Stable

@Stable
sealed interface UIState {
    data object EmptyData : UIState
    data object Idle : UIState
    data object Loading : UIState
    data object Success : UIState
    data class Error(val message: String?) : UIState
}
