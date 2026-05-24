package com.simona.mamacitamk.features.libertabebecentar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simona.mamacitamk.core.ui_state.UIState
import com.simona.mamacitamk.core.ui_state.UIStateHolder
import com.simona.mamacitamk.data.libertabebecentar.FirestoreLibertaBebeCentarRepository
import com.simona.mamacitamk.domain.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LibertaBebeCentarViewModel @Inject constructor(
    repository: FirestoreLibertaBebeCentarRepository,
) : ViewModel() {

    val state: StateFlow<UIStateHolder<List<Product>>> = repository.observe()
        .map { products ->
            if (products.isEmpty()) {
                UIStateHolder(uiState = UIState.EmptyData)
            } else {
                UIStateHolder(uiState = UIState.Success, payload = products)
            }
        }
        .onStart { emit(UIStateHolder(uiState = UIState.Loading)) }
        .catch { emit(UIStateHolder(uiState = UIState.Error(it.message))) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = UIStateHolder(),
        )
}
