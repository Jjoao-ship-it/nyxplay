package com.jay.nyxplay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.data.PlaylistEntity
import com.jay.nyxplay.ui.LibraryViewModel
import com.jay.nyxplay.ui.music.MusicBrowseScreen
import com.jay.nyxplay.ui.music.MusicPlayerScreen
import com.jay.nyxplay.ui.music.MusicPlayerViewModel
import com.jay.nyxplay.ui.music.SimpleSongListScreen
import com.jay.nyxplay.ui.playlists.PlaylistDetailScreen
import com.jay.nyxplay.ui.playlists.PlaylistManagerScreen
import com.jay.nyxplay.ui.settings.SettingsScreen
import com.jay.nyxplay.ui.splash.SplashScreen
import com.jay.nyxplay.ui.video.VideoFeedScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashHandler(this)
        setContent {
            val settingsViewModel: com.jay.nyxplay.ui.settings.SettingsViewModel = viewModel()
            val accentHex by settingsViewModel.accentColorHex.collectAsState()
            NyxPlayTheme(accentColor = Color(accentHex)) {
                var showSplash by remember { mutableStateOf(true) }
                var minTimeElapsed by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(1600)
                    minTimeElapsed = true
                }
                LaunchedEffect(minTimeElapsed) {
                    if (minTimeElapsed) showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    NyxPlayApp()
                }
            }
        }
    }
}

private fun installCrashHandler(context: android.content.Context) {
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            val text = "NyxPlay crash — ${java.util.Date()}\n\n" + android.util.Log.getStackTraceString(throwable)
            val resolver = context.contentResolver
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "nyxplay_crash.txt")
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream -> stream.write(text.toByteArray()) }
            }
        } catch (e: Exception) {
            // melhor esforço — não mascara o crash original
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }
}

