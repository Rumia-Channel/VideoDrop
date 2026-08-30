package uk.rumia_ch.videodrop

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads APK from GitHub Release and triggers install.
 * Uses HttpURLConnection to avoid extra deps, writes to getExternalFilesDir.
 * Requires REQUEST_INSTALL_PACKAGES permission and FileProvider.
 */
object ApkUpdateInstaller {

    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        onProgress: ((downloaded: Long, total: Long?) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "VideoDrop/${getVersionName(context)}")
                instanceFollowRedirects = true
                // GitHub releases redirect to S3, follow is needed
            }
            conn.connect()
            if (conn.responseCode !in 200..299) {
                return@withContext Result.failure(Exception("HTTP ${conn.responseCode}: ${conn.responseMessage}"))
            }
            val total = conn.contentLengthLong.takeIf { it > 0 }
            val input = conn.inputStream

            val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir
            destDir.mkdirs()
            val destFile = File(destDir, "VideoDrop-update-${System.currentTimeMillis()}.apk")

            var downloaded = 0L
            FileOutputStream(destFile).use { out ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n == -1) break
                    out.write(buf, 0, n)
                    downloaded += n
                    onProgress?.invoke(downloaded, total)
                }
            }
            input.close()
            conn.disconnect()

            // Trigger install via FileProvider
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)

            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getVersionName(context: Context): String {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            info.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
}
