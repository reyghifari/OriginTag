package com.origintag.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.data.model.PassportDto
import com.origintag.app.data.model.StatsDto
import com.origintag.app.data.repository.PassportRepository
import com.origintag.app.wallet.WalletManager
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
    private val walletManager: WalletManager,
) : ViewModel() {

    companion object {
        // Wallet demo untuk mode "Lewati" saat Client ID Web3Auth belum diisi
        const val DEMO_WALLET = "0x0000000000000000000000000000000000000001"
    }

    data class UiState(
        val loading: Boolean = false,
        val walletAddress: String? = null,
        val passports: List<PassportDto> = emptyList(),
        val error: String? = null,
        val name: String? = null,
        val avatar: String? = null,
        val stats: StatsDto? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Muat passport milik wallet aktif dari sesi (FR-09). */
    fun load() {
        viewModelScope.launch {
            val address = walletManager.currentAddress ?: DEMO_WALLET
            val profile = walletManager.currentProfile
            _uiState.update {
                it.copy(loading = true, error = null, walletAddress = address, name = profile?.name, avatar = profile?.profileImage)
            }
            launch {
                runCatching { repository.getStats(address) }.onSuccess { s -> _uiState.update { it.copy(stats = s) } }
            }
            runCatching { repository.getPassports(address) }
                .onSuccess { list ->
                    _uiState.update { it.copy(loading = false, passports = list) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message ?: "Gagal memuat") }
                }
        }
    }
}
