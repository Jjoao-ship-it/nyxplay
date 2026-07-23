package com.jay.nyxplay.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.jay.nyxplay.data.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thumbnail local, sem scraping online:
 * - Vídeo: frame extraído do próprio ficheiro.
 * - Áudio: capa de álbum embutida no ficheiro (ID3/metadata), se existir.
 *   Sem capa embutida, mostra só o fundo — não inventamos capa de terceiros.
 */
@Composable
fun MediaThumbnail(uri: String, type: MediaType, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri, type) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                val parsedUri = Uri.parse(uri)
                when {
                    type == MediaType.VIDEO && Build.VERSION.SDK_INT >= 29 ->
                        context.contentResolver.loadThumbnail(parsedUri, Size(320, 240), null)

                    type == MediaType.VIDEO -> {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, parsedUri)
                            retriever.frameAtTime
                        } finally {
                            retriever.release()
                        }
                    }

                    else -> { // MediaType.AUDIO — capa embutida no ficheiro
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, parsedUri)
                            val art = retriever.embeddedPicture
                            art?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                        } finally {
                            retriever.release()
                        }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(modifier = modifier.background(Color(0xFF232330))) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
