package com.jay.nyxplay.ui.video

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.ui.MediaThumbnail

/**
 * Feed vertical estilo TikTok. Um único ExoPlayer é reutilizado e
 * re-associado ao vídeo assim que uma página "assenta" (settledPage) —
 * evitar um player por item é essencial num aparelho com memória
 * limitada, e evita reprodução simultânea de vários vídeos.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoFeedScreen(videos: List<MediaEntity>, startIndex: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = startIndex) { videos.size }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(pagerState.settledPage) {
        val video = videos.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(video.uri)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page == pagerState.settledPage) {
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
                // Páginas fora do foco mostram só o thumbnail estático —
                // nunca dois vídeos a reproduzir ao mesmo tempo.
                MediaThumbnail(
                    uri = videos[page].uri,
                    type = MediaType.VIDEO,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
        }

        videos.getOrNull(pagerState.currentPage)?.let { video ->
            Text(
                video.displayName,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            )
        }
    }
}
