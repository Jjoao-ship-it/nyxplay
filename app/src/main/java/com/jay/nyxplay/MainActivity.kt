package com.jay.nyxplay

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
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
import com.jay.nyxplay.data.PlaylistEntity
import com.jay.nyxplay.ui.LibraryViewModel
import com.jay.nyxplay.ui.music.MusicPlayerScreen
import com.jay.nyxplay.ui.music.MusicPlayerViewModel
import com.jay.nyxplay.ui.playlists.PlaylistDetailScreen
import com.jay.nyxplay.ui.playlists.PlaylistManagerScreen
import com.jay.nyxplay.ui.settings.SettingsScreen
import com.jay.nyxplay.ui.video.VideoFeedScreen

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

/** Estado de navegação global — Settings e Playlists são partilhados
 * entre Vídeo e Música, por isso vivem acima das secções, não dentro. */
private sealed class Overlay {
    object None : Overlay()
    object Settings : Overlay()
    data class PlaylistManager(val type: MediaType) : Overlay()
    data class PlaylistDetail(val playlist: PlaylistEntity) : Overlay()
    data class VideoFeedFromPlaylist(val playlist: PlaylistEntity, val videos: List<MediaEntity>, val startIndex: Int) : Overlay()
}

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
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }

    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()

    val playerViewModel: MusicPlayerViewModel = viewModel()
    val playerUi by playerViewModel.state.collectAsState()
    var showFullPlayer by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val videoGranted = results[videoPermission] == true
        val audioGranted = results[audioPermission] == true
        hasVideoPermission = videoGranted
        hasAudioPermission = audioGranted
        if (videoGranted) viewModel.scanVideos()
        if (audioGranted) viewModel.scanAudio()
    }

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(arrayOf(videoPermission, audioPermission))
    }

    Scaffold(
        floatingActionButton = {
            if (overlay == Overlay.None && !showFullPlayer) {
                FloatingActionButton(
                    onClick = { section = if (section == NyxSection.VIDEOS) NyxSection.MUSICA else NyxSection.VIDEOS },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (section == NyxSection.VIDEOS) Icons.Default.MusicNote else Icons.Default.Movie,
                        contentDescription = "Trocar secção"
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val ov = overlay) {
                is Overlay.Settings -> SettingsScreen(
                    videos = videos,
                    onBack = { overlay = Overlay.None },
                    onOpenPlaylists = { overlay = Overlay.PlaylistManager(if (section == NyxSection.VIDEOS) MediaType.VIDEO else MediaType.AUDIO) }
                )
                is Overlay.PlaylistManager -> PlaylistManagerScreen(
                    type = ov.type,
                    onBack = { overlay = Overlay.Settings },
                    onOpenPlaylist = { playlist -> overlay = Overlay.PlaylistDetail(playlist) }
                )
                is Overlay.PlaylistDetail -> PlaylistDetailScreen(
                    playlist = ov.playlist,
                    onBack = { overlay = Overlay.PlaylistManager(ov.playlist.type) },
                    onItemClick = { media, index ->
                        if (ov.playlist.type == MediaType.AUDIO) {
                            playerViewModel.playQueue(media, index)
                            showFullPlayer = true
                            overlay = Overlay.None
                        } else {
                            overlay = Overlay.VideoFeedFromPlaylist(ov.playlist, media, index)
                        }
                    }
                )
                is Overlay.VideoFeedFromPlaylist -> VideoFeedScreen(
                    videos = ov.videos,
                    startIndex = ov.startIndex,
                    onBack = { overlay = Overlay.PlaylistDetail(ov.playlist) }
                )
                Overlay.None -> {
                    if (showFullPlayer && playerUi.isActive) {
                        MusicPlayerScreen(viewModel = playerViewModel, onBack = { showFullPlayer = false })
                    } else {
                        when (section) {
                            NyxSection.VIDEOS -> VideosSection(
                                hasPermission = hasVideoPermission,
                                videos = videos,
                                onSettingsClick = { overlay = Overlay.Settings }
                            )
                            NyxSection.MUSICA -> MusicaSection(
                                hasPermission = hasAudioPermission,
                                audios = audios,
                                playerViewModel = playerViewModel,
                                showFullPlayer = showFullPlayer,
                                onShowFullPlayerChange = { showFullPlayer = it },
                                onSettingsClick = { overlay = Overlay.Settings }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideosSection(
    hasPermission: Boolean,
    videos: List<MediaEntity>,
    onSettingsClick: () -> Unit
) {
    var selectedCatalog by remember { mutableStateOf<com.jay.nyxplay.ui.video.VideoCatalog?>(null) }
    var feedStartIndex by remember { mutableStateOf<Int?>(null) }

    when {
        !hasPermission -> CenteredMessage("A aguardar permissão de acesso aos vídeos…")
        videos.isEmpty() -> CenteredMessage("A indexar vídeos do dispositivo…", showSpinner = true)

        selectedCatalog != null && feedStartIndex != null -> com.jay.nyxplay.ui.video.VideoFeedScreen(
            videos = selectedCatalog!!.videos,
            startIndex = feedStartIndex!!,
            onBack = { feedStartIndex = null }
        )

        selectedCatalog != null -> com.jay.nyxplay.ui.video.Video3DGalleryScreen(
            catalog = selectedCatalog!!,
            onBack = { selectedCatalog = null },
            onOpenFeed = { index -> feedStartIndex = index }
        )

        else -> Column(modifier = Modifier.fillMaxSize()) {
            SectionHeader(title = "Vídeos", subtitle = "${videos.size} vídeos — catálogos por origem", onSettingsClick = onSettingsClick)
            com.jay.nyxplay.ui.video.VideoCatalogScreen(
                videos = videos,
                onCatalogClick = { catalog -> selectedCatalog = catalog }
            )
        }
    }
}

@Composable
private fun MusicaSection(
    hasPermission: Boolean,
    audios: List<MediaEntity>,
    playerViewModel: MusicPlayerViewModel,
    showFullPlayer: Boolean,
    onShowFullPlayerChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    val playerUi by playerViewModel.state.collectAsState()
    var showFullLibrary by remember { mutableStateOf(false) }

    when {
        !hasPermission -> CenteredMessage("A aguardar permissão de acesso à música…")
        audios.isEmpty() -> CenteredMessage("A indexar música do dispositivo…", showSpinner = true)
        else -> Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    TextButton(onClick = { showFullLibrary = false }) {
                        Text("Início", fontWeight = if (!showFullLibrary) FontWeight.Bold else FontWeight.Normal)
                    }
                    TextButton(onClick = { showFullLibrary = true }) {
                        Text("Música", fontWeight = if (showFullLibrary) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Configurações")
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (showFullLibrary) {
                    com.jay.nyxplay.ui.music.MusicLibraryScreen(
                        audios = audios,
                        onSongClick = { index ->
                            playerViewModel.playQueue(audios, index)
                            onShowFullPlayerChange(true)
                        },
                        onShufflePlay = {
                            playerViewModel.playQueue(audios.shuffled(), 0)
                            onShowFullPlayerChange(true)
                        }
                    )
                } else {
                    com.jay.nyxplay.ui.music.MusicHomeScreen(
                        audios = audios,
                        onSongClick = { queue, index ->
                            playerViewModel.playQueue(queue, index)
                            onShowFullPlayerChange(true)
                        },
                        onShufflePlay = {
                            playerViewModel.playQueue(audios.shuffled(), 0)
                            onShowFullPlayerChange(true)
                        }
                    )
                }
            }

            if (playerUi.isActive) {
                com.jay.nyxplay.ui.music.MiniPlayerBar(
                    ui = playerUi,
                    onOpenPlayer = { onShowFullPlayerChange(true) },
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::next
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Configurações")
        }
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
