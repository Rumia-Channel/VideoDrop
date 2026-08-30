package uk.rumia_ch.videodrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.AlertDialog
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
import uk.rumia_ch.videodrop.core.NoOpYtDlpEngine
import uk.rumia_ch.videodrop.core.YtDlpEngine
import uk.rumia_ch.videodrop.ui.BrowserScreen
import uk.rumia_ch.videodrop.ui.DownloadScreen
import uk.rumia_ch.videodrop.ui.MediaDetailScreen
import uk.rumia_ch.videodrop.ui.MyCollectionScreen
import uk.rumia_ch.videodrop.ui.PlayerScreen
import uk.rumia_ch.videodrop.ui.Screen
import uk.rumia_ch.videodrop.ui.SettingsScreen
import uk.rumia_ch.videodrop.ui.VideoDropViewModel
import uk.rumia_ch.videodrop.ui.theme.ClipboxColors
import uk.rumia_ch.videodrop.ui.theme.ClipboxTheme

private fun isYouTubeUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains("youtube.com") || lower.contains("youtu.be") || lower.contains("m.youtube.com")
}

@Composable
fun App(
    engine: YtDlpEngine = NoOpYtDlpEngine()
) {
    ClipboxTheme {
        val repository = remember(engine) { DefaultDownloadRepository(engine) }
        val viewModel: VideoDropViewModel = viewModel { VideoDropViewModel(repository) }
        var screen by remember { mutableStateOf(Screen.Browser) }
        var pendingUrl by remember { mutableStateOf<String?>(null) }
        var showAreYouOk by remember { mutableStateOf(false) }
        var showReally by remember { mutableStateOf(false) }
        var showNotYouTube by remember { mutableStateOf(false) }
        var playerUri by remember { mutableStateOf("") }
        var playerTitle by remember { mutableStateOf("") }
        var playerIsVideo by remember { mutableStateOf(true) }

        val analyzeState by viewModel.analyzeState.collectAsState()
        LaunchedEffect(analyzeState) {
            if (analyzeState is AnalyzeState.Success) {
                screen = Screen.Detail
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
                            if (!isYouTubeUrl(url)) {
                                pendingUrl = url
                                showNotYouTube = true
                            } else {
                                pendingUrl = url
                                showAreYouOk = true
                            }
                        }
                    )
                    Screen.MyCollection -> MyCollectionScreen(
                        viewModel = viewModel,
                        onOpenFile = { uri, title, isVideo ->
                            playerUri = uri
                            playerTitle = title
                            playerIsVideo = isVideo
                            screen = Screen.Player
                        }
                    )
                    Screen.Detail -> MediaDetailScreen(
                        viewModel = viewModel,
                        onDownload = { sel, out ->
                            val url = pendingUrl
                                ?: (viewModel.analyzeState.value as? AnalyzeState.Success)?.media?.let { "https://www.youtube.com/watch?v=${it.id}" }
                                ?: ""
                            if (url.isNotBlank()) {
                                viewModel.download(url, sel, out)
                                screen = Screen.MyCollection
                            }
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
                    Screen.Settings -> SettingsScreen(viewModel = viewModel, onBack = { screen = Screen.Browser })
                }
            }

            // Bottom nav: Clipbox流 ブラウザ / マイコレクション / 設定
            // Player と Detail はフルスクリーンなので BottomNav は表示しない
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

        if (showNotYouTube && pendingUrl != null) {
            AlertDialog(
                onDismissRequest = { showNotYouTube = false; pendingUrl = null },
                title = { Text("YouTubeのみ対応") },
                text = { Text("このアプリはブラウザからYouTubeの動画を直接ダウンロードする機能に特化しています。\n\nURL: ${pendingUrl}\n\nYouTubeの動画ページで「＋ クリップ」を押してください。PDF等の文書ビューアーは対象外です。") },
                confirmButton = {
                    TextButton(onClick = { showNotYouTube = false; pendingUrl = null }) { Text("OK") }
                }
            )
        }

        if (showAreYouOk && pendingUrl != null) {
            AlertDialog(
                onDismissRequest = { showAreYouOk = false },
                title = { Text("Are you ok?") },
                text = { Text("このYouTubeページをクリップしますか？\n${pendingUrl}\n\nヒント: 動画は一度再生してから「＋」で検出率UP") },
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
                text = { Text("YouTubeを解析して、動画 or 音楽 の保存形式を選べます。\n保存後はマイコレクションで再生できます。") },
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
