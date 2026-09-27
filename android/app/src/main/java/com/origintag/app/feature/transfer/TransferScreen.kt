package com.origintag.app.feature.transfer

import com.origintag.app.ui.components.OtTopBar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.origintag.app.wallet.LocalWeb3Auth

/**
 * Part 5d — transfer kepemilikan (FR-07, FR-08).
 * Menandatangani transferPassport(tokenId, to) dengan wallet Web3Auth pemilik
 * lalu broadcast ke BSC Testnet. Validasi kepemilikan terjadi on-chain (PRD §8.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    tokenId: String,
    onDone: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val web3Auth = LocalWeb3Auth.current

    var buyerAddress by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.phase) {
        if (state.phase == TransferViewModel.Phase.DONE) onDone()
    }

    Scaffold(
        topBar = { OtTopBar("Transfer Passport #$tokenId") },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = buyerAddress,
                onValueChange = { buyerAddress = it.trim() },
                label = { Text("Alamat wallet buyer (0x...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                // TODO(Part 5e): tombol scan QR wallet buyer di trailingIcon
            )

            when (state.phase) {
                TransferViewModel.Phase.SENDING -> {
                    CircularProgressIndicator()
                    Text("Menandatangani & mengirim transaksi...")
                }

                TransferViewModel.Phase.ERROR -> Text(
                    "Gagal: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                )

                TransferViewModel.Phase.DONE -> Text(
                    "✅ Terkirim: ${state.txHash}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                )

                TransferViewModel.Phase.IDLE -> Unit
            }

            Button(
                onClick = {
                    // Kosong di mode demo (belum login Web3Auth) → backend mock menangani
                    val privateKey = web3Auth
                        ?.let { runCatching { it.getPrivateKey() }.getOrNull() }
                        .orEmpty()
                    viewModel.transfer(tokenId, buyerAddress, privateKey)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValidEvmAddress(buyerAddress) &&
                    state.phase != TransferViewModel.Phase.SENDING,
            ) {
                Text("Kirim Passport")
            }
        }
    }
}

private fun isValidEvmAddress(addr: String): Boolean =
    addr.length == 42 && addr.startsWith("0x") &&
        addr.drop(2).all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
