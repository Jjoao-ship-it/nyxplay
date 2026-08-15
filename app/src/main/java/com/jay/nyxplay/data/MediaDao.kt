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
     * Todos os uid já indexados de um tipo — usado para calcular,
     * do lado do Kotlin, quais itens desapareceram do dispositivo
     * (em vez de uma única query gigante NOT IN, que estoura o
     * limite de 999 variáveis do SQLite em bibliotecas grandes).
     */
    @Query("SELECT uid FROM media_items WHERE type = :type")
    suspend fun getAllUidsForType(type: MediaType): List<String>

    /**
     * Apaga por uid explícito — seguro para dividir em blocos,
     * ao contrário de NOT IN (cada bloco aqui apaga um subconjunto
     * inequívoco, sem risco de apagar o que não devia).
     */
    @Query("DELETE FROM media_items WHERE uid IN (:uids)")
    suspend fun deleteByUids(uids: List<String>)

    // --- Rastreio real de reprodução ---

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
