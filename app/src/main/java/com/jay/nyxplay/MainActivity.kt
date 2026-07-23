package com.jay.nyxplay

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.ui.LibraryViewModel
import com.jay.nyxplay.ui.MediaThumbnail

/**
 * Fase 1 (revista) — media-scanner para DOIS centros de media: vídeo e áudio,
 * como duas secções separadas no mesmo app (bottom navigation), cada uma
 * com a sua própria playlist automática internamente.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NyxPlayTheme {
                NyxPlayApp()
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

private enum class NyxSection { VIDEOS, MUSICA }

private val videoPermission =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private val audioPermission =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun NyxPlayApp(viewModel: LibraryViewModel = viewModel()) {
    var section by remember { mutableStateOf(NyxSection.VIDEOS) }
    var hasVideoPermission by remember { mutableStateOf(false) }
    var hasAudioPermission by remember { mutableStateOf(false) }

    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()

    val videoPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasVideoPermission = granted
        if (granted) viewModel.scanVideos()
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.scanAudio()
    }

    LaunchedEffect(Unit) {
        videoPermLauncher.launch(videoPermission)
        audioPermLauncher.launch(audioPermission)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = section == NyxSection.VIDEOS,
                    onClick = { section = NyxSection.VIDEOS },
                    icon = { Icon(Icons.Default.Movie, contentDescription = null) },
                    label = { Text("Vídeos") }
                )
                NavigationBarItem(
                    selected = section == NyxSection.MUSICA,
                    onClick = { section = NyxSection.MUSICA },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                    label = { Text("Música") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (section) {
                NyxSection.VIDEOS -> VideosSection(
                    hasPermission = hasVideoPermission,
                    videos = videos
                )
                NyxSection.MUSICA -> MusicaSection(
                    hasPermission = hasAudioPermission,
                    audios = audios
                )
            }
        }
    }
}

@Composable
private fun VideosSection(hasPermission: Boolean, videos: List<MediaEntity>) {
    when {
        !hasPermission -> CenteredMessage("A aguardar permissão de acesso aos vídeos…")
        videos.isEmpty() -> CenteredMessage("A indexar vídeos do dispositivo…", showSpinner = true)
        else -> Column(modifier = Modifier.fillMaxSize()) {
            SectionHeader(title = "Vídeos", subtitle = "${videos.size} vídeos — Todos os vídeos")
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(videos, key = { it.uid }) { video ->
                    MediaThumbnail(
                        uri = video.uri,
                        type = MediaType.VIDEO,
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicaSection(hasPermission: Boolean, audios: List<MediaEntity>) {
    when {
        !hasPermission -> CenteredMessage("A aguardar permissão de acesso à música…")
        audios.isEmpty() -> CenteredMessage("A indexar música do dispositivo…", showSpinner = true)
        else -> Column(modifier = Modifier.fillMaxSize()) {
            SectionHeader(title = "Música", subtitle = "${audios.size} músicas — Todas as músicas")
            LazyColumn {
                items(audios, key = { it.uid }) { audio ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MediaThumbnail(
                            uri = audio.uri,
                            type = MediaType.AUDIO,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                audio.displayName,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                audio.artist ?: "Artista desconhecido",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun CenteredMessage(text: String, showSpinner: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showSpinner) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(text, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp)
    }
}
