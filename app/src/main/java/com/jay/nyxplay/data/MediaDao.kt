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
}
