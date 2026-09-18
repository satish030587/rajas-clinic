package com.rajashomoeocare.clinic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

/**
 * A soft brand wash used behind every screen: a hint of the primary colour at
 * the top, settling into the normal background before it reaches any content.
 * Deliberately subtle — it should read as polish, not decoration, and must
 * never fight with patient data for attention. Works in both themes because
 * it is built from the active [MaterialTheme.colorScheme], not fixed colours.
 */
@Composable
fun clinicBackgroundBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0f to scheme.primaryContainer.copy(alpha = 0.5f),
            0.32f to scheme.background,
            1f to scheme.background,
        ),
    )
}
