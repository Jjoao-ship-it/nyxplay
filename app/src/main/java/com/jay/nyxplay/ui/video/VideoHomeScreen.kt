package com.jay.nyxplay.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.nyxplay.data.HomeLayoutStyle
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.ui.MediaThumbnail
import com.jay.nyxplay.ui.music.SimpleSongListScreen
import com.jay.nyxplay.ui.settings.SettingsViewModel

private enum class QuickList { HISTORICO, RECENTES, MAIS_ASSISTIDOS }

@Composable
fun VideoHomeScreen(
    videos: List<MediaEntity>,
    onVideoClick: (List<MediaEntity>, Int) -> Unit,
    onShufflePlay: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    var openList by remember { mutableStateOf<QuickList?>(null) }
    val layoutStyle by settingsViewModel.homeLayoutStyle.collectAsState()

    val historico = remember(videos) { videos.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt } }
    val recentes = remember(videos) { videos.sortedByDescending { it.dateAdded } }
    val maisAssistidos = remember(videos) { videos.filter { it.playCount > 0 }.sortedByDescending { it.playCount } }
    val jaVistosUids = remember(historico) { historico.take(30).map { it.uid }.toSet() }
    val sugestoes = remember(videos, jaVistosUids) {
        videos.filterNot { it.uid in jaVistosUids }.shuffled().take(12)
    }
    val topAlbunsPorReproducoes = remember(videos) {
        videos.groupBy { it.bucketName?.takeIf { b -> b.isNotBlank() } ?: "Outros vídeos" }
            .map { (name, list) -> Triple(name, list.sumOf { it.playCount }, list.minByOrNull { it.dateAdded }?.uri) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(8)
    }

    when (openList) {
        QuickList.HISTORICO -> SimpleSongListScreen("Histórico", historico, { openList = null }) { idx -> onVideoClick(historico, idx) }
        QuickList.RECENTES -> SimpleSongListScreen("Última adicionada", recentes, { openList = null }) { idx -> onVideoClick(recentes, idx) }
        QuickList.MAIS_ASSISTIDOS -> SimpleSongListScreen("Mais assistidos", maisAssistidos, { openList = null }) { idx -> onVideoClick(maisAssistidos, idx) }
        null -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Text(
                    "Bem-vindo(a)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionCard("Histórico", Icons.Default.History, Modifier.weight(1f)) { openList = QuickList.HISTORICO }
                        QuickActionCard("Última adicionada", Icons.Default.LibraryAdd, Modifier.weight(1f)) { openList = QuickList.RECENTES }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionCard("Mais assistidos", Icons.Default.TrendingUp, Modifier.weight(1f)) { openList = QuickList.MAIS_ASSISTIDOS }
                        QuickActionCard("Aleatório", Icons.Default.Shuffle, Modifier.weight(1f), onShufflePlay)
                    }
                }
            }

            if (sugestoes.isNotEmpty()) {
                item { SectionLabel("Sugestões") }
                if (layoutStyle == HomeLayoutStyle.LISTA) {
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                            item { NovoMixTile("Novo mix\nde vídeo") { onVideoClick(sugestoes, 0) } }
                            items(sugestoes.drop(1), key = { it.uid }) { video ->
                                Column(
                                    modifier = Modifier.padding(end = 12.dp).clickable { onVideoClick(sugestoes, sugestoes.indexOf(video)) }
                                ) {
                                    MediaThumbnail(
                                        uri = video.uri, type = MediaType.VIDEO,
                                        modifier = Modifier.size(width = 100.dp, height = 140.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            sugestoes.chunked(3).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { video ->
                                        MediaThumbnail(
                                            uri = video.uri, type = MediaType.VIDEO,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onVideoClick(sugestoes, sugestoes.indexOf(video)) }
                                        )
                                    }
                                    repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            if (maisAssistidos.isNotEmpty()) {
                item { SectionLabel("Mais assistidos") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(maisAssistidos.take(10), key = { it.uid }) { video ->
                            Column(
                                modifier = Modifier.padding(end = 12.dp).clickable { onVideoClick(maisAssistidos, maisAssistidos.indexOf(video)) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MediaThumbnail(uri = video.uri, type = MediaType.VIDEO, modifier = Modifier.size(84.dp).clip(CircleShape))
                                Text(video.displayName, fontSize = 11.sp, maxLines = 1, modifier = Modifier.width(84.dp).padding(top = 4.dp))
                            }
                        }
                    }
                }
            }

            if (topAlbunsPorReproducoes.isNotEmpty()) {
                item { SectionLabel("Álbuns mais assistidos") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(topAlbunsPorReproducoes) { (name, _, coverUri) ->
                            Column(modifier = Modifier.padding(end = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (coverUri != null) {
                                    MediaThumbnail(uri = coverUri, type = MediaType.VIDEO, modifier = Modifier.size(72.dp).clip(CircleShape))
                                } else {
                                    Box(
                                        modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF2A2A38)),
                                        contentAlignment = Alignment.Center
                                    ) { Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) }
                                }
                                Text(name, fontSize = 11.sp, maxLines = 1, modifier = Modifier.width(72.dp).padding(top = 4.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun NovoMixTile(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(width = 130.dp, height = 150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF2A1E5C))))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun QuickActionCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A22))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}
