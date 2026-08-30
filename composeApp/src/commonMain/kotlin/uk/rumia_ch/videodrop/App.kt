package uk.rumia_ch.videodrop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.rumia_ch.videodrop.core.AnalyzeState
import uk.rumia_ch.videodrop.core.DefaultDownloadRepository
import uk.rumia_ch.videodrop.core.FormatSelection
import uk.rumia_ch.videodrop.core.NoOpYtDlpEngine
import uk.rumia_ch.videodrop.core.OutputType
import uk.rumia_ch.videodrop.core.YtDlpEngine
import uk.rumia_ch.videodrop.ui.BrowserScreen
import uk.rumia_ch.videodrop.ui.DownloadScreen
import uk.rumia_ch.videodrop.ui.FolderUi
import uk.rumia_ch.videodrop.ui.MediaDetailScreen
import uk.rumia_ch.videodrop.ui.MyCollectionScreen
import uk.rumia_ch.videodrop.ui.PlayerScreen
import uk.rumia_ch.videodrop.ui.Screen
import uk.rumia_ch.videodrop.ui.SettingsScreen
import uk.rumia_ch.videodrop.ui.VideoDropViewModel
import uk.rumia_ch.videodrop.ui.theme.ClipboxColors
import uk.rumia_ch.videodrop.ui.theme.ClipboxTheme

