package com.origintag.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Part 5a — Onboarding & wallet (FR-01).
 *
 * TODO(Part 5a):
 *  1. Integrasi Web3Auth/Particle Android SDK → social login (email/Google)
 *     menghasilkan embedded wallet, tanpa seed phrase (PRD §8.4).
 *  2. Opsi WalletConnect v2 untuk MetaMask/Trust Wallet.
 *  3. Simpan wallet address ke DataStore sebagai sesi.
 */
@Composable
fun OnboardingScreen(onLoggedIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🏷️ OriginTag", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Digital passport untuk barang fisik",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onLoggedIn) {
            Text("Masuk (stub — TODO Web3Auth)")
        }
    }
}
