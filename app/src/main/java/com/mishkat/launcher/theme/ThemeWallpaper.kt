package com.mishkat.launcher.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class WallpaperPreset(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val assetFile: String,
    val isAnimated: Boolean = true,
)

val WALLPAPER_PRESETS = listOf(
    WallpaperPreset(
        id = "auto",
        titleAr = "تلقائي مع الثيم النشط ✦",
        titleEn = "Automatic with Active Theme ✦",
        subtitleAr = "زخرفة وخطوط عربية للثيمات الإسلامية • وخلفيات حية 1080p للفصول والرياضة",
        subtitleEn = "Islamic calligraphy & ornament for Islamic themes • 1080p live scenes for seasons",
        assetFile = "real_makkah_kaaba.jpg",
    ),
    WallpaperPreset(
        id = "calligraphy_ruqah",
        titleAr = "خط الرقعة والزخرفة الإسلامية المذهّبة ✒",
        titleEn = "Arabic Ruq'ah Calligraphy & Gold Arabesque ✒",
        subtitleAr = "لوحة حروف عربية (الرقعة والديواني) مع شمسة هندسية مذهّبة",
        subtitleEn = "Ornate Arabic Ruq'ah calligraphy letters with golden geometric shamsa",
        assetFile = "real_islamic_mosque.jpg",
    ),
    WallpaperPreset(
        id = "calligraphy_thuluth",
        titleAr = "حزام الكسوة وخط الثلث المذهّب 🕋",
        titleEn = "Kiswah Belt & Golden Thuluth Calligraphy 🕋",
        subtitleAr = "زخرفة كسوة الكعبة المشرفة مع آيات بخط الثلث المذهّب",
        subtitleEn = "Royal black & gold Kaaba Kiswah embroidery with Thuluth calligraphy",
        assetFile = "real_makkah_kaaba.jpg",
    ),
    WallpaperPreset(
        id = "arabesque_mandala",
        titleAr = "شمسة الأرابيسك والقناديل المضيئة 🕌",
        titleEn = "Islamic Shamsa Mandala & Mishkat Lanterns 🕌",
        subtitleAr = "زخارف هندسية إسلامية دقيقة مع قناديل المشكاة ونور الآيات",
        subtitleEn = "Intricate 16-fold Islamic geometric rosettes & hanging Mishkat lanterns",
        assetFile = "real_madinah_mosque.jpg",
    ),
    WallpaperPreset(
        id = "weather_sync",
        titleAr = "طقس مدينتك الحي مباشر 🌦 (1080p)",
        titleEn = "Live City Weather Sync 🌦 (1080p)",
        subtitleAr = "يتحول تلقائياً لثلج أو مطر أو برق أو ربيع أو غروب حسب طقس مدينتك",
        subtitleEn = "Adapts live between snow, rain, lightning, spring, or sunset by city weather",
        assetFile = "real_winter_rain.jpg",
    ),
    WallpaperPreset(
        id = "winter_snow",
        titleAr = "شتاء وغابة الثلوج الحية ❄ (1080p HD)",
        titleEn = "Live Winter Snowy Forest ❄ (1080p HD)",
        subtitleAr = "مشهد طبيعي حقيقي بدقة 1080p مع تساقط ثلوج ثلاثي الأبعاد",
        subtitleEn = "True 1080p snowy forest landscape with multi-layer 3D snowfall",
        assetFile = "real_winter_snow.jpg",
    ),
    WallpaperPreset(
        id = "winter_rain",
        titleAr = "أمطار الشتاء وقطرات الزجاج الحية 🌧 (1080p HD)",
        titleEn = "Live Winter Rain & Glass Droplets 🌧 (1080p HD)",
        subtitleAr = "مشهد مطر حقيقي عالي الدقة مع قطرات وسيول المطر المتحركة",
        subtitleEn = "True 1080p rain scene with animated raindrops and glass streaks",
        assetFile = "real_winter_rain.jpg",
    ),
    WallpaperPreset(
        id = "storm_lightning",
        titleAr = "عاصفة رعدية وبرق السماء الحي ⚡ (1080p HD)",
        titleEn = "Live Thunderstorm & Lightning ⚡ (1080p HD)",
        subtitleAr = "سحب رعدية حقيقية مع ومضات برق متفرعة وأمطار غزيرة",
        subtitleEn = "True 1080p storm clouds with live branching lightning flashes",
        assetFile = "real_storm_lightning.jpg",
    ),
    WallpaperPreset(
        id = "spring_petals",
        titleAr = "ربيع وأزهار الساكورا الحية 🌸 (1080p HD)",
        titleEn = "Live Spring Cherry Blossoms 🌸 (1080p HD)",
        subtitleAr = "أشجار زهر الكرز الطبيعية بدقة 1080p مع بتلات متطايرة",
        subtitleEn = "True 1080p cherry blossom landscape with drifting pink petals",
        assetFile = "real_spring_blossom.jpg",
    ),
    WallpaperPreset(
        id = "summer_waves",
        titleAr = "شاطئ الصيف وغروب الشمس الحي 🌊 (1080p HD)",
        titleEn = "Live Summer Sunset Beach 🌊 (1080p HD)",
        subtitleAr = "غروب البحر الطبيعي بدقة 1080p مع بريق الأمواج الذهبي",
        subtitleEn = "True 1080p ocean sunset with shimmering golden water reflections",
        assetFile = "real_summer_sunset.jpg",
    ),
    WallpaperPreset(
        id = "autumn_leaves",
        titleAr = "غابة الخريف الذهبية الحية 🍂 (1080p HD)",
        titleEn = "Live Golden Autumn Forest 🍂 (1080p HD)",
        subtitleAr = "طبيعة الخريف الساحرة بدقة 1080p مع تساقط أوراق الشجر",
        subtitleEn = "True 1080p autumn forest landscape with drifting golden leaves",
        assetFile = "real_autumn_forest.jpg",
    ),
    WallpaperPreset(
        id = "sports_stadium",
        titleAr = "استاد البطولة والأضواء الكاشفة الحية ⚽ (1080p HD)",
        titleEn = "Live Championship Stadium Arena ⚽ (1080p HD)",
        subtitleAr = "ملعب كرة قدم حقيقي 1080p مع أضواء الملاعب الكاشفة المتحركة",
        subtitleEn = "True 1080p football stadium with live sweeping floodlight beams",
        assetFile = "real_sports_stadium.jpg",
    ),
    WallpaperPreset(
        id = "cinema_imax",
        titleAr = "قاعة السينما والأفلام الحية 🎬 (1080p HD)",
        titleEn = "Live Cinema & Action Theater 🎬 (1080p HD)",
        subtitleAr = "أجواء السينما الاحترافية 1080p مع إضاءة العرض السينمائي",
        subtitleEn = "True 1080p cinema hall with live projector light beams & bokeh",
        assetFile = "real_cinema_night.jpg",
    ),
    WallpaperPreset(
        id = "kaaba_silk",
        titleAr = "الحرم المكي والكعبة المشرفة 🕋 (صورة 1080p)",
        titleEn = "Holy Kaaba Sanctuary in Makkah 🕋 (1080p Photo)",
        subtitleAr = "صورة فوتوغرافية حقيقية عريضة 1080p للمسجد الحرام",
        subtitleEn = "Full HD 1080p photograph of Al-Masjid Al-Haram in Makkah",
        assetFile = "real_makkah_kaaba.jpg",
    ),
    WallpaperPreset(
        id = "season_spring_4k",
        titleAr = "ربيع الطبيعة والزهور 4K",
        titleEn = "Spring Blossom 4K",
        subtitleAr = "أزهار الربيع والطبيعة الخضراء الفاتنة بدقة 4K",
        subtitleEn = "High-definition 4K vibrant green spring landscape",
        assetFile = "season_spring_4k.jpg",
    ),
    WallpaperPreset(
        id = "season_summer_4k",
        titleAr = "شمس وأمواج الصيف 4K",
        titleEn = "Summer Coast 4K",
        subtitleAr = "شاطئ البحر الذهبي وأمواج الصيف الساحرة بدقة 4K",
        subtitleEn = "High-definition 4K golden summer sea coast",
        assetFile = "season_summer_4k.jpg",
    ),
    WallpaperPreset(
        id = "season_autumn_4k",
        titleAr = "خريف الغابات الذهبية 4K",
        titleEn = "Autumn Forest 4K",
        subtitleAr = "غابات الخريف وسحر الأوراق المتساقطة بدقة 4K",
        subtitleEn = "High-definition 4K golden autumn forest scenery",
        assetFile = "season_autumn_4k.jpg",
    ),
    WallpaperPreset(
        id = "season_winter_4k",
        titleAr = "شتاء وثلوج نقية 4K",
        titleEn = "Winter Snow 4K",
        subtitleAr = "ثلوج الشتاء النقية والسكينة البيضاء بدقة 4K",
        subtitleEn = "High-definition 4K pure winter snow scenery",
        assetFile = "season_winter_4k.jpg",
    ),
    WallpaperPreset(
        id = "nature_mountains_4k",
        titleAr = "قمم الجبال الساحرة 4K",
        titleEn = "Majestic Mountains 4K",
        subtitleAr = "جبال الألب والبحيرات الفيروزية الطبيعية بدقة 4K",
        subtitleEn = "High-definition 4K majestic alpine mountain lake",
        assetFile = "nature_mountains_4k.jpg",
    ),
    WallpaperPreset(
        id = "nature_waterfall_4k",
        titleAr = "شلالات الطبيعة العذبة 4K",
        titleEn = "Crystal Waterfall 4K",
        subtitleAr = "شلالات المياه العذبة بين أحضان الطبيعة بدقة 4K",
        subtitleEn = "High-definition 4K crystal waterfall in lush nature",
        assetFile = "nature_waterfall_4k.jpg",
    ),
    WallpaperPreset(
        id = "dark_luxury_onyx_4k",
        titleAr = "أونيكس أسود فاخر OLED 4K",
        titleEn = "Onyx Luxury OLED 4K",
        subtitleAr = "أمواج الأونيكس الداكنة والعميقة لشاشات OLED بدقة 4K",
        subtitleEn = "High-definition 4K deep onyx dark luxury texture",
        assetFile = "dark_luxury_onyx_4k.jpg",
    ),
    WallpaperPreset(
        id = "dark_carbon_mesh_4k",
        titleAr = "ألياف الكربون المظلمة 4K",
        titleEn = "Carbon Dark Mesh 4K",
        subtitleAr = "شبكة ألياف الكربون التكنولوجية المظلمة بدقة 4K",
        subtitleEn = "High-definition 4K stealth carbon fiber mesh",
        assetFile = "dark_carbon_mesh_4k.jpg",
    ),
    WallpaperPreset(
        id = "birds_falcon_eagle_4k",
        titleAr = "صقور ونسور الجبال 4K",
        titleEn = "Royal Falcon 4K",
        subtitleAr = "هيبة الصقور والنسور الجارحة في قمم الجبال بدقة 4K",
        subtitleEn = "High-definition 4K royal mountain eagle portrait",
        assetFile = "birds_falcon_eagle_4k.jpg",
    ),
    WallpaperPreset(
        id = "birds_colorful_kingfisher_4k",
        titleAr = "طيور ملونة وببغاوات 4K",
        titleEn = "Tropical Birds 4K",
        subtitleAr = "ألوان الطيور الاستوائية والببغاوات البديعة بدقة 4K",
        subtitleEn = "High-definition 4K vibrant exotic tropical bird",
        assetFile = "birds_colorful_kingfisher_4k.jpg",
    ),
    WallpaperPreset(
        id = "mihrab_arch",
        titleAr = "المسجد النبوي الشريف بالمدينة 🕌 (صورة 1080p)",
        titleEn = "Prophet's Mosque in Madinah 🕌 (1080p Photo)",
        subtitleAr = "صورة فوتوغرافية حقيقية عريضة 1080p للمسجد النبوي الشريف",
        subtitleEn = "Full HD 1080p photograph of Al-Masjid An-Nabawi in Madinah",
        assetFile = "real_madinah_mosque.jpg",
    ),
)

