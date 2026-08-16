package com.jay.nyxplay.ui.music

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.NyxDatabase
import com.jay.nyxplay.data.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val exoPlayer = ExoPlayer.Builder(application).build()
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val settingsRepository = SettingsRepository(application)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var pausedByVolumeZero = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                viewModelScope.launch {
                    if (settingsRepository.pauseOnBluetoothDisconnect.first()) {
                        exoPlayer.pause()
                    }
                }
            }
        }
    }

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            viewModelScope.launch {
                if (!settingsRepository.pauseOnVolumeZero.first()) return@launch
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVolume <= 0) {
                    if (exoPlayer.isPlaying) {
                        pausedByVolumeZero = true
                        exoPlayer.pause()
                    }
                } else if (pausedByVolumeZero) {
                    pausedByVolumeZero = false
                    exoPlayer.play()
                }
            }
        }
    }

    private var tickCount = 0

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) next()
            }
        })

        ContextCompat.registerReceiver(
            application,
            becomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        application.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, volumeObserver
        )

        viewModelScope.launch {
            while (true) {
                _state.update {
                    it.copy(
                        positionMs = exoPlayer.currentPosition,
                        durationMs = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
                tickCount++
                if (tickCount % 10 == 0 && _state.value.isActive) {
                    saveQueueState()
                }
                delay(500)
            }
        }
    }

    private fun saveQueueState() {
        val s = _state.value
        viewModelScope.launch {
            settingsRepository.saveLastQueue(s.queue.map { it.uid }, s.currentIndex, exoPlayer.currentPosition)
        }
    }

    /**
     * Restaura a última fila guardada (se existir) a partir da
     * biblioteca já carregada — sem começar a tocar sozinho, só
     * prepara o player na posição onde ficaste.
     */
    fun restoreLastQueueIfAny(fullLibrary: List<MediaEntity>) {
        if (_state.value.isActive || fullLibrary.isEmpty()) return
        viewModelScope.launch {
            val uids = settingsRepository.lastQueueUids.first()
            if (uids.isEmpty()) return@launch
            val restoredQueue = uids.mapNotNull { uid -> fullLibrary.firstOrNull { it.uid == uid } }
            if (restoredQueue.isEmpty()) return@launch
            val index = settingsRepository.lastQueueIndex.first().coerceIn(0, restoredQueue.lastIndex)
            val positionMs = settingsRepository.lastPositionMs.first()

            _state.update { it.copy(queue = restoredQueue, currentIndex = index, isActive = true) }
            val audio = restoredQueue[index]
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(audio.uri)))
            exoPlayer.prepare()
            exoPlayer.seekTo(positionMs)
            exoPlayer.playWhenReady = false

            val art = loadAlbumArt(getApplication(), audio.uri)
            _state.update { it.copy(albumArt = art.bitmap, dominantColor = art.dominantColor) }
        }
    }

    fun playQueue(queue: List<MediaEntity>, startIndex: Int) {
        _state.update { it.copy(queue = queue, currentIndex = startIndex, isActive = true) }
        loadCurrent()
    }

    fun playAtIndex(index: Int) {
        if (index !in _state.value.queue.indices) return
        _state.update { it.copy(currentIndex = index) }
        loadCurrent()
    }

    fun removeFromQueue(uid: String) {
        val s = _state.value
        val removingCurrent = s.queue.getOrNull(s.currentIndex)?.uid == uid
        val newQueue = s.queue.filterNot { it.uid == uid }
        val newIndex = when {
            newQueue.isEmpty() -> 0
            removingCurrent -> s.currentIndex.coerceIn(0, newQueue.lastIndex)
            else -> newQueue.indexOfFirst { it.uid == s.currentSong?.uid }.coerceAtLeast(0)
        }
        _state.update { it.copy(queue = newQueue, currentIndex = newIndex) }
        if (removingCurrent && newQueue.isNotEmpty()) loadCurrent()
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
            if (settingsRepository.historyEnabled.first()) {
                NyxDatabase.getInstance(getApplication()).mediaDao().registerPlay(audio.uid, System.currentTimeMillis())
            }
        }

        viewModelScope.launch {
            val art = loadAlbumArt(getApplication(), audio.uri)
            _state.update { it.copy(albumArt = art.bitmap, dominantColor = art.dominantColor) }
        }
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(becomingNoisyReceiver)
        getApplication<Application>().contentResolver.unregisterContentObserver(volumeObserver)
        exoPlayer.release()
        super.onCleared()
    }
}
