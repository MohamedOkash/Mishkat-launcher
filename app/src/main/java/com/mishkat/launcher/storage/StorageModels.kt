package com.mishkat.launcher.storage

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class StorageSourceType {
    LOCAL_USB,
    ROUTER_SMB,
    ROUTER_FTP
}

enum class FileCategory {
    FOLDER,
    VIDEO,
    AUDIO,
    IMAGE,
    APK,
    DOCUMENT,
    OTHER
}

@Immutable
data class StorageItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val category: FileCategory = FileCategory.OTHER,
    val streamUri: String? = null,
) {
    val formattedSize: String
        get() {
            if (isDirectory) return ""
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
                else -> "$sizeBytes B"
            }
        }
}

@Immutable
data class StorageDevice(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val type: StorageSourceType,
    val isConnected: Boolean,
    val host: String = "",
    val port: Int = 0,
    val rootPath: String = "/",
    val freeSpaceGb: Double = 0.0,
    val totalSpaceGb: Double = 0.0,
)

@Serializable
data class SmbFtpSettings(
    val smbHost: String = "",
    val smbPort: Int = 445,
    val smbDomain: String = "",
    val smbUser: String = "guest",
    val smbPass: String = "",
    val smbShare: String = "",
    val smbIsAnonymous: Boolean = true,

    val ftpHost: String = "",
    val ftpPort: Int = 21,
    val ftpUser: String = "anonymous",
    val ftpPass: String = "",
    val ftpIsAnonymous: Boolean = true,
)

@Immutable
data class StorageUiState(
    val activeTab: StorageSourceType = StorageSourceType.LOCAL_USB,
    val currentPath: String = "/",
    val items: List<StorageItem> = emptyList(),
    val breadcrumbs: List<String> = listOf("/"),
    val devices: List<StorageDevice> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val activeMediaItem: StorageItem? = null,
)