/** Maps WMO weather code to a real photographic seasonal wallpaper when `weather_sync` is active. */
fun resolveWeatherSyncedWallpaper(weatherCode: Int?): String = when (weatherCode) {
    null -> "winter_snow"
    in 71..77, 85, 86 -> "winter_snow"
    in 95..99 -> "storm_lightning"
    in 51..67, in 80..82 -> "winter_rain"
    in 1..3 -> "spring_petals"
    0 -> "summer_waves"
    else -> "autumn_leaves"
}

/** Returns true when the effective style should render ornate Islamic Calligraphy & Geometric Arabesque. */
private fun isIslamicOrnamentStyle(effectiveStyle: String, themeId: String): Boolean = when (effectiveStyle) {
    "calligraphy_ruqah",
    "calligraphy_thuluth",
    "arabesque_mandala",
    "islamic_arch",
    "kaaba_kiswah",
    -> true
    "sports_stadium", "cinema_imax", "winter_snow", "winter_rain", "storm_lightning", "spring_petals", "summer_waves", "autumn_leaves",
    "season_spring_4k", "season_summer_4k", "season_autumn_4k", "season_winter_4k",
    "nature_mountains_4k", "nature_waterfall_4k",
    "dark_luxury_onyx_4k", "dark_carbon_mesh_4k",
    "birds_falcon_eagle_4k", "birds_colorful_kingfisher_4k" -> false
    else -> when (themeId) {
        "kaaba_gold", "noor", "zomorod", "rimal", "layla", "rawda_sharifa",
        "andalus_arch", "fajr_sky", "madinah_marble", "damascus_mosaic",
        "turqoise_dome", "sahara_dunes" -> true
        else -> false
    }
}

