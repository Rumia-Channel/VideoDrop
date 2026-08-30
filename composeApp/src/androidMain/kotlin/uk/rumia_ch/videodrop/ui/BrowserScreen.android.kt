package uk.rumia_ch.videodrop.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import uk.rumia_ch.videodrop.ui.theme.ClipboxColors
import uk.rumia_ch.videodrop.ytdlp.DefaultBrowserResolver

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun BrowserScreen(
    onClipRequest: (url: String) -> Unit
) {
    val context = LocalContext.current
    val defaultBrowser = remember { DefaultBrowserResolver.resolve(context) }
    val ytDlpCookieKey = remember { DefaultBrowserResolver.ytDlpCookiesArg(context) }

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
                Text("ブラウザ — yt-dlp対応 (1800+サイト)", style = MaterialTheme.typography.titleMedium, color = ClipboxColors.TextPrimary)
                Text("YouTube / ニコニコ / X / Instagram / TikTok など。動画ページを開き再生してから「＋ クリップ」", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("https://...") },
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
                Spacer(Modifier.height(8.dp))
                // Default browser hook — Chromeに限らずデフォルトブラウザ
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("🔗 デフォルトブラウザ連携: ${defaultBrowser.label} (${defaultBrowser.packageName})", style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextPrimary)
                        Text(
                            if (ytDlpCookieKey != null) "ログイン状態を yt-dlp --cookies-from-browser ${ytDlpCookieKey} でフック可能" else "このブラウザは yt-dlpの自動Cookie取得に未対応 — 共有経由でURLをクリップしてください",
                            style = MaterialTheme.typography.bodySmall, color = ClipboxColors.TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Button(
                                onClick = {
                                    val u = webViewRef?.url ?: currentUrl
                                    // Launch in default browser via CustomTabs (shares login state with default browser)
                                    val customTabsIntent = CustomTabsIntent.Builder().build()
                                    // Try to force default browser by setting package
                                    customTabsIntent.intent.setPackage(defaultBrowser.packageName)
                                    try {
                                        customTabsIntent.launchUrl(context, Uri.parse(u))
                                    } catch (_: Exception) {
                                        // Fallback to generic VIEW intent (system picks default browser)
                                        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(u))
                                        context.startActivity(fallback)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ClipboxColors.Primary),
                                modifier = Modifier.weight(1f)
                            ) { Text("外部ブラウザで開く") }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    // Share current URL to trigger our ShareReceiver (hooks default browser's Share)
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, webViewRef?.url ?: currentUrl)
                                    }
                                    context.startActivity(Intent.createChooser(share, "VideoDropでクリップ"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClipboxColors.TextPrimary),
                                modifier = Modifier.weight(1f)
                            ) { Text("共有→クリップ") }
                        }
                        Text("💡 ヒント: 普段使っているブラウザでログイン済みの状態で「共有 → VideoDrop」を選ぶと、ログイン状態をフックできます (CustomTabsはデフォルトブラウザとCookieを共有)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E40AF), modifier = Modifier.padding(top = 6.dp))
                    }
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
                "💡 内蔵ブラウザでも外部ブラウザでもOK。デフォルトブラウザ(現在: ${defaultBrowser.label})でログイン→共有→VideoDropでCookieを活用。yt-dlp対応サイト全般に対応。",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9A3412),
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
