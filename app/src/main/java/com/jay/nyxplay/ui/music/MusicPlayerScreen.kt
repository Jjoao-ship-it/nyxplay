package com.jay.nyxplay.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.nyxplay.data.PlayerSkin
import com.jay.nyxplay.ui.settings.SettingsViewModel

@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val ui by viewModel.state.collectAsState()
    val skin by settingsViewModel.playerSkin.collectAsState()
    val song = ui.currentSong
    var showQueue by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val animatedBg by animateColorAsState(targetValue = ui.dominantColor, label = "playerBackground")

    Box(modifier = Modifier.fillMaxSize().background(if (skin == PlayerSkin.GRADIENT) Color.Black else animatedBg)) {

        if (skin == PlayerSkin.GRADIENT) {
            ui.albumArt?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(60.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.3f), animatedBg.copy(alpha = 0.9f))
                            )
                        )
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Fila de reprodução", tint = Color.White)
                }
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
                ui.albumArt?.let {
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

            Text(song?.displayName ?: "", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                song?.artist?.takeIf { it.isNotBlank() } ?: "Artista desconhecido",
                color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp, maxLines = 1
            )

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = if (ui.durationMs > 0) ui.positionMs.toFloat() / ui.durationMs.toFloat() else 0f,
                onValueChange = { viewModel.seekTo(it) },
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
                IconButton(onClick = viewModel::previous, enabled = ui.currentIndex > 0) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White)
                }
                IconButton(
                    onClick = viewModel::togglePlayPause,
                    modifier = Modifier.padding(horizontal = 16.dp).size(64.dp)
                        .clip(RoundedCornerShape(32.dp)).background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        if (ui.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = viewModel::next, enabled = ui.currentIndex < ui.queue.lastIndex) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Seguinte", tint = Color.White)
                }
            }
        }
    }

    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            title = { Text("Fila de reprodução (${ui.queue.size})") },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(ui.queue, key = { it.uid }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playAtIndex(ui.queue.indexOf(item))
                                    showQueue = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.displayName,
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (item.uid == song?.uid) FontWeight.Bold else FontWeight.Normal
                            )
                            IconButton(onClick = { viewModel.removeFromQueue(item.uid) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remover da fila")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showQueue = false }) { Text("Fechar") }
            }
        )
    }
}
