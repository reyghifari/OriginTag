package com.origintag.app.feature.detail

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

/** Part 5c — detail passport */
@HiltViewModel
class PassportDetailViewModel @Inject constructor(
    private val repository: PassportRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val passport: PassportDto? = null,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(tokenId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.getPassport(tokenId) }
                .onSuccess { p -> _uiState.update { it.copy(loading = false, passport = p) } }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message ?: "Gagal memuat") }
                }
        }
    }
}
