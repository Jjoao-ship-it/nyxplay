package com.jay.nyxplay.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.ui.MediaThumbnail

@Composable
fun MusicBrowseScreen(
    title: String,
    audios: List<MediaEntity>,
    groupOf: (MediaEntity) -> String,
    onSongClick: (List<MediaEntity>, Int) -> Unit
) {
    val groups = remember(audios) {
        audios.groupBy { groupOf(it).takeIf { g -> g.isNotBlank() } ?: "Desconhecido" }
            .entries.sortedByDescending { it.value.size }
    }
    var selected by remember { mutableStateOf<String?>(null) }

    val selectedGroup = groups.firstOrNull { it.key == selected }
    if (selectedGroup != null) {
        SimpleSongListScreen(
            title = selectedGroup.key,
            songs = selectedGroup.value,
            onBack = { selected = null },
            onSongClick = { idx -> onSongClick(selectedGroup.value, idx) }
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(groups, key = { it.key }) { (name, songs) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = name }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val cover = songs.firstOrNull()
                    if (cover != null) {
                        MediaThumbnail(
                            uri = cover.uri, type = cover.type,
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF232330))
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(name, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(
                        "${songs.size}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
