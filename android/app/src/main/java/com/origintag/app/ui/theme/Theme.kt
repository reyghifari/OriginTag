package com.origintag.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.origintag.app.R

/** Palet: biru royal + navy + abu terang (gaya "Sneaker Assistant"). */
object Ot {
    val Blue = Color(0xFF3B57E6)
    val BlueDeep = Color(0xFF2E47C9)
    val Navy = Color(0xFF1E2A78)
    val Sky = Color(0xFFA9D2FF)
    val Mint = Color(0xFF9EE6A2)
    val MintInk = Color(0xFF1F6B2A)
    val Paper = Color(0xFFF1F1F4)
    val NavBar = Color(0xFFE7E8F1)
    val Line = Color(0xFFE1E2EA)
    val Muted = Color(0xFF8A8FA8)
    val Amber = Color(0xFFF2A516)
}

@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(
    R.font.nunito,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Nunito = FontFamily(nunito(400), nunito(500), nunito(600), nunito(700), nunito(800), nunito(900))

private val Base = Typography()
// Skala 0.88: ukuran default M3 terasa terlalu besar di HP dengan display size diperbesar.
private fun TextStyle.n(w: FontWeight? = null) =
    copy(fontFamily = Nunito, fontWeight = w ?: fontWeight, fontSize = fontSize * 0.88f, lineHeight = lineHeight * 0.88f)

private val AppTypography = Typography(
    displayLarge = Base.displayLarge.n(FontWeight.Black),
    displayMedium = Base.displayMedium.n(FontWeight.Black),
    displaySmall = Base.displaySmall.n(FontWeight.Black),
    headlineLarge = Base.headlineLarge.n(FontWeight.Black),
    headlineMedium = Base.headlineMedium.n(FontWeight.Black),
    headlineSmall = Base.headlineSmall.n(FontWeight.ExtraBold),
    titleLarge = Base.titleLarge.n(FontWeight.ExtraBold),
    titleMedium = Base.titleMedium.n(FontWeight.Bold),
    titleSmall = Base.titleSmall.n(FontWeight.Bold),
    bodyLarge = Base.bodyLarge.n(FontWeight.Medium),
    bodyMedium = Base.bodyMedium.n(FontWeight.Medium),
    bodySmall = Base.bodySmall.n(FontWeight.Medium),
    labelLarge = Base.labelLarge.n(FontWeight.Bold),
    labelMedium = Base.labelMedium.n(FontWeight.Bold),
    labelSmall = Base.labelSmall.n(FontWeight.Bold),
)

private val AppColors = lightColorScheme(
    primary = Ot.Blue,
    onPrimary = Color.White,
    primaryContainer = Ot.Sky,
    onPrimaryContainer = Ot.Navy,
    secondary = Ot.Navy,
    onSecondary = Color.White,
    background = Ot.Paper,
    onBackground = Ot.Navy,
    surface = Color.White,
    onSurface = Ot.Navy,
    surfaceVariant = Color.White,
    onSurfaceVariant = Ot.Muted,
    outline = Ot.Line,
    // Card bawaan M3 memakai warna ini → putih seperti kartu referensi
    surfaceContainerHighest = Color.White,
    surfaceContainerHigh = Color.White,
    outlineVariant = Ot.Line,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ponytail: selalu light — palet biru/abu referensi belum punya varian gelap.
@Composable
fun OriginTagTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
