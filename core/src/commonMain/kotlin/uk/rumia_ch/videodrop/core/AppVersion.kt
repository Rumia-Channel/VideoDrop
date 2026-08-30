package uk.rumia_ch.videodrop.core

/**
 * Returns current app versionName, e.g. "1.0.0"
 * Android: PackageManager, JVM: jar manifest, iOS: Bundle
 */
expect fun getAppVersion(): String
