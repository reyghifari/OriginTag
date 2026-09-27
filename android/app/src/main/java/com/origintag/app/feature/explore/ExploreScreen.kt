package com.origintag.app.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.origintag.app.BuildConfig
import com.origintag.app.data.model.PassportDto
import com.origintag.app.ui.components.Chip
import com.origintag.app.ui.components.OtCard
import com.origintag.app.ui.components.ScreenHeader
import com.origintag.app.ui.components.SectionTitle
import com.origintag.app.ui.theme.Ot

/** Part wow — explore: telusuri semua passport terverifikasi */
@Composable
fun ExploreScreen(
    onItemClick: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.search() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("Explore", "Telusuri passport terverifikasi") {
                Spacer(Modifier.height(20.dp))
                TextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Cari brand...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item { SectionTitle("Semua barang", Modifier.padding(horizontal = 20.dp)) }
        if (state.loading) {
            item { CircularProgressIndicator(Modifier.padding(20.dp)) }
        } else {
            items(state.items) { p -> ExploreCard(p) { onItemClick(p.tokenId) } }
        }
    }
}

@Composable
private fun ExploreCard(p: PassportDto, onClick: () -> Unit) {
    val verified = p.authenticityScore >= 70
    OtCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = "${BuildConfig.API_BASE_URL}items/${p.tokenId}/photo/0",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(p.brand, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(p.category, style = MaterialTheme.typography.bodySmall, color = Ot.Muted)
            }
            Chip(
                if (verified) "✓ ${p.authenticityScore}" else "⚠ ${p.authenticityScore}",
                color = if (verified) Ot.Blue else MaterialTheme.colorScheme.error,
            )
        }
    }
}
