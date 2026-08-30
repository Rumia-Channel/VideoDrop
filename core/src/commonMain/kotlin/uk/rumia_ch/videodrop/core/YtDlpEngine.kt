package uk.rumia_ch.videodrop.core

import kotlinx.coroutines.flow.Flow

interface YtDlpEngine {
    suspend fun extract(url: String): Result<MediaInfo>
    fun download(request: DownloadRequest): Flow<DownloadEvent>
    suspend fun cancel(downloadId: String)
    suspend fun runtimeStatus(): RuntimeStatus
}
