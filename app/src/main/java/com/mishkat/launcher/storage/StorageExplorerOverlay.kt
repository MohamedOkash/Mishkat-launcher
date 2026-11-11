package com.mishkat.launcher.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.Brush
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mishkat.launcher.focus.mishkatFocus
import com.mishkat.launcher.theme.LocalThemeTokens
import com.mishkat.launcher.theme.liquidGlass
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun StorageExplorerOverlay(
    repository: NetworkAndUsbStorageRepository,
    onDismiss: () -> Unit,
) {
    val state by repository.state.collectAsState()
    val tokens = LocalThemeTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSettingsDialog by remember { mutableStateOf(false) }
    val firstItemFocus = remember { FocusRequester() }
    val closeBtnFocus = remember { FocusRequester() }

    BackHandler {
        if (showSettingsDialog) {
            showSettingsDialog = false
        } else if (state.activeMediaItem != null) {
            repository.playMediaItem(null)
        } else if (!repository.navigateUp()) {
            onDismiss()
        }
    }

    // Auto-focus first item when items load or folder path changes
    LaunchedEffect(state.items.size, state.currentPath) {
        if (state.items.isNotEmpty()) {
            withFrameNanos { }
            runCatching { firstItemFocus.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF5060A14))
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Back) {
                    if (showSettingsDialog) {
                        showSettingsDialog = false
                        true
                    } else if (state.activeMediaItem != null) {
                        repository.playMediaItem(null)
                        true
                    } else if (repository.navigateUp()) {
                        true
                    } else {
                        onDismiss()
                        true
                    }
                } else {
                    false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 20.dp),
        ) {
            // Top Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Logo title
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xEE090E1A))
                            .border(1.dp, tokens.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "🖴 مستعرض الملفات والوسائط",
                            color = tokens.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Source Tabs (D-Pad Navigable)
                    SourceTab(
                        id = "tab:upnp",
                        label = "📡 الشبكة UPnP (H188A)",
                        isSelected = state.activeTab == StorageSourceType.UPNP_DLNA,
                        onClick = { repository.selectTab(StorageSourceType.UPNP_DLNA) },
                    )
                    SourceTab(
                        id = "tab:usb",
                        label = "💾 USB والهارد",
                        isSelected = state.activeTab == StorageSourceType.LOCAL_USB,
                        onClick = { repository.selectTab(StorageSourceType.LOCAL_USB) },
                    )
                    SourceTab(
                        id = "tab:smb",
                        label = "🖴 Samba الراوتر",
                        isSelected = state.activeTab == StorageSourceType.ROUTER_SMB,
                        onClick = { repository.selectTab(StorageSourceType.ROUTER_SMB) },
                    )
                    SourceTab(
                        id = "tab:ftp",
                        label = "🌐 خادم FTP",
                        isSelected = state.activeTab == StorageSourceType.ROUTER_FTP,
                        onClick = { repository.selectTab(StorageSourceType.ROUTER_FTP) },
                    )
                }

                // Action Buttons on Top Right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Server Settings Button
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "storage:settings_btn", radius = 10.dp)
                            .focusable()
                            .clip(RoundedCornerShape(10.dp))
                            .liquidGlass(tokens, tokens.card, 10.dp, 0.85f)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp &&
                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                                    showSettingsDialog = true
                                    true
                                } else false
                            }
                            .clickable { showSettingsDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "⚙️ إعدادات السيرفر",
                            color = tokens.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Refresh Button
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "storage:refresh_btn", radius = 10.dp)
                            .focusable()
                            .clip(RoundedCornerShape(10.dp))
                            .liquidGlass(tokens, tokens.card, 10.dp, 0.85f)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp &&
                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                                    scope.launch { repository.scanAllStorageDevices() }
                                    true
                                } else false
                            }
                            .clickable { scope.launch { repository.scanAllStorageDevices() } }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(text = "🔄 فحص", color = tokens.onSurface, fontSize = 12.sp)
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .focusRequester(closeBtnFocus)
                            .mishkatFocus(id = "storage:close_btn", radius = 10.dp)
                            .focusable()
                            .clip(RoundedCornerShape(10.dp))
                            .liquidGlass(tokens, tokens.cardFocused, 10.dp, 0.85f, borderColor = tokens.primary)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp &&
                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                                    onDismiss()
                                    true
                                } else false
                            }
                            .clickable { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "إغلاق ✕",
                            color = tokens.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Breadcrumbs & Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEE090E1A))
                    .border(1.dp, tokens.glassRim.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "📁 المسار: ${state.currentPath}",
                    color = tokens.onSurface,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (state.breadcrumbs.size > 1) {
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "storage:up_btn", radius = 6.dp)
                            .focusable()
                            .clip(RoundedCornerShape(6.dp))
                            .background(tokens.primary.copy(alpha = 0.2f))
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp &&
                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                                    repository.navigateUp()
                                    true
                                } else false
                            }
                            .clickable { repository.navigateUp() }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(text = "⬆️ للأعلى", color = tokens.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Error banner if any
            state.errorMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x887F1D1D))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    Text(text = msg, color = Color.White, fontSize = 12.sp)
                }
            }

            // Grid of files / folders
            if (state.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "⏳ جارٍ فحص وقراءة الملفات والمجلدات...", color = tokens.primary, fontSize = 15.sp)
                }
            } else if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "لا توجد ملفات أو مجلدات في هذا المسار",
                            color = tokens.onSurfaceMuted,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = "تأكد من تركيب الفلاشة أو تفعيل مشاركة الملفات بالراوتر والضغط على ⚙️ إعدادات السيرفر",
                            color = tokens.onSurfaceMuted.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 145.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(state.items, key = { _, it -> it.path }) { index, item ->
                        StorageItemCard(
                            item = item,
                            focusRequester = if (index == 0) firstItemFocus else null,
                            onClick = {
                                if (item.isDirectory) {
                                    repository.openFolder(item.path)
                                } else {
                                    handleItemClick(context, item, repository)
                                }
                            },
                        )
                    }
                }
            }
        }

        // Server Settings Modal
        if (showSettingsDialog) {
            ServerSettingsModal(
                initialSettings = repository.getSettings(),
                onSave = { updated ->
                    scope.launch { repository.saveSettings(updated) }
                    showSettingsDialog = false
                },
                onDismiss = { showSettingsDialog = false },
            )
        }

        // Active Media Player Modal (Video / Audio)
        state.activeMediaItem?.let { media ->
            if (!media.streamUri.isNullOrBlank() || media.category == FileCategory.VIDEO || media.category == FileCategory.AUDIO) {
                MediaPlayerModal(
                    item = media,
                    onClose = { repository.playMediaItem(null) },
                )
            }
        }
    }
}

