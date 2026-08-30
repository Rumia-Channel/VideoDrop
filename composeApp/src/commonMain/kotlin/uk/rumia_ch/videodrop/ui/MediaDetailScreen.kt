package uk.rumia_ch.videodrop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import uk.rumia_ch.videodrop.core.AnalyzeState
import uk.rumia_ch.videodrop.core.FormatSelection
import uk.rumia_ch.videodrop.core.OutputType
import uk.rumia_ch.videodrop.ui.theme.ClipboxColors

/**
 * yt-dlp対応サイト全般 詳細 → 動画 or 音楽 選択
 * YouTubeだけでなくニコニコ/X/TikTok等も解析可能
 */
@Composable
fun MediaDetailScreen(
    viewModel: VideoDropViewModel,
    onDownload: (FormatSelection, OutputType) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.analyzeState.collectAsState()
    val media = (state as? AnalyzeState.Success)?.media

    if (media == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("情報を取得できませんでした", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
            Text("ブラウザで動画ページを開き、再生してから「＋ クリップ」を試してください。yt-dlpが対応していないサイトの場合はエラーになります。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("ブラウザに戻る") }
        }
        return
    }

    Column(modifier = Modifier.padding(16.dp).background(ClipboxColors.Background)) {
        Text(media.title, style = MaterialTheme.typography.titleLarge, color = ClipboxColors.TextPrimary)
        Text("投稿者: ${media.uploader ?: "不明"}", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
        Text(
            "長さ: ${media.durationSeconds?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" } ?: "不明"}  •  解析サイト",
            style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary
        )
        if (media.thumbnailUrl != null) {
            Text("サムネイルあり", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
        }
        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("保存形式を選んでください", style = MaterialTheme.typography.titleSmall, color = ClipboxColors.TextPrimary)
                Text("動画のまま保存するか、音楽(音声のみ)として保存するか選べます。yt-dlp対応サイトならYouTube以外も同様に保存できます。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🎬 動画として保存", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
                Text("最高画質 (bestvideo*+bestaudio) で保存。FFmpegで自動結合。YouTube/ニコニコ/Xなど対応。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onDownload(FormatSelection.Best, OutputType.Video) },
                    colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("動画でダウンロード") }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🎵 音楽として保存", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
                Text("音声のみ (bestaudio) で保存。音楽・ラジオ・ポッドキャスト向け。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onDownload(FormatSelection.Exact("bestaudio/best"), OutputType.Audio) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("音楽でダウンロード") }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("保存後は マイコレクション で再生できます。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
        Spacer(Modifier.height(12.dp))

        Row {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary)
            ) { Text("戻る") }
            Spacer(Modifier.width(8.dp))
            Text("${media.formats.size}件のフォーマットが解析されました", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
