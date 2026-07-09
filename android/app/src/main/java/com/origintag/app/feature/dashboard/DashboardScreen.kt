package com.origintag.app.feature.dashboard

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.origintag.app.BuildConfig
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
        viewModel.load()
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
            state.walletAddress?.let { address ->
                val clipboardManager = LocalClipboardManager.current
                val context = LocalContext.current

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alamat Wallet Anda", style = MaterialTheme.typography.labelMedium)
                            Text(address, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            
                            Text(
                                "Total Token Passport", 
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                "${state.passports.size} Token", 
                                style = MaterialTheme.typography.bodyMedium, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(address))
                                Toast.makeText(context, "Address disalin", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBox,
                                contentDescription = "Copy Address"
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onScanClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
        androidx.compose.foundation.layout.Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "${BuildConfig.API_BASE_URL}items/${passport.tokenId}/photo/0",
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
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
}
