package uk.rumia_ch.videodrop.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import uk.rumia_ch.videodrop.ui.theme.ClipboxColors

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun BrowserScreen(
    onClipRequest: (url: String) -> Unit
) {
    var urlInput by remember { mutableStateOf("https://m.youtube.com") }
    var currentUrl by remember { mutableStateOf("https://m.youtube.com") }
    var progress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClipboxColors.Background)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("ブラウザ — YouTube専用", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
                Text("YouTubeの動画ページを開き、再生してから「＋ クリップ」で動画/音楽を選択保存", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("https://m.youtube.com/...") },
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            var u = urlInput.trim()
                            if (!u.startsWith("http")) u = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(u, "UTF-8")
                            currentUrl = u
                            webViewRef?.loadUrl(u)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary)
                    ) { Text("移動") }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary)
                    ) { Text("◀ 戻る") }
                    Button(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary)
                    ) { Text("進む ▶") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { webViewRef?.reload() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary)
                    ) { Text("更新") }
                }
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text("🔖 ブックマーク", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary, modifier = Modifier.padding(end = 12.dp))
                    Text("🕒 履歴", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                }
            }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowFileAccess = true
                        settings.userAgentString = settings.userAgentString + " VideoDrop/1.0 ClipboxLike"
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                url?.let {
                                    currentUrl = it
                                    urlInput = it
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                url?.let {
                                    currentUrl = it
                                    urlInput = it
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                        }
                        setDownloadListener { url, _, _, _, _ ->
                            onClipRequest(url)
                        }
                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                update = { view ->
                    webViewRef = view
                }
            )
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
            ) {
                Button(
                    onClick = {
                        val u = webViewRef?.url ?: currentUrl
                        onClipRequest(u)
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("＋ クリップ", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
        ) {
            Text(
                "💡 YouTube専用: ブラウザでYouTube動画を開き一度再生→「＋ クリップ」→動画/音楽を選択。PDF等の文書は対象外。",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9A3412),
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
