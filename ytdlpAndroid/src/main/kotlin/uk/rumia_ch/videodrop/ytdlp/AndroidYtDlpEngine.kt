package uk.rumia_ch.videodrop.ytdlp

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import uk.rumia_ch.videodrop.core.ComponentStatus
import uk.rumia_ch.videodrop.core.DownloadEvent
import uk.rumia_ch.videodrop.core.DownloadRequest
import uk.rumia_ch.videodrop.core.MediaInfo
import uk.rumia_ch.videodrop.core.RuntimeStatus
import uk.rumia_ch.videodrop.core.YtDlpEngine
import uk.rumia_ch.videodrop.core.YtDlpException
import uk.rumia_ch.videodrop.core.YtDlpJsonParser
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class AndroidYtDlpEngine(
    private val context: Context
) : YtDlpEngine {

    private val downloadMutex = Mutex()
    private val cancelledIds = ConcurrentHashMap.newKeySet<String>()
    private val lastProgressEmit = AtomicLong(0)

    private fun ensurePythonStarted() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }

    fun resolveQuickJsPath(): String =
        File(context.applicationInfo.nativeLibraryDir, "libqjsexec.so").absolutePath

    fun resolveFfmpegPath(): String =
        File(context.applicationInfo.nativeLibraryDir, "libffmpegexec.so").absolutePath

    fun resolveFfprobePath(): String =
        File(context.applicationInfo.nativeLibraryDir, "libffprobeexec.so").absolutePath

    fun resolveFfmpegDir(): String = context.applicationInfo.nativeLibraryDir

    private fun stagingDir(downloadId: String): File {
        // per spec 16: cacheDir or externalFilesDir staging
        val base = context.cacheDir ?: context.filesDir
        return File(base, "downloads/$downloadId").apply { mkdirs() }
    }

    override suspend fun extract(url: String): Result<MediaInfo> = withContext(Dispatchers.IO) {
        try {
            ensurePythonStarted()
            val py = Python.getInstance()
            val mod = py.getModule("ytdlp_bridge")
            // initialize native paths before extract
            try {
                mod.callAttr("initialize", resolveQuickJsPath(), resolveFfmpegDir())
            } catch (_: Exception) {}
            val jsonStr = mod.callAttr("extract_info", url).toString()
            // jsonStr is either media info dict JSON or error JSON with "error" key
            YtDlpJsonParser.parseMediaInfo(jsonStr)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun download(request: DownloadRequest): Flow<DownloadEvent> = callbackFlow {
        // Enforce single concurrent download per spec 13
        // We use trySend for events; callbackFlow ensures not on Main thread
        val id = request.id
        // If already cancelled before start, emit Cancelled
        if (cancelledIds.contains(id)) {
            trySend(DownloadEvent.Cancelled(id))
            close()
            return@callbackFlow
        }

        // Launch IO work; mutex ensures maxConcurrent=1
        // Note: callbackFlow builder itself runs in caller's context; we need to offload
        // The actual Python download blocks, so we run it in Dispatchers.IO within this flow
        // Use withLock to serialize
        val job = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launchWithMutex {
            try {
                trySend(DownloadEvent.Started(id))

                ensurePythonStarted()
                val py = Python.getInstance()
                val mod = py.getModule("ytdlp_bridge")
                try {
                    mod.callAttr("initialize", resolveQuickJsPath(), resolveFfmpegDir())
                } catch (_: Exception) {}

                val staging = stagingDir(id)
                // per spec, use outtmpl inside staging dir
                val outTmpl = File(staging, "%(title).200B-%(id)s.%(ext)s").absolutePath

                val formatId = when (val sel = request.selection) {
                    is uk.rumia_ch.videodrop.core.FormatSelection.Best -> null
                    is uk.rumia_ch.videodrop.core.FormatSelection.Exact -> sel.formatId
                }
                val outputTypeStr = request.output.name // Video/Audio

                // Callback for progress_hook; throttle to 100ms per spec 14
                val callback = DownloadCallback(
                    downloadId = id,
                    onProgress = { jsonStr ->
                        // Throttle: only emit if 100ms passed or is terminal event
                        val now = System.currentTimeMillis()
                        val last = lastProgressEmit.get()
                        val isTerminal = jsonStr.contains("\"_type\": \"error\"") ||
                            jsonStr.contains("\"_type\": \"completed\"") ||
                            jsonStr.contains("\"_type\": \"cancelled\"") ||
                            jsonStr.contains("\"_type\": \"postprocessing\"")
                        val shouldEmit = isTerminal || (now - last) >= 100
                        if (!shouldEmit) return@DownloadCallback
                        lastProgressEmit.set(now)

                        try {
                            val el = Json.parseToJsonElement(jsonStr)
                            if (el is JsonObject) {
                                val type = el["_type"]?.jsonPrimitive?.contentOrNull
                                when (type) {
                                    "progress" -> {
                                        val status = el["status"]?.jsonPrimitive?.contentOrNull
                                        if (status == "downloading") {
                                            val downloaded = el["downloaded_bytes"]?.jsonPrimitive?.longOrNull
                                            val total = el["total_bytes"]?.jsonPrimitive?.longOrNull
                                            val speed = el["speed"]?.jsonPrimitive?.doubleOrNull?.toLong()
                                            val eta = el["eta"]?.jsonPrimitive?.longOrNull
                                            val percent = el["percent"]?.jsonPrimitive?.doubleOrNull
                                                ?: if (downloaded != null && total != null && total > 0) downloaded.toDouble() / total * 100 else null
                                            trySend(
                                                DownloadEvent.Progress(id, downloaded, total, speed, eta, percent)
                                            )
                                        } else if (status == "finished") {
                                            trySend(DownloadEvent.PostProcessing(id))
                                        }
                                    }
                                    "postprocessing" -> trySend(DownloadEvent.PostProcessing(id))
                                    "started" -> { /* already sent */ }
                                    "completed" -> {
                                        // Python reports completed; Kotlin will verify file and emit Completed with uri
                                        // Don't emit yet; wait for return value handling
                                    }
                                    "cancelled" -> trySend(DownloadEvent.Cancelled(id))
                                    "error" -> {
                                        val errObj = el["error"]?.jsonObject
                                        val errStr = errObj?.get("error")?.jsonPrimitive?.contentOrNull ?: "Unknown"
                                        val msg = errObj?.get("message")?.jsonPrimitive?.contentOrNull
                                        val error = mapStringToYtDlpError(errStr, msg)
                                        trySend(DownloadEvent.Failed(id, error))
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                    },
                    isCancelled = { cancelledIds.contains(id) }
                )

                // Blocking Python call
                val resultJson = mod.callAttr(
                    "download",
                    request.url,
                    outTmpl,
                    id,
                    callback,
                    formatId,
                    outputTypeStr
                ).toString()

                // Parse result: if cancelled, already emitted; if error, emit Failed; if completed, find file
                try {
                    val resEl = Json.parseToJsonElement(resultJson)
                    if (resEl is JsonObject) {
                        if (resEl.containsKey("error")) {
                            val errStr = resEl["error"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                            val msg = resEl["message"]?.jsonPrimitive?.contentOrNull
                            val error = mapStringToYtDlpError(errStr, msg)
                            trySend(DownloadEvent.Failed(id, error))
                        } else if (resEl["status"]?.jsonPrimitive?.contentOrNull == "Cancelled") {
                            trySend(DownloadEvent.Cancelled(id))
                        } else if (resEl["status"]?.jsonPrimitive?.contentOrNull == "Completed") {
                            // Find actual file in staging dir (yt-dlp may have produced .mp4, .webm etc.)
                            val files = staging.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
                            val mainFile = files?.maxByOrNull { it.length() }
                            if (mainFile != null) {
                                // Phase 9 will move via MediaStore; for now emit staging uri
                                trySend(DownloadEvent.Completed(id, mainFile.absolutePath))
                            } else {
                                trySend(DownloadEvent.Failed(id, uk.rumia_ch.videodrop.core.YtDlpError.Unknown("No output file found in $staging")))
                            }
                        }
                    }
                } catch (e: Exception) {
                    trySend(DownloadEvent.Failed(id, uk.rumia_ch.videodrop.core.YtDlpError.Unknown(e.message)))
                }

                // Cleanup cancel flag
                cancelledIds.remove(id)
                try {
                    py.getModule("ytdlp_bridge").callAttr("clear_cancel", id)
                } catch (_: Exception) {}

            } catch (e: Exception) {
                val err = if (e is YtDlpException) e.error else uk.rumia_ch.videodrop.core.YtDlpError.Unknown(e.message)
                trySend(DownloadEvent.Failed(id, err))
            } finally {
                close()
            }
        }

        awaitClose {
            // If flow cancelled, mark as cancelled and try to interrupt Python
            cancelledIds.add(id)
            try {
                if (Python.isStarted()) {
                    Python.getInstance().getModule("ytdlp_bridge").callAttr("cancel_download", id)
                }
            } catch (_: Exception) {}
            job.cancel()
        }
    }

    private suspend fun kotlinx.coroutines.CoroutineScope.launchWithMutex(block: suspend () -> Unit): kotlinx.coroutines.Job {
        return kotlinx.coroutines.launch(Dispatchers.IO) {
            downloadMutex.withLock {
                block()
            }
        }
    }

    private fun mapStringToYtDlpError(err: String, msg: String?): uk.rumia_ch.videodrop.core.YtDlpError {
        return when (err) {
            "InvalidUrl" -> uk.rumia_ch.videodrop.core.YtDlpError.InvalidUrl
            "UnsupportedUrl" -> uk.rumia_ch.videodrop.core.YtDlpError.UnsupportedUrl
            "PrivateVideo" -> uk.rumia_ch.videodrop.core.YtDlpError.PrivateVideo
            "LoginRequired" -> uk.rumia_ch.videodrop.core.YtDlpError.LoginRequired
            "VideoUnavailable" -> uk.rumia_ch.videodrop.core.YtDlpError.VideoUnavailable
            "GeoRestricted" -> uk.rumia_ch.videodrop.core.YtDlpError.GeoRestricted
            "FormatUnavailable" -> uk.rumia_ch.videodrop.core.YtDlpError.FormatUnavailable
            "PoTokenRequired" -> uk.rumia_ch.videodrop.core.YtDlpError.PoTokenRequired
            "JavaScriptRuntimeError" -> uk.rumia_ch.videodrop.core.YtDlpError.JavaScriptRuntimeError
            "QuickJsUnavailable" -> uk.rumia_ch.videodrop.core.YtDlpError.QuickJsUnavailable
            "FfmpegUnavailable" -> uk.rumia_ch.videodrop.core.YtDlpError.FfmpegUnavailable
            "StorageError" -> uk.rumia_ch.videodrop.core.YtDlpError.StorageError
            "Cancelled" -> uk.rumia_ch.videodrop.core.YtDlpError.Cancelled
            else -> uk.rumia_ch.videodrop.core.YtDlpError.Unknown(msg ?: err)
        }
    }

    override suspend fun cancel(downloadId: String) {
        cancelledIds.add(downloadId)
        withContext(Dispatchers.IO) {
            try {
                if (Python.isStarted()) {
                    Python.getInstance().getModule("ytdlp_bridge").callAttr("cancel_download", downloadId)
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun runtimeStatus(): RuntimeStatus = withContext(Dispatchers.IO) {
        val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull()
        var pythonStatus: ComponentStatus
        var ytDlpVersion: String? = null
        var ytDlpEjsVersion: String? = null
        try {
            ensurePythonStarted()
            val py = Python.getInstance()
            val mod = py.getModule("ytdlp_bridge")
            val v = mod.callAttr("version").toString()
            pythonStatus = if (v == "python-ok") ComponentStatus.Ok else ComponentStatus.Unavailable("version=$v")
            try {
                val yt = mod.callAttr("check_ytdlp").toString()
                if (yt.contains("yt_dlp")) {
                    val regex = Regex("\"yt_dlp\"\\s*:\\s*\"([^\"]+)\"")
                    ytDlpVersion = regex.find(yt)?.groupValues?.get(1)
                }
            } catch (_: Exception) {}
            try {
                val ejs = mod.callAttr("check_ytdlp_ejs").toString()
                if (ejs.contains("yt_dlp_ejs")) {
                    val regex = Regex("\"yt_dlp_ejs\"\\s*:\\s*\"([^\"]+)\"")
                    ytDlpEjsVersion = regex.find(ejs)?.groupValues?.get(1)
                }
            } catch (_: Exception) {}
            try {
                mod.callAttr("initialize", resolveQuickJsPath(), resolveFfmpegDir())
            } catch (_: Exception) {}
        } catch (e: Exception) {
            pythonStatus = ComponentStatus.Unavailable(e.message ?: "Python start failed: ${e::class.simpleName}")
        }
        RuntimeStatus(
            python = pythonStatus,
            ytDlpVersion = ytDlpVersion,
            ytDlpEjsVersion = ytDlpEjsVersion,
            quickJsVersion = checkExecutableVersion(resolveQuickJsPath()),
            ffmpegVersion = checkExecutableVersion(resolveFfmpegPath()),
            ffprobeVersion = checkExecutableVersion(resolveFfprobePath()),
            nativeAbi = abi
        )
    }

    private fun checkExecutableVersion(path: String): String? {
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val process = ProcessBuilder(path, "--version").redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText().trim().take(200)
            process.waitFor()
            if (process.exitValue() == 0 && output.isNotEmpty()) output else "exists (no version output)"
        } catch (e: Exception) {
            try {
                val p2 = ProcessBuilder(path, "-version").redirectErrorStream(true).start()
                val out2 = p2.inputStream.bufferedReader().readText().trim().take(200)
                p2.waitFor()
                if (p2.exitValue() == 0 && out2.isNotEmpty()) out2 else "exists"
            } catch (_: Exception) {
                "exists"
            }
        }
    }
}
