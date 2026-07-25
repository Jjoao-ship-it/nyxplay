package com.jay.nyxplay.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nyxplay_settings")

enum class PlayerSkin { CARD, GRADIENT }

/**
 * Definições persistentes da app — pastas de vídeo excluídas do
 * catálogo, e estilo do player de música escolhido.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val EXCLUDED_BUCKETS = stringSetPreferencesKey("excluded_video_buckets")
        val PLAYER_SKIN = stringPreferencesKey("player_skin")
    }

    val excludedBuckets: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.EXCLUDED_BUCKETS] ?: emptySet()
    }

    val playerSkin: Flow<PlayerSkin> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.PLAYER_SKIN]) {
            "GRADIENT" -> PlayerSkin.GRADIENT
            else -> PlayerSkin.CARD
        }
    }

    suspend fun setBucketExcluded(bucketName: String, excluded: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.EXCLUDED_BUCKETS] ?: emptySet()
            prefs[Keys.EXCLUDED_BUCKETS] = if (excluded) current + bucketName else current - bucketName
        }
    }

    suspend fun setPlayerSkin(skin: PlayerSkin) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PLAYER_SKIN] = skin.name
        }
    }
}
