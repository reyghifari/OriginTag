package com.origintag.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.feature.dashboard.DashboardViewModel
import com.origintag.app.wallet.WalletManager
import com.origintag.app.wallet.WalletManager.SessionState
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
        // true selama menunggu hasil restore sesi Web3Auth (initialize)
        val checkingSession: Boolean = true,
        val loading: Boolean = false,
        val error: String? = null,
        val loggedIn: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Ikuti status sesi Web3Auth: Checking → spinner, LoggedIn → masuk, LoggedOut → tampil login.
        viewModelScope.launch {
            walletManager.session.collect { s ->
                _uiState.update {
                    when (s) {
                        is SessionState.Checking -> it.copy(checkingSession = true)
                        is SessionState.LoggedIn -> it.copy(checkingSession = false, loggedIn = true)
                        is SessionState.LoggedOut -> it.copy(checkingSession = false, loggedIn = false)
                    }
                }
            }
        }
    }

    fun setLoading() = _uiState.update { it.copy(loading = true, error = null) }

    fun setError(message: String) =
        _uiState.update { it.copy(loading = false, error = message) }

    /** Dipanggil setelah Web3Auth login sukses; turunkan alamat & set sesi login. */
    fun completeLogin(privateKey: String, profile: WalletManager.UserProfile? = null) {
        viewModelScope.launch {
            runCatching { walletManager.addressFromPrivateKey(privateKey) }
                .onSuccess { address ->
                    _uiState.update { it.copy(loading = false) }
                    walletManager.loginWithAddress(address, profile) // memicu loggedIn via collector
                }
                .onFailure { e -> setError(e.message ?: "Gagal memproses wallet") }
        }
    }

    /** Bypass login untuk demo (sesi ini tidak persist antar cold-start). */
    fun skipWithDemoWallet() {
        walletManager.loginWithAddress(DashboardViewModel.DEMO_WALLET)
    }
}
