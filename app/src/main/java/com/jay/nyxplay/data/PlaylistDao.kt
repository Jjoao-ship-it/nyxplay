package com.jay.nyxplay.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): PlaylistEntity?

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlist_media_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearCrossRefs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<PlaylistMediaCrossRef>)
}
