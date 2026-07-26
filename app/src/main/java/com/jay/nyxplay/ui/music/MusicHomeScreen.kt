package com.jay.nyxplay.ui.music

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.ui.MediaThumbnail

private enum class QuickList { HISTORICO, RECENTES, MAIS_TOCADAS }

@Composable
fun MusicHomeScreen(
    audios: List<MediaEntity>,
    onSongClick: (List<MediaEntity>, Int) -> Unit,
    onShufflePlay: () -> Unit
) {
    var openList by remember { mutableStateOf<QuickList?>(null) }

    val historico = remember(audios) { audios.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt } }
    val recentes = remember(audios) { audios.sortedByDescending { it.dateAdded } }
    val maisTocadas = remember(audios) { audios.filter { it.playCount > 0 }.sortedByDescending { it.playCount } }
    val jaOuvidasUids = remember(historico) { historico.take(30).map { it.uid }.toSet() }
    val sugestoes = remember(audios, jaOuvidasUids) {
        audios.filterNot { it.uid in jaOuvidasUids }.shuffled().take(12)
    }
    val topArtistasPorReproducoes = remember(audios) {
        audios.groupBy { it.artist?.takeIf { a -> a.isNotBlank() } ?: "Desconhecido" }
            .mapValues { it.value.sumOf { a -> a.playCount } }
            .entries.filter { it.value > 0 }
            .sortedByDescending { it.value }
            .take(8)
    }

    when (openList) {
        QuickList.HISTORICO -> SimpleSongListScreen("Histórico", historico, { openList = null }) { idx -> onSongClick(historico, idx) }
        QuickList.RECENTES -> SimpleSongListScreen("Última adicionada", recentes, { openList = null }) { idx -> onSongClick(recentes, idx) }
        QuickList.MAIS_TOCADAS -> SimpleSongListScreen("Mais tocadas", maisTocadas, { openList = null }) { idx -> onSongClick(maisTocadas, idx) }
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
                        QuickActionCard("Mais tocadas", Icons.Default.TrendingUp, Modifier.weight(1f)) { openList = QuickList.MAIS_TOCADAS }
                        QuickActionCard("Aleatório", Icons.Default.Shuffle, Modifier.weight(1f), onShufflePlay)
                    }
                }
            }

            if (sugestoes.isNotEmpty()) {
                item { SectionLabel("Sugestões") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(sugestoes, key = { it.uid }) { song ->
                            Column(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable { onSongClick(sugestoes, sugestoes.indexOf(song)) }
                            ) {
                                MediaThumbnail(
                                    uri = song.uri, type = MediaType.AUDIO,
                                    modifier = Modifier.size(110.dp).clip(RoundedCornerShape(10.dp))
                                )
                                Text(song.displayName, fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(110.dp).padding(top = 4.dp))
                            }
                        }
                    }
                }
            }

            if (maisTocadas.isNotEmpty()) {
                item { SectionLabel("Mais tocadas") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(maisTocadas.take(10), key = { it.uid }) { song ->
                            Column(
                                modifier = Modifier.padding(end = 12.dp).clickable { onSongClick(maisTocadas, maisTocadas.indexOf(song)) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MediaThumbnail(
                                    uri = song.uri, type = MediaType.AUDIO,
                                    modifier = Modifier.size(84.dp).clip(CircleShape)
                                )
                                Text(song.displayName, fontSize = 11.sp, maxLines = 1, modifier = Modifier.width(84.dp).padding(top = 4.dp))
                            }
                        }
                    }
                }
            }

            if (topArtistasPorReproducoes.isNotEmpty()) {
                item { SectionLabel("Artistas mais ouvidos") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(topArtistasPorReproducoes) { (artist, _) ->
                            Column(
                                modifier = Modifier.padding(end = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF2A2A38)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                                }
                                Text(artist, fontSize = 11.sp, maxLines = 1, modifier = Modifier.width(72.dp).padding(top = 4.dp))
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