@Composable
private fun SourceTab(
    id: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalThemeTokens.current
    Box(
        modifier = Modifier
            .mishkatFocus(id = id, radius = 10.dp)
            .focusable()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) tokens.primary.copy(alpha = 0.28f) else Color(0xEE090E1A))
            .border(
                1.5.dp,
                if (isSelected) tokens.primary else tokens.glassRim.copy(alpha = 0.35f),
                RoundedCornerShape(10.dp),
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                    onClick()
                    true
                } else false
            }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (isSelected) tokens.primary else tokens.onSurface,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun StorageItemCard(
    item: StorageItem,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val tokens = LocalThemeTokens.current
    val icon = when {
        item.isDirectory -> "📁"
        item.category == FileCategory.VIDEO -> "🎬"
        item.category == FileCategory.AUDIO -> "🎵"
        item.category == FileCategory.IMAGE -> "🖼️"
        item.category == FileCategory.APK -> "📦"
        item.category == FileCategory.DOCUMENT -> "📄"
        !item.streamUri.isNullOrBlank() -> "🎬"
        else -> "📎"
    }

    val focusMod = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

    Column(
        modifier = focusMod
            .fillMaxWidth()
            .height(102.dp)
            .mishkatFocus(id = "file:${item.path}", radius = 12.dp)
            .focusable()
            .clip(RoundedCornerShape(12.dp))
            .liquidGlass(
                tokens = tokens,
                baseColor = if (item.isDirectory) tokens.cardFocused else tokens.card,
                radius = 12.dp,
                glassIntensity = 0.88f,
                borderColor = if (item.isDirectory) tokens.primary.copy(alpha = 0.45f) else tokens.glassRim,
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                    onClick()
                    true
                } else false
            }
            .clickable { onClick() }
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = icon, fontSize = 20.sp)
            if (!item.isDirectory && item.sizeBytes > 0) {
                Text(text = item.formattedSize, color = tokens.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = item.name,
            color = tokens.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ServerSettingsModal(
    initialSettings: SmbFtpSettings,
    onSave: (SmbFtpSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalThemeTokens.current
    var host by remember { mutableStateOf(initialSettings.smbHost.ifBlank { "192.168.1.1" }) }
    var share by remember { mutableStateOf(initialSettings.smbShare) }
    var user by remember { mutableStateOf(initialSettings.smbUser) }
    var pass by remember { mutableStateOf(initialSettings.smbPass) }
    var isAnon by remember { mutableStateOf(initialSettings.smbIsAnonymous) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFA0B101D))
                .border(1.5.dp, tokens.primary, RoundedCornerShape(16.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "⚙️ إعدادات فلاشة وسيرفر الراوتر",
                color = tokens.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "عنوان IP الراوتر الافتراضي (Gateway):",
                color = tokens.onSurfaceMuted,
                fontSize = 12.sp,
            )

            // Host presets
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("192.168.1.1", "192.168.0.1", "192.168.100.1").forEach { ip ->
                    val isSel = host == ip
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "ip:$ip", radius = 8.dp)
                            .focusable()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) tokens.primary else Color(0x331E293B))
                            .clickable { host = ip }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = ip,
                            color = if (isSel) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "نوع المصادقة:", color = tokens.onSurface, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "auth:guest", radius = 8.dp)
                            .focusable()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAnon) tokens.primary else Color(0x331E293B))
                            .clickable { isAnon = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(text = "ضيف (Guest)", color = if (isAnon) Color.Black else Color.White, fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "auth:user", radius = 8.dp)
                            .focusable()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isAnon) tokens.primary else Color(0x331E293B))
                            .clickable { isAnon = false }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(text = "اسم وكلمة سر", color = if (!isAnon) Color.Black else Color.White, fontSize = 11.sp)
                    }
                }
            }

            if (!isAnon) {
                Text(
                    text = "بيانات حساب الراوتر (المستخدم: $user)",
                    color = tokens.onSurfaceMuted,
                    fontSize = 11.sp,
                )
            }

            // Save & Cancel Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "modal:cancel", radius = 10.dp)
                        .focusable()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(text = "إلغاء", color = tokens.onSurfaceMuted, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "modal:save", radius = 10.dp)
                        .focusable()
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.primary)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp &&
                                (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                                onSave(
                                    initialSettings.copy(
                                        smbHost = host,
                                        ftpHost = host,
                                        smbShare = share,
                                        smbUser = if (isAnon) "guest" else user,
                                        smbPass = if (isAnon) "" else pass,
                                        smbIsAnonymous = isAnon,
                                        ftpUser = if (isAnon) "anonymous" else user,
                                        ftpPass = if (isAnon) "" else pass,
                                        ftpIsAnonymous = isAnon,
                                    )
                                )
                                true
                            } else false
                        }
                        .clickable {
                            onSave(
                                initialSettings.copy(
                                    smbHost = host,
                                    ftpHost = host,
                                    smbShare = share,
                                    smbUser = if (isAnon) "guest" else user,
                                    smbPass = if (isAnon) "" else pass,
                                    smbIsAnonymous = isAnon,
                                    ftpUser = if (isAnon) "anonymous" else user,
                                    ftpPass = if (isAnon) "" else pass,
                                    ftpIsAnonymous = isAnon,
                                )
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(text = "💾 حفظ وفحص", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MediaPlayerModal(
    item: StorageItem,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val uriStr = item.streamUri ?: return

    val resumePrefs = remember { context.getSharedPreferences("mishkat_resume_playback", Context.MODE_PRIVATE) }
    val resumeKey = remember(item.path, item.name) { "resume_${(item.path.ifBlank { item.name }).hashCode()}" }

    val exoPlayer = remember(uriStr) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)))
            prepare()
            playWhenReady = true
        }
    }

    var isOsdVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPos by remember { mutableLongStateOf(0L) }
    var seekNotice by remember { mutableStateOf<String?>(null) }
    var resumeNotice by remember { mutableStateOf<String?>(null) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var consecutiveSeeks by remember { mutableIntStateOf(0) }
    var lastSeekDir by remember { mutableIntStateOf(0) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }

    val playBtnFocus = remember { FocusRequester() }
    val modalFocus = remember { FocusRequester() }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    val saved = resumePrefs.getLong(resumeKey, 0L)
                    if (saved > 15_000L && (duration <= 0L || saved < duration - 30_000L)) {
                        exoPlayer.seekTo(saved)
                        resumeNotice = "تم استئناف المشاهدة من ${formatMs(saved)}"
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            val finalPos = exoPlayer.currentPosition
            if (finalPos > 10_000L) {
                resumePrefs.edit().putLong(resumeKey, finalPos).apply()
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Auto-update position & save resume
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            bufferedPos = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            if (currentPos > 10_000L) {
                resumePrefs.edit().putLong(resumeKey, currentPos).apply()
            }
            delay(500)
        }
    }

    // Auto-hide OSD after 4 seconds of idle
    LaunchedEffect(lastInteractionTime, isPlaying) {
        if (isPlaying) {
            delay(4000)
            if (System.currentTimeMillis() - lastInteractionTime >= 4000) {
                isOsdVisible = false
                seekNotice = null
            }
        }
    }

    // Clear notices after 3s
    LaunchedEffect(resumeNotice) {
        if (resumeNotice != null) {
            delay(3500)
            resumeNotice = null
        }
    }

    // Seek handler with accelerating leaps (+15s, +30s, +1m, +5m)
    fun doSeek(forward: Boolean) {
        val now = System.currentTimeMillis()
        val dir = if (forward) 1 else -1
        if (dir == lastSeekDir && now - lastSeekTime < 1200) {
            consecutiveSeeks++
        } else {
            consecutiveSeeks = 1
            lastSeekDir = dir
        }
        lastSeekTime = now
        lastInteractionTime = now
        isOsdVisible = true

        val leapSeconds = when {
            consecutiveSeeks >= 5 -> 300 // +5m
            consecutiveSeeks >= 3 -> 60  // +1m
            consecutiveSeeks >= 2 -> 30  // +30s
            else -> 15                   // +15s
        }
        val leapMs = leapSeconds * 1000L
        val target = if (forward) {
            (exoPlayer.currentPosition + leapMs).coerceAtMost(duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
        } else {
            (exoPlayer.currentPosition - leapMs).coerceAtLeast(0L)
        }
        exoPlayer.seekTo(target)
        currentPos = target
        seekNotice = if (forward) "⏩ +${formatMs(leapMs)} (${formatMs(target)})" else "⏪ -${formatMs(leapMs)} (${formatMs(target)})"
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { modalFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(modalFocus)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyUp) return@onKeyEvent false
                when (keyEvent.key) {
                    Key.Back -> {
                        onClose()
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause -> {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            isOsdVisible = true
                        } else {
                            exoPlayer.play()
                        }
                        lastInteractionTime = System.currentTimeMillis()
                        true
                    }
                    Key.DirectionRight, Key.MediaFastForward -> {
                        doSeek(forward = true)
                        true
                    }
                    Key.DirectionLeft, Key.MediaRewind -> {
                        doSeek(forward = false)
                        true
                    }
                    Key.DirectionUp, Key.DirectionDown -> {
                        isOsdVisible = !isOsdVisible
                        lastInteractionTime = System.currentTimeMillis()
                        if (isOsdVisible) {
                            runCatching { playBtnFocus.requestFocus() }
                        }
                        true
                    }
                    else -> false
                }
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            update = { pv ->
                pv.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Floating Center Seek Notification HUD
        seekNotice?.let { notice ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xDD0B1220))
                    .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Text(
                    text = notice,
                    color = Color(0xFFF59E0B),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Resume notice pill at top center
        resumeNotice?.let { notice ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEE1E293B))
                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "✦ $notice",
                    color = Color(0xFF10B981),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Animated OSD Header & Footer
        AnimatedVisibility(
            visible = isOsdVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC000000),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xEE000000),
                            )
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "مشغل مشكاة السينمائي • ${if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) "ملء الشاشة" else "الأبعاد الأصلية"}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .mishkatFocus(id = "player:close_btn", radius = 10.dp)
                            .focusable()
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable { onClose() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(text = "✕ خروج", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Bottom Controls & SeekBar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Time & Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = formatMs(currentPos),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )

                        // Custom SeekBar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x55FFFFFF)),
                        ) {
                            val progress = if (duration > 0) (currentPos.toFloat() / duration).coerceIn(0f, 1f) else 0f
                            val buffered = if (duration > 0) (bufferedPos.toFloat() / duration).coerceIn(0f, 1f) else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(buffered)
                                    .background(Color(0x44F59E0B)),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(Color(0xFFF59E0B)),
                            )
                        }

                        Text(
                            text = formatMs(duration),
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // TV Remote OSD Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OsdButton("player:rewind_10m", "⏪ -10د") {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 600_000L).coerceAtLeast(0L))
                            lastInteractionTime = System.currentTimeMillis()
                        }
                        Spacer(Modifier.width(8.dp))
                        OsdButton("player:rewind_15s", "⏪ -15ث") {
                            doSeek(false)
                        }
                        Spacer(Modifier.width(12.dp))
                        OsdButton(
                            id = "player:play_pause",
                            label = if (isPlaying) "⏸ إيقاف" else "▶ تشغيل",
                            focusRequester = playBtnFocus,
                            highlighted = true,
                        ) {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            lastInteractionTime = System.currentTimeMillis()
                        }
                        Spacer(Modifier.width(12.dp))
                        OsdButton("player:forward_15s", "⏩ +15ث") {
                            doSeek(true)
                        }
                        Spacer(Modifier.width(8.dp))
                        OsdButton("player:forward_10m", "⏩ +10د") {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 600_000L).coerceAtMost(duration))
                            lastInteractionTime = System.currentTimeMillis()
                        }
                        Spacer(Modifier.width(16.dp))
                        OsdButton(
                            id = "player:aspect",
                            label = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) "🔲 أبعاد أصلية" else "🔲 ملء الشاشة",
                        ) {
                            resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            } else {
                                AspectRatioFrameLayout.RESIZE_MODE_FILL
                            }
                            lastInteractionTime = System.currentTimeMillis()
                        }
                        Spacer(Modifier.width(8.dp))
                        OsdButton("player:restart", "🔁 من البداية") {
                            exoPlayer.seekTo(0L)
                            exoPlayer.play()
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OsdButton(
    id: String,
    label: String,
    focusRequester: FocusRequester? = null,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val fMod = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = fMod
            .mishkatFocus(id = id, radius = 10.dp)
            .focusable()
            .clip(shape)
            .background(if (highlighted) Color(0xFFF59E0B) else Color(0x771E293B))
            .border(1.dp, if (highlighted) Color(0xFFF59E0B) else Color(0x66FFFFFF), shape)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                    onClick()
                    true
                } else false
            }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (highlighted) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hours = totalSec / 3600
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, min, sec)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", min, sec)
    }
}

private fun handleItemClick(context: Context, item: StorageItem, repo: NetworkAndUsbStorageRepository) {
    if (!item.streamUri.isNullOrBlank()) {
        repo.playMediaItem(item)
        return
    }
    when (item.category) {
        FileCategory.VIDEO, FileCategory.AUDIO -> {
            repo.playMediaItem(item)
        }
        FileCategory.APK -> {
            runCatching {
                val file = File(item.path)
                val uri = if (file.exists()) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else {
                    Uri.parse(item.streamUri ?: item.path)
                }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }
        }
        else -> Unit
    }
}
