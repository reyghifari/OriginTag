package com.origintag.app.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.origintag.app.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.origintag.app.wallet.LocalWeb3Auth
import com.origintag.app.wallet.WalletManager
import com.web3auth.core.types.AuthConnection
import com.web3auth.core.types.LoginParams

/**
 * Part 5a — Onboarding & wallet (FR-01).
 * Social login (email/Google) via Web3Auth → embedded wallet, tanpa seed phrase
 * (PRD §8.4). Alamat disimpan ke sesi lewat WalletManager (DataStore).
 */
@Composable
fun OnboardingScreen(
    onLoggedIn: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val web3Auth = LocalWeb3Auth.current
    val context = LocalContext.current

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }

    fun login(connection: AuthConnection) {
        val auth = web3Auth
        if (auth == null) {
            viewModel.setError("Web3Auth belum siap")
            return
        }
        viewModel.setLoading()
        auth.connectTo(LoginParams(authConnection = connection))
            .whenComplete { _, error ->
                if (error == null) {
                    val pk = runCatching { auth.getPrivateKey() }.getOrNull()
                    if (!pk.isNullOrBlank()) {
                        val profile = runCatching {
                            val info = auth.getUserInfo()
                            WalletManager.UserProfile(info?.name, info?.email, info?.profileImage)
                        }.getOrNull()
                        viewModel.completeLogin(pk, profile)
                    } else {
                        viewModel.setError("Login berhasil tapi private key kosong")
                    }
                } else {
                    viewModel.setError(error.message ?: "Login dibatalkan")
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_origintag_logo),
            contentDescription = "Logo OriginTag",
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFF0B90B)),
        )
        Spacer(Modifier.height(16.dp))
        Text("OriginTag", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Digital passport untuk barang fisik",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(40.dp))

        if (state.checkingSession) {
            CircularProgressIndicator()
        } else if (state.loading) {
            CircularProgressIndicator()
            Text("Menghubungkan wallet...", Modifier.padding(top = 12.dp))
        } else {
            Button(
                onClick = { login(AuthConnection.GOOGLE) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Masuk dengan Google")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { login(AuthConnection.EMAIL_PASSWORDLESS) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Masuk dengan Email")
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { viewModel.skipWithDemoWallet() }) {
                Text("Lewati (mode demo)")
            }
        }

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
