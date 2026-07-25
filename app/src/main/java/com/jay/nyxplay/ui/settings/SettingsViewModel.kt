package com.jay.nyxplay.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jay.nyxplay.data.PlayerSkin
import com.jay.nyxplay.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val excludedBuckets: StateFlow<Set<String>> = repository.excludedBuckets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val playerSkin: StateFlow<PlayerSkin> = repository.playerSkin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerSkin.CARD)

    fun setBucketExcluded(bucketName: String, excluded: Boolean) {
        viewModelScope.launch { repository.setBucketExcluded(bucketName, excluded) }
    }

    fun setPlayerSkin(skin: PlayerSkin) {
        viewModelScope.launch { repository.setPlayerSkin(skin) }
    }
}
