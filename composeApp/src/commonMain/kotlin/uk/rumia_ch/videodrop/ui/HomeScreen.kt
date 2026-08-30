package uk.rumia_ch.videodrop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.rumia_ch.videodrop.core.AnalyzeState

@Composable
fun HomeScreen(
    viewModel: VideoDropViewModel,
    onAnalyzeSuccess: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    val analyzeState by viewModel.analyzeState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("VideoDrop", style = MaterialTheme.typography.headlineMedium)
        Text("Compose Multiplatform + yt-dlp", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("YouTube URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Button(
                onClick = {
                    // TODO: implement Clipboard paste via expect/actual
                    // For now, no-op (Phase 1 stub)
                }
            ) {
                Text("Paste")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { viewModel.analyze(url) },
                enabled = analyzeState !is AnalyzeState.Loading && url.isNotBlank()
            ) {
                Text("Analyze")
            }
        }
        Spacer(Modifier.height(16.dp))
        when (val s = analyzeState) {
            is AnalyzeState.Idle -> Text("Enter URL and tap Analyze", style = MaterialTheme.typography.bodySmall)
            is AnalyzeState.Loading -> {
                Row {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(8.dp))
                    Text("Analyzing...")
                }
            }
            is AnalyzeState.Success -> {
                Text("Found: ${s.media.title}", style = MaterialTheme.typography.titleMedium)
                Text("Duration: ${s.media.durationSeconds?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" } ?: "unknown"}")
                Text("Uploader: ${s.media.uploader ?: "unknown"}")
                Text("Formats: ${s.media.formats.size}")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onAnalyzeSuccess) {
                    Text("View Formats")
                }
                Button(
                    onClick = { viewModel.resetAnalyze() },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Reset")
                }
            }
            is AnalyzeState.Error -> {
                Text("Error: ${s.error}", color = MaterialTheme.colorScheme.error)
                Text(
                    "Details: ${when (s.error) {
                        is uk.rumia_ch.videodrop.core.YtDlpError.InvalidUrl -> "Invalid URL"
                        is uk.rumia_ch.videodrop.core.YtDlpError.PoTokenRequired -> "PO Token required (Phase 10)"
                        is uk.rumia_ch.videodrop.core.YtDlpError.QuickJsUnavailable -> "QuickJS not available"
                        is uk.rumia_ch.videodrop.core.YtDlpError.FfmpegUnavailable -> "FFmpeg not available"
                        else -> s.error.toString()
                    }}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
