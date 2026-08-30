package uk.rumia_ch.videodrop.core

import kotlinx.serialization.Serializable

/**
 * GitHub Releases based update info.
 * Repo is looked up via GitHub API: /repos/{owner}/{repo}/releases/latest
 */
@Serializable
data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val isUpdateAvailable: Boolean,
    val releaseName: String? = null,
    val releaseNotes: String? = null,
    val htmlUrl: String? = null,
    val publishedAt: String? = null,
    val downloadUrl: String? = null // first apk asset if present
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data object UpToDate : UpdateState
    data class Error(val message: String) : UpdateState
}

object UpdateConfig {
    // GitHub: https://github.com/Rumia-Channel/VideoDrop
    const val REPO_OWNER = "Rumia-Channel"
    const val REPO_NAME = "VideoDrop"
    const val GITHUB_API_BASE = "https://api.github.com"
}
