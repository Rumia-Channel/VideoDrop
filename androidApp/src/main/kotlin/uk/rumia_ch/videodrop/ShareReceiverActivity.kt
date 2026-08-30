package uk.rumia_ch.videodrop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Hooks the user's default browser via Share.
 * Clipbox flow adapted: User browses in default browser (already logged in) → Share → VideoDrop
 * This preserves login state because the URL is from the authenticated session.
 * Cookies are handled separately via DefaultBrowserResolver + yt-dlp --cookies-from-browser
 */
class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { extractUrl(it) }
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }

        // Forward to MainActivity with clip URL
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_CLIP_URL
            putExtra(MainActivity.EXTRA_CLIP_URL, sharedUrl)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(mainIntent)
        finish()
    }

    private fun extractUrl(text: String): String {
        // Share text may be "Check this https://..." -> extract first http url
        val regex = Regex("""https?://\S+""")
        return regex.find(text)?.value?.trimEnd('.', ',', ')', '"', '\'') ?: text.trim()
    }
}
