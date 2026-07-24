package com.jay.nyxplay.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

data class VideoCatalog(
    val name: String,
    val videos: List<MediaEntity>
) {
    // Capa = vídeo mais antigo adicionado ao catálogo, por definição do utilizador
    val coverUri: String get() = videos.minByOrNull { it.dateAdded }?.uri ?: videos.first().uri
}

@Composable
fun VideoCatalogScreen(videos: List<MediaEntity>, onCatalogClick: (VideoCatalog) -> Unit) {
    val catalogs = remember(videos) {
        videos
            .groupBy { it.bucketName?.takeIf { name -> name.isNotBlank() } ?: "Outros vídeos" }
            .map { (name, list) -> VideoCatalog(name, list) }
            .sortedByDescending { it.videos.size }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(catalogs, key = { it.name }) { catalog ->
            Column(modifier = Modifier.clickable { onCatalogClick(catalog) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A22))
                ) {
                    MediaThumbnail(
                        uri = catalog.coverUri,
                        type = MediaType.VIDEO,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${catalog.videos.size} vídeos",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    catalog.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
