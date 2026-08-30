package uk.rumia_ch.videodrop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "VideoDrop",
    ) {
        App()
    }
}
