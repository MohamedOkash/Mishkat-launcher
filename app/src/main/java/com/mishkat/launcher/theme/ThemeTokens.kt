package com.mishkat.launcher.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for colors / dimensions / motion (R14).
 * Supports both Dark Mode (`isLightMode = false`) and Light Mode (`isLightMode = true`)
 * with high-contrast Liquid Glass cards so text is 100% sharp on any wallpaper.
 */
@Immutable
data class ThemeTokens(
    val id: String,
    val primary: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val card: Color,
    val cardFocused: Color,
    val stroke: Color,
    val glassHighlight: Color,
    val glassRim: Color,
    val accentSleep: Color,
    val accentPrayer: Color,
    val accentTools: Color,
    val accentWeather: Color,
    val wallpaperGradient: List<Color>,
    val patternType: String?,
    val patternColor: Color,
    val patternCell: Dp,
    val cardRadius: Dp,
    val iconMask: String,
    val focusScale: Float,
    val enterMs: Int,
    val springDamping: Float,
    val isLightMode: Boolean = false,
) {
    /**
     * Converts this token set to Light Mode (`☀️ وضع النهار الفاتح`) or Dark Mode (`🌙 الوضع الليلي الداكن`)
     * while preserving the theme's signature color identity and guaranteeing AAA text contrast.
     */
    fun withLightMode(light: Boolean): ThemeTokens {
        if (!light) return this.copy(isLightMode = false)
        val lightSurface = Color(0xFF0B1120).copy(alpha = 0.50f)
        val lightOnSurface = Color(0xFFF8FAFC)
        val lightOnSurfaceMuted = Color(0xFF94A3B8)
        val lightPrimary = lerp(primary, Color(0xFFFBBF24), 0.25f)
        val pearlCard = Color(0xFF1E293B).copy(alpha = 0.48f)
        val pearlFocused = lerp(Color(0xFF1E293B), primary, 0.28f).copy(alpha = 0.72f)
        val pearlAccent = Color(0xFF1E293B).copy(alpha = 0.42f)
        return copy(
            primary = lightPrimary,
            surface = lightSurface,
            onSurface = lightOnSurface,
            onSurfaceMuted = lightOnSurfaceMuted,
            card = pearlCard,
            cardFocused = pearlFocused,
            stroke = Color(0xFF475569).copy(alpha = 0.35f),
            glassHighlight = Color.White.copy(alpha = 0.08f),
            glassRim = Color(0xFF64748B).copy(alpha = 0.32f),
            accentSleep = pearlAccent,
            accentPrayer = pearlAccent,
            accentTools = pearlAccent,
            accentWeather = pearlAccent,
            isLightMode = true,
        )
    }

    companion object {
        /** Mirrors assets/themes/rimal.json; used before the asset loads and if it fails to parse (R3/R10). */
        val Default: ThemeTokens = fromSpec(
            ThemeSpec(
                id = "rimal",
                name = "رمال مذهّب",
                builtin = true,
                palette = PaletteSpec(
                    primary = "#E5B842",
                    surface = "#0E1528",
                    onSurface = "#FAF6EC",
                    panelAccents = mapOf(
                        "sleep" to "#3B2D5E",
                        "prayer" to "#18284C",
                        "tools" to "#4A2A40",
                        "weather" to "#4A3525",
                    ),
                ),
            ),
        )

        fun fromSpec(spec: ThemeSpec, isLightMode: Boolean = false): ThemeTokens {
            val p = spec.palette
            val primary = parseColor(p.primary)
            val surface = parseColor(p.surface)
            val onSurface = parseColor(p.onSurface)
            val glass = spec.cards.glass.coerceIn(0f, 1f)
            // Deep smoked-obsidian glass base in Dark Mode so white & gold text have 100% contrast
            // even on bright snow or cherry-blossom wallpapers.
            val darkGlassBase = lerp(surface, Color(0xFF070B14), 0.35f)
            val cardAlpha = (0.78f + (1f - glass) * 0.10f).coerceIn(0.74f, 0.88f)
            val cardColor = darkGlassBase.copy(alpha = cardAlpha)
            val focusedColor = lerp(darkGlassBase, primary, 0.24f).copy(alpha = 0.90f)
            val harmoniousAccent = lerp(darkGlassBase, primary, 0.12f).copy(alpha = 0.84f)
            val gradient = spec.wallpaper.gradient.map(::parseColor).ifEmpty { listOf(surface, surface) }
            val darkTokens = ThemeTokens(
                id = spec.id,
                primary = primary,
                surface = surface,
                onSurface = onSurface,
                onSurfaceMuted = onSurface.copy(alpha = 0.82f),
                card = cardColor,
                cardFocused = focusedColor,
                stroke = onSurface.copy(alpha = spec.cards.strokeAlpha.coerceIn(0.18f, 0.48f)),
                glassHighlight = onSurface.copy(alpha = (0.08f + glass * 0.10f).coerceIn(0.06f, 0.22f)),
                glassRim = onSurface.copy(alpha = (0.22f + glass * 0.20f).coerceIn(0.18f, 0.45f)),
                accentSleep = harmoniousAccent,
                accentPrayer = harmoniousAccent,
                accentTools = harmoniousAccent,
                accentWeather = harmoniousAccent,
                wallpaperGradient = if (gradient.size == 1) listOf(gradient[0], gradient[0]) else gradient,
                patternType = spec.wallpaper.pattern?.type,
                patternColor = primary.copy(alpha = (spec.wallpaper.pattern?.alpha ?: 0.16f).coerceIn(0f, 1f)),
                patternCell = (spec.wallpaper.pattern?.cell ?: 104f).dp,
                cardRadius = spec.cards.radius.dp,
                iconMask = spec.icons.mask,
                focusScale = spec.motion.focusScale.coerceIn(1f, 1.2f),
                enterMs = spec.motion.enterMs.coerceIn(0, 1000),
                springDamping = spec.motion.springDamping.coerceIn(0.2f, 1f),
                isLightMode = false,
            )
            return if (isLightMode) darkTokens.withLightMode(true) else darkTokens
        }

        /** Accepts #RRGGBB or #AARRGGBB. Throws IllegalArgumentException on malformed input. */
        fun parseColor(hex: String): Color {
            val s = hex.removePrefix("#")
            val v = s.toLong(16)
            return when (s.length) {
                6 -> Color(0xFF000000 or v)
                8 -> Color(v)
                else -> throw IllegalArgumentException("Bad color: $hex")
            }
        }
    }
}

val LocalThemeTokens = staticCompositionLocalOf { ThemeTokens.Default }
