package com.jay.nyxplay.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nyxplay_settings")

enum class PlayerSkin { CARD, GRADIENT }

data class AccentOption(val name: String, val hex: Long)

val ACCENT_OPTIONS = listOf(
    AccentOption("Roxo", 0xFF7C5CFF),
    AccentOption("Azul", 0xFF3D8BFF),
    AccentOption("Verde", 0xFF33C77E),
    AccentOption("Rosa", 0xFFFF4FA0),
    AccentOption("Laranja", 0xFFFF8A3D)
)

/**
 * Definições persistentes — Gerais (afetam vídeo e áudio) e
 * Individuais (só afetam a secção onde foram definidas).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val EXCLUDED_VIDEO_BUCKETS = stringSetPreferencesKey("excluded_video_buckets")
        val EXCLUDED_AUDIO_BUCKETS = stringSetPreferencesKey("excluded_audio_buckets")
        val PLAYER_SKIN = stringPreferencesKey("player_skin")
        val ACCENT_COLOR_HEX = stringPreferencesKey("accent_color_hex")
        val PAUSE_ON_VOLUME_ZERO = booleanPreferencesKey("pause_on_volume_zero")
        val PAUSE_ON_BLUETOOTH_DISCONNECT = booleanPreferencesKey("pause_on_bluetooth_disconnect")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val REMEMBER_LAST_SECTION = booleanPreferencesKey("remember_last_section")
        val LAST_SECTION = stringPreferencesKey("last_section")
    }

    // --- Gerais ---

    val accentColorHex: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACCENT_COLOR_HEX]?.toLongOrNull() ?: ACCENT_OPTIONS.first().hex
    }

    suspend fun setAccentColor(hex: Long) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR_HEX] = hex.toString() }
    }

    val rememberLastSection: Flow<Boolean> = context.dataStore.data.map { it[Keys.REMEMBER_LAST_SECTION] ?: true }

    suspend fun setRememberLastSection(value: Boolean) {
        context.dataStore.edit { it[Keys.REMEMBER_LAST_SECTION] = value }
    }

    val lastSection: Flow<String> = context.dataStore.data.map { it[Keys.LAST_SECTION] ?: "VIDEOS" }

    suspend fun setLastSection(section: String) {
        context.dataStore.edit { it[Keys.LAST_SECTION] = section }
    }

    // --- Individuais: Vídeo ---

    val excludedVideoBuckets: Flow<Set<String>> = context.dataStore.data.map { it[Keys.EXCLUDED_VIDEO_BUCKETS] ?: emptySet() }

    suspend fun setVideoBucketExcluded(bucketName: String, excluded: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.EXCLUDED_VIDEO_BUCKETS] ?: emptySet()
            prefs[Keys.EXCLUDED_VIDEO_BUCKETS] = if (excluded) current + bucketName else current - bucketName
        }
    }

    // --- Individuais: Áudio ---

    val excludedAudioBuckets: Flow<Set<String>> = context.dataStore.data.map { it[Keys.EXCLUDED_AUDIO_BUCKETS] ?: emptySet() }

    suspend fun setAudioBucketExcluded(bucketName: String, excluded: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.EXCLUDED_AUDIO_BUCKETS] ?: emptySet()
            prefs[Keys.EXCLUDED_AUDIO_BUCKETS] = if (excluded) current + bucketName else current - bucketName
        }
    }

    val playerSkin: Flow<PlayerSkin> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.PLAYER_SKIN]) {
            "GRADIENT" -> PlayerSkin.GRADIENT
            else -> PlayerSkin.CARD
        }
    }

    suspend fun setPlayerSkin(skin: PlayerSkin) {
        context.dataStore.edit { it[Keys.PLAYER_SKIN] = skin.name }
    }

    val pauseOnVolumeZero: Flow<Boolean> = context.dataStore.data.map { it[Keys.PAUSE_ON_VOLUME_ZERO] ?: true }

    suspend fun setPauseOnVolumeZero(value: Boolean) {
        context.dataStore.edit { it[Keys.PAUSE_ON_VOLUME_ZERO] = value }
    }

    val pauseOnBluetoothDisconnect: Flow<Boolean> = context.dataStore.data.map { it[Keys.PAUSE_ON_BLUETOOTH_DISCONNECT] ?: true }

    suspend fun setPauseOnBluetoothDisconnect(value: Boolean) {
        context.dataStore.edit { it[Keys.PAUSE_ON_BLUETOOTH_DISCONNECT] = value }
    }

    val historyEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.HISTORY_ENABLED] ?: true }

    suspend fun setHistoryEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.HISTORY_ENABLED] = value }
    }
}
