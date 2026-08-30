package uk.rumia_ch.videodrop.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlaylistItem(
    val id: String,
    val uri: String,
    val title: String,
    val isVideo: Boolean
)

enum class RepeatMode { OFF, ONE, ALL }

class PlaylistViewModel : ViewModel() {

    private val _queue = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val queue: StateFlow<List<PlaylistItem>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow<Int>(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    val currentItem: PlaylistItem?
        get() = _queue.value.getOrNull(_currentIndex.value)

    fun add(item: PlaylistItem, playNow: Boolean = false) {
        _queue.update { it + item }
        if (playNow || _queue.value.size == 1) {
            _currentIndex.value = _queue.value.lastIndex
        }
    }

    fun addAll(items: List<PlaylistItem>, playFirst: Boolean = false) {
        if (items.isEmpty()) return
        _queue.update { it + items }
        if (playFirst) {
            _currentIndex.value = _queue.value.size - items.size
        } else if (_currentIndex.value == -1) {
            _currentIndex.value = 0
        }
    }

    fun removeAt(index: Int) {
        _queue.update { list ->
            list.toMutableList().also { it.removeAt(index) }
        }
        if (_currentIndex.value >= _queue.value.size) {
            _currentIndex.value = _queue.value.lastIndex
        }
    }

    fun clear() {
        _queue.value = emptyList()
        _currentIndex.value = -1
    }

    fun playAt(index: Int) {
        if (index in _queue.value.indices) {
            _currentIndex.value = index
        }
    }

    fun next() {
        val size = _queue.value.size
        if (size == 0) return
        when (_repeatMode.value) {
            RepeatMode.ONE -> {} // stay
            else -> {
                var next = _currentIndex.value + 1
                if (next >= size) {
                    next = if (_repeatMode.value == RepeatMode.ALL) 0 else size - 1
                }
                _currentIndex.value = next
            }
        }
    }

    fun previous() {
        val size = _queue.value.size
        if (size == 0) return
        var prev = _currentIndex.value - 1
        if (prev < 0) prev = if (_repeatMode.value == RepeatMode.ALL) size - 1 else 0
        _currentIndex.value = prev
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        if (_isShuffle.value) {
            // Simple shuffle: randomize queue but keep current at 0
            val current = currentItem
            val shuffled = _queue.value.shuffled()
            _queue.value = shuffled
            if (current != null) {
                _currentIndex.value = shuffled.indexOf(current)
            }
        }
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun isEmpty(): Boolean = _queue.value.isEmpty()
}
