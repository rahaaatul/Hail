package com.aistra.hail.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HailBottomSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    hideHandleOnExpand: Boolean = false,
    fillMaxHeight: Boolean = false,
    partialExpandKey: Any? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded),
        )
    @Suppress("MagicNumber")
    LaunchedEffect(partialExpandKey) {
        if (partialExpandKey != null) {
            withTimeoutOrNull(500L) { snapshotFlow { sheetState.hasPartiallyExpandedState }.first { it } }
            if (sheetState.hasPartiallyExpandedState) sheetState.partialExpand()
        }
    }
    val handleAlpha by animateFloatAsState(targetValue = if (hideHandleOnExpand && sheetState.currentValue == SheetValue.Expanded) 0f else 1f, label = "handleAlpha")
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = if (fillMaxHeight) Modifier.fillMaxHeight() else Modifier,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = containerColor,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
        content = {
            HailDragHandle(modifier = Modifier.alpha(handleAlpha))
            content()
        },
    )
}

@Composable
private fun HailDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(top = dimensionResource(R.dimen.space250), bottom = 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = dimensionResource(R.dimen.space400), height = dimensionResource(R.dimen.space50))
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
    }
}