@Composable
fun App(
    engine: YtDlpEngine = NoOpYtDlpEngine(),
    externalClipUrl: String? = null,
    onClipUrlConsumed: (() -> Unit)? = null,
    downloadRootDisplay: String = "内部ストレージ (cacheDir)",
    onPickDownloadFolder: (() -> Unit)? = null,
    onResetDownloadFolder: (() -> Unit)? = null,
    folders: List<FolderUi> = emptyList(),
    currentFolderId: String? = null,
    onSelectFolder: ((String?) -> Unit)? = null,
    onCreateFolder: ((String) -> Unit)? = null,
    onRenameFolder: ((String, String) -> Unit)? = null,
    onDeleteFolder: ((String) -> Unit)? = null,
    onMoveFile: ((String, String) -> Unit)? = null
) {
    ClipboxTheme {
        val repository = remember(engine) { DefaultDownloadRepository(engine) }
        val viewModel: VideoDropViewModel = viewModel { VideoDropViewModel(repository) }
        var screen by remember { mutableStateOf(Screen.Browser) }
        var pendingUrl by remember { mutableStateOf<String?>(null) }
        var showAreYouOk by remember { mutableStateOf(false) }
        var showReally by remember { mutableStateOf(false) }
        var playerUri by remember { mutableStateOf("") }
        var playerTitle by remember { mutableStateOf("") }
        var playerIsVideo by remember { mutableStateOf(true) }
        // Folder chooser for download destination
        var showFolderChooser by remember { mutableStateOf(false) }
        var pendingSelection by remember { mutableStateOf<FormatSelection?>(null) }
        var pendingOutput by remember { mutableStateOf<OutputType?>(null) }

        val analyzeState by viewModel.analyzeState.collectAsState()
        LaunchedEffect(analyzeState) {
            if (analyzeState is AnalyzeState.Success) {
                screen = Screen.Detail
            }
        }

        LaunchedEffect(externalClipUrl) {
            if (!externalClipUrl.isNullOrBlank()) {
                pendingUrl = externalClipUrl
                showAreYouOk = true
                onClipUrlConsumed?.invoke()
            }
        }

        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .background(ClipboxColors.Background)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (screen) {
                    Screen.Browser -> BrowserScreen(
                        onClipRequest = { url ->
                            pendingUrl = url
                            showAreYouOk = true
                        }
                    )
                    Screen.MyCollection -> MyCollectionScreen(
                        viewModel = viewModel,
                        onOpenFile = { uri, title, isVideo ->
                            playerUri = uri
                            playerTitle = title
                            playerIsVideo = isVideo
                            screen = Screen.Player
                        },
                        folders = folders,
                        onCreateFolder = onCreateFolder,
                        onRenameFolder = onRenameFolder,
                        onDeleteFolder = onDeleteFolder,
                        onMoveFile = onMoveFile,
                        downloadRootDisplay = downloadRootDisplay,
                        currentFolderId = currentFolderId,
                        onSelectFolder = onSelectFolder
                    )
                    Screen.Detail -> MediaDetailScreen(
                        viewModel = viewModel,
                        onDownload = { sel, out ->
                            // Clipbox流: 保存先フォルダを選んでからダウンロード
                            pendingSelection = sel
                            pendingOutput = out
                            showFolderChooser = true
                        },
                        onBack = { screen = Screen.Browser }
                    )
                    Screen.Player -> PlayerScreen(
                        uri = playerUri,
                        title = playerTitle,
                        isVideo = playerIsVideo,
                        onBack = { screen = Screen.MyCollection }
                    )
                    Screen.Download -> DownloadScreen(viewModel = viewModel, onBack = { screen = Screen.MyCollection })
                    Screen.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { screen = Screen.Browser },
                        downloadRootDisplay = downloadRootDisplay,
                        onPickDownloadFolder = onPickDownloadFolder,
                        onResetToInternal = onResetDownloadFolder
                    )
                }
            }

            if (screen != Screen.Player && screen != Screen.Detail) {
                NavigationBar(containerColor = ClipboxColors.Surface) {
                    NavigationBarItem(
                        selected = screen == Screen.Browser,
                        onClick = { screen = Screen.Browser },
                        icon = { Text("🌐") },
                        label = { Text("ブラウザ") }
                    )
                    NavigationBarItem(
                        selected = screen == Screen.MyCollection || screen == Screen.Download || screen == Screen.Player,
                        onClick = { screen = Screen.MyCollection },
                        icon = { Text("📁") },
                        label = { Text("マイコレクション") }
                    )
                    NavigationBarItem(
                        selected = screen == Screen.Settings,
                        onClick = { screen = Screen.Settings },
                        icon = { Text("⚙") },
                        label = { Text("設定") }
                    )
                }
            }
        }

        // Folder chooser for download destination (Clipbox: 保存先フォルダーを選び、OK)
        if (showFolderChooser) {
            AlertDialog(
                onDismissRequest = { showFolderChooser = false; pendingSelection = null; pendingOutput = null },
                title = { Text("保存先フォルダを選択") },
                text = {
                    Column {
                        Text("「${downloadRootDisplay}」配下のどこに保存しますか？", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                        Text("フォルダが無い場合はマイコレクションで作成できます。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                        // Root
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                val url = pendingUrl ?: (viewModel.analyzeState.value as? AnalyzeState.Success)?.media?.let { "https://www.youtube.com/watch?v=${it.id}" } ?: ""
                                if (url.isNotBlank() && pendingSelection != null && pendingOutput != null) {
                                    viewModel.download(url, pendingSelection!!, pendingOutput!!, null)
                                    showFolderChooser = false
                                    pendingSelection = null
                                    pendingOutput = null
                                    screen = Screen.MyCollection
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = if (currentFolderId == null) androidx.compose.ui.graphics.Color(0xFFDBEAFE) else androidx.compose.ui.graphics.Color.White)
                        ) {
                            Text("📁 ルート (${downloadRootDisplay})", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (folders.isEmpty()) {
                            Text("サブフォルダはありません", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary, modifier = Modifier.padding(8.dp))
                        } else {
                            LazyColumn {
                                items(folders) { f ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                            val url = pendingUrl ?: (viewModel.analyzeState.value as? AnalyzeState.Success)?.media?.let { "https://www.youtube.com/watch?v=${it.id}" } ?: ""
                                            if (url.isNotBlank() && pendingSelection != null && pendingOutput != null) {
                                                viewModel.download(url, pendingSelection!!, pendingOutput!!, f.id)
                                                showFolderChooser = false
                                                pendingSelection = null
                                                pendingOutput = null
                                                screen = Screen.MyCollection
                                            }
                                        },
                                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
                                    ) {
                                        Text("${f.icon} ${f.name}", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showFolderChooser = false }) { Text("キャンセル") } }
            )
        }

        if (showAreYouOk && pendingUrl != null) {
            AlertDialog(
                onDismissRequest = { showAreYouOk = false },
                title = { Text("Are you ok?") },
                text = { Text("このページをクリップしますか？\n${pendingUrl}\n\n対応: YouTube / ニコニコ / X / Instagram / TikTok など yt-dlp 1800+ サイト\n外部ブラウザから共有されたURLもここで解析できます。") },
                confirmButton = {
                    TextButton(onClick = {
                        showAreYouOk = false
                        showReally = true
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showAreYouOk = false; pendingUrl = null }) { Text("キャンセル") }
                }
            )
        }
        if (showReally && pendingUrl != null) {
            AlertDialog(
                onDismissRequest = { showReally = false },
                title = { Text("Really?") },
                text = { Text("yt-dlpで解析して、動画 or 音楽 を選んで保存します。\n保存先「${downloadRootDisplay}」配下に保存され、マイコレクションで整理できます。") },
                confirmButton = {
                    TextButton(onClick = {
                        showReally = false
                        val u = pendingUrl!!
                        viewModel.analyze(u)
                    }) { Text("クリップ") }
                },
                dismissButton = {
                    TextButton(onClick = { showReally = false; pendingUrl = null }) { Text("キャンセル") }
                }
            )
        }
    }
}
