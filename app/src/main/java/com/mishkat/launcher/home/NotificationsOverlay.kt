package com.mishkat.launcher.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.mishkat.launcher.R
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.i18n.AppSettings
import com.mishkat.launcher.notification.NotificationRepository
import com.mishkat.launcher.theme.ThemeTokens
import com.mishkat.launcher.theme.liquidGlass
import com.mishkat.launcher.update.UpdateRepository
import com.mishkat.launcher.update.UpdateUiState

@Composable
fun NotificationsOverlay(
    tokens: ThemeTokens,
    appSettings: AppSettings,
    updateState: UpdateUiState,
    updateRepository: UpdateRepository,
    onToggleDataSaver: () -> Unit,
    onOpenDeveloperProfile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isArabic = appSettings.isArabic
    val glass = appSettings.glassIntensity
    val checkFocus = remember { FocusRequester() }
    val context = LocalContext.current
    val appNotifications by NotificationRepository.notifications.collectAsState()
    val hasPermission = remember(context) { NotificationRepository.isPermissionGranted(context) }

    BackHandler { onDismiss() }

    LaunchedEffect(Unit) {
        updateRepository.markNotificationsRead()
        checkFocus.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(tokens.cardRadius + 8.dp))
                .liquidGlass(
                    tokens = tokens,
                    baseColor = tokens.surface,
                    radius = tokens.cardRadius + 8.dp,
                    glassIntensity = glass,
                    borderColor = tokens.primary,
                )
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "🔔", fontSize = 26.sp)
                    Column {
                        Text(
                            text = if (isArabic) "مركز الإشعارات والتحديثات" else "Notifications & Updates Hub",
                            color = tokens.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isArabic) "إصدارات النظام • باقة الإنترنت • التنبيهات الذكية" else "System Releases • Data Saver • Alerts",
                            color = tokens.onSurfaceMuted,
                            fontSize = 12.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "notif:close", radius = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .liquidGlass(tokens, tokens.cardFocused, 12.dp, glass)
                        .clickable { onDismiss() }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.radio_close),
                        color = tokens.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Scrollable Notifications List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 1) Launcher System Update Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .liquidGlass(tokens, tokens.card, 14.dp, glass, tokens.primary.copy(alpha = 0.5f))
                        .padding(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(text = "🚀", fontSize = 20.sp)
                                Text(
                                    text = if (isArabic) "تحديث لانشر مشكاة الذكي" else "Mishkat Smart Launcher Update",
                                    color = tokens.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (updateState.updateAvailable) Color(0xFF10B981) else Color(0xFF64748B))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = if (updateState.updateAvailable) "v${updateState.latestVersionName} جديد" else "v${updateState.currentVersionName} محدّث",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        val log = if (isArabic) updateState.changelogAr else updateState.changelogEn
                        if (log.isNotBlank()) {
                            Text(
                                text = log,
                                color = tokens.onSurfaceMuted,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Check button
                            Box(
                                modifier = Modifier
                                    .focusRequester(checkFocus)
                                    .mishkatFocus(id = "notif:check_update", radius = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .liquidGlass(tokens, tokens.cardFocused, 10.dp, glass)
                                    .clickable { updateRepository.checkForUpdates(forceManual = true) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = if (updateState.isChecking) "⏳ جارٍ الفحص..." else (if (isArabic) "🔄 فحص التحديثات" else "🔄 Check Updates"),
                                    color = tokens.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            // Install button
                            if (updateState.updateAvailable && updateState.downloadUrl.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .mishkatFocus(id = "notif:install_update", radius = 10.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(tokens.primary)
                                        .clickable { updateRepository.startInstall() }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = if (isArabic) "⬇ تثبيت التحديث الآن" else "⬇ Install Update Now",
                                        color = Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                            }
                        }

                        updateState.errorMessage?.let { err ->
                            Text(text = err, color = Color(0xFF10B981), fontSize = 12.sp)
                        }
                    }
                }

                // 2) Real App Notifications Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .liquidGlass(tokens, tokens.card, 14.dp, glass)
                        .padding(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(text = "📱", fontSize = 20.sp)
                                Text(
                                    text = if (isArabic) "إشعارات التطبيقات والبرامج" else "Installed Apps Notifications",
                                    color = tokens.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            if (!hasPermission) {
                                Box(
                                    modifier = Modifier
                                        .mishkatFocus(id = "notif:grant_perm", radius = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(tokens.primary)
                                        .clickable { NotificationRepository.openPermissionSettings(context) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = if (isArabic) "⚙ تفعيل إذن قراءة الإشعارات" else "⚙ Grant Notification Access",
                                        color = Color(0xFF0F172A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        if (!hasPermission) {
                            Text(
                                text = if (isArabic)
                                    "لم يتم منح اللانشر إذن قراءة إشعارات برامج التلفزيون (مثل يوتيوب، واتساب، داونلودر). اضغط بالزر أعلاه لتفعيله من إعدادات النظام مباشرة."
                                else
                                    "Notification Listener permission is not granted. Click the button above to enable it in Android TV Settings.",
                                color = tokens.onSurfaceMuted,
                                fontSize = 12.sp,
                            )
                        } else if (appNotifications.isEmpty()) {
                            Text(
                                text = if (isArabic)
                                    "✓ إذن قراءة الإشعارات مفعّل بنجاح. لا توجد إشعارات واردة من التطبيقات حالياً."
                                else
                                    "✓ Notification access is active. No pending notifications from apps right now.",
                                color = tokens.onSurfaceMuted,
                                fontSize = 12.sp,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                appNotifications.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.06f))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Text(
                                                text = "${item.appName}: ${item.title}",
                                                color = tokens.primary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            if (item.text.isNotBlank()) {
                                                Text(
                                                    text = item.text,
                                                    color = tokens.onSurface,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White.copy(alpha = 0.12f))
                                                .clickable { NotificationRepository.removeNotification(item.key) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Text(text = "✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3) Data Saver Mode Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .liquidGlass(tokens, tokens.card, 14.dp, glass)
                        .padding(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(text = "🛡", fontSize = 20.sp)
                                Text(
                                    text = if (isArabic) "نظام توفير باقة الإنترنت المنزلية" else "Home Internet Data Saver",
                                    color = tokens.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                text = if (isArabic)
                                    "يحدد جودة البث التلفزيوني تلقائياً (480p/720p) ويوفر أكثر من 70% من الجيجابايتس لحماية باقة النت في مصر."
                                else
                                    "Caps live stream resolution and bitrate to save over 70% of data quota.",
                                color = tokens.onSurfaceMuted,
                                fontSize = 12.sp,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .mishkatFocus(id = "notif:toggle_data_saver", radius = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .liquidGlass(
                                    tokens = tokens,
                                    baseColor = if (appSettings.dataSaverMode) tokens.cardFocused else tokens.card,
                                    radius = 10.dp,
                                    glassIntensity = glass,
                                    borderColor = if (appSettings.dataSaverMode) tokens.primary else tokens.glassRim,
                                )
                                .clickable { onToggleDataSaver() }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = if (appSettings.dataSaverMode)
                                    (if (isArabic) "✓ مفعّل (توفير)" else "✓ Enabled")
                                else
                                    (if (isArabic) "معطّل (جودة كاملة)" else "Disabled"),
                                color = if (appSettings.dataSaverMode) tokens.primary else tokens.onSurfaceMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // 3) Developer Info Quick Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .liquidGlass(tokens, tokens.card, 14.dp, glass)
                        .padding(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(text = "👨‍💻", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = if (isArabic) "مطور التطبيق: Eng. Mohamed Okash" else "Developer: Eng. Mohamed Okash",
                                    color = tokens.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "mohamed-okash.web.app • IT Infrastructure & HSE Engineer",
                                    color = tokens.onSurfaceMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .mishkatFocus(id = "notif:open_dev", radius = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .liquidGlass(tokens, tokens.cardFocused, 10.dp, glass)
                                .clickable {
                                    onDismiss()
                                    onOpenDeveloperProfile()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = if (isArabic) "عرض الملف الشخصي" else "View Portfolio",
                                color = tokens.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
