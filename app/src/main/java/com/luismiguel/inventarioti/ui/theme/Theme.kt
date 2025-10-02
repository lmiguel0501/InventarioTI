package com.luismiguel.inventarioti.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

// Paleta clara NEUTRA (blancos reales)
private val LightColors = lightColorScheme(
    primary = Color(0xFF1F67D2),
    onPrimary = Color.White,
    secondary = Color(0xFF5B6B7A),
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF),      // blanco puro
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),          // blanco puro
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE5E7EB),   // gris claro p/ tarjetas/bordes
    onSurfaceVariant = Color(0xFF49515A),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFB3261E),
    onError = Color.White
)

// Paleta oscura NEUTRA
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DB3FF),
    onPrimary = Color(0xFF0A213F),
    secondary = Color(0xFF9AA6B2),
    onSecondary = Color(0xFF0D1117),
    background = Color(0xFF0D1117),       // gris/negro neutro
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF0F141A),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF1C232B),
    onSurfaceVariant = Color(0xFFBAC4CE),
    outline = Color(0xFF2A343E),
    outlineVariant = Color(0xFF2A343E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Forzamos colores neutros: no usar Dynamic Color (evita el rosado)
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Ignoramos dynamic color a propósito para evitar tintes del wallpaper
    val colorScheme = if (darkTheme) DarkColors else LightColors

    // Ajusta iconos de status/navigation bar según tema
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                // Iconos oscuros en claro, claros en oscuro
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
