package com.example.orbitlist.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Definisikan warna di luar class agar rapi
val DeepBlueBlack = Color(0xFF050B18) // Biru kehitaman utama
val SoftBlueBlack = Color(0xFF0E1421) // Sedikit lebih terang untuk kartu/surface

private val DarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    secondary = NeonCyan,
    tertiary = VividViolet,
    background = DeepBlueBlack,   // <--- Latar belakang utama
    surface = SoftBlueBlack,      // <--- Latar belakang komponen (Card, dsb)
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricIndigo,
    secondary = NeonCyan,
    tertiary = VividViolet,
    background = DeepBlueBlack,   // Disamakan jika ingin tema gelap di kedua mode
    surface = SoftBlueBlack,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun SchoolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
