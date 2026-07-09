package com.origintag.app.feature.marketplace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.origintag.app.wallet.LocalWeb3Auth

/** Part wow — marketplace: barang dijual + tombol beli */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    onItemClick: (String) -> Unit,
    viewModel: MarketplaceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val web3Auth = LocalWeb3Auth.current

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Marketplace") }) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading && state.listings.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.error != null && state.listings.isEmpty() ->
                    Text("Gagal memuat: ${state.error}", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)

                state.listings.isEmpty() ->
                    Text("Belum ada barang dijual.", Modifier.padding(16.dp))

                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.listings) { listing ->
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

            state.message?.let {
                Text(it, Modifier.align(Alignment.BottomCenter).padding(16.dp), color = MaterialTheme.colorScheme.primary)
            }
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
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = "${BuildConfig.API_BASE_URL}items/${listing.tokenId}/photo/0",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("${listing.brand} — ${listing.category}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Skor ${listing.authenticityScore}/100", style = MaterialTheme.typography.bodySmall)
                Text("${listing.priceBnb} tBNB", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onBuy, enabled = !buying) {
                Text(if (buying) "..." else "Beli")
            }
        }
    }
}