/** Resolves the 1920x1080 Full HD photograph filename in `assets/wallpapers/`. */
fun resolveRealPhotoAsset(effectiveStyle: String, themeId: String): String = when (effectiveStyle) {
    "season_spring_4k" -> "season_spring_4k.jpg"
    "season_summer_4k" -> "season_summer_4k.jpg"
    "season_autumn_4k" -> "season_autumn_4k.jpg"
    "season_winter_4k" -> "season_winter_4k.jpg"
    "nature_mountains_4k" -> "nature_mountains_4k.jpg"
    "nature_waterfall_4k" -> "nature_waterfall_4k.jpg"
    "dark_luxury_onyx_4k" -> "dark_luxury_onyx_4k.jpg"
    "dark_carbon_mesh_4k" -> "dark_carbon_mesh_4k.jpg"
    "birds_falcon_eagle_4k" -> "birds_falcon_eagle_4k.jpg"
    "birds_colorful_kingfisher_4k" -> "birds_colorful_kingfisher_4k.jpg"
    "winter_snow" -> "real_winter_snow.jpg"
    "winter_rain" -> "real_winter_rain.jpg"
    "storm_lightning" -> "real_storm_lightning.jpg"
    "spring_petals" -> "real_spring_blossom.jpg"
    "summer_waves" -> "real_summer_sunset.jpg"
    "autumn_leaves" -> "real_autumn_forest.jpg"
    "sports_stadium" -> "real_sports_stadium.jpg"
    "cinema_imax" -> "real_cinema_night.jpg"
    "kaaba_silk", "calligraphy_thuluth", "kaaba_kiswah" -> "real_makkah_kaaba.jpg"
    "mihrab_arch", "islamic_arch" -> "real_madinah_mosque.jpg"
    "calligraphy_ruqah", "arabesque_mandala", "aurora_glass" -> "real_islamic_mosque.jpg"
    "desert_crescent", "desert_night" -> "real_desert_night.jpg"
    else -> when (themeId) {
        "sports_arena" -> "real_sports_stadium.jpg"
        "cinema_action" -> "real_cinema_night.jpg"
        "season_spring_4k" -> "season_spring_4k.jpg"
    "season_summer_4k" -> "season_summer_4k.jpg"
    "season_autumn_4k" -> "season_autumn_4k.jpg"
    "season_winter_4k" -> "season_winter_4k.jpg"
    "nature_mountains_4k" -> "nature_mountains_4k.jpg"
    "nature_waterfall_4k" -> "nature_waterfall_4k.jpg"
    "dark_luxury_onyx_4k" -> "dark_luxury_onyx_4k.jpg"
    "dark_carbon_mesh_4k" -> "dark_carbon_mesh_4k.jpg"
    "birds_falcon_eagle_4k" -> "birds_falcon_eagle_4k.jpg"
    "birds_colorful_kingfisher_4k" -> "birds_colorful_kingfisher_4k.jpg"
    "winter_snow" -> "real_winter_snow.jpg"
        "spring_blossom" -> "real_spring_blossom.jpg"
        "summer_sunset" -> "real_summer_sunset.jpg"
        "kaaba_gold" -> "real_makkah_kaaba.jpg"
        "noor" -> "real_madinah_mosque.jpg"
        "zomorod" -> "real_islamic_mosque.jpg"
        "rawda_sharifa", "madinah_marble", "fajr_sky" -> "real_madinah_mosque.jpg"
        "andalus_arch", "damascus_mosaic", "turqoise_dome" -> "real_islamic_mosque.jpg"
        "aurora_borealis" -> "real_storm_lightning.jpg"
        "cyber_onyx" -> "real_cinema_night.jpg"
        "ruby_velvet" -> "real_autumn_forest.jpg"
        "sahara_dunes", "rimal", "layla" -> "real_desert_night.jpg"
        else -> "real_desert_night.jpg"
    }
}

