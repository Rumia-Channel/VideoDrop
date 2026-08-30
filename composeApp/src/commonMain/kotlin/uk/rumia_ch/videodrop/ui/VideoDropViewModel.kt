package uk.rumia_ch.videodrop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import uk.rumia_ch.videodrop.core.AnalyzeState
import uk.rumia_ch.videodrop.core.DownloadEvent
import uk.rumia_ch.videodrop.core.DownloadRepository
import uk.rumia_ch.videodrop.core.DownloadRequest
import uk.rumia_ch.videodrop.core.FormatSelection
import uk.rumia_ch.videodrop.core.GitHubUpdateChecker
import uk.rumia_ch.videodrop.core.OutputType
import uk.rumia_ch.videodrop.core.RuntimeStatus
import uk.rumia_ch.videodrop.core.UpdateConfig
import uk.rumia_ch.videodrop.core.UpdateState
import uk.rumia_ch.videodrop.core.YtDlpError
import uk.rumia_ch.videodrop.core.getAppVersion
import kotlin.random.Random

class VideoDropViewModel(
    private val repository: DownloadRepository,
    private val updateChecker: GitHubUpdateChecker = GitHubUpdateChecker()
) : ViewModel() {

    private val _analyzeState = MutableStateFlow<AnalyzeState>(AnalyzeState.Idle)
    val analyzeState: StateFlow<AnalyzeState> = _analyzeState.asStateFlow()

    private val _downloadEvents = MutableStateFlow<Map<String, DownloadEvent>>(emptyMap())
    val downloadEvents: StateFlow<Map<String, DownloadEvent>> = _downloadEvents.asStateFlow()

    private val _runtimeStatus = MutableStateFlow<RuntimeStatus?>(null)
    val runtimeStatus: StateFlow<RuntimeStatus?> = _runtimeStatus.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _repoOwner = MutableStateFlow(UpdateConfig.REPO_OWNER)
    val repoOwner: StateFlow<String> = _repoOwner.asStateFlow()
    private val _repoName = MutableStateFlow(UpdateConfig.REPO_NAME)
    val repoName: StateFlow<String> = _repoName.asStateFlow()

    private var currentDownloadJob: Job? = null
    private var currentDownloadId: String? = null

    fun analyze(url: String) {
        if (url.isBlank()) {
            _analyzeState.value = AnalyzeState.Error(YtDlpError.InvalidUrl)
            return
        }
        viewModelScope.launch {
            _analyzeState.value = AnalyzeState.Loading
            val result = repository.extract(url.trim())
            _analyzeState.value = result.fold(
                onSuccess = { AnalyzeState.Success(it) },
                onFailure = { e ->
                    val err = (e as? uk.rumia_ch.videodrop.core.YtDlpException)?.error
                        ?: YtDlpError.Unknown(e.message)
                    AnalyzeState.Error(err)
                }
            )
        }
    }

    fun resetAnalyze() {
        _analyzeState.value = AnalyzeState.Idle
    }

    fun download(url: String, selection: FormatSelection, output: OutputType) {
        val id = generateDownloadId(url)
        currentDownloadId = id
        val request = DownloadRequest(id, url, selection, output)
        currentDownloadJob?.cancel()
        currentDownloadJob = viewModelScope.launch {
            repository.download(request)
                .catch { e ->
                    val err = (e as? uk.rumia_ch.videodrop.core.YtDlpException)?.error
                        ?: YtDlpError.Unknown(e.message)
                    _downloadEvents.value = _downloadEvents.value + (id to DownloadEvent.Failed(id, err))
                }
                .collect { event ->
                    _downloadEvents.value = _downloadEvents.value + (id to event)
                }
        }
    }

    fun cancelCurrent() {
        val id = currentDownloadId ?: return
        viewModelScope.launch {
            repository.cancel(id)
            _downloadEvents.value = _downloadEvents.value + (id to DownloadEvent.Cancelled(id))
        }
        currentDownloadJob?.cancel()
    }

    fun refreshRuntime() {
        viewModelScope.launch {
            _runtimeStatus.value = repository.runtimeStatus()
        }
    }

    fun setRepo(owner: String, name: String) {
        _repoOwner.value = owner.trim()
        _repoName.value = name.trim()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            val current = try { getAppVersion() } catch (_: Exception) { "1.0.0" }
            val owner = _repoOwner.value.ifBlank { UpdateConfig.REPO_OWNER }
            val name = _repoName.value.ifBlank { UpdateConfig.REPO_NAME }
            val result = updateChecker.check(current, owner, name)
            _updateState.value = result.fold(
                onSuccess = { info ->
                    if (info.isUpdateAvailable) UpdateState.Available(info) else UpdateState.UpToDate
                },
                onFailure = { e ->
                    UpdateState.Error(e.message ?: "Unknown error: ${e::class.simpleName}")
                }
            )
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    private fun generateDownloadId(url: String): String {
        return "${Random.nextLong()}-${url.hashCode().toString(16)}"
    }
}
