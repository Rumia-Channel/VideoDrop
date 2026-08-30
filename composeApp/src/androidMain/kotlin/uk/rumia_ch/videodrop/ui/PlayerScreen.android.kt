package uk.rumia_ch.videodrop.ui

import android.content.ComponentName
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import uk.rumia_ch.videodrop.ui.theme.ClipboxColors
import uk.rumia_ch.videodrop.ytdlp.PlaybackService

@Composable
actual fun PlayerScreen(
    uri: String,
    title: String,
    isVideo: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var controllerFuture by remember { mutableStateOf<ListenableFuture<MediaController>?>(null) }
    var controller by remember { mutableStateOf<MediaController?>(null) }

    // For single-item play, we also support playlist via PlaylistViewModel
    // If uri is provided, we will set it as a single-item playlist
    LaunchedEffect(uri) {
        if (uri.isNotBlank()) {
            // Build MediaItem for single playback
            // If we have a controller, set media item directly
            controller?.let { c ->
                val item = MediaItem.Builder().setUri(uri).setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()
                ).build()
                c.setMediaItem(item)
                c.prepare()
                c.playWhenReady = true
                c.play()
            }
        }
    }

    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val c = future.get()
                controller = c
                // If we have a pending uri, set it
                if (uri.isNotBlank()) {
                    val item = MediaItem.Builder().setUri(uri).setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()
                    ).build()
                    c.setMediaItem(item)
                    c.prepare()
                    c.playWhenReady = true
                }
                // Enable background: player will be kept by service
                c.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {}
                })
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            controllerFuture?.let { MediaController.releaseFuture(it) }
            // Do NOT release controller here if we want background to continue
            // But for single screen, we keep controller for background, so don't release
            // MediaController will be released when service is destroyed or app is killed
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ClipboxColors.Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("← 戻る") }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title.ifBlank { "再生" }, style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary, maxLines = 1)
                Text(if (isVideo) "動画 • バックグラウンド対応" else "音楽 • バックグラウンド再生", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
            }
        }

        if (uri.isBlank()) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("ファイルが見つかりません", color = ClipboxColors.TextSecondary)
            }
        } else {
            // ExoPlayer PlayerView via AndroidView (supports video, audio, playlist, notification)
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        controllerShowTimeoutMs = 3000
                        // Controller will be set via player assignment
                    }
                },
                update = { playerView ->
                    controller?.let { playerView.player = it }
                }
            )

            // Playlist & controls (simple, integrated with controller)
            // Note: For full playlist, we would use controller.setMediaItems(list) and handle next/prev
            // Here we show basic controls that delegate to controller
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                Button(
                    onClick = { controller?.seekToPreviousMediaItem() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary)
                ) { Text("⏮ 前") }
                // Play/Pause toggle
                var isPlaying by remember { mutableStateOf(false) }
                LaunchedEffect(controller) {
                    controller?.let { c ->
                        isPlaying = c.isPlaying
                        c.addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
                        })
                    }
                }
                Button(
                    onClick = {
                        controller?.let { if (it.isPlaying) it.pause() else it.play() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary)
                ) { Text(if (isPlaying) "⏸ 一時停止" else "▶ 再生") }
                // Next (for playlist)
                Button(
                    onClick = { controller?.seekToNextMediaItem() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary)
                ) { Text("次 ⏭") }
            }

            // Background hint
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("バックグラウンド再生: 有効", style = MaterialTheme.typography.titleSmall, color = ClipboxColors.TextPrimary)
                    Text("ホームに戻ったり画面を消しても、音楽は再生し続けます。通知から一時停止/次へが操作できます。動画はバックグラウンドで一時停止しますが、音楽として保存したものは継続します。", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(
                            onClick = { controller?.let { it.repeatMode = if (it.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE } },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary),
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        ) { Text("🔁 リピート") }
                        Button(
                            onClick = { controller?.shuffleModeEnabled = !(controller?.shuffleModeEnabled ?: false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary),
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        ) { Text("🔀 シャッフル") }
                    }
                }
            }
        }
    }
}

// Extended PlayerScreen for playlist-aware usage (overload for MyCollection)
// This composable is used when a full playlist is available
@Composable
fun PlayerScreenWithPlaylist(
    playlist: List<PlaylistItem>,
    startIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var controllerFuture by remember { mutableStateOf<ListenableFuture<MediaController>?>(null) }
    var controller by remember { mutableStateOf<MediaController?>(null) }

    LaunchedEffect(playlist, startIndex) {
        controller?.let { c ->
            val items = playlist.map { item ->
                MediaItem.Builder().setUri(item.uri).setMediaId(item.id)
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(item.title).build())
                    .build()
            }
            c.setMediaItems(items, startIndex, 0)
            c.prepare()
            c.playWhenReady = true
        }
    }

    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val c = future.get()
                controller = c
                if (playlist.isNotEmpty()) {
                    val items = playlist.map { item ->
                        MediaItem.Builder().setUri(item.uri).setMediaId(item.id)
                            .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(item.title).build()).build()
                    }
                    c.setMediaItems(items, startIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0)), 0)
                    c.prepare()
                    c.playWhenReady = true
                }
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
        onDispose { controllerFuture?.let { MediaController.releaseFuture(it) } }
    }

    Column(modifier = Modifier.fillMaxSize().background(ClipboxColors.Background)) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack) { Text("← 戻る") }
            Spacer(Modifier.width(8.dp))
            Text("プレイリスト再生 (${playlist.size}件)", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary, modifier = Modifier.weight(1f))
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black),
            factory = { ctx -> PlayerView(ctx).apply { useController = true } },
            update = { view -> controller?.let { view.player = it } }
        )
        // Simple queue list
        LazyColumn(modifier = Modifier.height(160.dp).background(Color.White)) {
            itemsIndexed(playlist) { idx, item ->
                val isCurrent = idx == (controller?.currentMediaItemIndex ?: -1)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isCurrent) Color(0xFFDBEAFE) else Color.White)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (item.isVideo) "🎬" else "🎵", modifier = Modifier.padding(end = 8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.bodyMedium, color = if (isCurrent) ClipboxColors.Primary else ClipboxColors.TextPrimary, maxLines = 1)
                            Text(item.uri.substringAfterLast("/"), style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary, maxLines = 1)
                        }
                        if (isCurrent) Text("▶", color = ClipboxColors.Primary)
                    }
                }
            }
        }
    }
}
