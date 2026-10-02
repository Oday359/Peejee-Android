package com.peejee.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PeejeePurple = Color(0xFF6750A4)
private val PeejeePurpleDark = Color(0xFF4F378B)
private val PeejeePurpleLight = Color(0xFFEADDFF)

private val PeejeeBackground = Color(0xFFF9F5FF)
private val PeejeeSurface = Color(0xFFFFFFFF)
private val PeejeeSurfaceVariant = Color(0xFFF1EAF9)

private val PeejeeText = Color(0xFF1D1B20)
private val PeejeeSecondaryText = Color(0xFF625B71)

private val PeejeeLightColorScheme = lightColorScheme(
    primary = PeejeePurple,
    onPrimary = Color.White,

    primaryContainer = PeejeePurpleLight,
    onPrimaryContainer = PeejeePurpleDark,

    secondary = Color(0xFF625B71),
    onSecondary = Color.White,

    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),

    background = PeejeeBackground,
    onBackground = PeejeeText,

    surface = PeejeeSurface,
    onSurface = PeejeeText,

    surfaceVariant = PeejeeSurfaceVariant,
    onSurfaceVariant = PeejeeSecondaryText,

    outline = Color(0xFF79747E)
)

private val PeejeeTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(),
    bodyMedium = TextStyle(),
    bodySmall = TextStyle(),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold
    )
)

private val PeejeeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PeejeeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PeejeeLightColorScheme,
        typography = PeejeeTypography,
        shapes = PeejeeShapes,
        content = content
    )
}
