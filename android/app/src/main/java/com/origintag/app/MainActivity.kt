package com.origintag.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.origintag.app.navigation.AppNavHost
import com.origintag.app.ui.theme.OriginTagTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // TODO(Part 5e): baca intent?.data untuk App Link origintag.app/verify/{tokenId}
        // lalu navigasi langsung ke PassportDetailScreen(tokenId).

        setContent {
            OriginTagTheme {
                AppNavHost()
            }
        }
    }
}
