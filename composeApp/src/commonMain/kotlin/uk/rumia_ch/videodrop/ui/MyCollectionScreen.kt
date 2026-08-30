package uk.rumia_ch.videodrop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import uk.rumia_ch.videodrop.core.DownloadEvent
import uk.rumia_ch.videodrop.ui.theme.ClipboxColors

/**
 * yt-dlp対応サイト全般 マイコレクション
 * - PDF/文書は対象外だが、YouTube以外 (ニコニコ/X/TikTok等) も含む
 */
@Composable
fun MyCollectionScreen(
    viewModel: VideoDropViewModel,
    onOpenFile: (uri: String, title: String, isVideo: Boolean) -> Unit = { _, _, _ -> }
) {
    val events by viewModel.downloadEvents.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("すべて") }
    var isGrid by remember { mutableStateOf(false) }

    val files = remember(events) {
        events.values.filterIsInstance<DownloadEvent.Completed>().map {
            val name = it.uri.substringAfterLast("/")
            val isVideo = name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv")
            val title = name.substringBeforeLast(".")
            FileItem(name = name, uri = it.uri, id = it.id, title = title, isVideo = isVideo)
        }
    }

    val filtered = remember(files, query, filter) {
        files.filter {
            val q = query.trim()
            val matchesQuery = q.isEmpty() || it.name.contains(q, ignoreCase = true) || it.title.contains(q, ignoreCase = true)
            val matchesFilter = when (filter) {
                "動画" -> it.isVideo
                "音楽" -> !it.isVideo
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ClipboxColors.Background).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("マイコレクション", style = MaterialTheme.typography.titleLarge, color = ClipboxColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("yt-dlp対応", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary, modifier = Modifier.padding(end = 8.dp))
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary),
                shape = RoundedCornerShape(8.dp)
            ) { Text("＋ フォルダ") }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("🔍 保存した動画/音楽を検索", color = ClipboxColors.TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("すべて", "動画", "音楽").forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        Text("フォルダ", style = MaterialTheme.typography.titleSmall, color = ClipboxColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            FolderCard(name = "動画", count = files.count { it.isVideo }, icon = "🎬")
            FolderCard(name = "音楽", count = files.count { !it.isVideo }, icon = "🎵")
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("保存した動画・音楽", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Button(
                onClick = { isGrid = !isGrid },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary)
            ) { Text(if (isGrid) "≡ リスト" else "▦ グリッド") }
        }

        if (filtered.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎬", style = MaterialTheme.typography.headlineLarge)
                    Text("保存した動画はありません", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
                    Text("ブラウザで YouTube / ニコニコ / X などの動画ページを開き「＋ クリップ」→動画/音楽を選択するとここに表示されタップで再生できます", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("対応: yt-dlpがサポートする約1800サイト (YouTube含む)。PDF等の文書は対象外", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                }
            }
        } else {
            if (isGrid) {
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                    items(filtered) { f -> FileGridCard(f, onOpenFile) }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { f -> FileRowCard(f, onOpenFile) }
                }
            }
        }

        val downloading = events.values.filterIsInstance<DownloadEvent.Progress>()
        if (downloading.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("ダウンロード中", style = MaterialTheme.typography.titleSmall)
            downloading.forEach { ev ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(ev.id.take(16) + "...", style = MaterialTheme.typography.bodySmall)
                        Text("${ev.percent?.toInt() ?: 0}%  ${ev.speedBytesPerSecond?.let { "${it/1024}KB/s"} ?: ""}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private data class FileItem(val name: String, val uri: String, val id: String, val title: String, val isVideo: Boolean)

@Composable
private fun FolderCard(name: String, count: Int, icon: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.width(110.dp).height(90.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Text(name, style = MaterialTheme.typography.bodyMedium, color = ClipboxColors.TextPrimary)
            Text("${count}件", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
        }
    }
}

@Composable
private fun FileRowCard(item: FileItem, onOpen: (String, String, Boolean) -> Unit) {
    val icon = if (item.isVideo) "🎬" else "🎵"
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpen(item.uri, item.title, item.isVideo) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, color = ClipboxColors.TextPrimary, maxLines = 1)
                Text(item.name, style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary, maxLines = 1)
            }
            Text("▶", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.Primary, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun FileGridCard(item: FileItem, onOpen: (String, String, Boolean) -> Unit) {
    Card(
        modifier = Modifier.padding(4.dp).clickable { onOpen(item.uri, item.title, item.isVideo) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (item.isVideo) "🎬" else "🎵", style = MaterialTheme.typography.headlineMedium)
            Text(item.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, minLines = 2)
            Text(if (item.isVideo) "動画" else "音楽", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
        }
    }
}
