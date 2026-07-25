package com.jay.nyxplay.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.data.PlaylistEntity
import com.jay.nyxplay.ui.MediaThumbnail

@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity,
    onBack: () -> Unit,
    onItemClick: (List<MediaEntity>, Int) -> Unit,
    viewModel: PlaylistViewModel = viewModel()
) {
    val media by viewModel.observeMediaForPlaylist(playlist.id).collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Text(playlist.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (media.isEmpty()) {
            Text(
                "Esta playlist está vazia. Adiciona itens a partir da biblioteca.",
                modifier = Modifier.padding(24.dp),
                fontSize = 14.sp
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(media, key = { _, m -> m.uid }) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(media, index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaThumbnail(
                        uri = item.uri,
                        type = playlist.type,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF232330))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(item.displayName, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = { viewModel.removeMediaFromPlaylist(playlist.id, item.uid) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remover da playlist")
                    }
                }
            }
        }
    }
}
