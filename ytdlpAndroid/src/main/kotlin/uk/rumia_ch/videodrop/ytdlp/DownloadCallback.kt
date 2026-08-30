package uk.rumia_ch.videodrop.ytdlp

/**
 * Bridge object passed to Python's download() as `callback`.
 * Python will call on_progress(json) and should_cancel().
 * Must be public for Chaquopy to expose to Python via PyObject.
 */
class DownloadCallback(
    private val downloadId: String,
    private val onProgress: (String) -> Unit,
    private val isCancelled: () -> Boolean
) {
    @Suppress("unused")
    fun on_progress(jsonStr: String) {
        onProgress(jsonStr)
    }

    @Suppress("unused")
    fun should_cancel(): Boolean = isCancelled()
}
