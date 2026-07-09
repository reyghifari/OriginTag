package com.origintag.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.origintag.app.navigation.AppNavHost
import com.origintag.app.ui.theme.OriginTagTheme
import com.origintag.app.wallet.LocalWeb3Auth
import com.origintag.app.wallet.WalletManager
import com.web3auth.core.Web3Auth
import com.web3auth.core.types.Web3AuthOptions
import dagger.hilt.android.AndroidEntryPoint
import org.torusresearch.fetchnodedetails.types.Web3AuthNetwork
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var walletManager: WalletManager

    private lateinit var web3Auth: Web3Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        web3Auth = Web3Auth(
            Web3AuthOptions(
                clientId = BuildConfig.WEB3AUTH_CLIENT_ID,
                // Ganti ke SAPPHIRE_MAINNET saat produksi
                web3AuthNetwork = Web3AuthNetwork.SAPPHIRE_DEVNET,
                redirectUrl = BuildConfig.WEB3AUTH_REDIRECT_URL,
                defaultChainId = BuildConfig.DEFAULT_CHAIN_ID,
            ),
            this,
        )

        // Tangani redirect jika Activity dibuka dari CustomTab OAuth
        web3Auth.setResultUrl(intent?.data)

        // Restore sesi Web3Auth. Hasilnya menentukan status login app (satu sumber
        // kebenaran) — tidak perlu penyimpanan sesi terpisah.
        web3Auth.initialize().whenComplete { _, error ->
            val pk = if (error == null) {
                runCatching { web3Auth.getPrivateKey() }.getOrNull()
            } else {
                Log.d("MainActivity", "Web3Auth initialize: tidak ada sesi (${error.message})")
                null
            }
            val profile = if (!pk.isNullOrBlank()) {
                runCatching {
                    val info = web3Auth.getUserInfo()
                    WalletManager.UserProfile(info?.name, info?.email, info?.profileImage)
                }.getOrNull()
            } else {
                null
            }
            walletManager.restoreFromWeb3Auth(pk, profile)
        }

        // TODO(Part 5e): baca intent?.data untuk App Link origintag.app/verify/{tokenId}
        // lalu navigasi langsung ke PassportDetailScreen(tokenId).

        setContent {
            OriginTagTheme {
                CompositionLocalProvider(LocalWeb3Auth provides web3Auth) {
                    AppNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Redirect OAuth kembali ke sini karena launchMode=singleTask
        web3Auth.setResultUrl(intent.data)
    }
}
