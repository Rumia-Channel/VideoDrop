package uk.rumia_ch.videodrop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import uk.rumia_ch.videodrop.core.AppVersionHolder
import uk.rumia_ch.videodrop.core.NoOpYtDlpEngine
import uk.rumia_ch.videodrop.ytdlp.AndroidYtDlpEngine

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_CLIP_URL = "uk.rumia_ch.videodrop.action.CLIP_URL"
        const val EXTRA_CLIP_URL = "extra_clip_url"
    }

    private var pendingClipUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Provide version for core/getAppVersion() (date-based versionName)
        try {
            val pi = packageManager.getPackageInfo(packageName, 0)
            AppVersionHolder.versionName = pi.versionName
            AppVersionHolder.appContext = applicationContext
        } catch (_: Exception) {}

        handleIntent(intent)

        val engine = AndroidYtDlpEngine(applicationContext)

        setContent {
            App(
                engine = engine,
                externalClipUrl = pendingClipUrl,
                onClipUrlConsumed = { pendingClipUrl = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val url = when (intent?.action) {
            ACTION_CLIP_URL -> intent.getStringExtra(EXTRA_CLIP_URL)
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { extractUrl(it) }
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
        if (!url.isNullOrBlank()) {
            pendingClipUrl = url
        }
    }

    private fun extractUrl(text: String): String {
        val regex = Regex("""https?://\S+""")
        return regex.find(text)?.value?.trimEnd('.', ',', ')', '"', '\'') ?: text.trim()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(engine = NoOpYtDlpEngine())
}
