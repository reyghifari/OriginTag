package com.origintag.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.data.model.BalanceDto
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

/** Part wow — halaman profil user */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: PassportRepository,
    private val walletManager: WalletManager,
) : ViewModel() {

    data class UiState(
        val address: String = "",
        val name: String? = null,
        val email: String? = null,
        val profileImage: String? = null,
        val stats: StatsDto? = null,
        val balance: BalanceDto? = null,
        val loading: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        val address = walletManager.currentAddress ?: return
        val profile = walletManager.currentProfile
        _uiState.update {
            it.copy(
                address = address,
                name = profile?.name,
                email = profile?.email,
                profileImage = profile?.profileImage,
                loading = true,
            )
        }
        viewModelScope.launch {
            val stats = runCatching { repository.getStats(address) }.getOrNull()
            val balance = runCatching { repository.getBalance(address) }.getOrNull()
            _uiState.update { it.copy(stats = stats, balance = balance, loading = false) }
        }
    }

    fun logout() = walletManager.logout()
}
