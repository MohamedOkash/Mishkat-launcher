package com.mishkat.launcher.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.io.File

@Composable
fun StorageExplorerOverlay(
    repository: NetworkAndUsbStorageRepository,
    onDismiss: () -> Unit,
) {
    val state by repository.state.collectAsState()
    val tokens = LocalThemeTokens.current
    val context = LocalContext.current

    BackHandler {
        if (state.activeMediaItem != null) {
            repository.playMediaItem(null)
        } else if (!repository.navigateUp()) {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0070B16)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "🖴 مستعرض الملفات والوسائط",
                        color = tokens.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    // Source Tabs
                    SourceTab(
                        label = "💾 USB والهارد",
                        isSelected = state.activeTab == StorageSourceType.LOCAL_USB,
                        onClick = { repository.selectTab(StorageSourceType.LOCAL_USB) },
                    )
                    SourceTab(
                        label = "🖴 Samba الراوتر",
                        isSelected = state.activeTab == StorageSourceType.ROUTER_SMB,
                        onClick = { repository.selectTab(StorageSourceType.ROUTER_SMB) },
                    )
                    SourceTab(
                        label = "🌐 خادم FTP",
                        isSelected = state.activeTab == StorageSourceType.ROUTER_FTP,
                        onClick = { repository.selectTab(StorageSourceType.ROUTER_FTP) },
                    )
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .mishkatFocus(id = "storage:close_btn", radius = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .liquidGlass(tokens, tokens.card, 10.dp, 0.85f)
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "إغلاق ✕",
                        color = tokens.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Breadcrumbs Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33000000))
                    .border(1.dp, tokens.glassRim.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "المسار: ${state.currentPath}",
                    color = tokens.onSurfaceMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Error banner if any
            state.errorMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x66B91C1C))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    Text(text = msg, color = Color.White, fontSize = 13.sp)
                }
            }

            // Grid of files / folders
            if (state.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "جارٍ قراءة الملفات...", color = tokens.primary, fontSize = 16.sp)
                }
            } else if (state.items.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "لا توجد ملفات في هذا المجلد",
                        color = tokens.onSurfaceMuted,
                        fontSize = 15.sp,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.path }) { item ->
                        StorageItemCard(
                            item = item,
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

        // Active Media Player Modal (Video / Audio)
        state.activeMediaItem?.let { media ->
            if (media.category == FileCategory.VIDEO || media.category == FileCategory.AUDIO) {
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
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalThemeTokens.current
    Box(
        modifier = Modifier
            .mishkatFocus(id = "tab:$label", radius = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) tokens.primary.copy(alpha = 0.22f) else Color(0x331C2844))
            .border(
                1.dp,
                if (isSelected) tokens.primary else tokens.glassRim.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp),
            )
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
    onClick: () -> Unit,
) {
    val tokens = LocalThemeTokens.current
    val icon = when (item.category) {
        FileCategory.FOLDER -> "📁"
        FileCategory.VIDEO -> "🎬"
        FileCategory.AUDIO -> "🎵"
        FileCategory.IMAGE -> "🖼️"
        FileCategory.APK -> "📦"
        FileCategory.DOCUMENT -> "📄"
        FileCategory.OTHER -> "📎"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .mishkatFocus(id = "file:${item.path}", radius = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .liquidGlass(
                tokens = tokens,
                baseColor = if (item.isDirectory) tokens.cardFocused else tokens.card,
                radius = 12.dp,
                glassIntensity = 0.85f,
                borderColor = if (item.isDirectory) tokens.primary.copy(alpha = 0.4f) else tokens.glassRim,
            )
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
                Text(text = item.formattedSize, color = tokens.onSurfaceMuted, fontSize = 10.sp)
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
private fun MediaPlayerModal(
    item: StorageItem,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val uriStr = item.streamUri ?: return

    val exoPlayer = remember(uriStr) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Title and close button overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x80000000))
                    .clickable { onClose() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(text = "✕ خروج", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

private fun handleItemClick(context: Context, item: StorageItem, repo: NetworkAndUsbStorageRepository) {
    when (item.category) {
        FileCategory.VIDEO, FileCategory.AUDIO -> {
            repo.playMediaItem(item)
        }
        FileCategory.APK -> {
            // Direct install APK
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
