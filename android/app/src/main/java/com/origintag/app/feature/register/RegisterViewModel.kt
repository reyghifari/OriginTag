package com.origintag.app.feature.register

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.data.model.RegisterResponse
import com.origintag.app.data.repository.PassportRepository
import com.origintag.app.feature.dashboard.DashboardViewModel
import com.origintag.app.wallet.WalletManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Part 5b — registrasi barang (FR-02, FR-03, FR-04) */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: PassportRepository,
    private val walletManager: WalletManager,
) : ViewModel() {

    /** Tahap yang ditampilkan ke user selama proses berjalan */
    enum class Phase { IDLE, SUBMITTING, MINTED, FLAGGED, ERROR }

    data class UiState(
        val photos: List<Uri> = emptyList(),
        val phase: Phase = Phase.IDLE,
        val result: RegisterResponse? = null,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun setPhotos(uris: List<Uri>) {
        _uiState.update { it.copy(photos = uris.take(5)) }
    }

    fun submit(
        brand: String,
        category: String,
        serialNumber: String,
        purchaseDate: String,
        warrantyDurationDays: Int,
    ) {
        val photos = _uiState.value.photos
        if (photos.isEmpty()) {
            _uiState.update { it.copy(phase = Phase.ERROR, error = "Tambahkan minimal 1 foto") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(phase = Phase.SUBMITTING, error = null) }
            val owner = walletManager.currentAddress ?: DashboardViewModel.DEMO_WALLET
            runCatching {
                repository.registerItem(
                    photoUris = photos,
                    brand = brand,
                    category = category,
                    serialNumber = serialNumber,
                    purchaseDate = purchaseDate,
                    warrantyDurationDays = warrantyDurationDays,
                    ownerAddress = owner,
                )
            }.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        phase = if (res.status == "minted") Phase.MINTED else Phase.FLAGGED,
                        result = res,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(phase = Phase.ERROR, error = e.message ?: "Gagal registrasi")
                }
            }
        }
    }
}
