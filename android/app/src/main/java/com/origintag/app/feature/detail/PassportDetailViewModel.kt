package com.origintag.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.BuildConfig
import com.origintag.app.data.model.PassportDto
import com.origintag.app.data.repository.PassportRepository
import com.origintag.app.wallet.TransactionSigner
import com.origintag.app.wallet.WalletManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Part 5c + wow — detail passport (foto, recall, riwayat servis, jual) */
@HiltViewModel
class PassportDetailViewModel @Inject constructor(
    private val repository: PassportRepository,
    private val walletManager: WalletManager,
    private val signer: TransactionSigner,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val passport: PassportDto? = null,
        val photoUrls: List<String> = emptyList(),
        val serviceRecords: List<String> = emptyList(),
        val isOwner: Boolean = false,
        val selling: Boolean = false,
        val message: String? = null,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(tokenId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.getPassport(tokenId) }
                .onSuccess { p ->
                    val isOwner =
                        p.owner?.equals(walletManager.currentAddress, ignoreCase = true) == true
                    _uiState.update { it.copy(loading = false, passport = p, isOwner = isOwner) }
                    loadPhotos(tokenId)
                    loadServiceRecords(tokenId)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message ?: "Gagal memuat") }
                }
        }
    }

    private suspend fun loadPhotos(tokenId: String) {
        runCatching { repository.getPhotoCount(tokenId) }
            .onSuccess { pc ->
                val urls = (0 until pc.count).map { i ->
                    "${BuildConfig.API_BASE_URL}items/$tokenId/photo/$i"
                }
                _uiState.update { it.copy(photoUrls = urls) }
            }
    }

    private suspend fun loadServiceRecords(tokenId: String) {
        runCatching { repository.getServiceRecords(tokenId) }
            .onSuccess { r -> _uiState.update { it.copy(serviceRecords = r.records) } }
    }

    /** Jual: approve marketplace lalu listItem (2 tx), ditandatangani wallet pemilik. */
    fun sell(tokenId: String, priceBnb: String, privateKey: String) {
        if (privateKey.isBlank()) {
            _uiState.update { it.copy(error = "Login dulu untuk menjual") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(selling = true, message = null, error = null) }
            runCatching {
                val approve = repository.approveTx(tokenId).unsignedTx ?: error("approve gagal")
                signer.signAndSend(approve.to, approve.data, privateKey)
                val list = repository.listTx(tokenId, priceBnb).unsignedTx ?: error("list gagal")
                signer.signAndSend(list.to, list.data, privateKey)
            }.onSuccess {
                _uiState.update { it.copy(selling = false, message = "Berhasil dijual di marketplace!") }
            }.onFailure { e ->
                _uiState.update { it.copy(selling = false, error = e.message ?: "Gagal menjual") }
            }
        }
    }
}
