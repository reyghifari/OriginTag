package com.origintag.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.feature.dashboard.DashboardViewModel
import com.origintag.app.wallet.WalletManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Part 5a — onboarding & wallet (FR-01) */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val walletManager: WalletManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val error: String? = null,
        val loggedIn: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun setLoading() = _uiState.update { it.copy(loading = true, error = null) }

    fun setError(message: String) =
        _uiState.update { it.copy(loading = false, error = message) }

    /** Dipanggil setelah Web3Auth login sukses; turunkan alamat & simpan sesi. */
    fun completeLogin(privateKey: String) {
        viewModelScope.launch {
            runCatching {
                val address = walletManager.addressFromPrivateKey(privateKey)
                walletManager.saveSession(address)
            }.onSuccess {
                _uiState.update { it.copy(loading = false, loggedIn = true) }
            }.onFailure { e ->
                setError(e.message ?: "Gagal memproses wallet")
            }
        }
    }

    /** Bypass login untuk demo saat Client ID Web3Auth belum diisi. */
    fun skipWithDemoWallet() {
        viewModelScope.launch {
            walletManager.saveSession(DashboardViewModel.DEMO_WALLET)
            _uiState.update { it.copy(loggedIn = true) }
        }
    }
}
