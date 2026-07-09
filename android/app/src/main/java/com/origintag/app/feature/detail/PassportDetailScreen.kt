package com.origintag.app.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.origintag.app.data.model.PassportDto
import com.origintag.app.wallet.LocalWeb3Auth

/** Part 5c — detail passport: skor, sisa garansi, riwayat kepemilikan */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportDetailScreen(
    tokenId: String,
    onTransferClick: () -> Unit,
    viewModel: PassportDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val web3Auth = LocalWeb3Auth.current

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

                state.error != null && state.passport == null -> Text(
                    "Gagal memuat: ${state.error}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )

                state.passport != null -> PassportDetail(
                    state = state,
                    onTransferClick = onTransferClick,
                    onSell = { priceBnb ->
                        val pk = web3Auth?.let { runCatching { it.getPrivateKey() }.getOrNull() }.orEmpty()
                        viewModel.sell(tokenId, priceBnb, pk)
                    },
                )
            }
        }
    }
}

@Composable
private fun PassportDetail(
    state: PassportDetailViewModel.UiState,
    onTransferClick: () -> Unit,
    onSell: (String) -> Unit,
) {
    val passport = state.passport ?: return
    var priceBnb by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Banner recall (FR-11)
        passport.recall?.let { recall ->
            Surface(color = Color(0xFFB00020), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠ RECALL PRODUK", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(recall.reason, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (state.photoUrls.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.photoUrls) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Foto barang",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(220.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
            }
        }

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

        // Riwayat servis (FR-10)
        if (state.serviceRecords.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Riwayat Servis", style = MaterialTheme.typography.titleMedium)
                    state.serviceRecords.forEach { rec -> Text("• $rec", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }

        // Aksi pemilik: jual + transfer
        if (state.isOwner) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Jual di Marketplace", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = priceBnb,
                        onValueChange = { priceBnb = it },
                        label = { Text("Harga (tBNB)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onSell(priceBnb) },
                        enabled = priceBnb.isNotBlank() && !state.selling,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.selling) "Memproses..." else "Jual")
                    }
                }
            }
            OutlinedButton(onClick = onTransferClick, modifier = Modifier.fillMaxWidth()) {
                Text("Transfer Kepemilikan (gratis)")
            }
        }

        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

/** Privasi (PRD §8.3): alamat wallet ditampilkan terpotong */
private fun anonymize(addr: String): String =
    if (addr.length > 10) "${addr.take(5)}...${addr.takeLast(4)}" else addr
