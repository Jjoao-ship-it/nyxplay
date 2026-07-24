package com.jay.nyxplay.data

enum class MediaType { VIDEO, AUDIO }

/**
 * Item de media indexado do MediaStore — vídeo ou áudio.
 *
 * O 'uid' é uma chave sintética "$type:$mediaStoreId", porque o _ID do
 * MediaStore não é globalmente único: um vídeo e uma música podem ter
 * o mesmo _ID em namespaces diferentes (content://media/external/video
 * vs .../audio). Usar só mediaStoreId como chave primária causaria
 * colisões entre os dois tipos.
 */
@androidx.room.Entity(tableName = "media_items")
data class MediaEntity(
    @androidx.room.PrimaryKey val uid: String,
    val mediaStoreId: Long,
    val type: MediaType,
    val uri: String,
    val displayName: String,
    val dateAdded: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val artist: String? = null,
    val album: String? = null,
    val bucketName: String? = null
)

fun buildMediaUid(type: MediaType, mediaStoreId: Long) = "${type.name}:$mediaStoreId"
