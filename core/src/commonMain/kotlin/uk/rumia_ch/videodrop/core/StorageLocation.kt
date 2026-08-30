package uk.rumia_ch.videodrop.core

import kotlinx.serialization.Serializable

/**
 * 保存先指定 — SDカード含む
 * SAF (Storage Access Framework) で永続URIを扱う
 */
@Serializable
sealed interface StorageLocation {

    @Serializable
    data object InternalCache : StorageLocation // cacheDir/downloads

    @Serializable
    data object AppExternalFiles : StorageLocation // getExternalFilesDir(null) — アプリ専用外部

    @Serializable
    data object PublicMovies : StorageLocation // MediaStore Movies

    @Serializable
    data object PublicMusic : StorageLocation // MediaStore Music

    @Serializable
    data object PublicDownloads : StorageLocation // MediaStore Downloads (Android 10+)

    @Serializable
    data class CustomTree(
        val treeUri: String, // SAF treeUri string (persisted permission)
        val displayName: String // SDカード / USB 等の表示名
    ) : StorageLocation

    companion object {
        val Default: StorageLocation = PublicMovies

        fun displayName(loc: StorageLocation): String = when (loc) {
            is InternalCache -> "内部キャッシュ (一時)"
            is AppExternalFiles -> "アプリ専用外部ストレージ"
            is PublicMovies -> "Movies (共有・動画)"
            is PublicMusic -> "Music (共有・音楽)"
            is PublicDownloads -> "Downloads (共有)"
            is CustomTree -> loc.displayName
        }

        fun description(loc: StorageLocation): String = when (loc) {
            is InternalCache -> "cacheDir — アンインストールで削除"
            is AppExternalFiles -> "Android/data/uk.rumia_ch.videodrop/files — SDカード内のアプリ領域も含む"
            is PublicMovies -> "共有ストレージ Movies — ギャラリー等から閲覧可"
            is PublicMusic -> "共有ストレージ Music — 音楽アプリから閲覧可"
            is PublicDownloads -> "共有ストレージ Downloads"
            is CustomTree -> "カスタム: ${loc.displayName} (${loc.treeUri.take(40)}...)"
        }
    }
}

@Serializable
data class StoragePreference(
    val videoLocation: StorageLocation = StorageLocation.PublicMovies,
    val musicLocation: StorageLocation = StorageLocation.PublicMusic,
    val customTreeUri: String? = null
)
