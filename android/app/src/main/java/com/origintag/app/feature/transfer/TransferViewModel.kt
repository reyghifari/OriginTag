package com.origintag.app.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origintag.app.data.repository.PassportRepository
import com.origintag.app.wallet.TransactionSigner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Part 5d — transfer kepemilikan (FR-07, FR-08) */
@HiltViewModel
class TransferViewModel @Inject constructor(
    private val repository: PassportRepository,
    private val signer: TransactionSigner,
) : ViewModel() {

    enum class Phase { IDLE, SENDING, DONE, ERROR }

    data class UiState(
        val phase: Phase = Phase.IDLE,
        val txHash: String? = null,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * @param privateKey key dari Web3Auth; kosong = mode demo (backend mock
     *   menerapkan transfer in-memory tanpa signing on-chain).
     */
    fun transfer(tokenId: String, toAddress: String, privateKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(phase = Phase.SENDING, error = null) }
            runCatching {
                val resp = repository.transfer(tokenId, toAddress)
                val unsigned = resp.unsignedTx
                if (unsigned != null && privateKey.isNotBlank()) {
                    // Mode nyata: tanda tangani calldata & broadcast dari wallet pemilik
                    signer.signAndSend(unsigned.to, unsigned.data, privateKey)
                } else {
                    // Mode mock: backend sudah menerapkan transfer, pakai txHash-nya
                    resp.txHash ?: "0x(mock)"
                }
            }.onSuccess { hash ->
                _uiState.update { it.copy(phase = Phase.DONE, txHash = hash) }
            }.onFailure { e ->
                _uiState.update { it.copy(phase = Phase.ERROR, error = e.message ?: "Transfer gagal") }
            }
        }
    }
}
