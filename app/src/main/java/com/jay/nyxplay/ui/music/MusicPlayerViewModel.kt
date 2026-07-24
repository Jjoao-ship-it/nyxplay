package com.jay.nyxplay.ui.music

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jay.nyxplay.data.MediaEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val queue: List<MediaEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val albumArt: Bitmap? = null,
    val dominantColor: Color = Color(0xFF232330),
    val isActive: Boolean = false
) {
    val currentSong: MediaEntity? get() = queue.getOrNull(currentIndex)
}

/**
 * Dono único do ExoPlayer de música — vive no ViewModel, não num
 * Composable, precisamente para sobreviver a navegação (voltar à
 * biblioteca) e troca de separador (Vídeos <-> Música), como um
 * leitor de música real faz.
 */
class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val exoPlayer = ExoPlayer.Builder(application).build()
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) next()
            }
        })

        viewModelScope.launch {
            while (true) {
                _state.update {
                    it.copy(
                        positionMs = exoPlayer.currentPosition,
                        durationMs = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
                delay(500)
            }
        }
    }

    fun playQueue(queue: List<MediaEntity>, startIndex: Int) {
        _state.update { it.copy(queue = queue, currentIndex = startIndex, isActive = true) }
        loadCurrent()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun next() {
        val s = _state.value
        if (s.currentIndex < s.queue.lastIndex) {
            _state.update { it.copy(currentIndex = it.currentIndex + 1) }
            loadCurrent()
        }
    }

    fun previous() {
        val s = _state.value
        if (s.currentIndex > 0) {
            _state.update { it.copy(currentIndex = it.currentIndex - 1) }
            loadCurrent()
        }
    }

    fun seekTo(fraction: Float) {
        val target = (fraction * _state.value.durationMs).toLong()
        exoPlayer.seekTo(target)
    }

    private fun loadCurrent() {
        val audio = _state.value.currentSong ?: return
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(audio.uri)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        viewModelScope.launch {
            val art = loadAlbumArt(getApplication(), audio.uri)
            _state.update { it.copy(albumArt = art.bitmap, dominantColor = art.dominantColor) }
        }
    }

    override fun onCleared() {
        exoPlayer.release()
        super.onCleared()
    }
}
