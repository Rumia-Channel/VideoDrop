package uk.rumia_ch.videodrop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * yt-dlp対応 マイコレクション — ダウンロード先ルート配下を可視化
 * - 設定で選んだ保存先フォルダ (内部/SDカードSAF) がルート
 * - その中にユーザーが自由にフォルダを作成/リネーム/削除、ファイルを移動
 */
@Composable
fun MyCollectionScreen(
    viewModel: VideoDropViewModel,
    onOpenFile: (uri: String, title: String, isVideo: Boolean) -> Unit = { _, _, _ -> },
    // Folder customization hooks — provided by App's androidMain via LocationViewModel
    folders: List<FolderUi> = emptyList(),
    onCreateFolder: ((String) -> Unit)? = null,
    onRenameFolder: ((String, String) -> Unit)? = null,
    onDeleteFolder: ((String) -> Unit)? = null,
    onMoveFile: ((fileId: String, targetFolderId: String) -> Unit)? = null,
    downloadRootDisplay: String = "内部ストレージ",
    currentFolderId: String? = null,
    onSelectFolder: ((String?) -> Unit)? = null
) {
    val events by viewModel.downloadEvents.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("すべて") }
    var isGrid by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showFolderMenuFor by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<FolderUi?>(null) }
    var renameText by remember { mutableStateOf("") }

    val files = remember(events) {
        events.values.filterIsInstance<DownloadEvent.Completed>().map {
            val name = it.uri.substringAfterLast("/")
            val isVideo = name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv")
            val title = name.substringBeforeLast(".")
            FileItem(name = name, uri = it.uri, id = it.id, title = title, isVideo = isVideo, folderId = null)
        }
    }

    // Merge with folder filter: if onCreateFolder is provided, we are in SAF mode and use actual folders
    val displayFolders = if (folders.isNotEmpty()) folders else listOf(
        FolderUi("sys_video", "動画", "🎬",  files.count { it.isVideo }),
        FolderUi("sys_music", "音楽", "🎵",  files.count { !it.isVideo })
    )

    val filtered = remember(files, query, filter, currentFolderId) {
        files.filter {
            val q = query.trim()
            val matchesQuery = q.isEmpty() || it.name.contains(q, ignoreCase = true) || it.title.contains(q, ignoreCase = true)
            val matchesFilter = when (filter) {
                "動画" -> it.isVideo
                "音楽" -> !it.isVideo
                else -> true
            }
            val matchesFolder = if (currentFolderId == null) true else {
                // Simple: folderId == null means root, else match
                // For now, map sys_video/sys_music to isVideo
                when (currentFolderId) {
                    "sys_video" -> it.isVideo
                    "sys_music" -> !it.isVideo
                    else -> it.folderId == currentFolderId
                }
            }
            matchesQuery && matchesFilter && matchesFolder
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ClipboxColors.Background).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("マイコレクション", style = MaterialTheme.typography.titleLarge, color = ClipboxColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(downloadRootDisplay.take(16), style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary, modifier = Modifier.padding(end = 8.dp))
            if (onCreateFolder != null) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("＋ フォルダ") }
            }
        }
        if (currentFolderId != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = { onSelectFolder?.invoke(null) }) { Text("← ルートに戻る") }
                Text(" / ${displayFolders.find { it.id == currentFolderId }?.name ?: currentFolderId}", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
            }
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

        Text("フォルダ — 保存先「${downloadRootDisplay}」配下", style = MaterialTheme.typography.titleSmall, color = ClipboxColors.TextPrimary)
        if (onCreateFolder == null) {
            Text("フォルダのカスタム作成は Android の設定から保存先を選ぶと有効になります", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
        }
        // Folder grid with menu
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(displayFolders.size) { idx ->
                val f = displayFolders[idx]
                Box {
                    FolderCard(name = f.name, count = f.count, icon = f.icon, isSelected = currentFolderId == f.id, onClick = {
                        onSelectFolder?.invoke(if (currentFolderId == f.id) null else f.id)
                    }, onLongClick = { showFolderMenuFor = f.id })
                    DropdownMenu(
                        expanded = showFolderMenuFor == f.id,
                        onDismissRequest = { showFolderMenuFor = null }
                    ) {
                        if (!f.isSystem) {
                            DropdownMenuItem(text = { Text("リネーム") }, onClick = {
                                showFolderMenuFor = null
                                renameTarget = f
                                renameText = f.name
                            })
                            DropdownMenuItem(text = { Text("削除", color = Color(0xFFB91C1C)) }, onClick = {
                                showFolderMenuFor = null
                                onDeleteFolder?.invoke(f.id)
                            })
                        } else {
                            DropdownMenuItem(text = { Text("システムフォルダ") }, onClick = { showFolderMenuFor = null })
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("保存した動画・音楽${if (currentFolderId != null) " — ${displayFolders.find { it.id == currentFolderId }?.name}" else ""}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
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
                    Text("ファイルがありません", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
                    Text("ブラウザで動画ページを開き「＋ クリップ」→動画/音楽を選択するとここに表示。フォルダを作って整理できます。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                }
            }
        } else {
            if (isGrid) {
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                    items(filtered) { f -> FileGridCard(f, onOpenFile, onMoveFile, displayFolders) }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { f -> FileRowCard(f, onOpenFile, onMoveFile, displayFolders) }
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

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("フォルダ作成") },
            text = {
                Column {
                    Text("保存先「${downloadRootDisplay}」直下に作成されます", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("例: 推し活") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        onCreateFolder?.invoke(newFolderName)
                        newFolderName = ""
                        showCreateDialog = false
                    }
                }) { Text("作成") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("キャンセル") } }
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("リネーム") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, placeholder = { Text(renameTarget!!.name) })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) onRenameFolder?.invoke(renameTarget!!.id, renameText)
                    renameTarget = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("キャンセル") } }
        )
    }
}

