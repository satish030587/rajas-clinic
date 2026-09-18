package com.rajashomoeocare.clinic.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand — sampled from the Rajas Homoeo Care logo.
val BrandBlue = Color(0xFF0E5D7D)
val BrandRed = Color(0xFFED1C24)

val LightScheme = lightColorScheme(
    primary = Color(0xFF0E5D7D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2E8FB),
    onPrimaryContainer = Color(0xFF00344A),
    secondary = Color(0xFF4C616C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6F2),
    onSecondaryContainer = Color(0xFF071E27),
    tertiary = Color(0xFFB3261E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF410002),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FAFC),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF6FAFC),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDCE4E8),
    onSurfaceVariant = Color(0xFF40484C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F7),
    surfaceContainer = Color(0xFFEAEEF1),
    surfaceContainerHigh = Color(0xFFE4E9EC),
    surfaceContainerHighest = Color(0xFFDEE3E6),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFC0C8CC),
    inverseSurface = Color(0xFF2C3134),
    inverseOnSurface = Color(0xFFEDF1F4),
    inversePrimary = Color(0xFF87D1F0),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFF87D1F0),
    onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF004C69),
    onPrimaryContainer = Color(0xFFC2E8FB),
    secondary = Color(0xFFB3CAD6),
    onSecondary = Color(0xFF1E333D),
    secondaryContainer = Color(0xFF344A54),
    onSecondaryContainer = Color(0xFFCFE6F2),
    tertiary = Color(0xFFFFB4AB),
    onTertiary = Color(0xFF690005),
    tertiaryContainer = Color(0xFF93000A),
    onTertiaryContainer = Color(0xFFFFDAD6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1417),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0F1417),
    onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C8CC),
    surfaceContainerLowest = Color(0xFF0A0F11),
    surfaceContainerLow = Color(0xFF171C1F),
    surfaceContainer = Color(0xFF1B2023),
    surfaceContainerHigh = Color(0xFF262B2E),
    surfaceContainerHighest = Color(0xFF313639),
    outline = Color(0xFF8A9296),
    outlineVariant = Color(0xFF40484C),
    inverseSurface = Color(0xFFDEE3E6),
    inverseOnSurface = Color(0xFF2C3134),
    inversePrimary = Color(0xFF0E5D7D),
)

/**
 * Recall urgency has its own colour ramp because it is the app's primary signal
 * and must stay legible independently of the Material roles.
 */
@Immutable
data class RecallColors(
    val dueToday: Color,
    val onDueToday: Color,
    val overdueSoon: Color,
    val onOverdueSoon: Color,
    val overdueLong: Color,
    val onOverdueLong: Color,
    val settled: Color,
    val onSettled: Color,
)

val LightRecallColors = RecallColors(
    dueToday = Color(0xFFCFE8FA),
    onDueToday = Color(0xFF0B4A63),
    overdueSoon = Color(0xFFFFE2B8),
    onOverdueSoon = Color(0xFF6B3F00),
    overdueLong = Color(0xFFFFDAD6),
    onOverdueLong = Color(0xFF8C1D18),
    settled = Color(0xFFCFEBD6),
    onSettled = Color(0xFF1B5E37),
)

val DarkRecallColors = RecallColors(
    dueToday = Color(0xFF0B4A63),
    onDueToday = Color(0xFFCFE8FA),
    overdueSoon = Color(0xFF4E2E00),
    onOverdueSoon = Color(0xFFFFE2B8),
    overdueLong = Color(0xFF6B1410),
    onOverdueLong = Color(0xFFFFDAD6),
    settled = Color(0xFF14432A),
    onSettled = Color(0xFFCFEBD6),
)

val LocalRecallColors = staticCompositionLocalOf { LightRecallColors }
