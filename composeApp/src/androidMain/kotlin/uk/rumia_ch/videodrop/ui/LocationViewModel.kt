package uk.rumia_ch.videodrop.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.rumia_ch.videodrop.ytdlp.DownloadLocationRepository

class LocationViewModel(context: Context) : ViewModel() {

    private val repo = DownloadLocationRepository(context.applicationContext)

    val rootFlow: StateFlow<DownloadLocationRepository.DownloadRoot> = repo.rootFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        DownloadLocationRepository.DownloadRoot("internal", null, "内部ストレージ (cacheDir)")
    )

    private val _folders = MutableStateFlow<List<DownloadLocationRepository.FolderItem>>(emptyList())
    val folders: StateFlow<List<DownloadLocationRepository.FolderItem>> = _folders

    private val _files = MutableStateFlow<List<DownloadLocationRepository.FileItem>>(emptyList())
    val files: StateFlow<List<DownloadLocationRepository.FileItem>> = _files

    var currentFolderUriOrPath: String? = null
        private set

    fun refresh() {
        viewModelScope.launch {
            val root = repo.rootFlow.first()
            _folders.value = repo.listFoldersSync(root)
            _files.value = repo.listFilesInFolder(root, currentFolderUriOrPath)
        }
    }

    fun setRootInternal() {
        viewModelScope.launch {
            repo.setRootInternal()
            currentFolderUriOrPath = null
            refresh()
        }
    }

    fun setRootSaf(uri: Uri, displayName: String) {
        viewModelScope.launch {
            repo.setRootSaf(uri, displayName)
            currentFolderUriOrPath = null
            refresh()
        }
    }

    fun openFolder(uriOrPath: String) {
        currentFolderUriOrPath = uriOrPath
        viewModelScope.launch {
            val root = repo.rootFlow.first()
            _files.value = repo.listFilesInFolder(root, uriOrPath)
        }
    }

    fun backToRoot() {
        currentFolderUriOrPath = null
        viewModelScope.launch {
            val root = repo.rootFlow.first()
            _files.value = repo.listFilesInFolder(root, null)
        }
    }

    fun createFolder(name: String, onResult: (Result<String>) -> Unit = {}) {
        viewModelScope.launch {
            val root = repo.rootFlow.first()
            val res = repo.createFolder(root, name)
            onResult(res)
            if (res.isSuccess) {
                refresh()
            }
        }
    }
}