private object RealWallpaperBitmapCache {
    private val cache = LruCache<String, ImageBitmap>(6)

    fun getOrLoad(context: Context, assetFileName: String): ImageBitmap? {
        cache.get(assetFileName)?.let { return it }
        return runCatching {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }
            context.assets.open("wallpapers/$assetFileName").use { stream ->
                val bmp = BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                if (bmp != null) {
                    cache.put(assetFileName, bmp)
                }
                bmp
            }
        }.getOrNull()
    }
}

/**
 * Liquid Glass surface modifier:
 * Renders a multi-layer frosted glass surface (high-contrast base + diagonal specular sheen
 * + top-edge specular rim highlight) supporting both Dark Mode and Light Mode.
 */
fun Modifier.liquidGlass(
    tokens: ThemeTokens,
    baseColor: Color = tokens.card,
    radius: Dp = tokens.cardRadius,
    glassIntensity: Float = 0.85f,
    borderColor: Color = tokens.glassRim,
): Modifier = this.drawWithCache {
    val rPx = radius.toPx()
    val corner = CornerRadius(rPx, rPx)
    val clampedGlass = glassIntensity.coerceIn(0.3f, 1.2f)
    val sheenBrush = Brush.linearGradient(
        colors = listOf(
            tokens.glassHighlight.copy(alpha = (tokens.glassHighlight.alpha * clampedGlass).coerceIn(0f, 0.55f)),
            Color.Transparent,
            tokens.primary.copy(alpha = if (tokens.isLightMode) 0.06f else (0.05f * clampedGlass).coerceIn(0f, 0.14f)),
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    val rimBrush = Brush.verticalGradient(
        colors = listOf(
            borderColor.copy(alpha = (borderColor.alpha * clampedGlass * 1.35f).coerceIn(0.22f, 0.78f)),
            tokens.stroke.copy(alpha = if (tokens.isLightMode) 0.35f else 0.22f),
        ),
    )
    val strokeStyle = Stroke(width = 1.dp.toPx())
    val minAlpha = if (tokens.isLightMode) 0.52f else 0.80f
    // High-contrast, rich deep Liquid Glass: ensures text is 100% legible over any wallpaper
    val fillAlpha = when {
        clampedGlass >= 1.05f -> (baseColor.alpha * 1.15f).coerceIn(0.88f, 0.94f) // Deep High-Contrast
        clampedGlass <= 0.60f -> (baseColor.alpha * 0.88f).coerceIn(0.68f, 0.76f) // Crystal Clear
        else -> (baseColor.alpha * 1.05f).coerceIn(minAlpha, 0.88f)              // Standard Liquid Glass
    }
    val effectiveFill = baseColor.copy(alpha = fillAlpha)

    onDrawWithContent {
        drawRoundRect(color = effectiveFill, cornerRadius = corner)
        drawRoundRect(brush = sheenBrush, cornerRadius = corner)
        drawContent()
        drawRoundRect(brush = rimBrush, cornerRadius = corner, style = strokeStyle)
    }
}

/**
 * Dual-Engine Wallpaper Renderer:
 * 1) **Islamic Themes & Calligraphy Modes** (`rimal`, `noor`, `layla`, `zomorod`, `kaaba_gold`, `calligraphy_ruqah`, `calligraphy_thuluth`, `arabesque_mandala`):
 *    Renders ultra-sharp vector Islamic Geometric Shamsa Mandalas, Golden Mihrab Arches, Hanging Mishkat Lanterns,
 *    and authentic Arabic Ruq'ah & Thuluth Calligraphy («اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ» / «أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ»)
 *    with floating golden light particles.
 * 2) **4-Seasons, Weather-Sync, Sports & Cinema Live Modes** (`winter_snow`, `winter_rain`, `storm_lightning`, `spring_petals`, `summer_waves`, `autumn_leaves`, `sports_stadium`, `cinema_imax`):
 *    Renders true 1920x1080 Full HD 16:9 landscape photography + multi-layer live animations.
 */
@Composable
fun ThemeWallpaper(
    tokens: ThemeTokens,
    wallpaperOverride: String = "auto",
    weatherCode: Int? = null,
    animate: Boolean = true,
    extraDimAlpha: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val effectiveStyle = remember(wallpaperOverride, tokens.patternType, weatherCode) {
        when {
            wallpaperOverride == "weather_sync" -> resolveWeatherSyncedWallpaper(weatherCode)
            wallpaperOverride.isBlank() || wallpaperOverride == "auto" -> tokens.patternType ?: "arabesque_mandala"
            else -> wallpaperOverride
        }
    }

    val isIslamicArt = remember(effectiveStyle, tokens.id) {
        isIslamicOrnamentStyle(effectiveStyle, tokens.id)
    }

    val photoFile = remember(effectiveStyle, tokens.id) {
        resolveRealPhotoAsset(effectiveStyle, tokens.id)
    }

    val photoBitmap = remember(photoFile) {
        RealWallpaperBitmapCache.getOrLoad(context, photoFile)
    }

    val calligPaintMain = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
    }
    val calligPaintSub = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wallpaper_anim")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "animPhase",
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (isIslamicArt) {
            // Calming warm slate titanium backdrop in Light Mode (zero white glare)
            val gradientColors = if (tokens.isLightMode) {
                listOf(Color(0xFF1E2638), Color(0xFF161E2E), Color(0xFF0F172A))
            } else {
                tokens.wallpaperGradient
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(gradientColors)),
            )
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = if (tokens.isLightMode) 0.12f else 0.18f,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIslamicCalligraphyAndOrnament(
                    style = effectiveStyle,
                    tokens = tokens,
                    phaseSec = if (animate) animPhase else 0f,
                    mainPaint = calligPaintMain,
                    subPaint = calligPaintSub,
                )
            }
        } else {
            // True 1920x1080 Full HD Real Landscape Photograph + Multi-Layer Live Seasonal/Cinema/Sports FX
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(tokens.wallpaperGradient)),
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLiveRealisticAtmosphere(
                    style = effectiveStyle,
                    phaseSec = if (animate) animPhase else 0f,
                    animate = animate,
                )
            }
        }

        // Balanced readability vignette (adapts to Light Mode, Dark Mode, and Lock Screen dimming)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val scrimBrush = if (tokens.isLightMode && extraDimAlpha <= 0.05f) {
                        // Soft frosted slate scrim for Light Mode — calm, soothing on TV eyes, zero white glare
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A).copy(alpha = 0.28f),
                                Color(0xFF0F172A).copy(alpha = 0.18f),
                                Color(0xFF0F172A).copy(alpha = 0.32f),
                            ),
                        )
                    } else {
                        // Smart Dark Vignette & Scrim: Prevents bright wallpaper spots (rain, snow, lightning) from overpowering text
                        val baseTop = if (isIslamicArt) 0.44f else 0.58f
                        val baseMid = if (isIslamicArt) 0.32f else 0.48f
                        val baseBot = if (isIslamicArt) 0.54f else 0.70f
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF040914).copy(alpha = (baseTop + extraDimAlpha).coerceAtMost(0.98f)),
                                Color(0xFF050B17).copy(alpha = (baseMid + extraDimAlpha).coerceAtMost(0.95f)),
                                Color(0xFF060D1A).copy(alpha = (baseBot + extraDimAlpha).coerceAtMost(0.98f)),
                            ),
                        )
                    }
                    onDrawBehind {
                        drawRect(brush = scrimBrush)
                    }
                },
        )
    }
}

