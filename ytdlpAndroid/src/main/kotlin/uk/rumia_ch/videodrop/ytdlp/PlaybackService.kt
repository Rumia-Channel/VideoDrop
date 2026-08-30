package uk.rumia_ch.videodrop.ytdlp

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * MediaSessionService for background playback and playlist.
 * Supports video/audio, playlist, notification controls.
 * ForegroundServiceType mediaPlayback (Android 14+).
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true // handle audio focus
            )
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                shuffleModeEnabled = false
            }

        mediaSession = MediaSession.Builder(this, player)
            .setId("VideoDropPlayback")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playing in background if audio, pause video? For now keep
        // Player will continue if not released
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        fun buildMediaItem(uri: String, title: String, isVideo: Boolean): MediaItem {
            return MediaItem.Builder()
                .setUri(uri)
                .setMediaId(uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(title)
                        .setMediaType(
                            if (isVideo) androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO
                            else androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
                        )
                        .build()
                )
                .build()
        }
    }
}
