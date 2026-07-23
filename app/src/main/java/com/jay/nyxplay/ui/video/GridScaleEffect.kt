package com.jay.nyxplay.ui.video

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import kotlin.math.abs

/**
 * Escala de 1.0 (centro do viewport) a ~0.82 (bordas) — o efeito de
 * profundidade do grid estilo Vyom, sem depender de nenhum código
 * decompilado, só da posição real do item durante o scroll.
 */
@Composable
fun rememberGridItemScale(gridState: LazyGridState, itemIndex: Int): State<Float> {
    return remember(gridState, itemIndex) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == itemIndex }

            if (itemInfo == null) {
                0.85f
            } else {
                val itemCenter = itemInfo.offset.y + itemInfo.size.height / 2f
                val distance = abs(itemCenter - viewportCenter)
                val maxDistance = (info.viewportSize.height / 2f).coerceAtLeast(1f)
                val normalized = (distance / maxDistance).coerceIn(0f, 1f)
                1f - (normalized * 0.18f)
            }
        }
    }
}
