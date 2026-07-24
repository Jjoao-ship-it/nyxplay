package com.jay.nyxplay.ui.video

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.ui.MediaThumbnail
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private enum class GalleryMode { ESPIRAL, CUBO }

/**
 * Galeria 3D — reservada só para dentro de um catálogo, nunca no ecrã
 * principal. Dois modos comutáveis (Espiral/Cubo). O item em foco (o
 * mais próximo do centro) reproduz vídeo real, mutado, em loop — os
 * restantes mostram só thumbnail, para não sobrecarregar memória com
 * múltiplos vídeos simultâneos.
 */
@Composable
fun Video3DGalleryScreen(
    catalog: VideoCatalog,
    onBack: () -> Unit,
    onOpenFeed: (Int) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var mode by remember { mutableStateOf(GalleryMode.ESPIRAL) }
    var focusedIndex by remember { mutableStateOf(0) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Determina o item em foco (mais próximo do centro do viewport)
    LaunchedEffect(listState, catalog.videos.size) {
        snapshotFlowFocusedIndex(listState) { index -> focusedIndex = index }
    }

    LaunchedEffect(focusedIndex) {
        val video = catalog.videos.getOrNull(focusedIndex) ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(android.net.Uri.parse(video.uri)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bgRotation")
    val bgRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bgRotationValue"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Fundo dinâmico — capa do catálogo, a rodar lentamente, desfocada quando a API permite
        MediaThumbnail(
            uri = catalog.coverUri,
            type = MediaType.VIDEO,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = bgRotation
                    scaleX = 1.6f
                    scaleY = 1.6f
                    alpha = 0.35f
                }
                .let { if (Build.VERSION.SDK_INT >= 31) it.blur(30.dp) else it }
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val itemWidth = 220.dp
            val sidePadding = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp)
            val density = LocalDensity.current

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = sidePadding),
                modifier = Modifier.align(Alignment.Center)
            ) {
                itemsIndexed(catalog.videos, key = { _, v -> v.uid }) { index, video ->
                    val offsetFraction by rememberItemOffsetFraction(listState, index)

                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(itemWidth * 1.3f)
                            .padding(horizontal = 8.dp)
                            .graphicsLayer {
                                cameraDistance = 12f * density.density
                                when (mode) {
                                    GalleryMode.ESPIRAL -> {
                                        rotationY = offsetFraction * 45f
                                        translationY = sin(offsetFraction * PI).toFloat() * 50f
                                        val s = 1f - abs(offsetFraction) * 0.25f
                                        scaleX = s
                                        scaleY = s
                                        alpha = 1f - abs(offsetFraction) * 0.35f
                                    }
                                    GalleryMode.CUBO -> {
                                        rotationY = offsetFraction * 90f
                                        transformOrigin = TransformOrigin(
                                            if (offsetFraction > 0f) 0f else 1f, 0.5f
                                        )
                                        alpha = 1f - abs(offsetFraction) * 0.5f
                                    }
                                }
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1A1A22))
                            .clickable { onOpenFeed(index) }
                    ) {
                        if (index == focusedIndex) {
                            AndroidView(
                                factory = {
                                    PlayerView(it).apply {
                                        player = exoPlayer
                                        useController = false
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            MediaThumbnail(
                                uri = video.uri,
                                type = MediaType.VIDEO,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
        }

        Text(
            catalog.name,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            GalleryMode.entries.forEach { m ->
                Text(
                    if (m == GalleryMode.ESPIRAL) "Espiral" else "Cubo",
                    color = if (mode == m) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { mode = m }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/**
 * Observa a posição de scroll e reporta o índice do item mais próximo
 * do centro do viewport — usado para saber qual item deve reproduzir
 * vídeo real.
 */
private suspend fun snapshotFlowFocusedIndex(
    listState: androidx.compose.foundation.lazy.LazyListState,
    onFocusedChanged: (Int) -> Unit
) {
    androidx.compose.runtime.snapshotFlow {
        val info = listState.layoutInfo
        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
        info.visibleItemsInfo.minByOrNull { item ->
            abs((item.offset + item.size / 2) - viewportCenter)
        }?.index ?: 0
    }.collect { index -> onFocusedChanged(index) }
}
