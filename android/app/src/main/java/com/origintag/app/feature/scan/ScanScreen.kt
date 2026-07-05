package com.origintag.app.feature.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Part 5e — scan QR barang (sisi buyer).
 *
 * TODO(Part 5e):
 *  1. Runtime permission CAMERA.
 *  2. CameraX Preview + ML Kit BarcodeScanning analyzer.
 *  3. Parse hasil scan: URL origintag.app/verify/{tokenId} → ekstrak tokenId
 *     → onResult(tokenId).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onResult: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Scan QR") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("TODO(Part 5e): CameraX + ML Kit barcode scanner")

            // Stub untuk uji navigasi selama scanner belum ada
            Button(onClick = { onResult("0") }) {
                Text("Simulasi scan → passport #0")
            }
        }
    }
}
