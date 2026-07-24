package com.jay.nyxplay.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.ui.MediaThumbnail

@Composable
fun MusicLibraryScreen(
    audios: List<MediaEntity>,
    onSongClick: (Int) -> Unit,
    onShufflePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var artistFilter by remember { mutableStateOf<String?>(null) }
    var albumFilter by remember { mutableStateOf<String?>(null) }

    val topArtists = remember(audios) {
        audios.groupBy { it.artist?.takeIf { a -> a.isNotBlank() } ?: "Desconhecido" }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .take(8)
    }

    val topAlbums = remember(audios) {
        audios.groupBy { it.album?.takeIf { a -> a.isNotBlank() } ?: "Desconhecido" }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .take(8)
    }

    val filtered = remember(audios, query, artistFilter, albumFilter) {
        audios.filter { a ->
            val matchesQuery = query.isBlank() ||
                a.displayName.contains(query, ignoreCase = true) ||
                (a.artist?.contains(query, ignoreCase = true) == true)
            val matchesArtist = artistFilter == null || a.artist == artistFilter ||
                (artistFilter == "Desconhecido" && a.artist.isNullOrBlank())
            val matchesAlbum = albumFilter == null || a.album == albumFilter ||
                (albumFilter == "Desconhecido" && a.album.isNullOrBlank())
            matchesQuery && matchesArtist && matchesAlbum
        }
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Pesquisar música ou artista…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(onClick = onShufflePlay),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Reproduzir aleatoriamente",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.padding(bottom = 8.dp))
        }

        if (artistFilter == null && albumFilter == null && query.isBlank()) {
            if (topArtists.isNotEmpty()) {
                item { SectionLabel("Top artistas") }
                item {
                    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
                        items(topArtists) { (artist, count) ->
                            ArtistCircle(
                                name = artist,
                                count = count,
                                onClick = { artistFilter = artist }
                            )
                        }
                    }
                }
            }

            if (topAlbums.isNotEmpty()) {
                item { SectionLabel("Top álbuns") }
                item {
                    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
                        items(topAlbums) { (album, count) ->
                            AlbumCard(
                                albumName = album,
                                count = count,
                                sampleUri = audios.firstOrNull { (it.album ?: "Desconhecido") == album }?.uri,
                                onClick = { albumFilter = album }
                            )
                        }
                    }
                }
            }

            item { SectionLabel("Todas as músicas") }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        artistFilter ?: albumFilter ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Limpar filtro",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            artistFilter = null
                            albumFilter = null
                        }
                    )
                }
            }
        }

        itemsIndexed(filtered, key = { _, a -> a.uid }) { _, audio ->
            val originalIndex = audios.indexOf(audio)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(originalIndex) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaThumbnail(
                    uri = audio.uri,
                    type = MediaType.AUDIO,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF232330))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        audio.displayName,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        audio.artist?.takeIf { it.isNotBlank() } ?: "Artista desconhecido",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun ArtistCircle(name: String, count: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(end = 14.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A38)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
        }
        Spacer(modifier = Modifier.padding(top = 4.dp))
        Text(
            name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.width(72.dp)
        )
        Text(
            "$count músicas",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun AlbumCard(albumName: String, count: Int, sampleUri: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(end = 14.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (sampleUri != null) {
            MediaThumbnail(
                uri = sampleUri,
                type = MediaType.AUDIO,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A2A38))
            )
        }
        Spacer(modifier = Modifier.padding(top = 4.dp))
        Text(
            albumName,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.width(96.dp)
        )
        Text(
            "$count músicas",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontSize = 10.sp
        )
    }
}
