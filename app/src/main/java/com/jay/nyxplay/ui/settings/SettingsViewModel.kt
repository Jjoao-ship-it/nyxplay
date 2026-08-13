package com.jay.nyxplay.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jay.nyxplay.data.PlayerSkin
import com.jay.nyxplay.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    // Gerais
    val accentColorHex: StateFlow<Long> = repository.accentColorHex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF7C5CFF)
    val rememberLastSection: StateFlow<Boolean> = repository.rememberLastSection
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Individuais — vídeo
    val excludedVideoBuckets: StateFlow<Set<String>> = repository.excludedVideoBuckets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Individuais — áudio
    val excludedAudioBuckets: StateFlow<Set<String>> = repository.excludedAudioBuckets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val playerSkin: StateFlow<PlayerSkin> = repository.playerSkin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerSkin.CARD)
    val pauseOnVolumeZero: StateFlow<Boolean> = repository.pauseOnVolumeZero
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val pauseOnBluetoothDisconnect: StateFlow<Boolean> = repository.pauseOnBluetoothDisconnect
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val historyEnabled: StateFlow<Boolean> = repository.historyEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setAccentColor(hex: Long) = viewModelScope.launch { repository.setAccentColor(hex) }
    fun setRememberLastSection(value: Boolean) = viewModelScope.launch { repository.setRememberLastSection(value) }
    fun setLastSection(section: String) = viewModelScope.launch { repository.setLastSection(section) }
    fun setVideoBucketExcluded(bucket: String, excluded: Boolean) = viewModelScope.launch { repository.setVideoBucketExcluded(bucket, excluded) }
    fun setAudioBucketExcluded(bucket: String, excluded: Boolean) = viewModelScope.launch { repository.setAudioBucketExcluded(bucket, excluded) }
    fun setPlayerSkin(skin: PlayerSkin) = viewModelScope.launch { repository.setPlayerSkin(skin) }
    fun setPauseOnVolumeZero(value: Boolean) = viewModelScope.launch { repository.setPauseOnVolumeZero(value) }
    fun setPauseOnBluetoothDisconnect(value: Boolean) = viewModelScope.launch { repository.setPauseOnBluetoothDisconnect(value) }
    fun setHistoryEnabled(value: Boolean) = viewModelScope.launch { repository.setHistoryEnabled(value) }

    suspend fun readLastSection(): String = repository.lastSection.first()
}
