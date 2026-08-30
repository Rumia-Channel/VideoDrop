package uk.rumia_ch.videodrop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import uk.rumia_ch.videodrop.core.FormatSelection
import uk.rumia_ch.videodrop.core.MediaFormat
import uk.rumia_ch.videodrop.core.OutputType

@Composable
fun MediaDetailScreen(
    viewModel: VideoDropViewModel,
    onDownload: (FormatSelection, OutputType) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.analyzeState.collectAsState()
    val media = (state as? AnalyzeState.Success)?.media

    var selectedPreset by remember { mutableStateOf("Best") }
    var showDetails by remember { mutableStateOf(false) }
    var selectedFormatId by remember { mutableStateOf<String?>(null) }

    if (media == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("No media - analyze a URL first")
            Button(onClick = onBack) { Text("Back") }
        }
        return
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(media.title, style = MaterialTheme.typography.titleLarge)
        Text("Uploader: ${media.uploader ?: "unknown"}", style = MaterialTheme.typography.bodySmall)
        Text("Duration: ${media.durationSeconds ?: "unknown"}s", style = MaterialTheme.typography.bodySmall)
        if (media.thumbnailUrl != null) {
            Text("Thumbnail: ${media.thumbnailUrl}", style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
        Spacer(Modifier.height(12.dp))
        Text("Quick presets:", style = MaterialTheme.typography.titleSmall)
        Row {
            listOf("Best", "Best video", "Audio only").forEach { preset ->
                FilterChip(
                    selected = selectedPreset == preset,
                    onClick = { selectedPreset = preset; selectedFormatId = null },
                    label = { Text(preset) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Row {
            FilterChip(
                selected = showDetails,
                onClick = { showDetails = !showDetails },
                label = { Text(if (showDetails) "Hide details" else "Show details") }
            )
        }
        Spacer(Modifier.height(12.dp))
        // Preset actions
        Row {
            Button(onClick = {
                val sel = when (selectedPreset) {
                    "Best" -> FormatSelection.Best
                    "Audio only" -> FormatSelection.Exact("bestaudio/best")
                    else -> FormatSelection.Best
                }
                val out = if (selectedPreset == "Audio only") OutputType.Audio else OutputType.Video
                onDownload(sel, out)
            }) {
                Text("Download: $selectedPreset")
            }
            if (selectedFormatId != null) {
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    onDownload(FormatSelection.Exact(selectedFormatId!!), OutputType.Video)
                }) {
                    Text("Download selected")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Available formats: ${media.formats.size}", style = MaterialTheme.typography.titleSmall)
        LazyColumn {
            items(media.formats) { fmt ->
                FormatRow(
                    format = fmt,
                    selected = selectedFormatId == fmt.formatId,
                    onSelect = { selectedFormatId = fmt.formatId }
                )
            }
        }
        Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("Back") }
    }
}

@Composable
private fun FormatRow(
    format: MediaFormat,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onSelect
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row {
                Text(format.formatId, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Text(format.extension ?: "unknown", style = MaterialTheme.typography.bodySmall)
                if (selected) {
                    Spacer(Modifier.width(8.dp))
                    Text("SELECTED", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                buildString {
                    append("Res: ${format.width ?: "?"}x${format.height ?: "?"}")
                    append(" FPS: ${format.fps ?: "?"}")
                    append(" vcodec:${format.videoCodec ?: "none"} acodec:${format.audioCodec ?: "none"}")
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "tbr:${format.bitrate ?: "?"} size:${format.fileSize ?: "?"} hasVideo:${format.hasVideo} hasAudio:${format.hasAudio}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
