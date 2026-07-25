package com.jay.nyxplay.ui.video

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay

/**
 * Feed vertical estilo TikTok, com gestos reais:
 * - Duplo-toque esquerda/direita: recua/avança 10s
 * - Arrastar vertical (metade direita): volume real do aparelho
 * - Arrastar horizontal: scrub com prévia do tempo
 * - Pinça: zoom no vídeo
 * - Toque único: mostrar/esconder controlos (auto-esconde em 3s)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoFeedScreen(videos: List<MediaEntity>, startIndex: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = startIndex) { videos.size }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var videoScale by remember { mutableFloatStateOf(1f) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(pagerState.settledPage) {
        val video = videos.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(video.uri)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        videoScale = 1f
    }

    // Posição/duração para a barra de progresso
    LaunchedEffect(pagerState.settledPage) {
        while (true) {
            positionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0)
            delay(300)
        }
    }

    // Auto-esconder controlos 3s depois de aparecerem
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    // Apagar feedback de seek (+10s/-10s) passado um instante
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(600)
            seekFeedback = null
        }
    }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = videoScale <= 1.05f // não competir com o pinch-zoom em curso
        ) { page ->
            if (page == pagerState.settledPage) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = {
                            PlayerView(it).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { scaleX = videoScale; scaleY = videoScale }
                    )

                    // Camada de gestos — separada do PlayerView para não interferir
                    // com a superfície de vídeo nativa
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    if (zoom != 1f) {
                                        videoScale = (videoScale * zoom).coerceIn(1f, 3f)
                                    } else if (kotlin.math.abs(pan.x) > kotlin.math.abs(pan.y)) {
                                        val deltaMs = (pan.x * 40).toLong()
                                        val target = (exoPlayer.currentPosition + deltaMs)
                                            .coerceIn(0, exoPlayer.duration.coerceAtLeast(0))
                                        exoPlayer.seekTo(target)
                                        showControls = true
                                    } else {
                                        val deltaVolume = -pan.y / 600f
                                        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val newLevel = (current + deltaVolume * maxVolume)
                                            .toInt().coerceIn(0, maxVolume)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newLevel, 0)
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showControls = !showControls },
                                    onDoubleTap = { offset ->
                                        val isLeftSide = offset.x < size.width / 2
                                        val deltaMs = if (isLeftSide) -10_000L else 10_000L
                                        val target = (exoPlayer.currentPosition + deltaMs)
                                            .coerceIn(0, exoPlayer.duration.coerceAtLeast(0))
                                        exoPlayer.seekTo(target)
                                        seekFeedback = if (isLeftSide) "-10s" else "+10s"
                                    }
                                )
                            }
                    )
                }
            } else {
                MediaThumbnail(
                    uri = videos[page].uri,
                    type = MediaType.VIDEO,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        seekFeedback?.let {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(it, color = Color.White, fontSize = 28.sp)
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
        ) {
            Box {
                IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp)
        ) {
            androidx.compose.foundation.layout.Column {
                videos.getOrNull(pagerState.currentPage)?.let { video ->
                    Text(video.displayName, color = Color.White)
                }
                if (durationMs > 0) {
                    LinearProgressIndicator(
                        progress = { (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