/**
 * Renders crisp Islamic Geometric Arabesque, Golden Shamsa Mandalas, Mihrab Arches,
 * Hanging Mishkat Lanterns, and Arabic Ruq'ah & Thuluth Calligraphy.
 */
private fun DrawScope.drawIslamicCalligraphyAndOrnament(
    style: String,
    tokens: ThemeTokens,
    phaseSec: Float,
    mainPaint: Paint,
    subPaint: Paint,
) {
    val w = size.width
    val h = size.height
    val gold = if (tokens.isLightMode) Color(0xFF92400E) else tokens.primary
    val softGold = gold.copy(alpha = if (tokens.isLightMode) 0.24f else 0.22f)
    val brightGold = gold.copy(alpha = if (tokens.isLightMode) 0.45f else 0.42f)

    // 1. Subtle Geometric Arabesque Grid across the background
    val cols = 8
    val rows = 5
    val cellW = w / cols
    val cellH = h / rows
    for (r in 0..rows) {
        for (c in 0..cols) {
            val cx = c * cellW
            val cy = r * cellH
            drawEightPointIslamicStar(
                center = Offset(cx, cy),
                radius = cellW * 0.26f,
                color = softGold.copy(alpha = 0.11f),
                strokeWidth = 1.2f,
            )
        }
    }

    // 2. Large Rotating 16-Fold Islamic Shamsa Rosettes (Top-Left & Bottom-Right corners)
    val rotDeg = phaseSec * 2.5f
    drawIslamicShamsaMandala(
        center = Offset(w * 0.16f, h * 0.24f),
        outerRadius = h * 0.34f,
        rotationDeg = rotDeg,
        primaryColor = brightGold,
        secondaryColor = softGold,
    )
    drawIslamicShamsaMandala(
        center = Offset(w * 0.86f, h * 0.78f),
        outerRadius = h * 0.38f,
        rotationDeg = -rotDeg * 0.8f,
        primaryColor = brightGold,
        secondaryColor = softGold,
    )

    // 3. Ornate Golden Mihrab Arch & Hanging Mishkat Lanterns
    val archCenterX = w * 0.50f
    val archTopY = h * 0.08f
    val archW = w * 0.46f
    val archPath = Path().apply {
        moveTo(archCenterX - archW / 2f, h * 0.92f)
        lineTo(archCenterX - archW / 2f, h * 0.34f)
        cubicTo(
            archCenterX - archW / 2f,
            h * 0.16f,
            archCenterX - archW * 0.18f,
            archTopY,
            archCenterX,
            archTopY,
        )
        cubicTo(
            archCenterX + archW * 0.18f,
            archTopY,
            archCenterX + archW / 2f,
            h * 0.16f,
            archCenterX + archW / 2f,
            h * 0.34f,
        )
        lineTo(archCenterX + archW / 2f, h * 0.92f)
    }
    drawPath(path = archPath, color = brightGold, style = Stroke(width = 2.2f))

    // Hanging Mishkat Lanterns on left & right of the Mihrab
    val sway = sin(phaseSec * 1.1f) * 6f
    drawMishkatLantern(
        anchor = Offset(w * 0.28f, 0f),
        dropLength = h * 0.22f,
        swayX = sway,
        color = brightGold,
    )
    drawMishkatLantern(
        anchor = Offset(w * 0.72f, 0f),
        dropLength = h * 0.26f,
        swayX = -sway,
        color = brightGold,
    )

    // 4. Kiswah Ornamental Golden Belt & Arabic Calligraphy Medallion (Ruq'ah & Thuluth)
    if (style == "calligraphy_thuluth" || style == "kaaba_kiswah" || tokens.id == "kaaba_gold") {
        val beltY = h * 0.19f
        val beltH = h * 0.11f
        drawRect(
            color = gold.copy(alpha = 0.12f),
            topLeft = Offset(0f, beltY),
            size = Size(w, beltH),
        )
        drawLine(
            color = brightGold,
            start = Offset(0f, beltY),
            end = Offset(w, beltY),
            strokeWidth = 2.5f,
        )
        drawLine(
            color = brightGold,
            start = Offset(0f, beltY + beltH),
            end = Offset(w, beltY + beltH),
            strokeWidth = 2.5f,
        )
    }

    // Draw Arabic Calligraphy & Ruq'ah Letter Flourishes via nativeCanvas
    val (ayahText, subCalligraphy, ruqahLetters) = when (style) {
        "calligraphy_thuluth", "kaaba_kiswah" -> Triple(
            "﴿ بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ﴾",
            "۞ جَعَلَ اللَّهُ الْكَعْبَةَ الْبَيْتَ الْحَرَامَ قِيَامًا لِلنَّاسِ ۞",
            "ح  •  ر  •  م  •  ن  •  و  •  ر",
        )
        "islamic_arch" -> Triple(
            "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾",
            "۞ إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ ۞",
            "م  •  ح  •  م  •  د  •  ﷺ",
        )
        "desert_night" -> Triple(
            "﴿ وَبِالنَّجْمِ هُمْ يَهْتَدُونَ ﴾",
            "۞ تَبَارَكَ الَّذِي جَعَلَ فِي السَّمَاءِ بُرُوجًا ۞",
            "س  •  ل  •  ا  •  م",
        )
        else -> Triple(
            "﴿ اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ ﴾",
            "۞ مَثَلُ نُورِهِ كَمِشْكَاةٍ فِيهَا مِصْبَاحٌ ۞",
            "نـور  •  حـق  •  سـلام  •  رحـمـة",
        )
    }

    drawContext.canvas.nativeCanvas.apply {
        // Large subtle Ruq'ah & Diwani artistic background letters on left/right wings
        mainPaint.color = gold.copy(alpha = if (tokens.isLightMode) 0.14f else 0.15f).toArgb()
        mainPaint.textSize = h * 0.13f
        drawText("نُــور", w * 0.17f, h * 0.56f, mainPaint)
        drawText("مِشْكَاة", w * 0.83f, h * 0.56f, mainPaint)

        // Central Calligraphic Medallion (Top-Center inside Mihrab)
        mainPaint.color = gold.copy(alpha = if (tokens.isLightMode) 0.42f else 0.38f).toArgb()
        mainPaint.textSize = h * 0.048f
        drawText(ayahText, w * 0.50f, h * 0.24f, mainPaint)

        subPaint.color = gold.copy(alpha = if (tokens.isLightMode) 0.30f else 0.28f).toArgb()
        subPaint.textSize = h * 0.028f
        drawText(subCalligraphy, w * 0.50f, h * 0.30f, subPaint)
        drawText(ruqahLetters, w * 0.50f, h * 0.88f, subPaint)
    }

    // 5. Floating Golden Light Motes
    for (i in 0 until 20) {
        val seed = i * 43 + 11
        val xNorm = ((seed * 67) % 100) / 100f
        val yNorm = ((seed * 0.041f - phaseSec * 0.015f + 10f) % 1.0f)
        val pulse = 0.5f + 0.5f * sin(phaseSec * 1.6f + i)
        drawCircle(
            color = gold.copy(alpha = 0.12f + 0.18f * pulse),
            radius = 2.2f + (seed % 3) * 1.4f,
            center = Offset(xNorm * w, yNorm * h),
        )
    }
}

