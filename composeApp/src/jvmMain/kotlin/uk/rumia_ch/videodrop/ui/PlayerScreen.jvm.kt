package uk.rumia_ch.videodrop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.rumia_ch.videodrop.ui.theme.ClipboxColors

@Composable
actual fun PlayerScreen(
    uri: String,
    title: String,
    isVideo: Boolean,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(ClipboxColors.Background).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isVideo) "🎬 動画プレイヤー" else "🎵 音楽プレイヤー", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
            Text(uri, style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            Text("Desktopではプレビューはスタブです。Android実機で再生できます。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text("戻る") }
        }
    }
}
