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
    primary = HarvestForest,
    onPrimary = Color.White,
    primaryContainer = HarvestForestContainer,
    onPrimaryContainer = OnHarvestForestContainer,
    secondary = TerracottaClay,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = GoldenTurmeric,
    onTertiary = Color.White,
    tertiaryContainer = WheatContainer,
    onTertiaryContainer = OnWheatContainer,
    background = ParchmentBg,
    onBackground = DeepLoamText,
    surface = IvorySurface,
    onSurface = DeepLoamText,
    surfaceVariant = OatSurfaceVariant,
    onSurfaceVariant = MutedMossText,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    error = ErrorCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = CanopyMint,
    onPrimary = OnCanopyDark,
    primaryContainer = CanopyForestContainer,
    onPrimaryContainer = HarvestForestContainer,
    secondary = CoralClay,
    onSecondary = OnTerracottaContainer,
    secondaryContainer = CoralClayContainer,
    onSecondaryContainer = TerracottaContainer,
    tertiary = BrightTurmeric,
    onTertiary = OnWheatContainer,
    tertiaryContainer = DarkWheatContainer,
    onTertiaryContainer = WheatContainer,
    background = NightCanopyBg,
    onBackground = CrispCreamText,
    surface = NightMossSurface,
    onSurface = CrispCreamText,
    surfaceVariant = NightMossVariant,
    onSurfaceVariant = SoftSageText,
    outline = DarkOutline,
    outlineVariant = NightMossVariant,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val KisanShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KisanDirectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KisanTypography,
        shapes = KisanShapes,
        content = content
    )
}
