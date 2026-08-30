package uk.rumia_ch.videodrop.core

import platform.Foundation.NSBundle

actual fun getAppVersion(): String {
    return (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String) ?: "1.0.0"
}
