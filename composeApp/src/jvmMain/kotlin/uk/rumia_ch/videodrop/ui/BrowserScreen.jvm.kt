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
actual fun BrowserScreen(
    onClipRequest: (url: String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(ClipboxColors.Background).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ブラウザ", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("アプリ内ブラウザは Android で利用できます", color = ClipboxColors.TextSecondary)
            Text("Desktopでは URL を直接入力してください", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onClipRequest("https://www.youtube.com/watch?v=dQw4w9WgXcQ") }) {
                Text("サンプルURLをクリップ")
            }
        }
    }
}
