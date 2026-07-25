package com.jay.nyxplay.ui.video

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.ui.MediaThumbnail
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class GalleryMode { ESPIRAL, CUBO }

/**
 * Carrossel 3D próprio (não usa LazyRow) — necessário para rotação
 * contínua e fluida, desacoplada do scroll bruto, que era a causa da
 * travagem na versão anterior. Cada item tem posição real calculada
 * por trigonometria (círculo em torno de um eixo vertical), não é
 * só uma fila com leve rotação.
 *
 * Fundo é estático (capa desfocada, sem animação) — a rotação é só
 * do próprio carrossel, como pedido.
 */
@Composable
fun Video3DGalleryScreen(
    catalog: VideoCatalog,
    onBack: () -> Unit,
    onOpenFeed: (Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var mode by remember { mutableStateOf(GalleryMode.ESPIRAL) }
    var angleDeg by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var isInteracting by remember { mutableStateOf(false) }
    var focusedIndex by remember { mutableIntStateOf(0) }

    val itemCount = catalog.videos.size
    val angleStep = if (itemCount > 0) 360f / itemCount else 0f

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Rotação automática contínua — pausa durante interação manual,
    // retoma sem saltos a partir do ângulo onde ficou.
    LaunchedEffect(isInteracting) {
        if (!isInteracting) {
            while (true) {
                delay(16)
                angleDeg = (angleDeg + 0.35f) % 360f
            }
        }
    }

    // Índice em foco (o mais próximo da "frente") — recalculado a
    // ritmo baixo (5x/s) num loop estável, não reiniciado a cada
    // frame de rotação (isso seria tão pesado quanto o problema original).
    LaunchedEffect(itemCount) {
        if (itemCount == 0) return@LaunchedEffect
        while (true) {
            var closest = 0
            var closestDist = Float.MAX_VALUE
            for (i in 0 until itemCount) {
                val itemAngle = (angleStep * i + angleDeg) % 360f
                val dist = kotlin.math.abs(((itemAngle + 180f) % 360f) - 180f)
                if (dist < closestDist) {
                    closestDist = dist
                    closest = i
                }
            }
            if (closest != focusedIndex) focusedIndex = closest
            delay(200)
        }
    }

    LaunchedEffect(focusedIndex) {
        val video = catalog.videos.getOrNull(focusedIndex) ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(video.uri)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    isInteracting = true
                    angleDeg = (angleDeg + pan.x * 0.4f) % 360f
                    scale = (scale * zoom).coerceIn(0.5f, 2f)
                }
            }
    ) {
        BackHandler(onBack = onBack)

        // Fundo ESTÁTICO — sem animação nenhuma
        MediaThumbnail(
            uri = catalog.coverUri,
            type = MediaType.VIDEO,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.35f }
                .let { if (Build.VERSION.SDK_INT >= 31) it.blur(35.dp) else it }
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        val radiusPx = with(density) { 140.dp.toPx() }
        val itemWidth = 190.dp
        val itemHeight = 250.dp
        val spiralStepPx = with(density) { 14.dp.toPx() }
        val centerIndex = itemCount / 2f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale }
        ) {
            for (index in 0 until itemCount) {
                val video = catalog.videos[index]

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(itemWidth)
                        .height(itemHeight)
                        .graphicsLayer {
                            cameraDistance = 16f * density.density
                            val itemAngleDeg = (angleStep * index + angleDeg) % 360f
                            val rad = itemAngleDeg * PI.toFloat() / 180f
                            val x = sin(rad) * radiusPx
                            val depthFactor = cos(rad) // -1 (trás) .. 1 (frente)
                            val itemScale = 0.55f + 0.45f * ((depthFactor + 1f) / 2f)
                            val itemAlpha = 0.3f + 0.7f * ((depthFactor + 1f) / 2f)

                            translationX = x
                            translationY = when (mode) {
                                GalleryMode.ESPIRAL -> (index - centerIndex) * spiralStepPx * 0.3f
                                GalleryMode.CUBO -> 0f
                            }
                            scaleX = itemScale
                            scaleY = itemScale
                            alpha = itemAlpha
                            rotationY = when (mode) {
                                GalleryMode.ESPIRAL -> -itemAngleDeg
                                GalleryMode.CUBO -> -itemAngleDeg * 0.5f
                            }
                        }
                        .zIndex(cos((angleStep * index + angleDeg) * PI.toFloat() / 180f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A22))
                        .pointerInput(index) {
                            detectTapGestures { onOpenFeed(index) }
                        }
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
                        val itemAngleDeg = (angleStep * index + angleDeg) % 360f
                        val depthFactor = cos(itemAngleDeg * PI.toFloat() / 180f)
                        if (depthFactor > -0.3f) {
                            MediaThumbnail(
                                uri = video.uri,
                                type = MediaType.VIDEO,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Itens de costas (depthFactor <= -0.3) ficam só com o
                        // fundo sólido já aplicado à Box — sem decode de thumbnail,
                        // para não sobrecarregar memória com catálogos grandes.
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

        androidx.compose.foundation.layout.Row(
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
                        .pointerInput(m) { detectTapGestures { mode = m } }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}
