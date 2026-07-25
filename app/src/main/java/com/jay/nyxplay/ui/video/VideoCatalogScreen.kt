package com.jay.nyxplay.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.ui.MediaThumbnail
import kotlin.math.abs

data class VideoCatalog(
    val name: String,
    val videos: List<MediaEntity>
) {
    val coverUri: String get() = videos.minByOrNull { it.dateAdded }?.uri ?: videos.first().uri
}

/**
 * Grid em cascata desalinhada, estilo "mesa de fotos espalhadas" —
 * cada cartão com leve rotação e altura variável, deterministicamente
 * derivadas do nome (mesma disposição sempre, sem recalcular ao
 * recompor).
 */
@Composable
fun VideoCatalogScreen(videos: List<MediaEntity>, onCatalogClick: (VideoCatalog) -> Unit) {
    val catalogs = remember(videos) {
        videos
            .groupBy { it.bucketName?.takeIf { name -> name.isNotBlank() } ?: "Outros vídeos" }
            .map { (name, list) -> VideoCatalog(name, list) }
            .sortedByDescending { it.videos.size }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
        verticalItemSpacing = 14.dp
    ) {
        items(catalogs, key = { it.name }) { catalog ->
            val seed = abs(catalog.name.hashCode())
            val tiltDegrees = ((seed % 9) - 4).toFloat() // -4..+4 graus
            val aspectVariance = 1.0f + (seed % 5) * 0.08f // 1.0..1.32

            Column(
                modifier = Modifier
                    .graphicsLayer { rotationZ = tiltDegrees }
                    .clickable { onCatalogClick(catalog) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / aspectVariance)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1A1A22))
                ) {
                    MediaThumbnail(
                        uri = catalog.coverUri,
                        type = MediaType.VIDEO,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f / aspectVariance)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("${catalog.videos.size} vídeos", color = Color.White, fontSize = 11.sp)
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
