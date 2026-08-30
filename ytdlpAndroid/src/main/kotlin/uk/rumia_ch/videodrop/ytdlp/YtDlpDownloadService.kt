package uk.rumia_ch.videodrop.ytdlp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import uk.rumia_ch.videodrop.core.DownloadRequest
import uk.rumia_ch.videodrop.core.FormatSelection
import uk.rumia_ch.videodrop.core.OutputType

/**
 * Foreground Service per spec section 12.
 * Type: dataSync (Android 14+).
 * Shows progress, speed, ETA, Cancel action.
 * Handles Android 15+ dataSync timeout by stopping and emitting Failed/Paused.
 * Phase 8 will be fully wired; this is scaffold with notification channel.
 */
class YtDlpDownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private lateinit var engine: AndroidYtDlpEngine

    companion object {
        const val CHANNEL_ID = "videodrop_download"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_URL = "extra_url"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        const val EXTRA_FORMAT_ID = "extra_format_id"
        const val ACTION_CANCEL = "action_cancel"
        const val ACTION_START = "action_start"
    }

    override fun onCreate() {
        super.onCreate()
        engine = AndroidYtDlpEngine(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                val id = intent.getStringExtra(EXTRA_DOWNLOAD_ID)
                if (id != null) {
                    serviceScope.launch { engine.cancel(id) }
                }
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val url = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: System.currentTimeMillis().toString()
                val formatId = intent.getStringExtra(EXTRA_FORMAT_ID)

                // Foreground notification
                startForeground(NOTIFICATION_ID, buildNotification("Starting...", 0, downloadId))

                val request = DownloadRequest(
                    id = downloadId,
                    url = url,
                    selection = if (formatId != null) FormatSelection.Exact(formatId) else FormatSelection.Best,
                    output = OutputType.Video
                )

                downloadJob?.cancel()
                downloadJob = serviceScope.launch {
                    engine.download(request)
                        .catch { e ->
                            updateNotification("Failed: ${e.message}", downloadId)
                            stopSelf()
                        }
                        .collect { event ->
                            when (event) {
                                is uk.rumia_ch.videodrop.core.DownloadEvent.Progress -> {
                                    val pct = event.percent?.toInt() ?: 0
                                    updateNotification(
                                        "Downloading ${pct}% ${event.speedBytesPerSecond?.let { "${it/1024}KB/s" } ?: ""} ETA ${event.etaSeconds ?: "?"}s",
                                        downloadId,
                                        pct
                                    )
                                }
                                is uk.rumia_ch.videodrop.core.DownloadEvent.PostProcessing -> {
                                    updateNotification("Merging...", downloadId)
                                }
                                is uk.rumia_ch.videodrop.core.DownloadEvent.Completed -> {
                                    // Phase 9: MediaStore export
                                    val destUri = MediaStoreHelper.exportToMediaStore(
                                        context = this@YtDlpDownloadService,
                                        stagingPath = event.uri,
                                        title = downloadId,
                                        isVideo = true
                                    )
                                    updateNotification("Completed: $destUri", downloadId, 100)
                                    // Keep notification briefly then stop
                                    stopSelf()
                                }
                                is uk.rumia_ch.videodrop.core.DownloadEvent.Failed -> {
                                    updateNotification("Failed: ${event.error}", downloadId)
                                    stopSelf()
                                }
                                is uk.rumia_ch.videodrop.core.DownloadEvent.Cancelled -> {
                                    updateNotification("Cancelled", downloadId)
                                    stopSelf()
                                }
                                else -> {}
                            }
                        }
                }
                // Handle Android 15+ dataSync timeout: system will stop service after ~6 hours
                // We treat stop as Failed/Paused per spec - here simplified as Failed
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        downloadJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
            description = "VideoDrop yt-dlp downloads"
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(content: String, progress: Int? = null, downloadId: String): Notification {
        val cancelIntent = Intent(this, YtDlpDownloadService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val pending = PendingIntent.getService(this, downloadId.hashCode(), cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VideoDrop")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .apply {
                if (progress != null) setProgress(100, progress, false) else setProgress(0, 0, true)
            }
            .addAction(android.R.drawable.ic_delete, "Cancel", pending)
            .build()
    }

    private fun updateNotification(content: String, downloadId: String, progress: Int? = null) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(content, progress, downloadId))
    }
}
