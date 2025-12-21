package com.vencehoje.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cores que você definiu na MainActivity para o seu tema verde
private val VerdeVenceHoje = Color(0xFF1B5E20)
private val VerdeClaroContainer = Color(0xFFC8E6C9)
private val VerdeFundoSecundario = Color(0xFFE8F5E9)
private val CinzaSuperficie = Color(0xFFF5F5F5)

private val LightColorScheme = lightColorScheme(
    primary = VerdeVenceHoje,
    primaryContainer = VerdeClaroContainer,
    secondaryContainer = VerdeFundoSecundario,
    surfaceVariant = CinzaSuperficie
)

@Composable
fun VenceHojeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        // typography = Typography,
        content = content
    )
}