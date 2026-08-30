package uk.rumia_ch.videodrop.ui

import androidx.compose.runtime.Composable

/**
 * YouTube downloaded video/audio player.
 * Android actual uses VideoView/ExoPlayer, JVM is placeholder.
 */
@Composable
expect fun PlayerScreen(
    uri: String,
    title: String,
    isVideo: Boolean,
    onBack: () -> Unit
)
