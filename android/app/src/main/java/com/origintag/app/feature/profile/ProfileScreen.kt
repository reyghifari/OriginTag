package com.origintag.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

/** Part wow — profil user: identitas, alamat (copy), statistik, trust, logout */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profil") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Avatar + nama + email
            if (state.profileImage != null) {
                AsyncImage(
                    model = state.profileImage,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(84.dp).clip(CircleShape),
                )
            } else {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(84.dp)) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👤", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
            Text(state.name ?: "Wallet Demo", style = MaterialTheme.typography.titleLarge)
            state.email?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            // Alamat wallet + copy
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Alamat Wallet", style = MaterialTheme.typography.labelMedium)
                        Text(
                            shortAddress(state.address),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(state.address)) }) {
                        Text("Salin")
                    }
                }
            }

            // Trust score
            state.stats?.let { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Trust Score", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "⭐ ${s.trustScore}/100 · ${s.trustLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text("${s.owned} barang dimiliki · ${s.highScoreCount} skor tinggi · ${s.salesCount} terjual")
                    }
                }
            }

            // Saldo
            state.balance?.let { b ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Saldo (gas)", style = MaterialTheme.typography.labelMedium)
                        Text("${trimBnb(b.bnb)} tBNB", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.logout(); onLoggedOut() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Keluar")
            }
        }
    }
}

private fun shortAddress(a: String): String =
    if (a.length > 12) "${a.take(6)}...${a.takeLast(4)}" else a

private fun trimBnb(bnb: String): String =
    runCatching { "%.4f".format(bnb.toDouble()) }.getOrDefault(bnb)
