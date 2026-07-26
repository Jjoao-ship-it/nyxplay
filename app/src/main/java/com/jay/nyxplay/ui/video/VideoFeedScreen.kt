package com.jay.nyxplay.ui.video

import android.app.Activity
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

/**
 * Feed vertical estilo TikTok — o swipe de trocar vídeo é sempre o
 * comportamento nativo do Pager, nunca interceptado por gestos custom.
 *
 * Gestos (convenção real do VLC, confirmada por documentação):
 * - Faixa esquerda (15%): arrastar vertical = brilho
 * - Faixa direita (15%): arrastar vertical = volume
 * - Centro (70%): só toque/duplo-toque — nunca captura arrasto,
 *   por isso nunca compete com o swipe de página
 *
 * Botões sempre visíveis: recuar 10s, vídeo anterior, play/pause,
 * próximo vídeo, avançar 10s.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoFeedScreen(videos: List<MediaEntity>, startIndex: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = startIndex) { videos.size }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    var showTopBar by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val video = videos.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(video.uri)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        com.jay.nyxplay.data.NyxDatabase.getInstance(context).mediaDao()
            .registerPlay(video.uid, System.currentTimeMillis())
    }

    LaunchedEffect(pagerState.settledPage) {
        while (true) {
            positionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0)
            delay(300)
        }
    }

    LaunchedEffect(showTopBar) {
        if (showTopBar) {
            delay(3000)
            showTopBar = false
        }
    }

    fun seekBy(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0, exoPlayer.duration.coerceAtLeast(0))
        exoPlayer.seekTo(target)
    }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
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
                        modifier = Modifier.fillMaxSize()
                    )

                    // Centro — só toque, NUNCA arrasto, para não competir
                    // com o swipe vertical de trocar de vídeo do Pager.
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.7f)
                            .align(androidx.compose.ui.Alignment.Center)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showTopBar = !showTopBar },
                                    onDoubleTap = { offset ->
                                        if (offset.x < size.width / 2) seekBy(-10_000L) else seekBy(10_000L)
                                    }
                                )
                            }
                    )

                    // Faixa esquerda — brilho
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(60.dp)
                            .align(androidx.compose.ui.Alignment.CenterStart)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    brightness = (brightness - dragAmount / 800f).coerceIn(0.02f, 1f)
                                    activity?.let { act ->
                                        val params = act.window.attributes
                                        params.screenBrightness = brightness
                                        act.window.attributes = params
                                    }
                                }
                            }
                    )

                    // Faixa direita — volume
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(60.dp)
                            .align(androidx.compose.ui.Alignment.CenterEnd)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val delta = -dragAmount / 800f * maxVolume
                                    val newLevel = (current + delta).toInt().coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newLevel, 0)
                                }
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

        AnimatedVisibility(
            visible = showTopBar,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box {
                IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
            }
        }

        // Barra de progresso + nome — sempre visível, discreta
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            videos.getOrNull(pagerState.currentPage)?.let { video ->
                Text(video.displayName, color = Color.White, fontSize = 13.sp)
            }
            if (durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }

            // Botões sempre visíveis, como pedido
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    scope.launch {
                        if (pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Vídeo anterior", tint = Color.White)
                }
                IconButton(onClick = { seekBy(-10_000L) }) {
                    Icon(Icons.Default.Replay10, contentDescription = "Recuar 10s", tint = Color.White)
                }
                IconButton(onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { seekBy(10_000L) }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Avançar 10s", tint = Color.White)
                }
                IconButton(onClick = {
                    scope.launch {
                        if (pagerState.currentPage < videos.lastIndex) pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Próximo vídeo", tint = Color.White)
                }
            }
        }
    }
}
