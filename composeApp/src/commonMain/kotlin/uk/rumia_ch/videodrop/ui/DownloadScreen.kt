package uk.rumia_ch.videodrop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.rumia_ch.videodrop.core.DownloadEvent

@Composable
fun DownloadScreen(
    viewModel: VideoDropViewModel,
    onBack: () -> Unit
) {
    val events by viewModel.downloadEvents.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Downloads", style = MaterialTheme.typography.headlineSmall)
        if (events.isEmpty()) {
            Text("No downloads yet. Analyze a URL and start a download.", style = MaterialTheme.typography.bodySmall)
        }
        events.values.forEach { event ->
            DownloadRow(event = event, onCancel = { viewModel.cancelCurrent() })
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Back") }
    }
}

@Composable
private fun DownloadRow(
    event: DownloadEvent,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        when (event) {
            is DownloadEvent.Started -> {
                Text("Started: ${event.id.take(12)}...", style = MaterialTheme.typography.bodyMedium)
                Text("queued / starting...", style = MaterialTheme.typography.bodySmall)
            }
            is DownloadEvent.Progress -> {
                Text("Downloading: ${event.id.take(12)}...", style = MaterialTheme.typography.bodyMedium)
                val pct = event.percent
                if (pct != null) {
                    LinearProgressIndicator(progress = { (pct / 100).toFloat().coerceIn(0f, 1f) })
                    Text("${pct.toInt()}% ${event.downloadedBytes ?: "?"} / ${event.totalBytes ?: "?"} bytes", style = MaterialTheme.typography.bodySmall)
                } else {
                    LinearProgressIndicator()
                    Text("${event.downloadedBytes ?: "?"} / ${event.totalBytes ?: "?"} bytes", style = MaterialTheme.typography.bodySmall)
                }
                Text("speed: ${event.speedBytesPerSecond?.let { "${it / 1024} KB/s" } ?: "?"} eta: ${event.etaSeconds ?: "?"}s", style = MaterialTheme.typography.bodySmall)
                Row {
                    Button(onClick = onCancel) { Text("Cancel") }
                }
            }
            is DownloadEvent.PostProcessing -> {
                Text("Merging: ${event.id.take(12)}...", style = MaterialTheme.typography.bodyMedium)
                Text("FFmpeg merging video+audio...", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator()
            }
            is DownloadEvent.Completed -> {
                Text("Completed: ${event.id.take(12)}", color = MaterialTheme.colorScheme.primary)
                Text("Saved: ${event.uri}", style = MaterialTheme.typography.bodySmall)
            }
            is DownloadEvent.Failed -> {
                Text("Failed: ${event.id.take(12)}", color = MaterialTheme.colorScheme.error)
                Text("Error: ${event.error}", style = MaterialTheme.typography.bodySmall)
            }
            is DownloadEvent.Cancelled -> {
                Text("Cancelled: ${event.id.take(12)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
