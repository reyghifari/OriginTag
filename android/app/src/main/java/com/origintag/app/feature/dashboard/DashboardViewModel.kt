package com.origintag.app.feature.dashboard

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

/** Part 5c — dashboard "Barang Saya" (FR-09) */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: PassportRepository,
) : ViewModel() {

    companion object {
        // TODO(Part 5a): ganti dengan wallet address dari sesi login (DataStore)
        const val DEMO_WALLET = "0x0000000000000000000000000000000000000001"
    }

    data class UiState(
        val loading: Boolean = false,
        val passports: List<PassportDto> = emptyList(),
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(walletAddress: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.getPassports(walletAddress) }
                .onSuccess { list ->
                    _uiState.update { it.copy(loading = false, passports = list) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message ?: "Gagal memuat") }
                }
        }
    }
}
