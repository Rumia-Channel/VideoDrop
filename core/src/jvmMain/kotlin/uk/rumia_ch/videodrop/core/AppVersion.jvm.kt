package uk.rumia_ch.videodrop.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter

actual fun getAppVersion(): String {
    return try {
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    } catch (_: Exception) {
        "1.0.0"
    }
}
