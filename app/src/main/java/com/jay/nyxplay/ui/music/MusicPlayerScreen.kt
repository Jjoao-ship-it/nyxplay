package com.jay.nyxplay.ui.music

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jay.nyxplay.data.MediaEntity
import kotlinx.coroutines.delay

/**
 * Player "Card" — capa grande, fundo tematizado pela cor dominante da
 * própria capa (técnica real do RetroMusic via Palette), controlos e
 * seek bar ligados a um ExoPlayer real.
 */
@Composable
fun MusicPlayerScreen(audios: List<MediaEntity>, startIndex: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var currentIndex by remember { mutableStateOf(startIndex) }
    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var albumArt by remember(currentIndex) { mutableStateOf<Bitmap?>(null) }
    var backgroundColor by remember { mutableStateOf(Color(0xFF232330)) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && currentIndex < audios.lastIndex) {
                    currentIndex += 1
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val currentAudio = audios.getOrNull(currentIndex)

    LaunchedEffect(currentIndex) {
        val audio = currentAudio ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(audio.uri)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        val art = loadAlbumArt(context, audio.uri)
        albumArt = art.bitmap
        backgroundColor = art.dominantColor
    }

    // Poll de posição — abordagem simples e suficiente para uma seek bar fiável
    LaunchedEffect(currentIndex, isPlaying) {
        while (true) {
            positionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0)
            delay(500)
        }
    }

    BackHandler(onBack = onBack)

    val animatedBg by animateColorAsState(targetValue = backgroundColor, label = "playerBackground")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBg)
            .padding(24.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            albumArt?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.height(96.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            currentAudio?.displayName ?: "",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            currentAudio?.artist?.takeIf { it.isNotBlank() } ?: "Artista desconhecido",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 15.sp,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f,
            onValueChange = { fraction ->
                val target = (fraction * durationMs).toLong()
                exoPlayer.seekTo(target)
                positionMs = target
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (currentIndex > 0) currentIndex -= 1 },
                enabled = currentIndex > 0
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White)
            }

            IconButton(
                onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = { if (currentIndex < audios.lastIndex) currentIndex += 1 },
                enabled = currentIndex < audios.lastIndex
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Seguinte", tint = Color.White)
            }
        }
    }
}
