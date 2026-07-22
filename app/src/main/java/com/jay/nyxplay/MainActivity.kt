package com.jay.nyxplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fase 0 — Esqueleto.
 * Objetivo único: confirmar que a stack (Gradle + Kotlin + Compose + Material3)
 * compila e corre no dispositivo/emulador, antes de introduzir MediaStore,
 * Room, Media3 ou o protocolo P2P.
 *
 * Próximas fases substituem NyxPlayScaffold() pelo feed vertical real.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NyxPlayTheme {
                NyxPlayScaffold()
            }
        }
    }
}

@Composable
fun NyxPlayTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFF7C5CFF),
        background = Color(0xFF0E0E12),
        surface = Color(0xFF16161C)
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun NyxPlayScaffold() {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NyxPlay",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fase 0 — esqueleto vivo",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Próxima fase: media-scanner (MediaStore + thumbnails)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}
