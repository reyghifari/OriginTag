package com.origintag.app.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/** Part 5c — dashboard "Barang Saya" (FR-09) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onRegisterClick: () -> Unit,
    onScanClick: () -> Unit,
    onPassportClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load(DashboardViewModel.DEMO_WALLET)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Barang Saya") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onRegisterClick) {
                Text("+ Daftarkan Barang")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            OutlinedButton(
                onClick = onScanClick,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Text("Scan QR Barang")
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.error != null -> Text(
                    "Gagal memuat: ${state.error}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )

                state.passports.isEmpty() -> Text(
                    "Belum ada passport. Daftarkan barang pertamamu!",
                    modifier = Modifier.padding(16.dp),
                )

                else -> LazyColumn {
                    items(state.passports) { passport ->
                        PassportCard(passport) { onPassportClick(passport.tokenId) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassportCard(passport: PassportDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "${passport.brand} — ${passport.category}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Skor keaslian: ${passport.authenticityScore}/100",
                style = MaterialTheme.typography.bodySmall,
            )
            val days = (passport.remainingWarrantySeconds ?: 0) / 86_400
            Text("Sisa garansi: $days hari", style = MaterialTheme.typography.bodySmall)
        }
    }
}
