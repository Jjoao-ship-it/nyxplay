package com.jay.nyxplay.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nyxplay_settings")

enum class PlayerSkin { CARD, GRADIENT }
enum class AppFontFamily { DEFAULT, SERIF, MONOSPACE }
enum class HomeLayoutStyle { LISTA, GRELHA }
enum class MusicCardStyle { COMPACTO, CARTAO }

data class AccentOption(val name: String, val hex: Long)

val ACCENT_OPTIONS = listOf(
    AccentOption("Roxo", 0xFF7C5CFF),
    AccentOption("Azul", 0xFF3D8BFF),
    AccentOption("Verde", 0xFF33C77E),
    AccentOption("Rosa", 0xFFFF4FA0),
    AccentOption("Laranja", 0xFFFF8A3D)
)

val WALLPAPER_OPTIONS = listOf(
    AccentOption("Preto", 0xFF0E0E12),
    AccentOption("Azul-marinho", 0xFF0B1220),
    AccentOption("Grafite", 0xFF16161C),
    AccentOption("Vinho", 0xFF1A0E14),
    AccentOption("Verde-escuro", 0xFF0E1A14)
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
        val WALLPAPER_COLOR_HEX = stringPreferencesKey("wallpaper_color_hex")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val HOME_LAYOUT_STYLE = stringPreferencesKey("home_layout_style")
        val MUSIC_CARD_STYLE = stringPreferencesKey("music_card_style")
        val PAUSE_ON_VOLUME_ZERO = booleanPreferencesKey("pause_on_volume_zero")
        val PAUSE_ON_BLUETOOTH_DISCONNECT = booleanPreferencesKey("pause_on_bluetooth_disconnect")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val REMEMBER_LAST_SECTION = booleanPreferencesKey("remember_last_section")
        val LAST_SECTION = stringPreferencesKey("last_section")
        val LAST_QUEUE_UIDS = stringPreferencesKey("last_queue_uids")
        val LAST_QUEUE_INDEX = intPreferencesKey("last_queue_index")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
    }

    // --- Memorizar última reprodução (música) ---

    val lastQueueUids: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_QUEUE_UIDS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
    val lastQueueIndex: Flow<Int> = context.dataStore.data.map { it[Keys.LAST_QUEUE_INDEX] ?: 0 }
    val lastPositionMs: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_POSITION_MS] ?: 0L }

    suspend fun saveLastQueue(uids: List<String>, index: Int, positionMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_QUEUE_UIDS] = uids.joinToString(",")
            prefs[Keys.LAST_QUEUE_INDEX] = index
            prefs[Keys.LAST_POSITION_MS] = positionMs
        }
    }

    // --- Gerais ---

    val accentColorHex: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACCENT_COLOR_HEX]?.toLongOrNull() ?: ACCENT_OPTIONS.first().hex
    }

    suspend fun setAccentColor(hex: Long) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR_HEX] = hex.toString() }
    }

    val wallpaperColorHex: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.WALLPAPER_COLOR_HEX]?.toLongOrNull() ?: WALLPAPER_OPTIONS.first().hex
    }

    suspend fun setWallpaperColor(hex: Long) {
        context.dataStore.edit { it[Keys.WALLPAPER_COLOR_HEX] = hex.toString() }
    }

    val fontFamily: Flow<AppFontFamily> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.FONT_FAMILY]) {
            "SERIF" -> AppFontFamily.SERIF
            "MONOSPACE" -> AppFontFamily.MONOSPACE
            else -> AppFontFamily.DEFAULT
        }
    }

    suspend fun setFontFamily(font: AppFontFamily) {
        context.dataStore.edit { it[Keys.FONT_FAMILY] = font.name }
    }

    val homeLayoutStyle: Flow<HomeLayoutStyle> = context.dataStore.data.map { prefs ->
        if (prefs[Keys.HOME_LAYOUT_STYLE] == "GRELHA") HomeLayoutStyle.GRELHA else HomeLayoutStyle.LISTA
    }

    suspend fun setHomeLayoutStyle(style: HomeLayoutStyle) {
        context.dataStore.edit { it[Keys.HOME_LAYOUT_STYLE] = style.name }
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

    val musicCardStyle: Flow<MusicCardStyle> = context.dataStore.data.map { prefs ->
        if (prefs[Keys.MUSIC_CARD_STYLE] == "CARTAO") MusicCardStyle.CARTAO else MusicCardStyle.COMPACTO
    }

    suspend fun setMusicCardStyle(style: MusicCardStyle) {
        context.dataStore.edit { it[Keys.MUSIC_CARD_STYLE] = style.name }
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
