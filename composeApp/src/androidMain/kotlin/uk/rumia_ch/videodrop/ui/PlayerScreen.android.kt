package uk.rumia_ch.videodrop.ui

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import uk.rumia_ch.videodrop.ui.theme.ClipboxColors
import java.io.File

@Composable
actual fun PlayerScreen(
    uri: String,
    title: String,
    isVideo: Boolean,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(ClipboxColors.Background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("← 戻る") }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary, maxLines = 1)
                Text(if (isVideo) "動画" else "音楽", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
            }
        }

        if (uri.isBlank()) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("ファイルが見つかりません", color = ClipboxColors.TextSecondary)
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val file = File(uri)
                        val videoUri = if (file.exists()) Uri.fromFile(file) else Uri.parse(uri)
                        setVideoURI(videoUri)
                        setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                        setOnPreparedListener { it.isLooping = false; start() }
                        setOnErrorListener { _, _, _ -> false }
                    }
                }
            )
            Text(
                if (isVideo) "ピンチで拡大/連続再生は設定で"
                else "バックグラウンド再生対応 — 今後 MediaSession で拡張",
                style = MaterialTheme.typography.bodySmall,
                color = ClipboxColors.TextSecondary,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
