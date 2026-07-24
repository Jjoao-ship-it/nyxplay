package com.jay.nyxplay.ui.video

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

/**
 * Fração normalizada (-1 a +1) da distância do item ao centro do
 * viewport: 0 = centro, -1/+1 = a sair pela borda esquerda/direita.
 * Base matemática partilhada pelos modos Espiral e Cubo.
 */
@Composable
fun rememberItemOffsetFraction(listState: LazyListState, itemIndex: Int): State<Float> {
    return remember(listState, itemIndex) {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == itemIndex }

            if (itemInfo == null) {
                if (itemIndex < listState.firstVisibleItemIndex) -1f else 1f
            } else {
                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                val maxDistance = (info.viewportSize.width / 2f).coerceAtLeast(1f)
                ((itemCenter - viewportCenter) / maxDistance).coerceIn(-1f, 1f)
            }
        }
    }
}
