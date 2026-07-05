package com.origintag.app.feature.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Part 5d — transfer kepemilikan (FR-07, FR-08).
 *
 * TODO(Part 5d):
 *  1. Validasi format address EVM + opsi scan QR wallet buyer.
 *  2. Panggil POST /items/{tokenId}/transfer → dapat unsignedTx.
 *  3. Sign & kirim via wallet SDK (Web3Auth/WalletConnect) — validasi
 *     kepemilikan terjadi on-chain di kontrak (PRD §8.1).
 *  4. Tampilkan konfirmasi + sisa garansi untuk pemilik baru.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    tokenId: String,
    onDone: () -> Unit,
) {
    var buyerAddress by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Transfer Passport #$tokenId") }) },
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
                onValueChange = { buyerAddress = it },
                label = { Text("Alamat wallet buyer (0x...)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { /* TODO(Part 5d): transfer lalu onDone() */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = buyerAddress.startsWith("0x") && buyerAddress.length == 42,
            ) {
                Text("Kirim Passport — TODO")
            }
        }
    }
}