private fun DrawScope.drawIslamicShamsaMandala(
    center: Offset,
    outerRadius: Float,
    rotationDeg: Float,
    primaryColor: Color,
    secondaryColor: Color,
) {
    val points = 16
    val stepRad = (2.0 * PI / points).toFloat()
    val baseRad = (rotationDeg * PI / 180.0).toFloat()

    drawCircle(color = secondaryColor, radius = outerRadius, center = center, style = Stroke(width = 1.6f))
    drawCircle(color = primaryColor, radius = outerRadius * 0.72f, center = center, style = Stroke(width = 1.8f))
    drawCircle(color = secondaryColor, radius = outerRadius * 0.42f, center = center, style = Stroke(width = 1.4f))

    val starPath = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerRadius else outerRadius * 0.58f
        val angle = baseRad + i * (stepRad / 2f)
        val px = center.x + cos(angle) * r
        val py = center.y + sin(angle) * r
        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
    }
    starPath.close()
    drawPath(path = starPath, color = primaryColor, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawEightPointIslamicStar(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
) {
    val path = Path()
    val pts = 16
    for (i in 0 until pts) {
        val r = if (i % 2 == 0) radius else radius * 0.52f
        val angle = (i * PI / 8.0).toFloat()
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawMishkatLantern(
    anchor: Offset,
    dropLength: Float,
    swayX: Float,
    color: Color,
) {
    val endX = anchor.x + swayX
    val endY = anchor.y + dropLength
    drawLine(
        color = color,
        start = anchor,
        end = Offset(endX, endY),
        strokeWidth = 1.6f,
    )
    val lanternPath = Path().apply {
        moveTo(endX, endY)
        lineTo(endX - 18f, endY + 22f)
        lineTo(endX - 12f, endY + 52f)
        lineTo(endX, endY + 64f)
        lineTo(endX + 12f, endY + 52f)
        lineTo(endX + 18f, endY + 22f)
        close()
    }
    drawPath(path = lanternPath, color = color, style = Stroke(width = 1.8f))
    drawCircle(
        color = color.copy(alpha = 0.25f),
        radius = 24f,
        center = Offset(endX, endY + 34f),
    )
}

/**
 * Multi-layer realistic atmosphere renderer over true 1920x1080 Full HD seasonal, sports & cinema photos.
 */
private fun DrawScope.drawLiveRealisticAtmosphere(
    style: String,
    phaseSec: Float,
    animate: Boolean,
) {
    if (!animate) return
    val w = size.width
    val h = size.height
    when (style) {
        "winter_snow" -> {
            // 3 depth layers of realistic falling snow with natural wind sway
            for (i in 0 until 42) {
                val seed = i * 37 + 13
                val depth = (i % 3) + 1 // 1=far, 2=mid, 3=near
                val speed = 0.022f * depth + (seed % 5) * 0.006f
                val yNorm = ((seed * 0.031f + phaseSec * speed) % 1.0f)
                val xBase = ((seed * 73) % 100) / 100f
                val sway = sin((phaseSec * (0.6f + depth * 0.2f) + i).toDouble()).toFloat() * (0.012f * depth)
                val x = ((xBase + sway + 1f) % 1f) * w
                val y = yNorm * h
                val r = 1.6f * depth + (seed % 3) * 0.8f
                drawCircle(
                    color = Color.White.copy(alpha = 0.24f + depth * 0.16f),
                    radius = r,
                    center = Offset(x, y),
                )
            }
        }
        "winter_rain" -> {
            // Slanted rain streaks + subtle glass water droplets
            for (i in 0 until 38) {
                val seed = i * 41 + 7
                val speed = 0.34f + (seed % 5) * 0.06f
                val yNorm = ((seed * 0.029f + phaseSec * speed) % 1.0f)
                val xNorm = (((seed * 61) % 100) / 100f)
                val x = xNorm * w
                val y = yNorm * h
                val len = 24f + (seed % 4) * 10f
                drawLine(
                    color = Color(0xFFE0F2FE).copy(alpha = 0.34f),
                    start = Offset(x, y),
                    end = Offset(x - 5f, y + len),
                    strokeWidth = 1.5f,
                )
            }
        }
        "storm_lightning" -> {
            // Heavy storm rain + periodic realistic sheet & branching lightning illumination
            for (i in 0 until 32) {
                val seed = i * 47 + 9
                val yNorm = ((seed * 0.033f + phaseSec * 0.42f) % 1.0f)
                val x = (((seed * 59) % 100) / 100f) * w
                val y = yNorm * h
                drawLine(
                    color = Color(0xFFBAE6FD).copy(alpha = 0.28f),
                    start = Offset(x, y),
                    end = Offset(x - 7f, y + 28f),
                    strokeWidth = 1.4f,
                )
            }
            val cycle = phaseSec % 5.5f
            val flashAlpha = when {
                cycle in 0.10f..0.22f -> 0.28f
                cycle in 0.28f..0.36f -> 0.42f
                else -> 0f
            }
            if (flashAlpha > 0f) {
                drawRect(color = Color(0xFFE0F2FE).copy(alpha = flashAlpha))
                val boltPath = Path().apply {
                    moveTo(w * 0.62f, 0f)
                    lineTo(w * 0.58f, h * 0.22f)
                    lineTo(w * 0.63f, h * 0.26f)
                    lineTo(w * 0.55f, h * 0.54f)
                    lineTo(w * 0.59f, h * 0.58f)
                    lineTo(w * 0.51f, h * 0.88f)
                }
                drawPath(
                    path = boltPath,
                    color = Color.White.copy(alpha = (flashAlpha * 1.8f).coerceAtMost(0.85f)),
                    style = Stroke(width = 3.2f),
                )
            }
        }
        "spring_petals" -> {
            for (i in 0 until 26) {
                val seed = i * 29 + 19
                val speed = 0.028f + (seed % 4) * 0.008f
                val yNorm = ((seed * 0.043f + phaseSec * speed) % 1.0f)
                val xBase = ((seed * 53) % 100) / 100f
                val sway = sin((phaseSec * 0.85f + i).toDouble()).toFloat() * 0.025f
                val x = ((xBase + sway + 1f) % 1f) * w
                val y = yNorm * h
                drawOval(
                    color = Color(0xFFFBCFE8).copy(alpha = 0.52f),
                    topLeft = Offset(x, y),
                    size = Size(9f + (seed % 4) * 2f, 5.5f + (seed % 3)),
                )
            }
        }
        "autumn_leaves" -> {
            for (i in 0 until 22) {
                val seed = i * 31 + 17
                val speed = 0.026f + (seed % 4) * 0.007f
                val yNorm = ((seed * 0.039f + phaseSec * speed) % 1.0f)
                val xBase = ((seed * 67) % 100) / 100f
                val sway = sin((phaseSec * 0.75f + i).toDouble()).toFloat() * 0.028f
                val x = ((xBase + sway + 1f) % 1f) * w
                val y = yNorm * h
                val leafColor = if (i % 2 == 0) Color(0xFFF59E0B) else Color(0xFFEA580C)
                drawOval(
                    color = leafColor.copy(alpha = 0.48f),
                    topLeft = Offset(x, y),
                    size = Size(11f + (seed % 4) * 2f, 6.5f + (seed % 3)),
                )
            }
        }
        "summer_waves" -> {
            // Shimmering golden sunset sparkles on the water
            for (i in 0 until 24) {
                val seed = i * 37 + 5
                val x = (((seed * 71) % 100) / 100f) * w
                val y = (0.58f + ((seed * 23) % 38) / 100f) * h
                val shimmer = 0.3f + 0.7f * ((sin(phaseSec * 2.2f + i) + 1f) * 0.5f)
                drawOval(
                    color = Color(0xFFFDE68A).copy(alpha = 0.28f * shimmer),
                    topLeft = Offset(x, y),
                    size = Size(18f + (seed % 5) * 6f, 3.5f),
                )
            }
        }
        "sports_stadium" -> {
            // Sweeping stadium floodlight beams & atmosphere particles
            val angleOffset = sin(phaseSec * 0.7f) * w * 0.08f
            val beamBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF86EFAC).copy(alpha = 0.16f), Color.Transparent),
                start = Offset(w * 0.2f, 0f),
                end = Offset(w * 0.45f + angleOffset, h * 0.85f),
            )
            drawRect(brush = beamBrush)
        }
        "cinema_imax" -> {
            // Floating warm cinema projector bokeh orbs
            for (i in 0 until 16) {
                val seed = i * 43 + 15
                val x = (((seed * 63) % 100) / 100f) * w
                val y = ((seed * 0.051f - phaseSec * 0.012f + 10f) % 1.0f) * h
                drawCircle(
                    color = Color(0xFFFDA4AF).copy(alpha = 0.18f),
                    radius = 12f + (seed % 5) * 6f,
                    center = Offset(x, y),
                )
            }
        }
    }
}
