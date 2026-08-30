package uk.rumia_ch.videodrop.core

actual fun getAppVersion(): String {
    // Try holder first (set by MainActivity), then fallback to date version
    AppVersionHolder.versionName?.let { return it }
    AppVersionHolder.appContext?.let { ctx ->
        try {
            val pm = ctx.packageManager
            val info = pm.getPackageInfo(ctx.packageName, 0)
            return info.versionName ?: "1.0.0"
        } catch (_: Exception) {}
    }
    return "1.0.0"
}
