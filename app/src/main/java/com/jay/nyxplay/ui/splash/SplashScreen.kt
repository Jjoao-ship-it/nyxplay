package com.jay.nyxplay.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

private val CYAN = Color(0xFF3DFBFF)

@Composable
fun SplashScreen() {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, animationSpec = tween(900, easing = LinearEasing))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier = Modifier
                    .padding(24.dp)
                    .size(220.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = 0.85f + entrance.value * 0.15f
                        scaleY = 0.85f + entrance.value * 0.15f
                        alpha = entrance.value
                    }
            ) {
                drawSplashArt(progress = entrance.value, glow = pulse)
            }

            Text(
                "NyxPlay",
                color = CYAN.copy(alpha = entrance.value),
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSplashArt(progress: Float, glow: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val alpha = (glow * 0.5f + 0.5f)

    val vTop = cy - 95f
    val vBottom = cy + 100f
    drawLine(
        color = CYAN.copy(alpha = alpha),
        start = Offset(cx, vTop),
        end = Offset(cx, vTop + (vBottom - vTop) * min(1f, progress * 1.3f)),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )

    if (progress > 0.3f) {
        val hProgress = min(1f, (progress - 0.3f) / 0.5f)
        val hHalf = 78f * hProgress
        drawLine(
            color = CYAN.copy(alpha = alpha),
            start = Offset(cx - hHalf, cy - 30f),
            end = Offset(cx + hHalf, cy - 30f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }

    if (progress > 0.5f) {
        val moonAlpha = min(1f, (progress - 0.5f) / 0.4f) * alpha
        drawCircle(
            color = CYAN.copy(alpha = moonAlpha),
            radius = 46f,
            center = Offset(cx - 4f, cy + 18f)
        )
        drawCircle(
            color = Color.Black,
            radius = 40f,
            center = Offset(cx + 16f, cy + 10f)
        )
    }

    if (progress > 0.75f) {
        val dAlpha = min(1f, (progress - 0.75f) / 0.25f) * alpha
        val dy = cy + 118f
        val path = Path().apply {
            moveTo(cx, dy - 9f)
            lineTo(cx + 6f, dy)
            lineTo(cx, dy + 9f)
            lineTo(cx - 6f, dy)
            close()
        }
        drawPath(path, color = CYAN.copy(alpha = dAlpha))
    }
}
