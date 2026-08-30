package uk.rumia_ch.videodrop.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.doubleOrNull

object YtDlpJsonParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseMediaInfo(jsonStr: String): Result<MediaInfo> {
        return try {
            val element = json.parseToJsonElement(jsonStr)
            if (element is JsonObject && element.containsKey("error")) {
                val err = element["error"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                val msg = element["message"]?.jsonPrimitive?.contentOrNull
                return Result.failure(mapError(err, msg))
            }
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return Result.failure(IllegalArgumentException("missing id"))
            val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
            val uploader = obj["uploader"]?.jsonPrimitive?.contentOrNull
                ?: obj["channel"]?.jsonPrimitive?.contentOrNull
            val duration = obj["duration"]?.jsonPrimitive?.longOrNull
                ?: obj["duration"]?.jsonPrimitive?.doubleOrNull?.toLong()
            val thumbnail = obj["thumbnail"]?.jsonPrimitive?.contentOrNull
                ?: (obj["thumbnails"] as? JsonArray)?.lastOrNull()?.let { (it as? JsonObject)?.get("url")?.jsonPrimitive?.contentOrNull }

            val formatsArray = obj["formats"] as? JsonArray ?: JsonArray(emptyList())
            val formats = formatsArray.mapNotNull { el ->
                try {
                    val f = el.jsonObject
                    val formatId = f["format_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val ext = f["ext"]?.jsonPrimitive?.contentOrNull
                    val width = f["width"]?.jsonPrimitive?.intOrNull
                    val height = f["height"]?.jsonPrimitive?.intOrNull
                    val fps = f["fps"]?.jsonPrimitive?.doubleOrNull
                    val vcodec = f["vcodec"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "none" }
                    val acodec = f["acodec"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "none" }
                    val bitrate = f["tbr"]?.jsonPrimitive?.doubleOrNull
                        ?: f["bitrate"]?.jsonPrimitive?.doubleOrNull
                    val fileSize = f["filesize"]?.jsonPrimitive?.longOrNull
                        ?: f["filesize_approx"]?.jsonPrimitive?.longOrNull
                    val hasVideo = vcodec != null && vcodec != "none"
                    val hasAudio = acodec != null && acodec != "none"
                    MediaFormat(formatId, ext, width, height, fps, vcodec, acodec, bitrate, fileSize, hasVideo, hasAudio)
                } catch (_: Exception) {
                    null
                }
            }

            Result.success(MediaInfo(id, title, uploader, duration, thumbnail, formats))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapError(err: String, msg: String?): Throwable {
        val error: YtDlpError = when (err) {
            "InvalidUrl" -> YtDlpError.InvalidUrl
            "UnsupportedUrl" -> YtDlpError.UnsupportedUrl
            "PrivateVideo" -> YtDlpError.PrivateVideo
            "LoginRequired" -> YtDlpError.LoginRequired
            "VideoUnavailable" -> YtDlpError.VideoUnavailable
            "GeoRestricted" -> YtDlpError.GeoRestricted
            "FormatUnavailable" -> YtDlpError.FormatUnavailable
            "PoTokenRequired" -> YtDlpError.PoTokenRequired
            "JavaScriptRuntimeError" -> YtDlpError.JavaScriptRuntimeError
            "QuickJsUnavailable" -> YtDlpError.QuickJsUnavailable
            "FfmpegUnavailable" -> YtDlpError.FfmpegUnavailable
            "StorageError" -> YtDlpError.StorageError
            "Cancelled" -> YtDlpError.Cancelled
            else -> YtDlpError.Unknown(msg ?: err)
        }
        return YtDlpException(error, msg)
    }
}

class YtDlpException(val error: YtDlpError, message: String?) : Exception(message ?: error.toString())
