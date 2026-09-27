package com.origintag.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.BuildConfig
import com.origintag.app.data.model.PassportDto
import com.origintag.app.data.repository.PassportRepository
import com.origintag.app.ui.components.TxResult
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
        /** Harga listing aktif di marketplace (null = tidak sedang dijual). */
        val listingPriceBnb: String? = null,
        val cancelling: Boolean = false,
        val result: TxResult? = null,
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
                    loadListing(tokenId)
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

    // ponytail: ambil semua listing lalu cari tokenId; tambah endpoint GET /marketplace/:id bila listing banyak.
    private suspend fun loadListing(tokenId: String) {
        runCatching { repository.getMarketplace() }
            .onSuccess { list ->
                _uiState.update { it.copy(listingPriceBnb = list.firstOrNull { l -> l.tokenId == tokenId }?.priceBnb) }
            }
    }

    private suspend fun loadServiceRecords(tokenId: String) {
        runCatching { repository.getServiceRecords(tokenId) }
            .onSuccess { r -> _uiState.update { it.copy(serviceRecords = r.records) } }
    }

    /** Jual: approve marketplace lalu listItem (2 tx), ditandatangani wallet pemilik. */
    fun sell(tokenId: String, priceBnb: String, privateKey: String) {
        if (privateKey.isBlank()) {
            _uiState.update { it.copy(result = TxResult(false, "Belum login", "Login dulu untuk menjual.")) }
            return
        }
        val brand = _uiState.value.passport?.brand ?: "Barang"
        viewModelScope.launch {
            _uiState.update { it.copy(selling = true, result = null) }
            runCatching {
                val approve = repository.approveTx(tokenId).unsignedTx ?: error("approve gagal")
                signer.signAndSend(approve.to, approve.data, privateKey)
                val list = repository.listTx(tokenId, priceBnb).unsignedTx ?: error("list gagal")
                signer.signAndSend(list.to, list.data, privateKey)
            }.onSuccess { hash ->
                _uiState.update {
                    it.copy(
                        selling = false,
                        listingPriceBnb = priceBnb,
                        result = TxResult(true, "Berhasil dipasang!", "$brand kini tampil di Pasar seharga $priceBnb tBNB.", hash),
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(selling = false, result = TxResult(false, "Gagal memasang", e.message ?: "Gagal menjual"))
                }
            }
        }
    }

    /** Batalkan listing (hanya seller), ditandatangani wallet pemilik. */
    fun cancelListing(tokenId: String, privateKey: String) {
        if (privateKey.isBlank()) {
            _uiState.update { it.copy(result = TxResult(false, "Belum login", "Login dulu untuk membatalkan.")) }
            return
        }
        val brand = _uiState.value.passport?.brand ?: "Barang"
        viewModelScope.launch {
            _uiState.update { it.copy(cancelling = true, result = null) }
            runCatching {
                val tx = repository.cancelTx(tokenId).unsignedTx ?: error("cancel gagal")
                signer.signAndSend(tx.to, tx.data, privateKey)
            }.onSuccess { hash ->
                _uiState.update {
                    it.copy(
                        cancelling = false,
                        listingPriceBnb = null,
                        result = TxResult(true, "Listing dibatalkan", "$brand tidak lagi dijual di Pasar.", hash),
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(cancelling = false, result = TxResult(false, "Gagal membatalkan", e.message ?: "Gagal"))
                }
            }
        }
    }

    fun dismissResult() = _uiState.update { it.copy(result = null) }
}
