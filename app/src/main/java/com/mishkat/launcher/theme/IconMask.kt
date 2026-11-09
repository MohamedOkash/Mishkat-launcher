package com.mishkat.launcher.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * §8 Icon mask engine: maps `ThemeSpec.icons.mask` (`squircle_22`, `circle`, `rounded_16`)
 * to a consistent Compose Shape so all TV app icons share a unified visual frame.
 */
object IconMask {
    fun shapeFor(maskId: String): Shape = when (maskId) {
        "circle" -> CircleShape
        "rounded_16" -> RoundedCornerShape(16.dp)
        else -> RoundedCornerShape(22.dp) // squircle_22 default
    }
}
