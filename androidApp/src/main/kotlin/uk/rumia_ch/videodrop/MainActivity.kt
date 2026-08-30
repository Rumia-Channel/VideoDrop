package uk.rumia_ch.videodrop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import uk.rumia_ch.videodrop.core.AppVersionHolder
import uk.rumia_ch.videodrop.core.NoOpYtDlpEngine
import uk.rumia_ch.videodrop.ui.FolderUi
import uk.rumia_ch.videodrop.ui.LocationViewModel
import uk.rumia_ch.videodrop.ytdlp.AndroidYtDlpEngine

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_CLIP_URL = "uk.rumia_ch.videodrop.action.CLIP_URL"
        const val EXTRA_CLIP_URL = "extra_clip_url"
    }

    private var pendingClipUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        try {
            val pi = packageManager.getPackageInfo(packageName, 0)
            AppVersionHolder.versionName = pi.versionName
            AppVersionHolder.appContext = applicationContext
        } catch (_: Exception) {}

        handleIntent(intent)

        val engine = AndroidYtDlpEngine(applicationContext)

        setContent {
            val locationViewModel = remember { LocationViewModel(applicationContext) }
            val root by locationViewModel.rootFlow.collectAsState(
                initial = uk.rumia_ch.videodrop.ytdlp.DownloadLocationRepository.DownloadRoot("internal", null, "内部ストレージ (cacheDir)")
            )
            val folders by locationViewModel.folders.collectAsState()
            // Refresh on start
            androidx.compose.runtime.LaunchedEffect(Unit) {
                locationViewModel.refresh()
            }

            val pickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    // Use last path segment as display name, or SDカード
                    val display = uri.lastPathSegment?.substringAfterLast(":")?.takeIf { it.isNotBlank() } ?: "外部ストレージ"
                    locationViewModel.setRootSaf(uri, display)
                }
            }

            // Map repository FolderItem to UI FolderUi with count (count is from ViewModel's files, but we approximate)
            val folderUis = remember(folders) {
                folders.map { f ->
                    FolderUi(id = f.uriOrPath, name = f.name, icon = "📁", count = 0)
                }
            }

            // Also add system folders as fallback if no custom folders yet?
            val allFolders = if (folderUis.isEmpty()) {
                emptyList()
            } else folderUis

            App(
                engine = engine,
                externalClipUrl = pendingClipUrl,
                onClipUrlConsumed = { pendingClipUrl = null },
                downloadRootDisplay = root.displayName,
                onPickDownloadFolder = { pickerLauncher.launch(null) },
                onResetDownloadFolder = { locationViewModel.setRootInternal() },
                folders = allFolders,
                currentFolderId = locationViewModel.currentFolderUriOrPath,
                onSelectFolder = { id ->
                    if (id == null) locationViewModel.backToRoot() else locationViewModel.openFolder(id)
                },
                onCreateFolder = { name ->
                    locationViewModel.createFolder(name)
                },
                onRenameFolder = { id, newName ->
                    // For SAF, rename via DocumentFile
                    // Simplified: delete and recreate? Use repository's rename is not yet implemented for SAF
                    // For now, just show not implemented
                },
                onDeleteFolder = { id ->
                    // TODO: delete via DocumentFile
                },
                onMoveFile = { _, _ -> }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val url = when (intent?.action) {
            ACTION_CLIP_URL -> intent.getStringExtra(EXTRA_CLIP_URL)
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { extractUrl(it) }
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
        if (!url.isNullOrBlank()) {
            pendingClipUrl = url
        }
    }

    private fun extractUrl(text: String): String {
        val regex = Regex("""https?://\S+""")
        return regex.find(text)?.value?.trimEnd('.', ',', ')', '"', '\'') ?: text.trim()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(engine = NoOpYtDlpEngine())
}
