package com.jay.nyxplay.data

import androidx.room.Entity

@Entity(
    tableName = "playlist_media_cross_ref",
    primaryKeys = ["playlistId", "mediaUid"]
)
data class PlaylistMediaCrossRef(
    val playlistId: Long,
    val mediaUid: String,
    val position: Int
)
