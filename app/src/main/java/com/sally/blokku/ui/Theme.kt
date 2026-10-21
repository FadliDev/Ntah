package com.sally.blokku.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Seed hijau rumput (placeholder; ganti lewat Material Theme Builder)
private val Light = lightColorScheme(
    primary = Color(0xFF2E7D32), onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F0B2), onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFF52634F), secondaryContainer = Color(0xFFD5E8CF),
    background = Color(0xFFF7FBF1), surface = Color(0xFFF7FBF1),
    onBackground = Color(0xFF181D17), onSurface = Color(0xFF181D17),
    surfaceVariant = Color(0xFFDDE5D8), onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF72796F),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9CD497), onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF1F5130), onPrimaryContainer = Color(0xFFB7F0B2),
    secondary = Color(0xFFB9CCB4), secondaryContainer = Color(0xFF3B4B39),
    background = Color(0xFF10140F), surface = Color(0xFF10140F),
    onBackground = Color(0xFFE0E4DB), onSurface = Color(0xFFE0E4DB),
    surfaceVariant = Color(0xFF424940), onSurfaceVariant = Color(0xFFC1C9BE),
    outline = Color(0xFF8B9389),
)

private val AppShapes = Shapes(
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BlokkuTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, shapes = AppShapes, content = content)
}
