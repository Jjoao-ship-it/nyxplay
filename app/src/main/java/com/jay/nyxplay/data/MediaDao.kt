package com.jay.nyxplay.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY dateAdded DESC")
    fun observeByType(type: MediaType): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaEntity>)

    /**
     * Remove itens de um tipo que já não existem no dispositivo.
     * Filtra por tipo para não apagar áudio ao re-scanear vídeo, e vice-versa.
     */
    @Query("DELETE FROM media_items WHERE type = :type AND uid NOT IN (:validUids)")
    suspend fun deleteMissing(type: MediaType, validUids: List<String>)

    // --- Rastreio real de reprodução (base para Início, Histórico, Mais tocadas) ---

    @Query(
        "UPDATE media_items SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE uid = :uid"
    )
    suspend fun registerPlay(uid: String, timestamp: Long)

    @Query("UPDATE media_items SET watchedPercent = :percent WHERE uid = :uid")
    suspend fun updateWatchedPercent(uid: String, percent: Int)

    @Query(
        "SELECT * FROM media_items WHERE type = :type AND playCount > 0 ORDER BY playCount DESC LIMIT :limit"
    )
    fun observeMostPlayed(type: MediaType, limit: Int): Flow<List<MediaEntity>>

    @Query(
        "SELECT * FROM media_items WHERE type = :type AND lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit"
    )
    fun observeRecentlyPlayed(type: MediaType, limit: Int): Flow<List<MediaEntity>>
}
