package com.jay.nyxplay.ui.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AlbumArtResult(
    val bitmap: Bitmap?,
    val dominantColor: Color
)

private const val FALLBACK_COLOR = 0xFF232330.toInt()

/**
 * Extrai a capa embutida em resolução maior (para o player full-screen)
 * e calcula a cor dominante com Palette — é assim que o RetroMusic
 * tematiza o player por música, e reproduzimos a mesma técnica real,
 * mesmo sem reaproveitar o código dele (View system vs Compose).
 */
suspend fun loadAlbumArt(context: Context, uri: String): AlbumArtResult = withContext(Dispatchers.IO) {
    try {
        val retriever = MediaMetadataRetriever()
        val bitmap = try {
            retriever.setDataSource(context, Uri.parse(uri))
            val art = retriever.embeddedPicture
            art?.let { decodeSampled(it, targetSize = 640) }
        } finally {
            retriever.release()
        }

        val dominant = bitmap?.let { bmp ->
            Palette.from(bmp).generate().let { palette ->
                palette.dominantSwatch?.rgb ?: palette.darkMutedSwatch?.rgb ?: FALLBACK_COLOR
            }
        } ?: FALLBACK_COLOR

        AlbumArtResult(bitmap, Color(dominant))
    } catch (e: Exception) {
        AlbumArtResult(null, Color(FALLBACK_COLOR))
    }
}

private fun decodeSampled(bytes: ByteArray, targetSize: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    var sampleSize = 1
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    while (largest / sampleSize > targetSize * 2) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}
