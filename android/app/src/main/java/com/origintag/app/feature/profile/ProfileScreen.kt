package com.origintag.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.origintag.app.ui.components.BlueHeader
import com.origintag.app.ui.components.OtCard
import com.origintag.app.ui.components.SectionTitle
import com.origintag.app.ui.theme.Ot

/** Part wow — profil user: identitas, alamat (copy), statistik, trust, logout */
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BlueHeader {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatar = Modifier.size(76.dp).clip(RoundedCornerShape(22.dp))
                if (state.profileImage != null) {
                    AsyncImage(state.profileImage, "Avatar", avatar, contentScale = ContentScale.Crop)
                } else {
                    Box(avatar.background(Ot.Sky), contentAlignment = Alignment.Center) {
                        Text((state.name ?: "W").take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = Ot.Navy)
                    }
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text(state.name ?: "Wallet Demo", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    state.email?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .65f)) }
                }
            }
            state.stats?.let { s ->
                Spacer(Modifier.height(24.dp))
                Text("TRUST SCORE", style = MaterialTheme.typography.labelLarge, color = Ot.Sky)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${s.trustScore}", style = MaterialTheme.typography.displayLarge, color = Color.White)
                    Text(" /100 · ${s.trustLabel}", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = .65f), modifier = Modifier.padding(bottom = 12.dp))
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Akun")

            OtCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Alamat wallet", style = MaterialTheme.typography.labelMedium, color = Ot.Muted)
                        Text(shortAddress(state.address), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(state.address)) }) { Text("Salin") }
                }
            }

            state.stats?.let { s ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("${s.owned}", "dimiliki", Modifier.weight(1f))
                    StatCard("${s.highScoreCount}", "skor tinggi", Modifier.weight(1f))
                    StatCard("${s.salesCount}", "terjual", Modifier.weight(1f))
                }
            }

            state.balance?.let { b ->
                OtCard(Modifier.fillMaxWidth()) {
                    Column {
                        Text("Saldo (gas)", style = MaterialTheme.typography.labelMedium, color = Ot.Muted)
                        Text("${trimBnb(b.bnb)} tBNB", style = MaterialTheme.typography.titleLarge, color = Ot.Blue)
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.logout(); onLoggedOut() },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Keluar") }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    OtCard(modifier) {
        Column {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Ot.Navy)
            Text(label, style = MaterialTheme.typography.bodySmall, color = Ot.Muted)
        }
    }
}

private fun shortAddress(a: String): String =
    if (a.length > 12) "${a.take(6)}...${a.takeLast(4)}" else a

private fun trimBnb(bnb: String): String =
    runCatching { "%.4f".format(bnb.toDouble()) }.getOrDefault(bnb)
