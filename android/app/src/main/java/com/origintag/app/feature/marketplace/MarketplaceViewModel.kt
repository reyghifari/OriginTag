package com.origintag.app.feature.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.data.model.MarketplaceListingDto
import com.origintag.app.data.repository.PassportRepository
import com.origintag.app.ui.components.TxResult
import com.origintag.app.wallet.TransactionSigner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Part wow — marketplace (browse + beli) */
@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val repository: PassportRepository,
    private val signer: TransactionSigner,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val listings: List<MarketplaceListingDto> = emptyList(),
        val buyingTokenId: String? = null,
        val result: TxResult? = null,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.getMarketplace() }
                .onSuccess { list -> _uiState.update { it.copy(loading = false, listings = list) } }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
        }
    }

    /** Beli passport: ambil buy-tx, tanda tangani (bayar harga), refresh. */
    fun buy(tokenId: String, privateKey: String) {
        if (privateKey.isBlank()) {
            _uiState.update { it.copy(result = TxResult(false, "Belum login", "Login dulu untuk membeli.")) }
            return
        }
        val brand = _uiState.value.listings.firstOrNull { it.tokenId == tokenId }?.brand ?: "Barang"
        viewModelScope.launch {
            _uiState.update { it.copy(buyingTokenId = tokenId, result = null) }
            runCatching {
                val resp = repository.buyTx(tokenId)
                val tx = resp.unsignedTx ?: error("Item tidak dijual")
                signer.signAndSend(tx.to, tx.data, privateKey, tx.value) // menunggu tx masuk blok
            }.onSuccess { hash ->
                _uiState.update { s ->
                    s.copy(
                        buyingTokenId = null,
                        listings = s.listings.filterNot { it.tokenId == tokenId },
                        result = TxResult(
                            true,
                            "Pembelian berhasil!",
                            "$brand kini milikmu. Riwayat kepemilikan sudah tercatat on-chain.",
                            hash,
                            tokenId,
                        ),
                    )
                }
                load()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(buyingTokenId = null, result = TxResult(false, "Pembelian gagal", e.message ?: "Gagal beli"))
                }
            }
        }
    }

    fun dismissResult() = _uiState.update { it.copy(result = null) }
}
