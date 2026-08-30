package uk.rumia_ch.videodrop.ytdlp

import android.content.Context
import java.io.File

object NativeExecutableResolver {
    fun quickJsPath(context: Context): String =
        File(context.applicationInfo.nativeLibraryDir, "libqjsexec.so").absolutePath

    fun ffmpegPath(context: Context): String =
        File(context.applicationInfo.nativeLibraryDir, "libffmpegexec.so").absolutePath

    fun ffprobePath(context: Context): String =
        File(context.applicationInfo.nativeLibraryDir, "libffprobeexec.so").absolutePath

    fun ffmpegDir(context: Context): String =
        context.applicationInfo.nativeLibraryDir
}
