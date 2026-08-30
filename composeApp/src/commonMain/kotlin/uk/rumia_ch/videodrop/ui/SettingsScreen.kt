package uk.rumia_ch.videodrop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: VideoDropViewModel,
    onBack: () -> Unit
) {
    val status by viewModel.runtimeStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshRuntime()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Default quality: Best (video+audio merge)", style = MaterialTheme.typography.bodyMedium)
        Text("Output type: Video / Audio", style = MaterialTheme.typography.bodyMedium)
        Text("Destination: Movies / Music (via MediaStore)", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.refreshRuntime() }) { Text("Refresh Runtime Status") }
        Spacer(Modifier.height(8.dp))
        if (status == null) {
            Text("Runtime: not checked yet", style = MaterialTheme.typography.bodySmall)
        } else {
            status?.let { s ->
                Text("ABI: ${s.nativeAbi ?: "unknown"}", style = MaterialTheme.typography.bodySmall)
                Text("Python: ${s.python}", style = MaterialTheme.typography.bodySmall)
                Text("yt-dlp: ${s.ytDlpVersion ?: "missing (pip not installed or Chaquopy build pending)"}", style = MaterialTheme.typography.bodySmall)
                Text("yt-dlp-ejs: ${s.ytDlpEjsVersion ?: "missing"}", style = MaterialTheme.typography.bodySmall)
                Text("QuickJS: ${s.quickJsVersion ?: "missing - place libqjsexec.so in jniLibs/arm64-v8a"}", style = MaterialTheme.typography.bodySmall)
                Text("FFmpeg: ${s.ffmpegVersion ?: "missing - place libffmpegexec.so"}", style = MaterialTheme.typography.bodySmall)
                Text("FFprobe: ${s.ffprobeVersion ?: "missing - place libffprobeexec.so"}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Debug logging: enabled in debug builds (no sensitive data logged)", style = MaterialTheme.typography.bodySmall)
        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Back") }
    }
}