@Composable
fun NyxPlayTheme(accentColor: Color = Color(0xFF7C5CFF), content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = accentColor,
        background = Color(0xFF0E0E12),
        surface = Color(0xFF16161C)
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private enum class NyxSection { VIDEOS, MUSICA }
private enum class VideoTab { INICIO, VIDEOS, ALBUNS }
private enum class AudioTab { INICIO, MUSICA, ALBUNS, ARTISTAS }

private sealed class Overlay {
    object None : Overlay()
    object Settings : Overlay()
    data class PlaylistManager(val type: MediaType) : Overlay()
    data class PlaylistDetail(val playlist: PlaylistEntity) : Overlay()
    data class VideoFeedFromPlaylist(val playlist: PlaylistEntity, val videos: List<MediaEntity>, val startIndex: Int) : Overlay()
}

private fun hasPermission(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private val videoPermission =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private val audioPermission =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun NyxPlayApp(viewModel: LibraryViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var section by remember { mutableStateOf(NyxSection.VIDEOS) }
    var hasVideoPermission by remember { mutableStateOf(hasPermission(context, videoPermission)) }
    var hasAudioPermission by remember { mutableStateOf(hasPermission(context, audioPermission)) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    val settingsViewModel: com.jay.nyxplay.ui.settings.SettingsViewModel = viewModel()

    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()

    val playerViewModel: MusicPlayerViewModel = viewModel()
    val playerUi by playerViewModel.state.collectAsState()
    var showFullPlayer by remember { mutableStateOf(false) }

    // Corrige o "re-scan a cada abertura": só pede/relança permissão se
    // ainda não estava concedida. Se já estava, os dados em cache do
    // Room aparecem de imediato e um scan silencioso corre uma vez.
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
        val missing = mutableListOf<String>()
        if (!hasVideoPermission) missing += videoPermission else viewModel.scanVideos()
        if (!hasAudioPermission) missing += audioPermission else viewModel.scanAudio()
        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        if (settingsViewModel.rememberLastSection.value) {
            section = if (settingsViewModel.readLastSection() == "MUSICA") NyxSection.MUSICA else NyxSection.VIDEOS
        }
    }
    LaunchedEffect(section) {
        settingsViewModel.setLastSection(if (section == NyxSection.MUSICA) "MUSICA" else "VIDEOS")
    }

    LaunchedEffect(audios) {
        if (audios.isNotEmpty()) playerViewModel.restoreLastQueueIfAny(audios)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val ov = overlay) {
            is Overlay.Settings -> SettingsScreen(
                videos = videos,
                contextType = if (section == NyxSection.VIDEOS) MediaType.VIDEO else MediaType.AUDIO,
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
                    val videoTabState = remember { mutableStateOf(VideoTab.INICIO) }
                    val audioTabState = remember { mutableStateOf(AudioTab.INICIO) }

                    Scaffold(
                        bottomBar = {
                            if (section == NyxSection.VIDEOS) {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = videoTabState.value == VideoTab.INICIO,
                                        onClick = { videoTabState.value = VideoTab.INICIO },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Início") }, label = null
                                    )
                                    NavigationBarItem(
                                        selected = videoTabState.value == VideoTab.VIDEOS,
                                        onClick = { videoTabState.value = VideoTab.VIDEOS },
                                        icon = { Icon(Icons.Default.Movie, contentDescription = "Vídeos") }, label = null
                                    )
                                    NavigationBarItem(
                                        selected = videoTabState.value == VideoTab.ALBUNS,
                                        onClick = { videoTabState.value = VideoTab.ALBUNS },
                                        icon = { Icon(Icons.Default.Folder, contentDescription = "Álbuns") }, label = null
                                    )
                                    NavigationBarItem(
                                        selected = false,
                                        onClick = { section = NyxSection.MUSICA },
                                        icon = { Icon(Icons.Default.MusicNote, contentDescription = "Música") }, label = null
                                    )
                                }
                            } else {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = audioTabState.value == AudioTab.INICIO,
                                        onClick = { audioTabState.value = AudioTab.INICIO },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Início") }, label = null
                                    )
                                    NavigationBarItem(
                                        selected = audioTabState.value == AudioTab.MUSICA,
                                        onClick = { audioTabState.value = AudioTab.MUSICA },
                                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Música") }, label = null
                                    )
                                    NavigationBarItem(
                                        selected = audioTabState.value == AudioTab.ALBUNS,
                                        onClick = { audioTabState.value = AudioTab.ALBUNS },
                                        icon = { Icon(Icons.Default.Folder, contentDescription = "Álbuns") }, label = null
                                    )
                                    NavigationBarItem(
                                        selected = audioTabState.value == AudioTab.ARTISTAS,
                                        onClick = { audioTabState.value = AudioTab.ARTISTAS },
                                        icon = { Icon(Icons.Default.Person, contentDescription = "Artistas") }, label = null
                                    )
                                    NavigationBarItem(
                                        selected = false,
                                        onClick = { section = NyxSection.VIDEOS },
                                        icon = { Icon(Icons.Default.Movie, contentDescription = "Vídeo") }, label = null
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                            when (section) {
                                NyxSection.VIDEOS -> VideosSection(
                                    hasPermission = hasVideoPermission,
                                    videos = videos,
                                    tab = videoTabState.value,
                                    onSettingsClick = { overlay = Overlay.Settings }
                                )
                                NyxSection.MUSICA -> MusicaSection(
                                    hasPermission = hasAudioPermission,
                                    audios = audios,
                                    tab = audioTabState.value,
                                    playerViewModel = playerViewModel,
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
}

@Composable
private fun VideosSection(
    hasPermission: Boolean,
    videos: List<MediaEntity>,
    tab: VideoTab,
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Configurações")
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    VideoTab.INICIO -> com.jay.nyxplay.ui.video.VideoHomeScreen(
                        videos = videos,
                        onVideoClick = { list, index ->
                            selectedCatalog = com.jay.nyxplay.ui.video.VideoCatalog("Sessão", list)
                            feedStartIndex = index
                        },
                        onShufflePlay = {
                            val shuffled = videos.shuffled()
                            selectedCatalog = com.jay.nyxplay.ui.video.VideoCatalog("Sessão", shuffled)
                            feedStartIndex = 0
                        }
                    )
                    VideoTab.VIDEOS -> SimpleSongListScreen(
                        title = "Vídeos",
                        songs = videos,
                        onBack = {},
                        onSongClick = { idx ->
                            selectedCatalog = com.jay.nyxplay.ui.video.VideoCatalog("Sessão", videos)
                            feedStartIndex = idx
                        }
                    )
                    VideoTab.ALBUNS -> com.jay.nyxplay.ui.video.VideoCatalogScreen(
                        videos = videos,
                        onCatalogClick = { catalog -> selectedCatalog = catalog }
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicaSection(
    hasPermission: Boolean,
    audios: List<MediaEntity>,
    tab: AudioTab,
    playerViewModel: MusicPlayerViewModel,
    onShowFullPlayerChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    val playerUi by playerViewModel.state.collectAsState()

    when {
        !hasPermission -> CenteredMessage("A aguardar permissão de acesso à música…")
        audios.isEmpty() -> CenteredMessage("A indexar música do dispositivo…", showSpinner = true)
        else -> Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Configurações")
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    AudioTab.INICIO -> com.jay.nyxplay.ui.music.MusicHomeScreen(
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
                    AudioTab.MUSICA -> com.jay.nyxplay.ui.music.MusicLibraryScreen(
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
                    AudioTab.ALBUNS -> MusicBrowseScreen(
                        title = "Álbuns",
                        audios = audios,
                        groupOf = { it.album ?: "" },
                        onSongClick = { queue, index ->
                            playerViewModel.playQueue(queue, index)
                            onShowFullPlayerChange(true)
                        }
                    )
                    AudioTab.ARTISTAS -> MusicBrowseScreen(
                        title = "Artistas",
                        audios = audios,
                        groupOf = { it.artist ?: "" },
                        onSongClick = { queue, index ->
                            playerViewModel.playQueue(queue, index)
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
