package com.mishkat.launcher.theme

import kotlinx.serialization.Serializable

/**
 * Theme JSON schema v1 (§8). Fields marked "additive" are optional extensions recorded in DECISIONS D-004;
 * every v1 file without them still parses.
 */
@Serializable
data class ThemeSpec(
    val id: String,
    val name: String,
    /** additive */
    val nameEn: String? = null,
    val builtin: Boolean = false,
    val palette: PaletteSpec,
    val wallpaper: WallpaperSpec = WallpaperSpec(),
    val cards: CardsSpec = CardsSpec(),
    val icons: IconsSpec = IconsSpec(),
    val motion: MotionSpec = MotionSpec(),
)

@Serializable
data class PaletteSpec(
    val primary: String,
    val surface: String,
    val onSurface: String,
    val panelAccents: Map<String, String> = emptyMap(),
)

@Serializable
data class WallpaperSpec(
    /** "gradient" | "image" | "video" (video only if RAM >= 2GB, R23). */
    val type: String = "gradient",
    val asset: String? = null,
    val parallax: Float = 0f,
    /** additive: gradient stops, top-start to bottom-end. Falls back to palette.surface. */
    val gradient: List<String> = emptyList(),
    /** additive: optional geometric overlay drawn once and cached. */
    val pattern: PatternSpec? = null,
)

@Serializable
data class PatternSpec(
    /** "star8" = 8-point star lattice. */
    val type: String = "star8",
    val alpha: Float = 0.06f,
    /** Cell size in dp. */
    val cell: Float = 96f,
)

@Serializable
data class CardsSpec(
    val radius: Float = 20f,
    val glass: Float = 0.35f,
    val strokeAlpha: Float = 0.18f,
)

@Serializable
data class IconsSpec(
    val mask: String = "squircle_22",
    val pack: String = "mishkat_cute_v1",
)

@Serializable
data class MotionSpec(
    val focusScale: Float = 1.06f,
    val enterMs: Int = 240,
    val springDamping: Float = 0.85f,
)
