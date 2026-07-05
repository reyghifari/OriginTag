package com.origintag.app.feature.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Part 5b — registrasi barang (FR-02).
 *
 * TODO(Part 5b):
 *  1. Ambil foto via CameraX / Photo Picker (1-5 foto), kompresi sebelum upload.
 *  2. Submit multipart ke POST /items/register via PassportRepository.
 *  3. Tampilkan progress: AI menganalisis → minting → passport aktif + QR
 *     (atau status "Flagged for Manual Review" jika skor < threshold).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onDone: () -> Unit) {
    var brand by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var serialNumber by rememberSaveable { mutableStateOf("") }
    var warrantyDays by rememberSaveable { mutableStateOf("365") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Daftarkan Barang") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Brand *") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Kategori * (mis. Sneakers, Tas)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = serialNumber,
                onValueChange = { serialNumber = it },
                label = { Text("Nomor seri (opsional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = warrantyDays,
                onValueChange = { warrantyDays = it },
                label = { Text("Durasi garansi (hari)") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(onClick = { /* TODO(Part 5b): CameraX / Photo Picker */ }) {
                Text("📷 Tambah Foto (1-5) — TODO")
            }

            Button(
                onClick = { /* TODO(Part 5b): submit ke repository lalu onDone() */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = brand.isNotBlank() && category.isNotBlank(),
            ) {
                Text("Verifikasi & Mint Passport — TODO")
            }
        }
    }
}
