package com.origintag.app.feature.dashboard

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.origintag.app.BuildConfig
import com.origintag.app.R
import com.origintag.app.data.model.PassportDto
import com.origintag.app.ui.components.BlueHeader
import com.origintag.app.ui.components.Chip
import com.origintag.app.ui.components.GlassButton
import com.origintag.app.ui.components.OtCard
import com.origintag.app.ui.components.SectionTitle
import com.origintag.app.ui.theme.Ot
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/** Part 5c — dashboard "Barang Saya" (FR-09), gaya kartu biru bertepi sobek. */
@Composable
fun DashboardScreen(
    onRegisterClick: () -> Unit,
    onScanClick: () -> Unit,
    onPassportClick: (String) -> Unit,
    onMarketClick: () -> Unit,
    onExploreClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BlueHeader {
            WalletPill(state.walletAddress.orEmpty())
            Spacer(Modifier.height(28.dp))
            DateRow(onScanClick)
            Spacer(Modifier.height(20.dp))
            Greeting(state)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton("+", "Daftar", onRegisterClick, Modifier.width(112.dp))
                GlassButton("→", "Pasar", onMarketClick, Modifier.width(112.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Ot.Sky)
                        .clickable(onClick = onExploreClick)
                        .padding(horizontal = 26.dp, vertical = 14.dp),
                ) {
                    Text("•••", color = Ot.Navy, fontSize = 20.sp, letterSpacing = 2.sp)
                }
            }
        }

        SectionTitle("Barang kamu", Modifier.padding(start = 20.dp, top = 8.dp, bottom = 16.dp))

        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(20.dp))
            state.error != null -> Text(
                "Gagal memuat: ${state.error}",
                Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.error,
            )
            state.passports.isEmpty() -> OtCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), onRegisterClick) {
                Text("Belum ada passport. Ketuk untuk mendaftarkan barang pertamamu!", color = Ot.Muted)
            }
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.passports) { p -> PassportTile(p) { onPassportClick(p.tokenId) } }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Pill akun di atas ("Sneaker Assistant ▾"): ketuk untuk salin alamat. */
@Composable
private fun WalletPill(address: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Ot.BlueDeep)
            .clickable {
                clipboard.setText(AnnotatedString(address))
                Toast.makeText(context, "Alamat disalin", Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Ot.Blue),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.ic_origintag_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Ot.Sky),
                modifier = Modifier.size(58.dp),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("OriginTag Wallet", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(short(address), color = Color.White.copy(alpha = .55f), style = MaterialTheme.typography.bodyMedium)
        }
        Text("▾", color = Color.White, fontSize = 18.sp)
    }
}

@Composable
private fun DateRow(onScanClick: () -> Unit) {
    val now = LocalDateTime.now()
    val id = Locale("id")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            now.dayOfWeek.getDisplayName(TextStyle.SHORT, id).uppercase(),
            color = Color.White,
            fontSize = 64.sp,
            lineHeight = 64.sp,
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            "${now.dayOfMonth} ${now.month.getDisplayName(TextStyle.SHORT, id)}",
            color = Ot.Sky,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 6.dp, top = 6.dp),
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Ot.Mint)
                .clickable(onClick = onScanClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("↗", color = Ot.MintInk, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Greeting(state: DashboardViewModel.UiState) {
    val muted = Color.White.copy(alpha = .6f)
    val big = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val hour = LocalDateTime.now().hour
    val salam = when {
        hour < 11 -> "Selamat pagi,"
        hour < 15 -> "Selamat siang,"
        hour < 18 -> "Selamat sore,"
        else -> "Selamat malam,"
    }
    val firstName = state.name?.substringBefore(' ') ?: "Kolektor"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(salam, color = muted, style = big)
            Spacer(Modifier.width(10.dp))
            Avatar(state.avatar, firstName)
            Spacer(Modifier.width(10.dp))
            Text("$firstName.", color = Color.White, style = big)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Kamu punya", color = muted, style = big)
            Spacer(Modifier.width(10.dp))
            Stat(Icons.Outlined.List, "${state.passports.size} passport aktif", big)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stat(Icons.Outlined.ShoppingCart, "${state.stats?.salesCount ?: 0} terjual", big)
            Text("  dan  ", color = muted, style = big)
            Stat(Icons.Outlined.CheckCircle, "${state.stats?.highScoreCount ?: 0} asli.", big)
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, text: String, style: androidx.compose.ui.text.TextStyle) {
    Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
    Spacer(Modifier.width(6.dp))
    Text(text, color = Color.White, style = style)
}

@Composable
private fun Avatar(url: String?, name: String) {
    val mod = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
    if (url != null) {
        AsyncImage(url, null, mod, contentScale = ContentScale.Crop)
    } else {
        Box(mod.background(Ot.Sky), contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), color = Ot.Navy, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Kartu produk horizontal: foto, badge status, chip skor. */
@Composable
private fun PassportTile(p: PassportDto, onClick: () -> Unit) {
    val verified = p.authenticityScore >= 70
    OtCard(Modifier.width(180.dp).height(160.dp), onClick) {
        AsyncImage(
            model = "${BuildConfig.API_BASE_URL}items/${p.tokenId}/photo/0",
            contentDescription = p.brand,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(top = 18.dp, bottom = 22.dp),
        )
        Text(
            p.brand,
            Modifier.align(Alignment.TopStart).padding(end = 28.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Ot.Navy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            if (verified) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
            contentDescription = null,
            tint = if (verified) Ot.Amber else MaterialTheme.colorScheme.error,
            modifier = Modifier.align(Alignment.TopEnd).size(20.dp),
        )
        Chip("${p.authenticityScore}/100", Modifier.align(Alignment.BottomEnd))
    }
}

private fun short(a: String) = if (a.length > 12) "${a.take(6)}…${a.takeLast(4)}" else a

