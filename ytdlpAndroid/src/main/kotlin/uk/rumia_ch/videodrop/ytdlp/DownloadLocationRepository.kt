package uk.rumia_ch.videodrop.ytdlp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.locationDataStore by preferencesDataStore(name = "download_location")

/**
 * ダウンロード先指定フォルダ — 設定で1つだけ指定し、その中にユーザーが自由にフォルダを作成
 * - 内部: cacheDir/downloads
 * - SAF: 取得した treeUri を永続化し、その配下を管理
 */
class DownloadLocationRepository(private val context: Context) {

    companion object {
        private val KEY_ROOT_URI = stringPreferencesKey("root_tree_uri")
        private val KEY_ROOT_DISPLAY = stringPreferencesKey("root_display_name")
        private val KEY_ROOT_TYPE = stringPreferencesKey("root_type") // internal | saf
    }

    data class DownloadRoot(
        val type: String, // "internal" or "saf"
        val uri: String?, // treeUri for saf
        val displayName: String
    )

    val rootFlow: Flow<DownloadRoot> = context.locationDataStore.data.map { prefs ->
        val type = prefs[KEY_ROOT_TYPE] ?: "internal"
        val uri = prefs[KEY_ROOT_URI]
        val display = prefs[KEY_ROOT_DISPLAY] ?: if (type == "saf" && uri != null) "カスタムフォルダ" else "内部ストレージ"
        DownloadRoot(type, uri, display)
    }

    suspend fun setRootInternal() {
        context.locationDataStore.edit { prefs ->
            prefs[KEY_ROOT_TYPE] = "internal"
            prefs.remove(KEY_ROOT_URI)
            prefs[KEY_ROOT_DISPLAY] = "内部ストレージ (cacheDir)"
        }
    }

    suspend fun setRootSaf(treeUri: Uri, displayName: String) {
        // 永続化
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}
        context.locationDataStore.edit { prefs ->
            prefs[KEY_ROOT_TYPE] = "saf"
            prefs[KEY_ROOT_URI] = treeUri.toString()
            prefs[KEY_ROOT_DISPLAY] = displayName
        }
    }

    fun getRootDocumentFile(): DocumentFile? {
        // Called synchronously is hard due to Flow; use runBlocking-like via helper
        // For now, provide file-based fallback and SAF via uri string
        return null // caller should collect rootFlow and use uri
    }

    /**
     * MyCollection用: ルート配下のフォルダ一覧を取得
     * SAFの場合: DocumentFile.listFiles()でフォルダのみ抽出
     * Internalの場合: File.listFiles()でフォルダのみ
     */
    fun listFoldersSync(root: DownloadRoot): List<FolderItem> {
        return when (root.type) {
            "saf" -> {
                val uriStr = root.uri ?: return emptyList()
                try {
                    val treeUri = Uri.parse(uriStr)
                    val doc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
                    doc.listFiles()
                        .filter { it.isDirectory }
                        .map { FolderItem(it.name ?: "無名", it.uri.toString(), it.uri.toString()) }
                } catch (_: Exception) { emptyList() }
            }
            else -> {
                val base = File(context.cacheDir, "downloads")
                if (!base.exists()) return emptyList()
                base.listFiles()?.filter { it.isDirectory }?.map {
                    FolderItem(it.name, it.absolutePath, it.absolutePath)
                } ?: emptyList()
            }
        }
    }

    fun listFilesInFolder(root: DownloadRoot, folderUriOrPath: String?): List<FileItem> {
        // folderUriOrPath == null => root直下のファイル
        return when (root.type) {
            "saf" -> {
                try {
                    val targetUri: Uri = if (folderUriOrPath == null) {
                        root.uri?.let { Uri.parse(it) } ?: return emptyList()
                    } else {
                        Uri.parse(folderUriOrPath)
                    }
                    val targetDoc = DocumentFile.fromTreeUri(context, targetUri)
                        ?: DocumentFile.fromSingleUri(context, targetUri)
                        ?: return emptyList()
                    targetDoc.listFiles()
                        .filter { !it.isDirectory }
                        .map { FileItem(it.name ?: "file", it.uri.toString(), it.name ?: "") }
                } catch (_: Exception) { emptyList() }
            }
            else -> {
                val base = if (folderUriOrPath == null) File(context.cacheDir, "downloads") else File(folderUriOrPath)
                if (!base.exists()) return emptyList()
                base.listFiles()?.filter { it.isFile }?.map {
                    FileItem(it.name, it.absolutePath, it.name)
                } ?: emptyList()
            }
        }
    }

    suspend fun createFolder(root: DownloadRoot, name: String): Result<String> {
        val clean = name.trim().takeIf { it.isNotEmpty() } ?: return Result.failure(IllegalArgumentException("フォルダ名が空"))
        return when (root.type) {
            "saf" -> {
                try {
                    val treeUri = Uri.parse(root.uri!!)
                    val parent = DocumentFile.fromTreeUri(context, treeUri) ?: return Result.failure(Exception("SAF root not found"))
                    // Check duplicate
                    if (parent.listFiles().any { it.name == clean }) {
                        return Result.failure(Exception("同名フォルダが既に存在"))
                    }
                    val newDoc = parent.createDirectory(clean) ?: return Result.failure(Exception("作成失敗"))
                    Result.success(newDoc.uri.toString())
                } catch (e: Exception) { Result.failure(e) }
            }
            else -> {
                try {
                    val base = File(context.cacheDir, "downloads")
                    base.mkdirs()
                    val newDir = File(base, clean)
                    if (newDir.exists()) return Result.failure(Exception("同名フォルダが既に存在"))
                    newDir.mkdirs()
                    Result.success(newDir.absolutePath)
                } catch (e: Exception) { Result.failure(e) }
            }
        }
    }

    data class FolderItem(val name: String, val uriOrPath: String, val id: String)
    data class FileItem(val name: String, val uri: String, val displayName: String)
}
