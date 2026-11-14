package com.mishkat.launcher.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.liquidGlass

enum class ActiveDropdownMenu {
    NONE,
    INPUTS_AND_NETWORK,
    TOOLS_AND_FEATURES,
    APPEARANCE_AND_SETTINGS,
}

@Composable
fun DropdownMenuDialog(
    menu: ActiveDropdownMenu,
    tokens: ThemeTokens,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    // Inputs & Network callbacks
    onOpenUpnpStorage: () -> Unit,
    onOpenSmbStorage: () -> Unit,
    onOpenFtpStorage: () -> Unit,
    onOpenUsbStorage: () -> Unit,
    onOpenDvbSatellite: () -> Unit,
    onOpenHdmiInputs: () -> Unit,
    // Tools callbacks
    onOpenSportsCenter: () -> Unit,
    onOpenRamBooster: () -> Unit,
    onOpenCleaner: () -> Unit,
    onOpenRemoteQr: () -> Unit,
    onOpenLockScreen: () -> Unit,
    // Appearance callbacks
    onOpenThemes: () -> Unit,
    onOpenWallpapers: () -> Unit,
    onOpenPrayerSettings: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenDeveloperProfile: () -> Unit,
) {
    if (menu == ActiveDropdownMenu.NONE) return

    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(menu) {
        repeat(3) {
            withFrameNanos { }
            if (runCatching { firstFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x9903060E))
                .clickable { onDismiss() }
                .padding(top = 58.dp, start = 48.dp, end = 48.dp, bottom = 36.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            val menuShape = RoundedCornerShape(18.dp)
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .width(420.dp)
                    .clip(menuShape)
                    .background(Color(0xF0080E1C))
                    .border(BorderStroke(1.2.dp, tokens.primary.copy(alpha = 0.65f)), menuShape)
                    .clickable(enabled = false) {}
                    .padding(18.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val title = when (menu) {
                    ActiveDropdownMenu.INPUTS_AND_NETWORK -> if (isArabic) "📂 المداخل والشبكة" else "📂 Inputs & Network"
                    ActiveDropdownMenu.TOOLS_AND_FEATURES -> if (isArabic) "🛠️ الأدوات والرياضة" else "🛠️ Tools & Sports"
                    ActiveDropdownMenu.APPEARANCE_AND_SETTINGS -> if (isArabic) "🎨 المظهر والضبط" else "🎨 Appearance & Settings"
                    ActiveDropdownMenu.NONE -> ""
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        color = tokens.primary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = if (isArabic) "✕ إغلاق" else "✕ Close",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                when (menu) {
                    ActiveDropdownMenu.INPUTS_AND_NETWORK -> {
                        DropdownItemRow(
                            id = "drop:upnp",
                            icon = "📡",
                            title = if (isArabic) "راوترات وسيرفرات UPnP / DLNA (ZTE / Huawei / TP-Link)" else "Universal UPnP / DLNA Routers (ZTE / Huawei / TP-Link)",
                            subtitle = if (isArabic) "تشغيل وسائط فلاشات جميع الراوترات والكمبيوتر" else "Shared media across all router brands & PC",
                            focusRequester = firstFocus,
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenUpnpStorage() },
                        )
                        DropdownItemRow(
                            id = "drop:smb",
                            icon = "🖴",
                            title = if (isArabic) "مشاركة شبكة SMB (Samba) لجميع الأجهزة والراوترات" else "Universal Network Storage (Samba SMB)",
                            subtitle = if (isArabic) "مشاركة ملفات ويندوز ولينكس وراوترات ZTE و Huawei" else "Windows, Linux, ZTE & Huawei network storage",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenSmbStorage() },
                        )
                        DropdownItemRow(
                            id = "drop:ftp",
                            icon = "🌐",
                            title = if (isArabic) "خادم ملفات FTP لجميع الراوترات والأجهزة" else "Universal FTP File Server (ZTE / Huawei / TP-Link)",
                            subtitle = if (isArabic) "اتصال مباشر بمنفذ 21 لجميع أنواع الراوترات" else "Direct port 21 across all router models",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenFtpStorage() },
                        )
                        DropdownItemRow(
                            id = "drop:usb",
                            icon = "💾",
                            title = if (isArabic) "الفلاشات والهاردات الخارجية (USB)" else "External USB & HDD Drives",
                            subtitle = if (isArabic) "استعراض ملفات الشاشة ووحدات التخزين" else "Internal and OTG USB drives",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenUsbStorage() },
                        )
                        DropdownItemRow(
                            id = "drop:sat",
                            icon = "🛰️",
                            title = if (isArabic) "القنوات الفضائية والستالايت (DVB-S2)" else "Satellite TV (DVB-S2)",
                            subtitle = if (isArabic) "بث القنوات عبر التيونر والمشغل المدمج" else "Built-in live TV and tuner stream",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenDvbSatellite() },
                        )
                        DropdownItemRow(
                            id = "drop:hdmi",
                            icon = "🔌",
                            title = if (isArabic) "مداخل الشاشة (HDMI 1, 2 / AV)" else "TV Inputs (HDMI 1, 2 / AV)",
                            subtitle = if (isArabic) "التبديل بين أجهزة الاستقبال والمنصات" else "Switch hardware input sources",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenHdmiInputs() },
                        )
                    }

                    ActiveDropdownMenu.TOOLS_AND_FEATURES -> {
                        DropdownItemRow(
                            id = "drop:sports",
                            icon = "⚽",
                            title = if (isArabic) "مركز المباريات والرياضة" else "Sports & Match Center",
                            subtitle = if (isArabic) "متابعة المباريات والنتائج مباشرة" else "Live match scores and fixtures",
                            focusRequester = firstFocus,
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenSportsCenter() },
                        )
                        DropdownItemRow(
                            id = "drop:ram",
                            icon = "⚡",
                            title = if (isArabic) "تسريع الرام وكنس البرامج" else "RAM Booster & App Killer",
                            subtitle = if (isArabic) "إيقاف تطبيقات الخلفية وتسريع المعالج" else "Kill background apps & free RAM",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenRamBooster() },
                        )
                        DropdownItemRow(
                            id = "drop:cleaner",
                            icon = "🗑️",
                            title = if (isArabic) "تنظيف الكاش ومخلفات الـ APK" else "Clean Cache & Leftover APKs",
                            subtitle = if (isArabic) "حذف حزم التثبيت المؤقتة وتفريغ المساحة" else "Delete leftover installers & free storage",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenCleaner() },
                        )
                        DropdownItemRow(
                            id = "drop:remote",
                            icon = "📱",
                            title = if (isArabic) "ريموت الموبايل والاقتران (QR)" else "Mobile Remote Companion (QR)",
                            subtitle = if (isArabic) "تحكم بالشاشة من المتصفح بدون تثبيت" else "Control TV from browser via Wi-Fi",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenRemoteQr() },
                        )
                        DropdownItemRow(
                            id = "drop:lock",
                            icon = "🔒",
                            title = if (isArabic) "وضع القفل والشاشة الجمالية" else "Ambient Lock Screen Mode",
                            subtitle = if (isArabic) "ساعة إسلامية أنيقة وأذكار مستمرة" else "Aesthetic clock and rotating azkar",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenLockScreen() },
                        )
                    }

                    ActiveDropdownMenu.APPEARANCE_AND_SETTINGS -> {
                        DropdownItemRow(
                            id = "drop:themes",
                            icon = "🎨",
                            title = if (isArabic) "باقة الثيمات الحية الفاخرة (56 ثيماً 4K)" else "Luxury Living Themes (56 Themes 4K)",
                            subtitle = if (isArabic) "تناغم كامل بين صور 4K وألوان الأيقونات وزجاج الواجهة" else "Harmonized 4K scenes, matching icon blurs & liquid glass",
                            focusRequester = firstFocus,
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenThemes() },
                        )
                        DropdownItemRow(
                            id = "drop:prayer",
                            icon = "🕌",
                            title = if (isArabic) "مواقيت الصلاة والمؤذن والتنبيهات" else "Prayer Times, Muezzin & Adhan",
                            subtitle = if (isArabic) "تحديد المدينة، 10 أصوات أذان فاخرة، وتنبيه الصلاة" else "City selection, 10 authentic adhan voices & alert",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenPrayerSettings() },
                        )
                        DropdownItemRow(
                            id = "drop:home_settings",
                            icon = "⚙️",
                            title = if (isArabic) "تخصيص شاشة الهوم والصفوف" else "Customize Home & Shelves",
                            subtitle = if (isArabic) "إعادة ترتيب الصفوف وإخفاء العناصر" else "Reorder shelves, glass intensity & layout",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenHomeSettings() },
                        )
                        DropdownItemRow(
                            id = "drop:lang",
                            icon = "🌐",
                            title = if (isArabic) "لغة الواجهة (English / عربي)" else "Language (العربية / English)",
                            subtitle = if (isArabic) "التبديل بين العربية والإنجليزية" else "Switch between Arabic and English",
                            tokens = tokens,
                            onClick = { onDismiss(); onToggleLanguage() },
                        )
                        DropdownItemRow(
                            id = "drop:about",
                            icon = "✦",
                            title = if (isArabic) "عن مشكاة لانشر (الإصدار 2.0.0)" else "About Mishkat Launcher (v2.0.0)",
                            subtitle = if (isArabic) "الإصدار 2.0.0 • أحدث إصدار للشاشات الذكية" else "Version 2.0.0 • Smart TV Edition",
                            tokens = tokens,
                            onClick = { onDismiss(); onOpenDeveloperProfile() },
                        )
                    }

                    ActiveDropdownMenu.NONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun DropdownItemRow(
    id: String,
    icon: String,
    title: String,
    subtitle: String,
    tokens: ThemeTokens,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val rowShape = RoundedCornerShape(12.dp)
    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

    Row(
        modifier = focusModifier
            .fillMaxWidth()
            .mishkatFocus(id = id, radius = 12.dp)
            .focusable()
            .clip(rowShape)
            .liquidGlass(
                tokens = tokens,
                baseColor = tokens.card,
                radius = 12.dp,
                glassIntensity = 0.85f,
                borderColor = tokens.glassRim.copy(alpha = 0.40f),
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                    onClick()
                    true
                } else false
            }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = icon, fontSize = 22.sp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = tokens.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = tokens.onSurfaceMuted,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
        Text(text = "❯", color = tokens.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
