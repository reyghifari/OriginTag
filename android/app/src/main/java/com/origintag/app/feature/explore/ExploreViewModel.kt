package com.origintag.app.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.data.model.PassportDto
import com.origintag.app.data.repository.PassportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Part wow — explore/discovery semua passport terverifikasi */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: PassportRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val query: String = "",
        val items: List<PassportDto> = emptyList(),
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun setQuery(q: String) {
        _uiState.update { it.copy(query = q) }
    }

    fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val q = _uiState.value.query
            runCatching { repository.explore(brand = q, category = null, minScore = null) }
                .onSuccess { list -> _uiState.update { it.copy(loading = false, items = list.sortedByDescending { p -> p.tokenId.toLongOrNull() ?: 0 }) } }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
        }
    }
}
