package com.origintag.app.feature.register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Part 5b — registrasi barang (FR-02).
 * Foto via Android Photo Picker (tanpa permission), submit multipart ke backend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onDone: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var brand by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var serialNumber by rememberSaveable { mutableStateOf("") }
    var warrantyDays by rememberSaveable { mutableStateOf("365") }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5),
    ) { uris -> viewModel.setPhotos(uris) }

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
                onValueChange = { warrantyDays = it.filter(Char::isDigit) },
                label = { Text("Durasi garansi (hari)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.photos.isEmpty()) "📷 Tambah Foto (1-5)"
                    else "✓ ${state.photos.size} foto dipilih — ganti",
                )
            }

            when (state.phase) {
                RegisterViewModel.Phase.SUBMITTING -> {
                    CircularProgressIndicator()
                    Text("Menganalisis keaslian & minting passport...")
                }

                RegisterViewModel.Phase.MINTED -> Text(
                    "✅ Passport aktif! Token #${state.result?.tokenId} — skor ${state.result?.aiResult?.score}/100",
                    color = MaterialTheme.colorScheme.primary,
                )

                RegisterViewModel.Phase.FLAGGED -> Text(
                    "⚠ Ditandai untuk review manual (skor ${state.result?.aiResult?.score}/100 di bawah ambang batas)",
                    color = MaterialTheme.colorScheme.error,
                )

                RegisterViewModel.Phase.ERROR -> Text(
                    "Gagal: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                )

                RegisterViewModel.Phase.IDLE -> Unit
            }

            val done = state.phase == RegisterViewModel.Phase.MINTED ||
                state.phase == RegisterViewModel.Phase.FLAGGED
            Button(
                onClick = {
                    if (done) {
                        onDone()
                    } else {
                        viewModel.submit(
                            brand = brand.trim(),
                            category = category.trim(),
                            serialNumber = serialNumber.trim(),
                            purchaseDate = "2026-01-01", // TODO(Part 5b): date picter
                            warrantyDurationDays = warrantyDays.toIntOrNull() ?: 365,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = when {
                    done -> true
                    state.phase == RegisterViewModel.Phase.SUBMITTING -> false
                    else -> brand.isNotBlank() && category.isNotBlank() && state.photos.isNotEmpty()
                },
            ) {
                Text(if (done) "Selesai" else "Verifikasi & Mint Passport")
            }
        }
    }
}
