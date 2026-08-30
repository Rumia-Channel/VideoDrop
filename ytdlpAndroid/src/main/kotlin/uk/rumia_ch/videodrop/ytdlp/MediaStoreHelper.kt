package uk.rumia_ch.videodrop.ytdlp

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * MediaStore export per spec section 16.
 * yt-dlp output goes to staging dir (cacheDir/downloads/<id>), then Kotlin moves to MediaStore.
 * Python never touches MediaStore directly.
 */
object MediaStoreHelper {

    /**
     * Export staging file to public MediaStore.
     * Videos -> Movies, Audio -> Music, fallback -> Downloads.
     * Returns content Uri string on success, or staging path if fallback to private.
     */
    fun exportToMediaStore(
        context: Context,
        stagingPath: String,
        title: String,
        isVideo: Boolean
    ): String {
        val srcFile = File(stagingPath)
        if (!srcFile.exists()) return stagingPath

        return try {
            val resolver = context.contentResolver
            val collection = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val displayName = srcFile.name
            val mimeType = when (srcFile.extension.lowercase()) {
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "mkv" -> "video/x-matroska"
                "m4a", "aac" -> "audio/mp4"
                "mp3" -> "audio/mpeg"
                "opus" -> "audio/opus"
                else -> if (isVideo) "video/mp4" else "audio/*"
            }

            val relativePath = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(collection, values) ?: return stagingPath
            resolver.openOutputStream(uri)?.use { out ->
                srcFile.inputStream().use { inp -> inp.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            // Optionally delete staging file after successful export (keep for now per spec .part handling)
            // srcFile.delete()

            uri.toString()
        } catch (e: Exception) {
            // StorageError per spec section 20
            e.printStackTrace()
            stagingPath
        }
    }

    /**
     * Fallback: copy to Downloads via MediaStore Downloads collection (Android 10+).
     * Not used in MVP unless user selects Downloads as destination per spec section 16.
     */
    fun exportToDownloads(context: Context, stagingPath: String): String {
        val srcFile = File(stagingPath)
        if (!srcFile.exists()) return stagingPath
        return try {
            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                // For < Q, use legacy external storage (not recommended for MVP)
                return stagingPath
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, srcFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return stagingPath
            resolver.openOutputStream(uri)?.use { out ->
                srcFile.inputStream().use { inp -> inp.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri.toString()
        } catch (e: Exception) {
            stagingPath
        }
    }
}
