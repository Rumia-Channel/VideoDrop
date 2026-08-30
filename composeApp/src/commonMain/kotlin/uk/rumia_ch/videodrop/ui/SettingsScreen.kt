package uk.rumia_ch.videodrop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import uk.rumia_ch.videodrop.core.UpdateState
import uk.rumia_ch.videodrop.core.getAppVersion
import uk.rumia_ch.videodrop.ui.theme.ClipboxColors

@Composable
fun SettingsScreen(
    viewModel: VideoDropViewModel,
    onBack: () -> Unit,
    // Download location picker (Android SAF) — provided by App's androidMain
    downloadRootDisplay: String = "内部ストレージ (cacheDir)",
    onPickDownloadFolder: (() -> Unit)? = null,
    onResetToInternal: (() -> Unit)? = null
) {
    val status by viewModel.runtimeStatus.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val repoOwner by viewModel.repoOwner.collectAsState()
    val repoName by viewModel.repoName.collectAsState()
    val uriHandler = LocalUriHandler.current

    var ownerEdit by remember { mutableStateOf(repoOwner) }
    var nameEdit by remember { mutableStateOf(repoName) }

    LaunchedEffect(Unit) {
        viewModel.refreshRuntime()
    }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("設定", style = MaterialTheme.typography.headlineSmall, color = ClipboxColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("バージョン: ${try { getAppVersion() } catch (_: Exception) { "1.0.0" }}  (日付ベース yyyy.MM.dd)", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
        Spacer(Modifier.height(16.dp))

        // Download destination folder — single root, user creates subfolders inside
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("保存先フォルダ", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
                Text("最初に1つだけ指定。マイコレクションではその中に自由にフォルダを作成できます。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("現在の保存先:", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                        Text(downloadRootDisplay, style = MaterialTheme.typography.bodyMedium, color = ClipboxColors.TextPrimary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (onPickDownloadFolder != null) {
                    Button(
                        onClick = onPickDownloadFolder,
                        colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("保存先フォルダを選択 (SDカード対応)") }
                    Spacer(Modifier.height(8.dp))
                    Text("SDカードや外部ストレージを選ぶと、SAFで永続的にアクセスが許可されます。深い階層から選ぶ必要はなく、ルート1つを選べばOK。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    if (onResetToInternal != null) {
                        Button(
                            onClick = onResetToInternal,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("内部ストレージに戻す") }
                    }
                } else {
                    Text("保存先の変更はAndroid版で利用できます", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // App update section - GitHub Releases
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("アップデート", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
                Text("GitHubリポジトリのリリースを確認し、APKをダウンロードします", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text("リポジトリ: https://github.com/${repoOwner}/${repoName}", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = ownerEdit,
                        onValueChange = { ownerEdit = it },
                        label = { Text("Owner") },
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )
                    OutlinedTextField(
                        value = nameEdit,
                        onValueChange = { nameEdit = it },
                        label = { Text("Repo") },
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = {
                            viewModel.setRepo(ownerEdit, nameEdit)
                            viewModel.checkForUpdates()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) { Text("更新を確認") }
                    TextButton(onClick = { uriHandler.openUri("https://github.com/${ownerEdit}/${nameEdit}/releases") }) {
                        Text("GitHubで見る")
                    }
                }
                Spacer(Modifier.height(12.dp))
                when (val s = updateState) {
                    is UpdateState.Idle -> Text("「更新を確認」を押すと最新のリリースをチェックします", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                    is UpdateState.Checking -> Row {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("確認中...", style = MaterialTheme.typography.bodySmall)
                    }
                    is UpdateState.UpToDate -> {
                        Text("✅ 最新です", style = MaterialTheme.typography.bodySmall, color = Color(0xFF065F46))
                        Text("現在 ${try { getAppVersion() } catch (_: Exception) { "1.0.0" }} が最新です", style = MaterialTheme.typography.bodySmall)
                    }
                    is UpdateState.Available -> {
                        val info = s.info
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🎉 アップデートあり: ${info.latestVersion} (現在 ${info.currentVersion})", style = MaterialTheme.typography.titleSmall, color = ClipboxColors.TextPrimary)
                                info.releaseName?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = ClipboxColors.TextPrimary) }
                                info.publishedAt?.let { Text("公開: $it", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary) }
                                info.releaseNotes?.let {
                                    Spacer(Modifier.height(8.dp))
                                    Text(it.take(500), style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                                    if (it.length > 500) Text("...", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { info.htmlUrl?.let { uriHandler.openUri(it) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("GitHubリリースを開く") }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { info.downloadUrl?.let { uriHandler.openUri(it) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("APKをダウンロード") }
                                Text("ダウンロード後、ファイルを開いてインストールしてください", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                    is UpdateState.Error -> {
                        Text("❌ エラー: ${s.message}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB91C1C))
                        Text("リポジトリが存在しない、リリースがまだ無い、またはネットワークエラーです。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("再生・保存", style = MaterialTheme.typography.titleSmall, color = ClipboxColors.TextPrimary)
        Text("保存先フォルダ配下に動画/音楽を保存。サブフォルダはマイコレクションで自由に作成できます。", style = MaterialTheme.typography.bodyMedium)
        Text("Destination: 選択した保存先フォルダ (SDカード対応)", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        Button(onClick = { viewModel.refreshRuntime() }) { Text("ランタイム情報を更新") }
        Spacer(Modifier.height(8.dp))
        if (status == null) {
            Text("Runtime: not checked yet", style = MaterialTheme.typography.bodySmall)
        } else {
            status?.let { s ->
                Text("ABI: ${s.nativeAbi ?: "unknown"}", style = MaterialTheme.typography.bodySmall)
                Text("Python: ${s.python}", style = MaterialTheme.typography.bodySmall)
                Text("yt-dlp: ${s.ytDlpVersion ?: "missing"}", style = MaterialTheme.typography.bodySmall)
                Text("yt-dlp-ejs: ${s.ytDlpEjsVersion ?: "missing"}", style = MaterialTheme.typography.bodySmall)
                Text("QuickJS: ${s.quickJsVersion ?: "missing - CIでビルド or jniLibsへ"}", style = MaterialTheme.typography.bodySmall)
                Text("FFmpeg: ${s.ffmpegVersion ?: "missing"}", style = MaterialTheme.typography.bodySmall)
                Text("FFprobe: ${s.ffprobeVersion ?: "missing"}", style = MaterialTheme.typography.bodySmall)
                Text("バージョン(日付): ${try { getAppVersion() } catch (_: Exception) { "1.0.0" }}", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("対応サイト: yt-dlpがサポートする約1800サイト。外部ブラウザ連携でログイン状態を共有。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("戻る") }
    }
}
