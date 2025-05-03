package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Couleurs extraites de votre logo
val ArkaBeige = Color(0xFFF5ECD9)
val ArkaDarkBlue = Color(0xFF0C2234)
val ArkaMediumBeige = Color(0xFFE5D2A7)  // Version plus foncée du beige pour certains éléments

// Palette claire
private val LightColorPalette = lightColors(
    primary = ArkaDarkBlue,
    primaryVariant = ArkaDarkBlue.copy(alpha = 0.8f),
    secondary = ArkaMediumBeige,
    background = ArkaBeige,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = ArkaDarkBlue,
    onBackground = ArkaDarkBlue,
    onSurface = ArkaDarkBlue
)

// Palette sombre (optionnelle)
private val DarkColorPalette = darkColors(
    primary = ArkaMediumBeige,
    primaryVariant = ArkaMediumBeige.copy(alpha = 0.8f),
    secondary = ArkaDarkBlue,
    background = ArkaDarkBlue,
    surface = ArkaDarkBlue.copy(alpha = 0.7f),
    onPrimary = ArkaDarkBlue,
    onSecondary = ArkaBeige,
    onBackground = ArkaBeige,
    onSurface = ArkaBeige
)

@Composable
fun ArkaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}