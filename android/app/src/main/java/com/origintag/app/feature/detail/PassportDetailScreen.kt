package com.origintag.app.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.origintag.app.data.model.PassportDto

/** Part 5c — detail passport: skor, sisa garansi, riwayat kepemilikan */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportDetailScreen(
    tokenId: String,
    onTransferClick: () -> Unit,
    viewModel: PassportDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(tokenId) { viewModel.load(tokenId) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Passport #$tokenId") }) },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.error != null -> Text(
                    "Gagal memuat: ${state.error}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )

                state.passport != null -> PassportDetail(
                    passport = state.passport!!,
                    onTransferClick = onTransferClick,
                )
            }
        }
    }
}

@Composable
private fun PassportDetail(passport: PassportDto, onTransferClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("${passport.brand} — ${passport.category}", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val verified = passport.authenticityScore >= 70
                Text(
                    if (verified) "✓ Terverifikasi — skor ${passport.authenticityScore}/100"
                    else "⚠ Perlu review — skor ${passport.authenticityScore}/100",
                    color = if (verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Text("Serial: ${passport.serialNumber ?: "-"}")
                val days = (passport.remainingWarrantySeconds ?: 0) / 86_400
                Text("Sisa garansi: $days hari")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Riwayat Kepemilikan", style = MaterialTheme.typography.titleMedium)
                passport.ownershipHistory.forEachIndexed { i, addr ->
                    Text("${i + 1}. ${anonymize(addr)}")
                }
                Text("Pemilik saat ini: ${anonymize(passport.owner ?: "-")}")
            }
        }

        Button(onClick = onTransferClick, modifier = Modifier.fillMaxWidth()) {
            Text("Transfer Kepemilikan")
        }
    }
}

/** Privasi (PRD §8.3): alamat wallet ditampilkan terpotong */
private fun anonymize(addr: String): String =
    if (addr.length > 10) "${addr.take(5)}...${addr.takeLast(4)}" else addr
