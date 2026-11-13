package com.englishquest.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Couleurs « gameplay », identiques en mode clair et sombre. */
object GameColors {
    val Fire = Color(0xFFFF7A00)
    val Gold = Color(0xFFFFC233)
    val Ice = Color(0xFF4FC3F7)
    val Gem = Color(0xFF7C5CFF)
    val Heart = Color(0xFFFF4D6D)
    val Correct = Color(0xFF22B573)
    val Wrong = Color(0xFFE5484D)
    val Locked = Color(0xFF9E9AB5)

    /** Une couleur par unité pour la carte. */
    val unitPalette = listOf(
        Color(0xFF5B4BDB), Color(0xFFFF8A00), Color(0xFF00A86B), Color(0xFFE5484D), Color(0xFF0091EA),
        Color(0xFFD63384), Color(0xFF7B61FF), Color(0xFF00897B), Color(0xFFF4511E), Color(0xFF3949AB),
    )
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BDB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E1FF),
    onPrimaryContainer = Color(0xFF1C1260),
    secondary = Color(0xFFFF8A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3C2),
    onSecondaryContainer = Color(0xFF3D2200),
    tertiary = Color(0xFF00A86B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC9F5E4),
    onTertiaryContainer = Color(0xFF00321F),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFFDADB),
    onErrorContainer = Color(0xFF410004),
    background = Color(0xFFF7F6FE),
    onBackground = Color(0xFF1B1A24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1A24),
    surfaceVariant = Color(0xFFEDEBF7),
    onSurfaceVariant = Color(0xFF545068),
    outline = Color(0xFFCBC7DD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9D8FFF),
    onPrimary = Color(0xFF1A1050),
    primaryContainer = Color(0xFF3B2F99),
    onPrimaryContainer = Color(0xFFE6E1FF),
    secondary = Color(0xFFFFA94D),
    onSecondary = Color(0xFF3D2200),
    secondaryContainer = Color(0xFF5C3700),
    onSecondaryContainer = Color(0xFFFFE3C2),
    tertiary = Color(0xFF3DDC97),
    onTertiary = Color(0xFF00321F),
    tertiaryContainer = Color(0xFF00573A),
    onTertiaryContainer = Color(0xFFC9F5E4),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF410004),
    errorContainer = Color(0xFF6B1A1D),
    onErrorContainer = Color(0xFFFFDADB),
    background = Color(0xFF0F0E17),
    onBackground = Color(0xFFEDEBF7),
    surface = Color(0xFF1A1926),
    onSurface = Color(0xFFEDEBF7),
    surfaceVariant = Color(0xFF272536),
    onSurfaceVariant = Color(0xFFC4C0D8),
    outline = Color(0xFF45415A),
)

private val base = Typography()

private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun EnglishQuestTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
