package uk.rumia_ch.videodrop.core

data class MediaInfo(
    val id: String,
    val title: String,
    val uploader: String?,
    val durationSeconds: Long?,
    val thumbnailUrl: String?,
    val formats: List<MediaFormat>
)

data class MediaFormat(
    val formatId: String,
    val extension: String?,
    val width: Int?,
    val height: Int?,
    val fps: Double?,
    val videoCodec: String?,
    val audioCodec: String?,
    val bitrate: Double?,
    val fileSize: Long?,
    val hasVideo: Boolean,
    val hasAudio: Boolean
)

data class DownloadRequest(
    val id: String,
    val url: String,
    val selection: FormatSelection,
    val output: OutputType,
    val targetFolderUri: String? = null // SAF tree child uri or File path, null = root of download destination
)

sealed interface FormatSelection {
    data object Best : FormatSelection
    data class Exact(val formatId: String) : FormatSelection
}

enum class OutputType {
    Video,
    Audio
}

sealed interface DownloadEvent {
    data class Started(val id: String) : DownloadEvent
    data class Progress(
        val id: String,
        val downloadedBytes: Long?,
        val totalBytes: Long?,
        val speedBytesPerSecond: Long?,
        val etaSeconds: Long?,
        val percent: Double?
    ) : DownloadEvent
    data class PostProcessing(val id: String) : DownloadEvent
    data class Completed(val id: String, val uri: String) : DownloadEvent
    data class Failed(val id: String, val error: YtDlpError) : DownloadEvent
    data class Cancelled(val id: String) : DownloadEvent
}

data class RuntimeStatus(
    val python: ComponentStatus,
    val ytDlpVersion: String?,
    val ytDlpEjsVersion: String?,
    val quickJsVersion: String?,
    val ffmpegVersion: String?,
    val ffprobeVersion: String?,
    val nativeAbi: String?
)

sealed interface ComponentStatus {
    data object Ok : ComponentStatus
    data class Unavailable(val reason: String) : ComponentStatus
    data object NotChecked : ComponentStatus
}

sealed interface YtDlpError {
    data object InvalidUrl : YtDlpError
    data object UnsupportedUrl : YtDlpError
    data object NetworkError : YtDlpError
    data object PrivateVideo : YtDlpError
    data object LoginRequired : YtDlpError
    data object VideoUnavailable : YtDlpError
    data object GeoRestricted : YtDlpError
    data object FormatUnavailable : YtDlpError
    data object PoTokenRequired : YtDlpError
    data object JavaScriptRuntimeError : YtDlpError
    data object QuickJsUnavailable : YtDlpError
    data object FfmpegUnavailable : YtDlpError
    data object StorageError : YtDlpError
    data object Cancelled : YtDlpError
    data class Unknown(val message: String?) : YtDlpError
}

interface PoTokenProvider {
    suspend fun status(): PoTokenStatus
}

sealed interface PoTokenStatus {
    data object NotRequired : PoTokenStatus
    data object Available : PoTokenStatus
    data object Unavailable : PoTokenStatus
}

class NoOpPoTokenProvider : PoTokenProvider {
    override suspend fun status(): PoTokenStatus = PoTokenStatus.NotRequired
}

sealed interface AnalyzeState {
    data object Idle : AnalyzeState
    data object Loading : AnalyzeState
    data class Success(val media: MediaInfo) : AnalyzeState
    data class Error(val error: YtDlpError) : AnalyzeState
}

interface DownloadRepository {
    suspend fun extract(url: String): Result<MediaInfo>
    fun download(request: DownloadRequest): kotlinx.coroutines.flow.Flow<DownloadEvent>
    suspend fun cancel(downloadId: String)
    suspend fun runtimeStatus(): RuntimeStatus
}
