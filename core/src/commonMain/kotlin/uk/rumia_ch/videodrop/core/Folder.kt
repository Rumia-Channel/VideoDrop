package uk.rumia_ch.videodrop.core

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val icon: String = "📁",
    val parentId: String? = null,
    val createdAt: Long = 0L,
    val isSystem: Boolean = false // true for 動画/音楽 など削除不可
) {
    companion object {
        fun create(name: String, icon: String = "📁", parentId: String? = null): Folder {
            return Folder(
                id = Random.nextLong().toString(36) + Random.nextInt(1000).toString(),
                name = name.trim(),
                icon = icon,
                parentId = parentId,
                createdAt = 0L,
                isSystem = false
            )
        }

        val SystemVideo = Folder(id = "sys_video", name = "動画", icon = "🎬", isSystem = true)
        val SystemMusic = Folder(id = "sys_music", name = "音楽", icon = "🎵", isSystem = true)
        val SystemDocuments = Folder(id = "sys_docs", name = "書類", icon = "📄", isSystem = true)

        val Defaults = listOf(SystemVideo, SystemMusic)
    }
}

@Serializable
data class FolderFileLink(
    val fileId: String, // DownloadEvent id or uri hash
    val folderId: String
)
