package uk.rumia_ch.videodrop.core

import kotlinx.coroutines.flow.Flow

/**
 * Default implementation of DownloadRepository that delegates to YtDlpEngine.
 * Keeps ViewModel decoupled from engine per spec section 19.
 */
class DefaultDownloadRepository(
    private val engine: YtDlpEngine
) : DownloadRepository {
    override suspend fun extract(url: String): Result<MediaInfo> = engine.extract(url)
    override fun download(request: DownloadRequest): Flow<DownloadEvent> = engine.download(request)
    override suspend fun cancel(downloadId: String) = engine.cancel(downloadId)
    override suspend fun runtimeStatus(): RuntimeStatus = engine.runtimeStatus()
}
