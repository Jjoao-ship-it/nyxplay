package com.jay.nyxplay.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun MusicLibraryScreen(audios: List<MediaEntity>, onSongClick: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(audios, key = { _, a -> a.uid }) { index, audio ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(index) }
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
