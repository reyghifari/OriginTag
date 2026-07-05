package com.origintag.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palet terinspirasi BNB Chain
private val BnbYellow = Color(0xFFF0B90B)
private val InkDark = Color(0xFF0B0E11)

private val LightColors = lightColorScheme(
    primary = BnbYellow,
    onPrimary = InkDark,
)

private val DarkColors = darkColorScheme(
    primary = BnbYellow,
    onPrimary = InkDark,
    background = InkDark,
)

@Composable
fun OriginTagTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
