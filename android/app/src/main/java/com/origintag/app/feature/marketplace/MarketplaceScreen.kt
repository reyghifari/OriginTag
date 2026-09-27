package com.origintag.app.feature.marketplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.origintag.app.BuildConfig
import com.origintag.app.data.model.MarketplaceListingDto
import com.origintag.app.ui.components.OtCard
import com.origintag.app.ui.components.ScreenHeader
import com.origintag.app.ui.components.SectionTitle
import com.origintag.app.ui.components.TxResultDialog
import com.origintag.app.ui.theme.Ot
import com.origintag.app.wallet.LocalWeb3Auth

/** Part wow — marketplace: barang dijual + tombol beli */
@Composable
fun MarketplaceScreen(
    onItemClick: (String) -> Unit,
    viewModel: MarketplaceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val web3Auth = LocalWeb3Auth.current

    LaunchedEffect(Unit) { viewModel.load() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenHeader("Pasar", "${state.listings.size} barang dijual · escrow on-chain") }
            item { SectionTitle("Dijual sekarang", Modifier.padding(horizontal = 20.dp)) }
            when {
                state.loading && state.listings.isEmpty() ->
                    item { CircularProgressIndicator(Modifier.padding(20.dp)) }

                state.error != null && state.listings.isEmpty() -> item {
                    Text("Gagal memuat: ${state.error}", Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error)
                }

                state.listings.isEmpty() -> item {
                    OtCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                        Text("Belum ada barang dijual.", color = Ot.Muted)
                    }
                }

                else -> items(state.listings) { listing ->
                    ListingCard(
                        listing = listing,
                        buying = state.buyingTokenId == listing.tokenId,
                        onClick = { onItemClick(listing.tokenId) },
                        onBuy = {
                            val pk = web3Auth?.let { runCatching { it.getPrivateKey() }.getOrNull() }.orEmpty()
                            viewModel.buy(listing.tokenId, pk)
                        },
                    )
                }
            }
        }

        state.result?.let { r ->
            TxResultDialog(
                result = r,
                onDismiss = viewModel::dismissResult,
                primaryLabel = if (r.success) "Lihat barang" else null,
                onPrimary = r.tokenId?.let { id -> { viewModel.dismissResult(); onItemClick(id) } },
            )
        }
    }
}

@Composable
private fun ListingCard(
    listing: MarketplaceListingDto,
    buying: Boolean,
    onClick: () -> Unit,
    onBuy: () -> Unit,
) {
    OtCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = "${BuildConfig.API_BASE_URL}items/${listing.tokenId}/photo/0",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                Text(listing.brand, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${listing.category} · skor ${listing.authenticityScore}", style = MaterialTheme.typography.bodySmall, color = Ot.Muted)
                Text("${listing.priceBnb} tBNB", style = MaterialTheme.typography.titleLarge, color = Ot.Blue)
            }
            Button(onClick = onBuy, enabled = !buying, shape = RoundedCornerShape(14.dp)) {
                Text(if (buying) "..." else "Beli")
            }
        }
    }
}
