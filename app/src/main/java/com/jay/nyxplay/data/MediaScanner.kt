package com.jay.nyxplay.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Indexa vídeo e áudio do dispositivo via MediaStore (duas fontes
 * independentes, dois namespaces de conteúdo), e sincroniza cada um
 * com a sua playlist automática — sem depender de estrutura de pastas.
 */
class MediaScanner(private val context: Context) {

    suspend fun scanVideos(db: NyxDatabase): Int = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaEntity>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                items += MediaEntity(
                    uid = buildMediaUid(MediaType.VIDEO, id),
                    mediaStoreId = id,
                    type = MediaType.VIDEO,
                    uri = contentUri.toString(),
                    displayName = cursor.getString(nameCol) ?: "Sem nome",
                    dateAdded = cursor.getLong(dateCol),
                    durationMs = cursor.getLong(durCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    bucketName = cursor.getString(bucketCol)
                )
            }
        }

        syncTypeAndAutoPlaylist(db, MediaType.VIDEO, items, PLAYLIST_TODOS_OS_VIDEOS)
        items.size
    }

    suspend fun scanAudio(db: NyxDatabase): Int = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaEntity>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )

        // IS_MUSIC filtra tons de chamada, notificações, etc. — só música real
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                items += MediaEntity(
                    uid = buildMediaUid(MediaType.AUDIO, id),
                    mediaStoreId = id,
                    type = MediaType.AUDIO,
                    uri = contentUri.toString(),
                    displayName = cursor.getString(nameCol) ?: "Sem nome",
                    dateAdded = cursor.getLong(dateCol),
                    durationMs = cursor.getLong(durCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    artist = cursor.getString(artistCol),
                    album = cursor.getString(albumCol)
                )
            }
        }

        syncTypeAndAutoPlaylist(db, MediaType.AUDIO, items, PLAYLIST_TODAS_AS_MUSICAS)
        items.size
    }

    suspend fun scanAll(db: NyxDatabase) {
        scanVideos(db)
        scanAudio(db)
    }

    private suspend fun syncTypeAndAutoPlaylist(
        db: NyxDatabase,
        type: MediaType,
        items: List<MediaEntity>,
        autoPlaylistName: String
    ) {
        val mediaDao = db.mediaDao()
        mediaDao.insertAll(items)
        if (items.isNotEmpty()) {
            mediaDao.deleteMissing(type, items.map { it.uid })
        }

        val playlistDao = db.playlistDao()
        val existing = playlistDao.findByName(autoPlaylistName)
        val playlistId = existing?.id
            ?: playlistDao.insert(PlaylistEntity(name = autoPlaylistName, type = type, isAutoGenerated = true))

        playlistDao.clearCrossRefs(playlistId)
        playlistDao.insertCrossRefs(
            items.mapIndexed { index, item -> PlaylistMediaCrossRef(playlistId, item.uid, index) }
        )
    }
}
