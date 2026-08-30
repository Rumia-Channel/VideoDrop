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
import androidx.compose.material3.Icon
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

@Composable
fun MyCollectionScreen(
    viewModel: VideoDropViewModel,
    onOpenFile: (String) -> Unit = {}
) {
    val events by viewModel.downloadEvents.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("すべて") }
    var isGrid by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("日付") }

    // Derive file list from Completed events
    val files = remember(events) {
        events.values.filterIsInstance<DownloadEvent.Completed>().map {
            // uri is staging path; display name from uri
            val name = it.uri.substringAfterLast("/")
            FileItem(name = name, uri = it.uri, id = it.id, size = "—", date = "今日")
        }
    }

    val filtered = remember(files, query, filter) {
        files.filter {
            val q = query.trim()
            val matchesQuery = q.isEmpty() || it.name.contains(q, ignoreCase = true)
            val matchesFilter = when (filter) {
                "動画" -> it.name.endsWith(".mp4") || it.name.endsWith(".webm") || it.name.endsWith(".mkv")
                "音楽" -> it.name.endsWith(".mp3") || it.name.endsWith(".m4a") || it.name.endsWith(".opus")
                "画像" -> it.name.endsWith(".jpg") || it.name.endsWith(".png")
                "書類" -> it.name.endsWith(".pdf") || it.name.endsWith(".zip")
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ClipboxColors.Background).padding(12.dp)
    ) {
        // Top: Clipbox-like header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("マイコレクション", style = MaterialTheme.typography.titleLarge, color = ClipboxColors.TextPrimary, modifier = Modifier.weight(1f))
            Button(
                onClick = { /* TODO: create folder dialog */ },
                colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary),
                shape = RoundedCornerShape(8.dp)
            ) { Text("＋ 新規フォルダ") }
        }
        Spacer(Modifier.height(8.dp))

        // Search like Clipbox: 🔍 ファイルを検索
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("🔍 ファイルを検索", color = ClipboxColors.TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Filter chips: すべて / 動画 / 音楽 / 画像 / 書類 (Clipbox doc categories)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("すべて", "動画", "音楽", "画像", "書類").forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Folders row (Clipbox folders: 動画 / 書類 etc.)
        Text("フォルダ", style = MaterialTheme.typography.titleSmall, color = ClipboxColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            FolderCard(name = "動画", count = files.count { it.name.contains(".mp4") }, icon = "🎬")
            FolderCard(name = "音楽", count = files.count { it.name.contains(".mp3") || it.name.contains(".m4a") }, icon = "🎵")
            FolderCard(name = "書類", count = files.count { it.name.contains(".pdf") }, icon = "📄")
        }

        // Sort & view toggle
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("ファイル", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            listOf("日付", "名前", "サイズ").forEach { s ->
                FilterChip(
                    selected = sortBy == s,
                    onClick = { sortBy = s },
                    label = { Text(s, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Button(
                onClick = { isGrid = !isGrid },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary)
            ) { Text(if (isGrid) "≡ リスト" else "▦ グリッド") }
        }

        if (filtered.isEmpty()) {
            // Empty state like Clipbox: 説明
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📁", style = MaterialTheme.typography.headlineLarge)
                    Text("ファイルがありません", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
                    Text("ブラウザで「＋ クリップ」して保存すると、ここに表示されます", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("対応: 動画 / 音楽 / 画像 / PDF / テキスト / zip / rar (Clipbox同等)", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
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

        // Downloading section (ongoing)
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

private data class FileItem(val name: String, val uri: String, val id: String, val size: String, val date: String)

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
private fun FileRowCard(item: FileItem, onOpen: (String) -> Unit) {
    val icon = when {
        item.name.endsWith(".mp4") || item.name.endsWith(".mkv") -> "🎬"
        item.name.endsWith(".mp3") || item.name.endsWith(".m4a") -> "🎵"
        item.name.endsWith(".pdf") -> "📄"
        item.name.endsWith(".jpg") || item.name.endsWith(".png") -> "🖼"
        item.name.endsWith(".zip") || item.name.endsWith(".rar") -> "🗜"
        else -> "📄"
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpen(item.uri) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, color = ClipboxColors.TextPrimary, maxLines = 1)
                Text("${item.date} • ${item.size}", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
            }
            Text("⋮", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextSecondary, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun FileGridCard(item: FileItem, onOpen: (String) -> Unit) {
    Card(
        modifier = Modifier.padding(4.dp).clickable { onOpen(item.uri) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when {
                    item.name.endsWith(".mp4") -> "🎬"
                    item.name.endsWith(".mp3") -> "🎵"
                    item.name.endsWith(".pdf") -> "📄"
                    else -> "📄"
                },
                style = MaterialTheme.typography.headlineMedium
            )
            Text(item.name, style = MaterialTheme.typography.bodySmall, maxLines = 2, minLines = 2)
        }
    }
}