data class FolderUi(val id: String, val name: String, val icon: String, val count: Int, val isSystem: Boolean = false)

private data class FileItem(val name: String, val uri: String, val id: String, val title: String, val isVideo: Boolean, val folderId: String? = null)

@Composable
private fun FolderCard(name: String, count: Int, icon: String, isSelected: Boolean = false, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFDBEAFE) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        modifier = Modifier.width(110.dp).height(90.dp).clickable(onClick = onClick).let { m ->
            // Long click via combinedClickable would need foundation 1.6, use clickable for now and rely on menu via long press handled by parent Box
            m
        }
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Text(name, style = MaterialTheme.typography.bodyMedium, color = ClipboxColors.TextPrimary, maxLines = 1)
            Text("${count}件", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
        }
    }
}

@Composable
private fun FileRowCard(item: FileItem, onOpen: (String, String, Boolean) -> Unit, onMoveFile: ((String, String) -> Unit)?, folders: List<FolderUi>) {
    var showMove by remember { mutableStateOf(false) }
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
            Box {
                Text("⋮", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextSecondary, modifier = Modifier.padding(start = 8.dp).clickable { showMove = true })
                DropdownMenu(expanded = showMove, onDismissRequest = { showMove = false }) {
                    folders.forEach { f ->
                        DropdownMenuItem(text = { Text("→ ${f.icon} ${f.name}") }, onClick = {
                            showMove = false
                            onMoveFile?.invoke(item.id, f.id)
                        })
                    }
                    DropdownMenuItem(text = { Text("詳細") }, onClick = { showMove = false; onOpen(item.uri, item.title, item.isVideo) })
                }
            }
        }
    }
}

@Composable
private fun FileGridCard(item: FileItem, onOpen: (String, String, Boolean) -> Unit, onMoveFile: ((String, String) -> Unit)?, folders: List<FolderUi>) {
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
