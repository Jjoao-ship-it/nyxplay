package com.jay.nyxplay.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaScanner
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.data.NyxDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NyxDatabase.getInstance(application)
    private val scanner = MediaScanner(application)

    val videos: StateFlow<List<MediaEntity>> = db.mediaDao().observeByType(MediaType.VIDEO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audios: StateFlow<List<MediaEntity>> = db.mediaDao().observeByType(MediaType.AUDIO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Scan de vídeo — disparado quando a permissão de vídeo é concedida. */
    fun scanVideos() {
        viewModelScope.launch { scanner.scanVideos(db) }
    }

    /** Scan de áudio — disparado quando a permissão de áudio é concedida. */
    fun scanAudio() {
        viewModelScope.launch { scanner.scanAudio(db) }
    }
}
