package uk.rumia_ch.videodrop.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
private data class GitHubRelease(
    val tag_name: String,
    val name: String? = null,
    val body: String? = null,
    val html_url: String? = null,
    val published_at: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
private data class GitHubAsset(
    val name: String,
    val browser_download_url: String
)

class GitHubUpdateChecker(
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    },
    private val apiBase: String = UpdateConfig.GITHUB_API_BASE
) {

    suspend fun check(
        currentVersion: String,
        repoOwner: String = UpdateConfig.REPO_OWNER,
        repoName: String = UpdateConfig.REPO_NAME
    ): Result<UpdateInfo> {
        return try {
            // If repo not configured, return error
            if (repoOwner.isBlank() || repoName.isBlank()) {
                return Result.failure(IllegalStateException("GitHub repo not configured (owner/repo empty)"))
            }
            val url = "$apiBase/repos/$repoOwner/$repoName/releases/latest"
            val release: GitHubRelease = httpClient.get(url) {
                header("Accept", "application/vnd.github.v3+json")
                header("User-Agent", "VideoDrop/${currentVersion}")
            }.body()

            val latest = release.tag_name.trim().removePrefix("v")
            val current = currentVersion.trim().removePrefix("v")
            val isUpdate = isNewer(latest, current)

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }?.browser_download_url

            Result.success(
                UpdateInfo(
                    currentVersion = currentVersion,
                    latestVersion = release.tag_name,
                    isUpdateAvailable = isUpdate,
                    releaseName = release.name,
                    releaseNotes = release.body,
                    htmlUrl = release.html_url,
                    publishedAt = release.published_at,
                    downloadUrl = apkAsset ?: release.html_url
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Simple semver compare: "1.0.1" vs "1.0.0" or "1.10" vs "1.9"
     * Non-numeric parts are compared lexicographically as fallback.
     */
    fun isNewer(latest: String, current: String): Boolean {
        if (latest == current) return false
        val latestParts = latest.split(".", "-", "_")
        val currentParts = current.split(".", "-", "_")
        val max = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until max) {
            val l = latestParts.getOrNull(i) ?: "0"
            val c = currentParts.getOrNull(i) ?: "0"
            val lNum = l.toIntOrNull()
            val cNum = c.toIntOrNull()
            if (lNum != null && cNum != null) {
                if (lNum != cNum) return lNum > cNum
            } else {
                if (l != c) return l > c
            }
        }
        return false
    }
}
