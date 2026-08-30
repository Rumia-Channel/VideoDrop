package uk.rumia_ch.videodrop.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Desktop stub per spec section 23. Real DesktopYtDlpEngine will use ProcessBuilder.
 */
class NoOpYtDlpEngine : YtDlpEngine {
    override suspend fun extract(url: String): Result<MediaInfo> =
        Result.failure(NotImplementedError("NoOp engine - desktop not yet implemented"))

    override fun download(request: DownloadRequest): Flow<DownloadEvent> = flow {
        emit(DownloadEvent.Failed(request.id, YtDlpError.Unknown("Desktop download not implemented")))
    }

    override suspend fun cancel(downloadId: String) {}

    override suspend fun runtimeStatus(): RuntimeStatus = RuntimeStatus(
        python = ComponentStatus.Unavailable("Desktop stub - no Chaquopy"),
        ytDlpVersion = null,
        ytDlpEjsVersion = null,
        quickJsVersion = null,
        ffmpegVersion = null,
        ffprobeVersion = null,
        nativeAbi = "jvm"
    )
}
