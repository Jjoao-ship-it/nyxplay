package com.jay.nyxplay.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.ui.MediaThumbnail

@Composable
fun VideoGridScreen(videos: List<MediaEntity>, onVideoClick: (Int) -> Unit) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(videos, key = { _, v -> v.uid }) { index, video ->
            val scale by rememberGridItemScale(gridState, index)
            MediaThumbnail(
                uri = video.uri,
                type = MediaType.VIDEO,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1A22))
                    .clickable { onVideoClick(index) }
            )
        }
    }
}
