package uk.rumia_ch.videodrop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uk.rumia_ch.videodrop.core.NoOpYtDlpEngine
import uk.rumia_ch.videodrop.ytdlp.AndroidYtDlpEngine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val engine = AndroidYtDlpEngine(applicationContext)

        setContent {
            App(engine = engine)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(engine = NoOpYtDlpEngine())
}
