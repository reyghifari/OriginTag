package com.origintag.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origintag.app.ui.theme.Ot

/**
 * Tepi bawah "kertas sobek": miring (kiri lebih rendah dari kanan) dengan gerigi kecil.
 * Gerigi deterministik (tabel tetap) supaya bentuk tidak berubah tiap recomposition.
 */
class TornEdgeShape(private val slant: Dp, private val seed: Int = 0) : Shape {
    private val jitter = floatArrayOf(0f, .6f, -.3f, .9f, -.5f, .4f, -.8f, .2f, .7f, -.6f, .3f, -.2f, .8f, -.4f)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val drop = with(density) { slant.toPx() }
        val amp = with(density) { 5.dp.toPx() }
        val step = with(density) { 14.dp.toPx() }
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - drop)
            var x = size.width
            var i = seed
            while (x > 0f) {
                x = (x - step).coerceAtLeast(0f)
                val baseY = size.height - drop * (x / size.width)
                lineTo(x, baseY + amp * jitter[i++ % jitter.size])
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/** Header biru bertepi sobek + strip kertas putih di belakangnya. Isi sudah aman dari status bar. */
@Composable
fun BlueHeader(
    modifier: Modifier = Modifier,
    slant: Dp = 56.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { translationY = 7.dp.toPx() }
                .clip(TornEdgeShape(slant, seed = 5))
                .background(Color.White),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(TornEdgeShape(slant))
                .background(Ot.Blue)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = slant + 28.dp),
            content = content,
        )
    }
}

/** Header layar tab: judul tebal kapital + subjudul. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, extra: @Composable ColumnScope.() -> Unit = {}) {
    BlueHeader {
        Text(title.uppercase(), style = MaterialTheme.typography.displaySmall, color = Color.White)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = .65f))
        }
        extra()
    }
}

/** Tombol kaca buram di atas biru (Buy/Sell di referensi). */
@Composable
fun GlassButton(symbol: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(Color.White.copy(alpha = .10f))
            .border(1.dp, Color.White.copy(alpha = .35f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(symbol, color = Color.White, fontSize = 22.sp, textAlign = TextAlign.Center)
        Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

/** Judul section navy tebal kapital ("BUYING THESE?"). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
        color = Ot.Navy,
        letterSpacing = .5.sp,
    )
}

/** Kartu putih bergaris tipis. */
@Composable
fun OtCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Ot.Line),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Box(Modifier.padding(16.dp), content = content)
    }
}

/** Chip putih kecil berbayang (label harga di referensi). */
@Composable
fun Chip(text: String, modifier: Modifier = Modifier, color: Color = Ot.Navy) {
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = Color.White, shadowElevation = 3.dp) {
        Text(
            text,
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

/** Top bar biru untuk layar non-tab (Daftar, Detail, Scan, Transfer). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OtTopBar(title: String) {
    androidx.compose.material3.TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = Ot.Blue,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
        ),
    )
}

/** Hasil transaksi on-chain untuk ditampilkan di [TxResultDialog]. */
data class TxResult(
    val success: Boolean,
    val title: String,
    val message: String,
    val txHash: String? = null,
    val tokenId: String? = null,
)

/** Dialog sukses/gagal transaksi: ikon besar, keterangan, link BscScan, tombol aksi. */
@Composable
fun TxResultDialog(
    result: TxResult,
    onDismiss: () -> Unit,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val red = Color(0xFFD93A3A)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (result.success) Ot.Mint else red.copy(alpha = .15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (result.success) "✓" else "!",
                        fontSize = 34.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        color = if (result.success) Ot.MintInk else red,
                    )
                }
                Text(result.title, style = MaterialTheme.typography.headlineSmall, color = Ot.Navy, textAlign = TextAlign.Center)
                Text(result.message, style = MaterialTheme.typography.bodyMedium, color = Ot.Muted, textAlign = TextAlign.Center)
                result.txHash?.let { hash ->
                    androidx.compose.material3.TextButton(onClick = { uri.openUri("https://testnet.bscscan.com/tx/$hash") }) {
                        Text("Lihat di BscScan ↗  ${hash.take(8)}…${hash.takeLast(6)}")
                    }
                }
                if (primaryLabel != null && onPrimary != null) {
                    androidx.compose.material3.Button(
                        onClick = onPrimary,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text(primaryLabel) }
                }
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        }
    }
}
