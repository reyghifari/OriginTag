package com.origintag.app.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Part 5c — detail passport.
 *
 * TODO(Part 5c):
 *  1. ViewModel: load PassportRepository.getPassport(tokenId).
 *  2. Tampilkan foto (Coil), skor keaslian, sisa garansi (countdown),
 *     riwayat kepemilikan (address dipotong), QR code untuk verifikasi publik.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportDetailScreen(
    tokenId: String,
    onTransferClick: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Passport #$tokenId") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "TODO(Part 5c): tampilkan detail passport #$tokenId di sini",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(onClick = onTransferClick, modifier = Modifier.fillMaxWidth()) {
                Text("Transfer Kepemilikan")
            }
        }
    }
}
