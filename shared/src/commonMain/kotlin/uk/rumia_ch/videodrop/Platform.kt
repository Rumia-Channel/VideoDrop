package uk.rumia_ch.videodrop

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform