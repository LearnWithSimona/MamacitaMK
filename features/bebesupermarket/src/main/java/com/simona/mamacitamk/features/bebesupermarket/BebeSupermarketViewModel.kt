package com.simona.mamacitamk.features.bebesupermarket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simona.mamacitamk.core.ui_state.UIState
import com.simona.mamacitamk.core.ui_state.UIStateHolder
import com.simona.mamacitamk.data.bebesupermarket.BebeSupermarketRepository
import com.simona.mamacitamk.domain.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BebeSupermarketViewModel @Inject constructor(
    private val repository: BebeSupermarketRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UIStateHolder<List<Product>>())
    val state: StateFlow<UIStateHolder<List<Product>>> = _state.asStateFlow()

    fun load(limit: Int = 40) {
        if (_state.value.isLoading) return
        _state.value = UIStateHolder(uiState = UIState.Loading)
        viewModelScope.launch {
            runCatching { repository.scrapeAll(concurrency = 4, limit = limit) }
                .onSuccess { products ->
                    _state.value = if (products.isEmpty()) {
                        UIStateHolder(uiState = UIState.EmptyData)
                    } else {
                        UIStateHolder(uiState = UIState.Success, payload = products)
                    }
                }
                .onFailure {
                    _state.value = UIStateHolder(uiState = UIState.Error(it.message))
                }
        }
    }
}
