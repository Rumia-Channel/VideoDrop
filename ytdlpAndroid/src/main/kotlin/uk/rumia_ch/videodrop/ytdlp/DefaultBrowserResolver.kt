package uk.rumia_ch.videodrop.ytdlp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Detects the user's default browser (not Chrome-limited) for hooking login state.
 * Uses PackageManager to resolve ACTION_VIEW for http.
 * Maps package to yt-dlp --cookies-from-browser key.
 */
object DefaultBrowserResolver {

    data class BrowserInfo(
        val packageName: String,
        val label: String,
        val ytDlpKey: String? // null if not directly supported by yt-dlp cookies-from-browser
    )

    private val MAP = mapOf(
        "com.android.chrome" to BrowserInfo("com.android.chrome", "Chrome", "chrome"),
        "com.chrome.beta" to BrowserInfo("com.chrome.beta", "Chrome Beta", "chrome"),
        "com.chrome.dev" to BrowserInfo("com.chrome.dev", "Chrome Dev", "chrome"),
        "com.chrome.canary" to BrowserInfo("com.chrome.canary", "Chrome Canary", "chrome"),
        "org.mozilla.firefox" to BrowserInfo("org.mozilla.firefox", "Firefox", "firefox"),
        "org.mozilla.firefox_beta" to BrowserInfo("org.mozilla.firefox_beta", "Firefox Beta", "firefox"),
        "org.mozilla.fenix" to BrowserInfo("org.mozilla.fenix", "Firefox", "firefox"),
        "com.sec.android.app.sbrowser" to BrowserInfo("com.sec.android.app.sbrowser", "Samsung Internet", null),
        "com.microsoft.emmx" to BrowserInfo("com.microsoft.emmx", "Edge", "edge"),
        "com.brave.browser" to BrowserInfo("com.brave.browser", "Brave", "brave"),
        "com.opera.browser" to BrowserInfo("com.opera.browser", "Opera", "opera"),
        "com.opera.mini.native" to BrowserInfo("com.opera.mini.native", "Opera Mini", "opera"),
        "com.vivaldi.browser" to BrowserInfo("com.vivaldi.browser", "Vivaldi", "vivaldi"),
        "com.duckduckgo.mobile.android" to BrowserInfo("com.duckduckgo.mobile.android", "DuckDuckGo", null)
    )

    fun resolve(context: Context): BrowserInfo {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
        val pm = context.packageManager
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val pkg = resolveInfo?.activityInfo?.packageName ?: "com.android.chrome"
        return MAP[pkg] ?: BrowserInfo(pkg, pkg.substringAfterLast("."), null)
    }

    fun ytDlpCookiesArg(context: Context): String? {
        return resolve(context).ytDlpKey
    }
}
