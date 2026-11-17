package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BrandCrimson,
    onPrimary = Color.White,
    primaryContainer = BrandCrimsonLight,
    onPrimaryContainer = BrandCrimsonDark,
    secondary = SacredGold,
    onSecondary = EspressoInk,
    secondaryContainer = SacredGoldLight,
    onSecondaryContainer = EspressoInk,
    tertiary = StockAvailableGreen,
    onTertiary = Color.White,
    tertiaryContainer = StockAvailableBg,
    onTertiaryContainer = StockAvailableGreen,
    background = WarmCream,
    onBackground = EspressoInk,
    surface = CardSurface,
    onSurface = EspressoInk,
    surfaceVariant = WarmCreamDeep,
    onSurfaceVariant = WarmSlate,
    outline = SubtleOutline,
    error = BrandCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkCrimson,
    onPrimary = Color.White,
    primaryContainer = BrandCrimsonDark,
    onPrimaryContainer = BrandCrimsonLight,
    secondary = SacredGold,
    onSecondary = EspressoInk,
    secondaryContainer = Color(0xFF47330B),
    onSecondaryContainer = SacredGoldLight,
    tertiary = Color(0xFF66BB6A),
    onTertiary = Color(0xFF0A2E0D),
    background = DarkBackground,
    onBackground = DarkCreamText,
    surface = DarkSurface,
    onSurface = DarkCreamText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSubtleText,
    outline = Color(0xFF4E3D36),
    error = DarkCrimson,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MahadevTradersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
