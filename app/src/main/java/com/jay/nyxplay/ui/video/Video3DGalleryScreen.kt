package com.jay.nyxplay.ui.video

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.ui.MediaThumbnail
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private enum class GalleryMode(val label: String) {
    ESPIRAL("Espiral"),
    HELICE("Hélice"),
    CUBO("Cubo"),
    TRANSLACAO("Translação")
}

/**
 * Galeria 3D — TOTALMENTE ESTÁTICA (só thumbnails, nenhum vídeo carrega
 * ou reproduz aqui; isso acontece só ao tocar num item, que abre o feed
 * completo). Fundo parado. Quatro modos com física distinta cada um.
 */
@Composable
fun Video3DGalleryScreen(
    catalog: VideoCatalog,
    onBack: () -> Unit,
    onOpenFeed: (Int) -> Unit
) {
    val density = LocalDensity.current
    var mode by remember { mutableStateOf(GalleryMode.ESPIRAL) }
    var angleDeg by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var isInteracting by remember { mutableStateOf(false) }

    val itemCount = catalog.videos.size
    val angleStep = if (itemCount > 0) 360f / itemCount else 0f

    // Rotação/deslocamento automático contínuo — pausa durante gesto,
    // retoma sem saltos. Velocidade varia por modo (Hélice mais lenta).
    LaunchedEffect(isInteracting, mode) {
        if (!isInteracting) {
            val speed = if (mode == GalleryMode.HELICE) 0.18f else 0.35f
            while (true) {
                delay(16)
                angleDeg = (angleDeg + speed) % 360f
            }
        }
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

        // Fundo estático — capa desfocada, sem nenhuma animação
        MediaThumbnail(
            uri = catalog.coverUri,
            type = MediaType.VIDEO,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.3f }
                .let { if (Build.VERSION.SDK_INT >= 31) it.blur(35.dp) else it }
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        val itemWidth = 190.dp
        val itemHeight = 250.dp
        val itemSpacingPx = with(density) { 130.dp.toPx() }

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

                            when (mode) {
                                GalleryMode.ESPIRAL -> {
                                    val radiusPx = with(density) { 140.dp.toPx() }
                                    val itemAngleDeg = (angleStep * index + angleDeg) % 360f
                                    val rad = itemAngleDeg * PI.toFloat() / 180f
                                    val depth = cos(rad)
                                    translationX = sin(rad) * radiusPx
                                    translationY = (index - itemCount / 2f) * (itemSpacingPx * 0.12f)
                                    val s = 0.55f + 0.45f * ((depth + 1f) / 2f)
                                    scaleX = s; scaleY = s
                                    alpha = 0.3f + 0.7f * ((depth + 1f) / 2f)
                                    rotationY = -itemAngleDeg
                                }
                                GalleryMode.HELICE -> {
                                    val radiusPx = with(density) { 95.dp.toPx() }
                                    val itemAngleDeg = (angleStep * index + angleDeg) % 360f
                                    val rad = itemAngleDeg * PI.toFloat() / 180f
                                    val depth = cos(rad)
                                    translationX = sin(rad) * radiusPx
                                    translationY = (index - itemCount / 2f) * (itemSpacingPx * 0.42f)
                                    val s = 0.5f + 0.4f * ((depth + 1f) / 2f)
                                    scaleX = s; scaleY = s
                                    alpha = 0.25f + 0.75f * ((depth + 1f) / 2f)
                                    rotationY = -itemAngleDeg
                                }
                                GalleryMode.CUBO -> {
                                    val radiusPx = with(density) { 150.dp.toPx() }
                                    val itemAngleDeg = (angleStep * index + angleDeg) % 360f
                                    val rad = itemAngleDeg * PI.toFloat() / 180f
                                    val depth = cos(rad)
                                    translationX = sin(rad) * radiusPx
                                    translationY = 0f
                                    val s = 0.6f + 0.4f * ((depth + 1f) / 2f)
                                    scaleX = s; scaleY = s
                                    alpha = 0.35f + 0.65f * ((depth + 1f) / 2f)
                                    rotationY = -itemAngleDeg * 0.5f
                                    transformOrigin = TransformOrigin(
                                        if (itemAngleDeg in 0f..180f) 0f else 1f, 0.5f
                                    )
                                }
                                GalleryMode.TRANSLACAO -> {
                                    val totalWidth = itemSpacingPx * itemCount
                                    val dragOffsetPx = (angleDeg / 360f) * totalWidth
                                    var tx = (index - itemCount / 2f) * itemSpacingPx - dragOffsetPx
                                    tx = floorMod(tx + totalWidth / 2f, totalWidth) - totalWidth / 2f
                                    val distFrac = (abs(tx) / (itemSpacingPx * 2.2f)).coerceIn(0f, 1f)
                                    translationX = tx
                                    translationY = 0f
                                    val s = 1f - distFrac * 0.35f
                                    scaleX = s; scaleY = s
                                    alpha = 1f - distFrac * 0.6f
                                    rotationY = (tx / itemSpacingPx).coerceIn(-1f, 1f) * -35f
                                }
                            }
                        }
                        .zIndex(zIndexFor(mode, index, angleStep, angleDeg, itemCount, itemSpacingPx))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A22))
                        .pointerInput(index) {
                            detectTapGestures { onOpenFeed(index) }
                        }
                ) {
                    MediaThumbnail(
                        uri = video.uri,
                        type = MediaType.VIDEO,
                        modifier = Modifier.fillMaxSize()
                    )
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
                    m.label,
                    color = if (mode == m) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .pointerInput(m) { detectTapGestures { mode = m } }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private fun floorMod(value: Float, modulus: Float): Float {
    val r = value % modulus
    return if (r < 0) r + modulus else r
}

private fun zIndexFor(
    mode: GalleryMode,
    index: Int,
    angleStep: Float,
    angleDeg: Float,
    itemCount: Int,
    itemSpacingPx: Float
): Float {
    return if (mode == GalleryMode.TRANSLACAO) {
        val totalWidth = itemSpacingPx * itemCount
        val dragOffsetPx = (angleDeg / 360f) * totalWidth
        var tx = (index - itemCount / 2f) * itemSpacingPx - dragOffsetPx
        tx = floorMod(tx + totalWidth / 2f, totalWidth) - totalWidth / 2f
        -abs(tx)
    } else {
        val itemAngleDeg = (angleStep * index + angleDeg) % 360f
        val rad = itemAngleDeg * PI.toFloat() / 180f
        cos(rad)
    }
}
